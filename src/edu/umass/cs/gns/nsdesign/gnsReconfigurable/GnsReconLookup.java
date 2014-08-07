package edu.umass.cs.gns.nsdesign.gnsReconfigurable;

import edu.umass.cs.gns.clientsupport.Defs;
import edu.umass.cs.gns.clientsupport.MetaDataTypeName;
import edu.umass.cs.gns.database.ColumnField;
import edu.umass.cs.gns.database.ColumnFieldType;
import edu.umass.cs.gns.exceptions.FailedDBOperationException;
import edu.umass.cs.gns.exceptions.FieldNotFoundException;
import edu.umass.cs.gns.exceptions.RecordNotFoundException;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nsdesign.Config;
import edu.umass.cs.gns.nsdesign.recordmap.NameRecord;
import edu.umass.cs.gns.nsdesign.clientsupport.NSAuthentication;
import edu.umass.cs.gns.nsdesign.packet.DNSPacket;
import edu.umass.cs.gns.nsdesign.packet.DNSRecordType;
import edu.umass.cs.gns.util.NSResponseCode;
import org.json.JSONException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

/**
 * This class executes lookupJSONArray requests sent by an LNS to an active replica. If name servers are replicated,
 * then methods in this class will be executed after the coordination among active replicas at name servers
 * is complete.
 *
 * Created by abhigyan on 2/27/14.
 */
public class GnsReconLookup {

  private static final ArrayList<ColumnField> dnsSystemFields = new ArrayList<ColumnField>();

  static {
    dnsSystemFields.add(NameRecord.ACTIVE_VERSION);
    dnsSystemFields.add(NameRecord.TIME_TO_LIVE);
  }

  /**
   *
   * @param dnsPacket
   * @param gnsApp
   * @param noCoordinatorState
   * @param recovery
   * @throws java.io.IOException
   * @throws org.json.JSONException
   * @throws java.security.InvalidKeyException
   * @throws java.security.spec.InvalidKeySpecException
   * @throws java.security.NoSuchAlgorithmException
   * @throws java.security.SignatureException
   * @throws edu.umass.cs.gns.exceptions.FailedDBOperationException
   */
  public static void executeLookupLocal(DNSPacket dnsPacket, GnsReconfigurable gnsApp,
          boolean noCoordinatorState, boolean recovery)
          throws IOException, JSONException, InvalidKeyException,
          InvalidKeySpecException, NoSuchAlgorithmException, SignatureException, FailedDBOperationException {

    if (Config.debuggingEnabled) {
      GNS.getLogger().info("Node " + gnsApp.getNodeID() + "; DNS Query Packet: " + dnsPacket.toString());
    }
    // if all replicas are coordinating on a read request, then check if this node should reply to client.
    // whether coordination is done or not, only the replica receiving client's request replies to the client.
    if (dnsPacket.getResponder() != -1 && // -1 means that we are not coordinating with other replicas upon reads.
            // so this replica should reply to client
            dnsPacket.getResponder() != gnsApp.getNodeID()) {
      return;
    }

    if (noCoordinatorState) {
      dnsPacket.getHeader().setResponseCode(NSResponseCode.ERROR_INVALID_ACTIVE_NAMESERVER);
      dnsPacket.getHeader().setQRCode(DNSRecordType.RESPONSE);
      if (!recovery) {
        gnsApp.getNioServer().sendToID(dnsPacket.getLnsId(), dnsPacket.toJSONObject());
      }
    } else {
      // First we do signature and ACL checks
      String guid = dnsPacket.getGuid();
      String field = dnsPacket.getKey();
      ArrayList<String> fields = dnsPacket.getKeys();
      String reader = dnsPacket.getAccessor();
      String signature = dnsPacket.getSignature();
      String message = dnsPacket.getMessage();
      // Check the signature and access
      NSResponseCode errorCode = NSResponseCode.NO_ERROR;

      // FIXME: ignore check for non-top-level fields
      if (reader != null) { // reader will be null for internal system reads
        if (field != null) {// single field check
          errorCode = NSAuthentication.signatureAndACLCheck(guid, field, reader, signature, message, MetaDataTypeName.READ_WHITELIST, gnsApp);
        } else { //multi field check - return an error if any field doesn't pass
          for (String key : fields) {
            NSResponseCode code;
            if ((code = NSAuthentication.signatureAndACLCheck(guid, key, reader, signature,
                    message, MetaDataTypeName.READ_WHITELIST, gnsApp)).isAnError()) {
              errorCode = code;
            }
          }
        }
      }
      // return an error packet if one of the checks doesn't pass
      if (errorCode.isAnError()) {
        dnsPacket.getHeader().setQRCode(DNSRecordType.RESPONSE);
        dnsPacket.getHeader().setResponseCode(errorCode);
        dnsPacket.setResponder(gnsApp.getNodeID());
        if (Config.debuggingEnabled) {
          GNS.getLogger().fine("Sending to " + dnsPacket.getLnsId() + " this error packet " + dnsPacket.toJSONObjectForErrorResponse());
        }
        if (!recovery) {
          gnsApp.getNioServer().sendToID(dnsPacket.getLnsId(), dnsPacket.toJSONObjectForErrorResponse());
        }
      } else {
        // All signature and ACL checks passed see if we can find the field to return;
        NameRecord nameRecord = null;
        // Try to look up the value in the database
        try {
          if (Defs.ALLFIELDS.equals(field)) {
            if (Config.debuggingEnabled) {
              GNS.getLogger().info("#### Field=" + field + " Format=" + dnsPacket.getReturnFormat());
            }
            // need everything so just grab all the fields
            nameRecord = NameRecord.getNameRecord(gnsApp.getDB(), guid);
          } else if (field != null) {
            if (Config.debuggingEnabled) {
              GNS.getLogger().info("#### Field=" + field + " Format=" + dnsPacket.getReturnFormat());
            }
            // otherwise grab a few system fields we need plus the field the user wanted
            nameRecord = NameRecord.getNameRecordMultiField(gnsApp.getDB(), guid, dnsSystemFields, dnsPacket.getReturnFormat(), field);
          } else { // multi-field lookup
            if (Config.debuggingEnabled) {
              GNS.getLogger().finer("#### Fields=" + fields + " Format=" + dnsPacket.getReturnFormat());
            }
            String[] fieldArray = new String[fields.size()];
            fieldArray = fields.toArray(fieldArray);
            // ograb a few system fields and the fields the user wanted
            nameRecord = NameRecord.getNameRecordMultiField(gnsApp.getDB(), guid, dnsSystemFields, dnsPacket.getReturnFormat(), fieldArray);
          }
        } catch (RecordNotFoundException e) {
          GNS.getLogger().fine("Record not found for name: " + guid + " Key = " + field);
        }
        if (Config.debuggingEnabled) {
          GNS.getLogger().fine("Name record read is: " + nameRecord);
        }
        // Now we either have a name record with stuff it in or a null one
        // Time to send something back to the client
        dnsPacket = checkAndMakeResponsePacket(dnsPacket, nameRecord, gnsApp);
        if (!recovery) {
          gnsApp.getNioServer().sendToID(dnsPacket.getLnsId(), dnsPacket.toJSONObject());
        }
      }
    }
  }

  /**
   * Handles the normal case of returning a valid record plus
   * a few different cases of the record not being found.
   *
   * @param dnsPacket
   * @param nameRecord
   * @return
   */
  private static DNSPacket checkAndMakeResponsePacket(DNSPacket dnsPacket, NameRecord nameRecord, GnsReconfigurable gnsApp) {
    dnsPacket.getHeader().setQRCode(DNSRecordType.RESPONSE);
    dnsPacket.setResponder(gnsApp.getNodeID());
    // change it to a response packet
    String guid = dnsPacket.getGuid();
    String key = dnsPacket.getKey();
    try {
      // Normative case... NameRecord was found and this server is one
      // of the active servers of the record
      if (nameRecord != null && nameRecord.getActiveVersion() != NameRecord.NULL_VALUE_ACTIVE_VERSION) {
        // does this check make sense? how can we find a nameRecord if the guid is null?
        if (guid != null) {
          //Generate the response packet
          // assume no error... change it below if there is an error
          dnsPacket.getHeader().setResponseCode(NSResponseCode.NO_ERROR);
          dnsPacket.setTTL(nameRecord.getTimeToLive());
          // Either returing one value or a bunch
          if (key != null && nameRecord.containsKey(key)) {
            // if it's a USER JSON access just return the entire map
            if (ColumnFieldType.USER_JSON.equals(dnsPacket.getReturnFormat())) {
              dnsPacket.setRecordValue(nameRecord.getValuesMap());
            } else {
              dnsPacket.setSingleReturnValue(nameRecord.getKey(key));
              if (Config.debuggingEnabled) {
                GNS.getLogger().fine("NS sending DNS lookup response: Name = " + guid + " Key = " + key + " Data = " + dnsPacket.getRecordValue());
              }
            }
          } else if (dnsPacket.getKeys() != null || Defs.ALLFIELDS.equals(key)) {
            dnsPacket.setRecordValue(nameRecord.getValuesMap());
            if (Config.debuggingEnabled) {
              GNS.getLogger().fine("NS sending multiple value DNS lookup response: Name = " + guid);
            }
            // or we don't actually have the field
          } else { // send error msg.
            if (Config.debuggingEnabled) {
              GNS.getLogger().info("Record doesn't contain field: " + key + " guid = " + guid + " record = " + nameRecord.toString());
            }
            dnsPacket.getHeader().setResponseCode(NSResponseCode.ERROR);
          }
          // For some reason the Guid of the packet is null
        } else { // send error msg.
          if (Config.debuggingEnabled) {
            GNS.getLogger().info("GUID of query is NULL!");
          }
          dnsPacket.getHeader().setResponseCode(NSResponseCode.ERROR);
        }
        // we're not the correct active name server so tell the client that
      } else { // send invalid error msg.
        dnsPacket.getHeader().setResponseCode(NSResponseCode.ERROR_INVALID_ACTIVE_NAMESERVER);
        if (nameRecord == null) {
          if (Config.debuggingEnabled) {
            GNS.getLogger().info("Invalid actives. Name = " + guid);
          }
        }
      }
    } catch (FieldNotFoundException e) {
      if (Config.debuggingEnabled) {
        GNS.getLogger().severe("Field not found exception: " + e.getMessage());
      }
      dnsPacket.getHeader().setResponseCode(NSResponseCode.ERROR);
    }
    return dnsPacket;

  }
}

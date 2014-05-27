package edu.umass.cs.gns.nsdesign.packet;

import edu.umass.cs.gns.nio.GNSNIOTransport;
import edu.umass.cs.gns.nsdesign.packet.Packet.PacketType;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Packet format sent from a client and handled by a local name server.
 * 
 */
public class CommandPacket extends BasicPacket {

  private final static String REQUESTID = "reqID";
  // later this might get done by the NIO send mechanism so
  // the field name might change
  private final static String SENDERADDRESS = GNSNIOTransport.DEFAULT_IP_FIELD;
  private final static String COMMAND = "command";

  /**
   * Identifier of the request.
   */
  private final int requestId;
  /**
   * The IP address of the sender as a string
   */
  private final String senderAddress;
  /**
   * The JSON form of the command. Always includes a COMMANDNAME field.
   * Almost always has a GUID field or NAME (for HRN records) field.
   */
  private final JSONObject command;
  

  /**
   *
   * @param requestId
   * @param command
   */
  public CommandPacket(int requestId, String senderAddress, JSONObject command) {
    this.setType(PacketType.COMMAND);
    this.requestId = requestId;
    this.senderAddress = senderAddress;
    this.command = command;
  }

  public CommandPacket(JSONObject json) throws JSONException {
    this.type = Packet.getPacketType(json);
    this.requestId = json.getInt(REQUESTID);
    this.senderAddress = json.getString(SENDERADDRESS);
    this.command = json.getJSONObject(COMMAND);
  }

  /**
   * Converts the command object into a JSONObject.
   *
   * @return
   * @throws org.json.JSONException
   */
  @Override
  public JSONObject toJSONObject() throws JSONException {
    JSONObject json = new JSONObject();
    Packet.putPacketType(json, getType());
    json.put(REQUESTID, this.requestId);
    json.put(COMMAND, this.command);
    json.put(SENDERADDRESS, this.senderAddress);
    return json;
  }

  public int getRequestId() {
    return requestId;
  }

  public String getSenderAddress() {
    return senderAddress;
  }

  public JSONObject getCommand() {
    return command;
  }
  
}

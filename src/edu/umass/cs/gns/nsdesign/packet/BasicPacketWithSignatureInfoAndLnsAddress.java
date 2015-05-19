/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gns.nsdesign.packet;

import java.net.InetSocketAddress;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides a packet with signature info and an LNS address field.
 *
 * @author westy
 */
public abstract class BasicPacketWithSignatureInfoAndLnsAddress extends BasicPacketWithCCPAddress implements PacketInterface, ExtensiblePacketInterface {

  public final static String ACCESSOR = "si_accessor";
  public final static String SIGNATURE = "si_signature";
  public final static String MESSAGE = "si_message";
  //
  private String accessor;
  private String signature;
  private String message;

  /**
   * Construct this guy with the address, but no signature info.
   *
   * @param lnsAddress
   */
  public BasicPacketWithSignatureInfoAndLnsAddress(InetSocketAddress lnsAddress) {
    this(lnsAddress, null, null, null);
  }

  /**
   * Construct this with the address and all the signature info.
   *
   * @param lnsAddress
   * @param accessor
   * @param signature
   * @param message
   */
  public BasicPacketWithSignatureInfoAndLnsAddress(InetSocketAddress lnsAddress, String accessor, String signature, String message) {
    super(lnsAddress);
    this.accessor = accessor;
    this.signature = signature;
    this.message = message;
  }

  /**
   * Construct this with all the address and signature info.
   *
   * @param address
   * @param port
   * @param accessor
   * @param signature
   * @param message
   */
  public BasicPacketWithSignatureInfoAndLnsAddress(String address, Integer port, String accessor, String signature, String message) {
    this(address != null && port != INVALID_PORT ? new InetSocketAddress(address, port) : null,
            accessor, signature, message);
  }

  @Override
  public void addToJSONObject(JSONObject json) throws JSONException {
    addToJSONObject(json, true);
  }

  public void addToJSONObject(JSONObject json, boolean includeSignatureSection) throws JSONException {
    super.addToJSONObject(json);
    if (includeSignatureSection) {
      if (accessor != null) {
        json.put(ACCESSOR, accessor);
      }
      if (signature != null) {
        json.put(SIGNATURE, signature);
      }
      if (message != null) {
        json.put(MESSAGE, message);
      }
    }
  }

  public String getAccessor() {
    return accessor;
  }

  public String getSignature() {
    return signature;
  }

  public String getMessage() {
    return message;
  }
}

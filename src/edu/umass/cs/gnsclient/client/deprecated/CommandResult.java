/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy, Emmanuel Cecchet
 *
 */
package edu.umass.cs.gnsclient.client.deprecated;

import edu.umass.cs.gnscommon.GNSResponseCode;
import edu.umass.cs.gnsserver.gnsapp.packet.CommandValueReturnPacket;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ActiveReplicaError;

import java.io.Serializable;

/**
 * Keeps track of the returned value from a command.
 * Also has some instrumentation for round trip times and what server responded.
 *
 * @author westy
 */
@Deprecated
public class CommandResult implements Serializable /* does it */ {

  private static final long serialVersionUID = 2326392043474125897L;

  /**
   * Set if the response is not an error.
   */
  private final String result;
  /**
   * Instrumentation - records the time the response is received back at the client
   */
  private final long receivedTime;
  /**
   * Indicates if the response is an error. Partially implemented.
   */
  private final GNSResponseCode errorCode;
  
  /* arun: Putting instrumentation fields like this in every packet is
   * poor and unnecessary.
   */
  
  /**
   * Instrumentation - The RTT as measured from the client out and back.
   */
  private final long clientLatency;
  /**
   * Instrumentation - The RTT as measured from the LNS out and back.
   */
//  private final long CCPRoundTripTime; // how long this query took from CPP out and back (set by CPP)
  /**
   * Instrumentation - Total command processing time at the LNS.
   */
//  private final long CCPProcessingTime; // how long this query took inside the LNS
  /**
   * Instrumentation - what nameserver responded to this query.
   */
//  private final String responder;
  /**
   * Instrumentation - the request counter from the LNS
   */
//  private final long requestCnt;
  /**
   * Instrumentation - the current requests per second from the LNS (can be used to tell how busy LNS is)
   */
//  private final int requestRate;

  public CommandResult(CommandValueReturnPacket packet, long receivedTime, long clientLatency) {
    this.result = packet.getReturnValue();
    this.receivedTime = receivedTime;
    this.errorCode = packet.getErrorCode();
//    this.CCPRoundTripTime = packet.getCPPRoundTripTime();
//    this.CCPProcessingTime = packet.getCPPProcessingTime();
//    this.responder = packet.getResponder();
//    this.requestCnt = packet.getRequestCnt();
//    this.requestRate = packet.getRequestRate();
    this.clientLatency = clientLatency;
  }

  // FIXME: Not sure what to do with these instrumentation fields
  public CommandResult(ActiveReplicaError packet, long receivedTime,
          long clientLatency) {
    this.result = packet.getResponseMessage();
    this.receivedTime = receivedTime;
    this.errorCode = GNSResponseCode.BAD_GUID_ERROR;
//    this.CCPRoundTripTime = 0;
//    this.CCPProcessingTime = 0;
//    this.responder = packet.getSender() != null ? packet.getSender().toString() : null;
//    this.requestCnt = 0;
//    this.requestRate = 0;
    this.clientLatency = clientLatency;
  }

  /**
   * Returns the result of the command as a string.
   *
   * @return
   */
  public String getResult() {
    return result;
  }

  /**
   * Instrumentation - holds the time when the return message is received.
   *
   * @return the time in milliseconds
   */
  public long getReceivedTime() {
    return receivedTime;
  }

  /**
   * Returns the error code if any returned by the execution of the command (could be null).'
   *
   * @return the code
   */
  public GNSResponseCode getErrorCode() {
    return errorCode;
  }

  /**
   * Instrumentation - The RTT as measured from the client out and back.
   *
   * @return the time in milliseconds
   */
  public long getClientLatency() {
    return clientLatency;
  }

  /**
   * Instrumentation - The RTT as measured from the CCP out and back.
   *
   * @return the time in milliseconds
   */
//  public long getCCPRoundTripTime() {
//    return CCPRoundTripTime;
//  }

  /**
   * Instrumentation - Returns the total command processing time at the LNS.
   *
   * @return
   */
//  public long getCCPProcessingTime() {
//    return CCPProcessingTime;
//  }

  /**
   * Instrumentation - what nameserver responded to this query (could be null for some non-query command)
   *
   * @return the id
   */
//  public String getResponder() {
//    return responder;
//  }

  /**
   * Instrumentation - the request counter from the LNS (can be used to tell how busy LNS is)
   *
   * @return the counter
   */
//  public long getRequestCnt() {
//    return requestCnt;
//  }

//  public int getRequestRate() {
//    return requestRate;
//  }

}

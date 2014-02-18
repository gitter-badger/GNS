/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.commands;

import static edu.umass.cs.gns.clientprotocol.Defs.*;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author westy
 */
public class SelectWithin extends GnsCommand {

  public SelectWithin(CommandModule module) {
    super(module);
  }

  @Override
  public String[] getCommandParameters() {
    return new String[]{FIELD, WITHIN};
  }

  @Override
  public String getCommandName() {
    return SELECT;
  }

  @Override
  public String execute(JSONObject json) throws JSONException {
    String field = json.getString(FIELD);
    String within = json.getString(WITHIN);
    return fieldAccess.selectWithin(field, within);
  }

  @Override
  public String getCommandDescription() {
    return "Key must be a GeoSpatial field. Returns all records whose fields are within value which is a bounding box specified. "
            + "Bounding box is a nested JSONArray string tuple of paired tuples: [[LONG_UL, LAT_UL],[LONG_BR, LAT_BR]]";
  }
}

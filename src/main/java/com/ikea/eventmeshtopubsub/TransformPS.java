package com.ikea.eventmeshtopubsub;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformPS {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TransformPS.class);
	
	public JSONObject splitMessage(JSONObject jsonmessage) {
		JSONObject message=new JSONObject();
		JSONObject service=jsonmessage.getJSONObject("orderManagement").getJSONObject("body");
		LOGGER.info("Service Lines"+service);
		if (service.has("subLineNo")) {
			message.put("subLineNo", service.getJSONObject("subLineNo"));
		} 
		return jsonmessage;
		
	}

}

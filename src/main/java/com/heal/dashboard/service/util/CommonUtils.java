package com.heal.dashboard.service.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.appnomic.appsone.model.JWTData;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringEscapeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.heal.dashboard.service.beans.AgentBean;
import com.heal.dashboard.service.beans.ControllerBean;
import com.heal.dashboard.service.beans.TagMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtils {

	public static String extractUserIdFromJWT(String jwtToken) {
		JWTData jwtData;
		try {
			String payload = jwtToken.split("\\.")[1];
			String body = new String(Base64.getDecoder().decode(payload.getBytes()));
			jwtData = (new Gson()).fromJson(body, JWTData.class);
		} catch (Exception e) {
			log.error("Invalid token supplied for username extraction. Details: ", e);
			return null;
		}

		return jwtData.getSub();
	}

	public static ObjectMapper getObjectMapperWithHtmlEncoder() {
		ObjectMapper objectMapper = new ObjectMapper();

		SimpleModule simpleModule = new SimpleModule("HTML-Encoder", objectMapper.version()).addDeserializer(String.class, new EscapeHTML());

		objectMapper.registerModule(simpleModule);

		return objectMapper;
	}
}

class EscapeHTML extends JsonDeserializer<String> {

	@Override
	public String deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		String s = jp.getValueAsString();
		return StringEscapeUtils.escapeHtml4(s);
	}
}




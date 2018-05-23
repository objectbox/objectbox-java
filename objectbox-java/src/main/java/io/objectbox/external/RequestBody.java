package io.objectbox.external;

import org.json.JSONObject;

/**
 * Class that wraps the parameter key and the object data that after is parsed to a {@link JSONObject} and 
 * 	sent to a HTTP endpoint
 * @author Juan Ramos - ptjuanramos
 *
 */
public class RequestBody {
	private String paramKey;
	private JSONObject body;
	
	public RequestBody(String paramKey, JSONObject parsedObject) {
		this.paramKey = paramKey;
		this.body = parsedObject;
	}

	public String getParamKey() {
		return paramKey;
	}

	public void setParamKey(String paramKey) {
		this.paramKey = paramKey;
	}

	public JSONObject getBody() {
		return body;
	}

	public void setBody(JSONObject body) {
		this.body = body;
	}
	
	public String toJsonString() {
		return this.toJsonObject().toString();
	}
	
	public JSONObject toJsonObject() {
		return new JSONObject(this);
	}
}

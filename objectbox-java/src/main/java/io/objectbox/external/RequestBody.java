package io.objectbox.external;

import org.json.JSONObject;

/**
 * TODO DOC
 * @author juan_
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

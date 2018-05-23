package io.objectbox.external;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonResponse {
	private boolean ok;
	private Object message;
	
	public JsonResponse(JsonResponseBuilder builder) {
		this.ok = builder.ok;
		this.message = builder.message;
	}
	
	//Default constructor
	public JsonResponse() {}
	
	public boolean getOk() {
		return this.ok;
	}
	
	public Object getMessage() {
		return this.message;
	}
	
	public JSONObject toJson() {
		JSONObject response;
		
		try {
			response = new JSONObject(this);
		} catch (JSONException e) {
			response = new JSONObject(new JsonResponse());
		}
		
		return response;
	}
	
	public static class JsonResponseBuilder {
		private boolean ok;
		private Object message;
		
		public JsonResponseBuilder isOk(boolean ok) {
			this.ok = ok;
			return this;
		}
		
		public JsonResponseBuilder setMessage(Object message) {
			this.message = message;
			return this;
		}
		
		public JsonResponse build() {
			return new JsonResponse(this);
		}
	}
}

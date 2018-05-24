package io.objectbox.external.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Bean class that is parsed to {@link JSONObject}.
 * <br>Template that is used to produce a response.
 * <br>This is instantiate by using the inner {@link JsonResponseBuilder} class
 * @author Juan Ramos - ptjuanramos
 */
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
	
	/**
	 * Parses this object to {@link JSONObject} that is used to persist 
	 * @return {@link JSONObject}
	 */
	public JSONObject toJson() {
		JSONObject response;
		
		try {
			response = new JSONObject(this);
		} catch (JSONException e) {
			response = new JSONObject(new JsonResponse());
		}
		
		return response;
	}
	
	/**
	 * Following the Builder design pattern this {@link JsonResponse} inner class builds
	 * 	a {@link JsonResponse} instance with all properties values
	 */
	public static class JsonResponseBuilder {
		private boolean ok;
		private Object message;
		
		/**
		 * Message that shows to the user if something went wrong with the HTTP request or not
		 * @return {@link JsonResponseBuilder} instance
		 */
		public JsonResponseBuilder isOk(boolean ok) {
			this.ok = ok;
			return this;
		}
		
		/**
		 * Sets a response message
		 * @return {@link JsonResponseBuilder} instance
		 */
		public JsonResponseBuilder setMessage(Object message) {
			this.message = message;
			return this;
		}
		
		public JsonResponse build() {
			return new JsonResponse(this);
		}
	}
}

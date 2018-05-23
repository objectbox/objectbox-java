package io.objectbox.external;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.sendgrid.Client;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;

import io.objectbox.Box;
import io.objectbox.exception.ExEndpointException;;

/**
 * 
 * @author Juan Ramos - ptjuanramos
 * @param <T>
 */
public class ExternalCalls<T> {
	private EndPoints endPoints;
	private Map<QueryType, String> endpoints;
	
	public ExternalCalls(EndPoints endPoints) {
		this.endPoints = endPoints;
		endpoints = endPoints.getEndPoints();
	}
	
	/**
	 * 
	 * @param query
	 * @param box
	 * @return
	 * @throws IOException 
	 * @throws ExEndpointNotFoundException 
	 */
	public JSONObject call(QueryType query, String paramKey, Object parsableObject, Method method) throws IOException, ExEndpointException {
		String topHierarchical = endPoints.getTopHierarchical();
		JSONObject parsedObject;
		
		try {
			parsedObject = MyJsonParser.parseToJson(parsableObject);
		} catch (JSONException e) {
			throw new ExEndpointException("Coudn't parsed the givin object");
		}
		
		RequestBody requestBody = null;
		
		if(!paramKey.isEmpty() || paramKey != null) {
			requestBody = new RequestBody(paramKey, parsedObject);
		}
		
		if(topHierarchical.isEmpty() || topHierarchical == null) {
			throw new ExEndpointException("Base URI not found");
		}
		
		String endpoint = endpoints.get(query);
		
		if(endpoint == null) {
			throw new ExEndpointException("Value not found by a givin key String");
		}
		
		Client client = new Client();
		Request request = new Request();
		
		if(requestBody != null) {
			request.setBody(requestBody.toJsonString());
		}
		
		request.setBaseUri(topHierarchical);
		request.setMethod(method);
		request.setEndpoint(endpoint);

		Response response = client.api(request);
		JSONObject jsonObject;
		JsonResponse jsonResponse;
		
		try {
			jsonObject = new JSONObject(response.getBody());
			jsonResponse = new JsonResponse.JsonResponseBuilder()
					.isOk(true)
					.setMessage(jsonObject)
					.build();
			
		} catch (JSONException e) {
			jsonResponse = new JsonResponse.JsonResponseBuilder()
					.isOk(false)
					.setMessage("Couldn't parse the JSON object from the giving endpoint")
					.build();
		}
		
		return jsonResponse.toJson();
	}
}

package io.objectbox.external.api;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.objectbox.exception.MyJsonParserException;

/**
 * 
 * @author Juan Ramos - ptjuanramos
 */
public class MyJsonParser {
	
	/**
	 * Parse a bean Class(class with getters and setters) to a {@link JSONObject}
	 * @param parsableObject serializable bean object
	 * @return {@link JSONObject}
	 * @throws JSONException if something went wrong when parsing to a {@link JSONObject}
	 */
	public static JSONObject parseToJson(Object parsableObject) throws JSONException{
		String className = parsableObject.getClass().getSimpleName();
		JSONObject objectParsed = new JSONObject();
		objectParsed.put(className, new JSONObject(parsableObject));
		
		return objectParsed;
	}
	
	/**
	 * The inverse process of {@link MyJsonParser#parseToJson(Object)}
	 * @param jsonObject data that is used to parse to Object
	 * @param classType this parameter is used for a successful object parse
	 * @return parsed object
	 * @throws MyJsonParserException if something when wrong in this method life cycle 
	 */
	public static Object getClassFromJson(JSONObject jsonObject, Class<?> classType) throws MyJsonParserException{
		JSONObject bodyJson;
		Object parsedObject = null;
		
		try {
			bodyJson = jsonObject.getJSONObject(classType.getSimpleName());
		} catch (JSONException e) {
			throw new MyJsonParserException("It seems that the JSON object doesn't exist..."); //really necessary?
		}
		
		Gson gson = new Gson();

		try {
			parsedObject = gson.fromJson(bodyJson.toString(), classType);
		} catch (JsonSyntaxException e) {
			throw new MyJsonParserException("The giving object class wasnt found....");
		}
		
		if(parsedObject == null) {
			throw new MyJsonParserException("Some error occured when parsing and as result the parsed object is null");
		}
		
		return parsedObject;
	}
}

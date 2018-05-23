package io.objectbox.external;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
	 * TODO DOC
	 * @param parsableObject
	 * @return
	 * @throws JSONException
	 */
	public static JSONObject parseToJson(Object parsableObject) throws JSONException{
		String className = parsableObject.getClass().getSimpleName();
		JSONObject objectParsed = new JSONObject();
		objectParsed.put(className, new JSONObject(parsableObject));
		
		return objectParsed;
	}
	
	/**
	 * 
	 * @param jsonObject
	 * @return
	 */
	public static Object getClassFromJson(JSONObject jsonObject, Class classType) throws MyJsonParserException{
		JSONObject bodyJson;
		Object parsedObject = null;
		
		try {
			bodyJson = jsonObject.getJSONObject(classType.getSimpleName());
		} catch (JSONException e) {
			throw new MyJsonParserException("It seems that the JSON object doesn't exist..."); //really necessary?
		}
		
		Gson gson = new Gson();
		Iterator<String> keys = bodyJson.keys();

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

package io.objectbox;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.json.JSONObject;
import org.junit.Test;

import com.sendgrid.Method;

import io.objectbox.exception.ExEndpointException;
import io.objectbox.exception.MyJsonParserException;
import io.objectbox.external.EndPoints;
import io.objectbox.external.ExternalCalls;
import io.objectbox.external.MyJsonParser;
import io.objectbox.external.QueryType;
import io.objectbox.index.model.MyObjectBox;

public class TestExternalObjectBox extends AbstractObjectBoxTest{
	
	@Test
	public void callTest() {
		String topHierarchical = "jsonplaceholder.typicode.com/posts/1";
		Map<QueryType, String> endpointsMap = new HashMap();
		endpointsMap.put(QueryType.PUBLISH, "");
		
		EndPoints endPoints = new EndPoints.EndPointsBuilder()
				.enable(true)
				.main(topHierarchical)
				.setEndpoints(endpointsMap)
				.build();
		
		final ExternalCalls<TestingDogClass> externalCalls = new ExternalCalls<>(endPoints);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					JSONObject jsonObject = externalCalls.call(QueryType.PUBLISH, "save", new TestingDogClass("Nome", "lavrador"), Method.GET);
					
					while(jsonObject == null);
					
					//printing all JSON content
					System.out.println(jsonObject.toString());
					System.out.println("is ok message: " + jsonObject.get("ok"));
					System.out.println("response content: " + jsonObject.get("message"));
					
				} catch (IOException | ExEndpointException e) {
					System.out.println("Something went wrong with this test");
				}
			}
		}).run();
		
	}
	
	@Test
	public void testingMyJsonParser() {
		System.out.println("*** testingMyJsonParser ****");
		
		//parsing the class to json
		JSONObject parsedClass = MyJsonParser.parseToJson(new TestingDogClass("Nome", "lavrador"));
		System.out.println("JSON object: " + parsedClass.toString());
		
		//parsing json to class
		try {
			TestingDogClass object = (TestingDogClass) MyJsonParser.getClassFromJson(parsedClass, TestingDogClass.class);
			System.out.println("JSON OBJECT OF THE PARSED OBJECT " + MyJsonParser.parseToJson(object));
			
			System.out.println("Name of the dog:" + object.getName());
			System.out.println("Race of the dog:" + object.getRace());
			
		} catch (MyJsonParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

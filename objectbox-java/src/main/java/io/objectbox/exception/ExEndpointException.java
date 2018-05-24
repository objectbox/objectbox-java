package io.objectbox.exception;

import java.util.Map;

/**
 * Exception class that is throw when a endpoint URI is not found by a giving key String, in the endpoints {@link Map}
 * @author Juan Ramos - ptjuanramos
 */
public class ExEndpointException extends Exception{
	private static final long serialVersionUID = 1994781887245358080L;

	public ExEndpointException(String message) {
		super(message);
	}

}

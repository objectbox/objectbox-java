package io.objectbox.exception;

import java.util.Map;

import io.objectbox.external.MyJsonParser;

/**
 * Exception class that is throw when some error occur in {@link MyJsonParser} class
 * @author Juan Ramos - ptjuanramos
 */
public class MyJsonParserException extends Exception {
	private static final long serialVersionUID = 2418235083591442031L;

	public MyJsonParserException(String message) {
		super(message);
	}
}

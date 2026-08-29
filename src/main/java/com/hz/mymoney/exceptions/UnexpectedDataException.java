package com.hz.mymoney.exceptions;

public class UnexpectedDataException extends RuntimeException {
	public UnexpectedDataException(String message) {
		super(message);
	}
	public UnexpectedDataException(String message, Throwable cause) { super(message, cause); }
}

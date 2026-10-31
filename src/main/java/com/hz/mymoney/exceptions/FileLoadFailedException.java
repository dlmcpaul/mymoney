package com.hz.mymoney.exceptions;

public class FileLoadFailedException extends RuntimeException {
	public FileLoadFailedException(String message) {
		super(message);
	}
	public FileLoadFailedException(String message, Throwable cause) { super(message, cause); }
}

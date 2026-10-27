package com.hz.mymoney.exceptions;

public class FileSaveFailedException extends RuntimeException {
	public FileSaveFailedException(String message) {
		super(message);
	}
	public FileSaveFailedException(String message, Throwable cause) { super(message, cause); }
}

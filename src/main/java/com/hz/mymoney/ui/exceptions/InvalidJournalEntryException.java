package com.hz.mymoney.ui.exceptions;

public class InvalidJournalEntryException extends RuntimeException {
	public InvalidJournalEntryException(String message) {
		super(message);
	}
	public InvalidJournalEntryException(String message, Throwable cause) { super(message, cause); }
}

package com.hz.mymoney.data.utilities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Dates {
	private static final String DATE_FORMAT_1 = "yyyy/MM/dd";
	private static final String DATE_FORMAT_2 = "yyyy-MM-dd";
	private static final String QUICKEN_DATE_FORMAT = "d/M/yy";

	private Dates() {}

	public static LocalDate parseDate(String dateString) {
		try {
			if (dateString.contains("/")) {
				return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(DATE_FORMAT_1));
			}
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(DATE_FORMAT_2));
		} catch (DateTimeParseException e) {
			throw new RuntimeException("Could not parse date " + dateString + " as either " + DATE_FORMAT_1 + " or " + DATE_FORMAT_2, e);
		}
	}

	public static LocalDate parseQuickenDate(String dateString) {
		return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(QUICKEN_DATE_FORMAT));
	}

}

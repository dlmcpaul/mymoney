package com.hz.mymoney.data.utilities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Dates {
	private static final String DATE_FORMAT_1 = "yyyy/MM/dd";
	private static final String DATE_FORMAT_2 = "yyyy-MM-dd";
	private static final String QUICKEN_DATE_FORMAT = "d/M/yy";

	private static final DateTimeFormatter DATE_FORMATTER_1 = DateTimeFormatter.ofPattern(DATE_FORMAT_1);
	private static final DateTimeFormatter DATE_FORMATTER_2 = DateTimeFormatter.ofPattern(DATE_FORMAT_2);

	private Dates() {}

	private static LocalDate fastParseDate(String date) {
		String year = date.substring(0, 4);
		String month = date.substring(5, 7);
		String day = date.substring(8, 10);

		return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
	}

	public static LocalDate parseDate(String dateString) {
		try {
			if (dateString.length() == 10 && (dateString.charAt(4) == '-' || dateString.charAt(4) == '/') && (dateString.charAt(7) == '-' || dateString.charAt(7) == '/')) {
				return fastParseDate(dateString);
			}

			if (dateString.contains("/")) {
				return LocalDate.parse(dateString, DATE_FORMATTER_1);
			}
			return LocalDate.parse(dateString, DATE_FORMATTER_2);
		} catch (DateTimeParseException e) {
			throw new RuntimeException("Could not parse date " + dateString + " as either " + DATE_FORMAT_1 + " or " + DATE_FORMAT_2, e);
		}
	}

	public static LocalDate parseQuickenDate(String dateString) {
		return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(QUICKEN_DATE_FORMAT));
	}

}

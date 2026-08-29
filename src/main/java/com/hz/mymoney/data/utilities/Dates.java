package com.hz.mymoney.data.utilities;

import com.hz.mymoney.exceptions.UnexpectedDataException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Dates {
	private static final String DATE_FORMAT_1 = "yyyy/MM/dd";
	private static final String DATE_FORMAT_2 = "yyyy-MM-dd";
	private static final String QUICKEN_DATE_FORMAT = "d/M/yy";
	private static final char ZERO = '0';

	private static final DateTimeFormatter DATE_FORMATTER_1 = DateTimeFormatter.ofPattern(DATE_FORMAT_1);
	private static final DateTimeFormatter DATE_FORMATTER_2 = DateTimeFormatter.ofPattern(DATE_FORMAT_2);

	private Dates() {}

	private static int fastParseInt2(String value) {
		return (value.charAt(0) - ZERO) * 10
				+ (value.charAt(1) - ZERO);
	}

	private static int fastParseInt4(String value) {
		return (value.charAt(0) - ZERO) * 1000
				+ (value.charAt(1) - ZERO) * 100
				+ (value.charAt(2) - ZERO) * 10
				+ (value.charAt(3) - ZERO);
	}

	private static LocalDate fastParseDate(String date) {
		int year = fastParseInt4(date.substring(0, 4));
		int month = fastParseInt2(date.substring(5, 7));
		int day = fastParseInt2(date.substring(8, 10));

		return LocalDate.of(year, month, day);
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
			throw new UnexpectedDataException("Could not parse date " + dateString + " as either " + DATE_FORMAT_1 + " or " + DATE_FORMAT_2, e);
		}
	}

	public static LocalDate parseQuickenDate(String dateString) {
		return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(QUICKEN_DATE_FORMAT));
	}

}

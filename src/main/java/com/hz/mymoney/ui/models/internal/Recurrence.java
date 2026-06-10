package com.hz.mymoney.ui.models.internal;

import java.util.List;

public class Recurrence {
	private Recurrence() {}

	private static final List<Alias> plural = List.of(
			new Alias("d", "Days"),
			new Alias("w", "Weeks"),
			new Alias("m", "Months"),
			new Alias("y", "Years"));

	private static final List<Alias> singlular = List.of(
			new Alias("d", "Day"),
			new Alias("w", "Week"),
			new Alias("m", "Month"),
			new Alias("y", "Year"));

	public static String makeRecurrence(int count, String code) {
		if (count == 1) {
			return singlular.stream().filter(alias -> alias.code().equals(code)).findFirst().orElseThrow().value();
		}
		return count + " " + plural.stream().filter(alias -> alias.code().equals(code)).findFirst().orElseThrow().value();
	}

}

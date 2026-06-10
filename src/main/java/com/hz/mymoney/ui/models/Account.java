package com.hz.mymoney.ui.models;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record Account(String name, String fullName, String category, BigDecimal balance, boolean isShares, String note) {

	public List<String> tokenise(String string) {
		return Arrays.stream(string.split(":")).toList();
	}

	public List<String> breadcrumb(String accountFilter) {
		return tokenise(fullName.substring(accountFilter.length()));
	}

	public boolean hasNote() {
		return note != null && note.length() > 0;
	}

	private String matchIcon(String categoryType) {
		return switch (categoryType) {
			case "Cash" -> "dollar sign";
			case "Credit" -> "credit card";
			case "Imputation" -> "money check";
			case "Investment" -> "chartline";
			case "Property" -> "home";
			case "Superannuation" -> "piggy bank";
			case "Fund" -> "hand holding usd";
			case "Mortgage" -> "landmark";
			default -> "question circle";
		};
	}

	public String icon() {
		List<String> breakdown = tokenise(this.fullName);
		return switch (breakdown.size()) {
			case 3 -> matchIcon(breakdown.get(1));
			case 4 -> matchIcon(breakdown.get(2));
			default -> "";
		};
	}
}

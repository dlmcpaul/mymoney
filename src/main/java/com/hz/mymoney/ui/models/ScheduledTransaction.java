package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.support.Recurrence;

import java.time.LocalDate;
import java.util.List;

public record ScheduledTransaction(
		String description,
		boolean isIncoming,
		LocalDate dueDate,
		Money amount,
		int recurrenceAmount,
		String recurrenceType,
		List<Journal> journals) implements Comparable<ScheduledTransaction> {

	public boolean isDueToday() {
		return dueDate.isEqual(LocalDate.now());
	}

	public boolean isDueTomorrow() {
		return dueDate.isEqual(LocalDate.now().plusDays(1));
	}

	public boolean isDueSoon() {
		return dueDate.isEqual(LocalDate.now().plusDays(2));
	}

	public boolean isInPast() {
		return dueDate.isBefore(LocalDate.now());
	}

	public boolean isNotYetDue() {
		return dueDate.isAfter(LocalDate.now().plusDays(2));
	}

	public String recurrenceDescription() {
		return "Every " + Recurrence.makeRecurrence(recurrenceAmount, recurrenceType);
	}

	private int oneYear() {
		return switch (recurrenceType) {
			case "d" -> 365;
			case "w" -> 52;
			case "m" -> 12;
			default -> 1;
		};
	}

	public Money totalForYear() {
		int count = oneYear() / recurrenceAmount;
		return amount.multiply(Money.valueOf(count));
	}

	@Override
	public int compareTo(ScheduledTransaction o) {
		return this.dueDate.compareTo(o.dueDate);
	}
}

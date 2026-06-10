package com.hz.mymoney.data.models.internal;

import com.hz.mymoney.data.models.ledger.LedgerEntry;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static java.time.temporal.ChronoUnit.*;

@AllArgsConstructor
@Log4j2
public class Schedule implements Comparable<Schedule> {
	public final String recurrence;
	public final LedgerEntry ledgerEntry;

	public void rollForward() {
		long recurrenceAmount = recurrenceAmount();

		switch (recurrenceType()) {
			case "d" -> ledgerEntry.changeDateBy(recurrenceAmount, DAYS);
			case "w" -> ledgerEntry.changeDateBy(recurrenceAmount, WEEKS);
			case "m" -> ledgerEntry.changeDateBy(recurrenceAmount, MONTHS);
			case "y" -> ledgerEntry.changeDateBy(recurrenceAmount, YEARS);
			default -> log.error("Unimplemented recurrence {}", recurrence);
		}
	}

	public String recurrenceType() {
		return this.recurrence.substring(recurrence.length() - 1);
	}

	public int recurrenceAmount() {
		return Integer.parseInt(this.recurrence.substring(0, this.recurrence.length() - 1));
	}

	public boolean isValid() {
		return (this.ledgerEntry.isBalanced() && this.recurrenceAmount() > 0);
	}

	@Override
	public int hashCode() {
		return this.ledgerEntry.getDescription().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Schedule schedule) {
			return schedule.ledgerEntry.getDescription().equals(this.ledgerEntry.getDescription());
		}
		return false;
	}

	@Override
	public int compareTo(Schedule o) {
		return this.ledgerEntry.getDate().compareTo(o.ledgerEntry.getDate());
	}
}

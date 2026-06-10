package com.hz.mymoney.data.models.internal;

import com.hz.mymoney.configuration.AccountConstants;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.hz.mymoney.configuration.AccountConstants.SUPER_CONTRIBUTION_NOTE;

/**
 * @param split Stock Split occurred
 */
@Log4j2
public record Movement(LocalDate date, String sourceAccount, String description, BigDecimal amount, BigDecimal price,
                       String code, boolean split, BigDecimal commission, String note) implements Comparable<Movement> {

	public BigDecimal getValue() {
		return price.multiply(amount);
	}

	public String getNote() {
		if (note == null) {
			return description;
		}
		return note;
	}

	@Override
	public String code() {
		if (code.isEmpty()) {
			// Guessing game
			if (sourceAccount.toLowerCase().startsWith(AccountConstants.FUNDS.toLowerCase())) {
				return sourceAccount.substring(sourceAccount.lastIndexOf(":") + 1);
			}

			// 5 character codes
			if (description.matches(".*[A-Z]{5}$")) {
				return description.substring(description.length() - 5);
			}
			if (description.matches("^[A-Z]{5}.*")) {
				return description.substring(0, 5);
			}

			// 4 character codes
			if (description.matches(".*[A-Z]{4}$")) {
				return description.substring(description.length() - 4);
			}
			if (description.matches("^[A-Z]{4}.*")) {
				return description.substring(0, 4);
			}

			// 3 character codes
			if (description.matches(".*[A-Z]{3}$")) {
				return description.substring(description.length() - 3);
			}
			if (description.matches("^[A-Z]{3}.*")) {
				return description.substring(0, 3);
			}

			log.warn("Could not find code in {} using {}", sourceAccount, description);
		}
		return code;
	}

	public BigDecimal getValue(BigDecimal newPrice) {
		return newPrice.multiply(amount);
	}

	public boolean isBetween(LocalDate startDate, LocalDate endDate) {
		return date.isAfter(startDate.minusDays(1)) && date.isBefore(endDate.plusDays(1));
	}

	public boolean isSameDate(LocalDate endDate) {
		return date.isEqual(endDate);
	}

	public boolean isAfter(LocalDate endDate) {
		return date.isAfter(endDate);
	}

	public boolean isBeforeOrEqual(LocalDate endDate) {
		return date.isBefore(endDate) || date.isEqual(endDate);
	}

	public boolean isSuperContribution() {
		return note != null && note.equalsIgnoreCase(SUPER_CONTRIBUTION_NOTE);
	}

	public boolean isSuperOpeningBalance() {
		return sourceAccount.toLowerCase().startsWith(AccountConstants.SUPER_OPENING_BALANCE.toLowerCase());
	}

	// Should be a transfer in from another super account
	public boolean isSuperTransferIn() {
		return sourceAccount.toLowerCase().startsWith(AccountConstants.SUPER_ACCOUNT.toLowerCase())
				&& amount.compareTo(BigDecimal.ZERO) > 0;
	}

	public boolean isSuperEarnings() {
		return sourceAccount.equalsIgnoreCase(AccountConstants.SUPER_RETURNS);
	}

	public boolean isSuperLosses() {
		return sourceAccount.equalsIgnoreCase(AccountConstants.SUPER_LOSSES);
	}

	public boolean isSuperTaxes() {
		return sourceAccount.equalsIgnoreCase(AccountConstants.SUPER_TAXES);
	}

	public boolean isSuperFees() {
		return sourceAccount.equalsIgnoreCase(AccountConstants.SUPER_FEES);
	}

	public boolean isSuperInsurance() {
		return sourceAccount.equalsIgnoreCase(AccountConstants.SUPER_INSURANCE);
	}

	public boolean isSuperTransferOut() {
		return sourceAccount.toLowerCase().startsWith(AccountConstants.SUPER_ACCOUNT.toLowerCase())
				&& amount.compareTo(BigDecimal.ZERO) < 0;
	}

	@Override
	public int compareTo(Movement o) {
		return this.date.compareTo(o.date);
	}
}

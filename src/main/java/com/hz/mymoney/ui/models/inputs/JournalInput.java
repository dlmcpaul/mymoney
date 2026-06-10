package com.hz.mymoney.ui.models.inputs;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class JournalInput implements JournalInputValidation {
	LocalDate journalDate;
	List<String> accounts;
	List<BigDecimal> amounts;

	@Override
	public boolean isValid() {
		return journalDate != null && accounts != null && amounts != null
				&& amounts.size() <= accounts.size() && amounts.size() > 0
				&& noNulls(accounts);
	}
}

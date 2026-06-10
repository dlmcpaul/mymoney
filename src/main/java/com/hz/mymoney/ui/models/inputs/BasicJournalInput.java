package com.hz.mymoney.ui.models.inputs;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BasicJournalInput extends JournalInput implements JournalInputValidation {
	String description;

	@Override
	public boolean isValid() {
		return super.isValid()
				&& description!= null && description.isEmpty() == false
				&& this.accounts.size() >= 2
				&& (this.amounts.getFirst() != null || this.amounts.getLast() != null);
	}
}

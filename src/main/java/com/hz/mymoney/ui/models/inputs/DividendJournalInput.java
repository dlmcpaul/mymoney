package com.hz.mymoney.ui.models.inputs;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DividendJournalInput extends JournalInput implements JournalInputValidation {

	@Override
	public boolean isValid() {
		return super.isValid()
				&& this.accounts.size() == 3 && this.amounts.size() == 3
				&& noNulls(this.amounts)
				&& noNulls(this.accounts);
	}
}

package com.hz.mymoney.ui.models.inputs;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DistributionTypeInput extends JournalInput implements JournalInputValidation {
	String distributionType;

	@Override
	public boolean isValid() {
		return super.isValid()
				&& noNulls(this.accounts)
				&& noNulls(this.amounts)
				&& this.accounts.size() == 2 && this.amounts.size() == 1
				&& distributionType != null && distributionType.isEmpty() == false
				&& (distributionType.equals("capital-gain")
					|| distributionType.equals("distribution")
					|| distributionType.equals("capital-return"));
	}
}

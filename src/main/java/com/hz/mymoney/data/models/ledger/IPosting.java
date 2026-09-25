package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

public interface IPosting {

	boolean isSameAccount(String account);
	boolean isCommissionPosting();
	boolean isIncomePosting();

	String postLine();
	String getAccount();
	Money getAmount();
	Money getPrice();
	String getCode();
	Money getValue();
	boolean isSplit();
	String getNote();

	void setAmount(Money amount);
}

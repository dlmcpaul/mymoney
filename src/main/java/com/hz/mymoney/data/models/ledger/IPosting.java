package com.hz.mymoney.data.models.ledger;

import java.math.BigDecimal;

public interface IPosting {

	boolean isSameAccount(String account);
	boolean isCommissionPosting();

	String postLine();
	String getAccount();
	BigDecimal getAmount();
	BigDecimal getPrice();
	String getCode();
	BigDecimal getValue();
	boolean isSplit();
	String getNote();

	void setAmount(BigDecimal amount);
}

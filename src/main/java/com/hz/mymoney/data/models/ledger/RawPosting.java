package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

public record RawPosting(String line) implements IPosting {

	@Override
	public boolean isSameAccount(String account) {
		return false;
	}

	@Override
	public boolean isCommissionPosting() {
		return false;
	}

	@Override
	public boolean isIncomePosting() {
		return false;
	}

	@Override
	public String postLine() {
		return line;
	}

	@Override
	public String getAccount() {
		return "";
	}

	@Override
	public Money getAmount() {
		return null;
	}

	@Override
	public Money getPrice() {
		return null;
	}

	@Override
	public String getCode() {
		return "";
	}

	@Override
	public Money getValue() {
		return null;
	}

	@Override
	public boolean isSplit() {
		return false;
	}

	@Override
	public String getNote() {
		return "";
	}

	@Override
	public void setAmount(Money amount) {

	}
}

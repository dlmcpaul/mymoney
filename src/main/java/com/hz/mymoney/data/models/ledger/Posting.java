package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.hz.mymoney.configuration.AccountConstants.COMMISSION;
import static com.hz.mymoney.configuration.AccountConstants.INCOME_PREFIX;

public class Posting implements IPosting {
	protected final String account;
	protected final String note;
	protected Money amount;

	public Posting(String account) {
		this.account = account;
		this.amount = Money.ZERO;
		this.note = null;
	}

	public Posting(String account, BigDecimal amount) {
		this.account = account;
		this.amount = new Money(amount);
		this.note = null;
	}

	public Posting(String account, BigDecimal amount, String note) {
		this.account = account;
		this.amount = new Money(amount);
		this.note = note;
	}

	public Posting(String account, Money amount) {
		this.account = account;
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
		this.note = null;
	}

	public Posting(String account, Money amount, String note) {
		this.account = account;
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
		this.note = note;
	}

	public boolean isSameAccount(String account) {
		return this.account.equals(account);
	}

	public String postLine() {
		return "  "
				+ account
				+ (amount != null ? "  " + amount : "")
				+ (note != null ? "  ;" + note : "");
	}

	@Override
	public String getAccount() {
		return account;
	}

	@Override
	public Money getAmount() {
		return amount;
	}

	@Override
	public Money getPrice() {
		return Money.ONE;
	}

	@Override
	public String getCode() {
		return "";
	}

	@Override
	public Money getValue() {
		return amount;
	}

	@Override
	public boolean isSplit() {
		return false;
	}

	@Override
	public String getNote() {
		return note;
	}

	@Override
	public void setAmount(Money amount) {
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public String toString() {
		return postLine();
	}

	@Override
	public boolean isCommissionPosting() {
		return this.account.equalsIgnoreCase(COMMISSION);
	}

	@Override
	public boolean isIncomePosting() {
		return this.account.toLowerCase().startsWith(INCOME_PREFIX.toLowerCase());
	}
}

package com.hz.mymoney.data.models.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.hz.mymoney.configuration.AccountConstants.COMMISSION;

public class Posting implements IPosting {
	protected final String account;
	protected final String note;
	protected BigDecimal amount;

	public Posting(String account) {
		this.account = account;
		this.amount = BigDecimal.ZERO;
		this.note = null;
	}

	public Posting(String account, BigDecimal amount) {
		this.account = account;
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
		this.note = null;
	}

	public Posting(String account, BigDecimal amount, String note) {
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
				+ (amount != null ? "  $" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "")
				+ (note != null ? "  ;" + note : "");
	}

	@Override
	public String getAccount() {
		return account;
	}

	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	@Override
	public BigDecimal getPrice() {
		return BigDecimal.ONE;
	}

	@Override
	public String getCode() {
		return "";
	}

	@Override
	public BigDecimal getValue() {
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
	public void setAmount(BigDecimal amount) {
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
}

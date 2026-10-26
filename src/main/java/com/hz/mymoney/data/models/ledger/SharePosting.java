package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

import java.math.RoundingMode;

public class SharePosting extends FundPosting implements IPosting {
	protected final Money price;
	protected final boolean split;      // Stock Split occurred

	public SharePosting(String account, Money amount, Money price, String code, String note) {
		super(account, amount, code, note);
		this.price = price;
		this.split = false;
	}

	public SharePosting(String account, Money amount, Money price, String code, boolean split, String note) {
		super(account, amount, code, note);
		this.price = price;
		this.split = split;
	}

	private String splitPosting() {
		return split ? "  =" : "";
	}

	private boolean showPrice() {
		return (price != null && price.compareTo(Money.ZERO) != 0 && split == false);
	}

	@Override
	public String postLine() {
		return "  "
				+ this.account
				+ splitPosting()
				+ (this.amount != null ? "  " + this.amount.stripTrailingZeros().toPlainString() : "")
				+ (getCode() != null ? "  " + getCode() : "")
				+ (showPrice() ? " @ " + this.price : "")
				+ (this.note != null ? "  ;" + this.note : "");
	}

	@Override
	public Money getValue() {
		return price.multiply(amount).setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public Money getPrice() {
		return price;
	}

	@Override
	public boolean isSplit() {
		return split;
	}

	@Override
	public String toString() {
		return postLine();
	}
}

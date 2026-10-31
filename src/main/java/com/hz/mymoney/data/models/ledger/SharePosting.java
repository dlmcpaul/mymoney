package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

import java.math.RoundingMode;

public class SharePosting extends FundPosting implements IPosting {
	protected final Money price;
	protected final Money totalCost;
	protected final boolean split;      // Stock Split occurred

	public SharePosting(String account, Money amount, Money price, String code, String note) {
		super(account, amount, code, note);
		this.price = price;
		this.totalCost = null;
		this.split = false;
	}

	public SharePosting(String account, Money amount, Money price, String code, boolean split, String note) {
		super(account, amount, code, note);
		this.price = price;
		this.totalCost = null;
		this.split = split;
	}

	public SharePosting(String account, Money amount, String code, Money totalCost, String note) {
		super(account, amount, code, note);
		this.price = totalCost.divide(amount, RoundingMode.HALF_EVEN);
		this.totalCost = totalCost;
		this.split = false;
	}

	private String splitPosting() {
		return split ? "  =" : "";
	}

	private boolean showPrice() {
		return (totalCost == null && price != null && price.compareTo(Money.ZERO) != 0 && split == false);
	}

	private boolean showTotalCost() {
		return (totalCost != null && split == false);
	}

	@Override
	public String postLine() {
		return "  "
				+ this.account
				+ splitPosting()
				+ (this.amount != null ? "  " + this.amount.stripTrailingZeros().toPlainString() : "")
				+ (getCode() != null ? "  " + getCode() : "")
				+ (showPrice() ? " @ " + this.price : "")
				+ (showTotalCost() ? " @@ " + this.totalCost : "")
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

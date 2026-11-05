package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SharePosting extends FundPosting implements IPosting {
	protected final BigDecimal totalShares;
	protected final Money price;
	protected final Money totalCost;
	protected final boolean split;      // Stock Split occurred

	public SharePosting(String account, BigDecimal totalShares, Money price, String code, String note) {
		super(account, Money.ZERO, code, note);
		this.price = price;
		this.totalShares = totalShares;
		this.totalCost = null;
		this.split = false;
	}

	public SharePosting(String account, BigDecimal totalShares, Money price, String code, boolean split, String note) {
		super(account, Money.ZERO, code, note);
		this.price = price;
		this.totalShares = totalShares;
		this.totalCost = null;
		this.split = split;
	}

	public SharePosting(String account, BigDecimal totalShares, String code, Money totalCost, String note) {
		super(account, Money.ZERO, code, note);

		BigDecimal tc = totalCost.amount().setScale(6, RoundingMode.HALF_UP);
		this.price = new Money(tc.divide(totalShares, RoundingMode.HALF_UP), totalCost.currency(), totalCost.currencySymbol());
		this.totalShares = totalShares;
		this.totalCost = totalCost;
		this.split = false;
	}

	private String splitPosting() {
		return split ? "  =" : "";
	}

	private boolean showPrice() {
		return (totalCost == null && price != null && price.isNonZero() && split == false);
	}

	private boolean showTotalCost() {
		return (totalCost != null && split == false);
	}

	@Override
	public String postLine() {
		return "  "
				+ this.account
				+ splitPosting()
				+ (totalShares != null ? "  " + totalShares.stripTrailingZeros().toPlainString() : "")
				+ (getCode() != null ? "  " + getCode() : "")
				+ (showPrice() ? " @ " + this.price : "")
				+ (showTotalCost() ? " @@ " + this.totalCost : "")
				+ (this.note != null ? "  ;" + this.note : "");
	}

	@Override
	public Money getAmount() {
		return new Money(this.totalShares, "", "");
	}

	@Override
	public Money getValue() {
		if (totalCost == null) {
			return price.multiply(totalShares).setScale(2, RoundingMode.HALF_UP);
		}
		return totalCost;
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

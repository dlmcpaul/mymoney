package com.hz.mymoney.data.models.ledger;

import java.math.BigDecimal;

public class SharePosting extends Posting implements IPosting {
	protected final BigDecimal price;
	protected final String code;
	protected final boolean split;      // Stock Split occurred

	public SharePosting(String account, BigDecimal amount, BigDecimal price, String code, String note) {
		super(account, amount, note);
		this.price = price;
		this.code = code;
		this.split = false;
	}

	public SharePosting(String account, BigDecimal amount, BigDecimal price, String code, boolean split, String note) {
		super(account, amount, note);
		this.price = price;
		this.code = code;
		this.split = split;
	}

	private String splitPosting() {
		return split ? "  =" : "";
	}

	private boolean showPrice() {
		return (price != null && price.compareTo(BigDecimal.ZERO) != 0 && split == false);
	}

	@Override
	public String postLine() {
		return "  "
				+ this.account
				+ splitPosting()
				+ (this.amount != null ? "  " + this.amount.stripTrailingZeros().toPlainString() : "")
				+ (this.code != null ? "  " + this.code : "")
				+ (showPrice() ? " @ $" + this.price.stripTrailingZeros().toPlainString() : "")
				+ (this.note != null ? "  ;" + this.note : "");
	}

	@Override
	public BigDecimal getValue() {
		return price.multiply(amount);
	}

	@Override
	public BigDecimal getPrice() {
		return price;
	}

	@Override
	public String getCode() {
		return code;
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

package com.hz.mymoney.data.models.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FundPosting extends Posting implements IPosting {
	private final String code;

	public FundPosting(String account, BigDecimal amount, String note) {
		super(account, amount, note);
		this.code = account.substring(account.lastIndexOf(":") + 1);
	}

	public FundPosting(String account, BigDecimal amount) {
		super(account, amount, null);
		this.code = account.substring(account.lastIndexOf(":") + 1);
	}

	@Override
	public String postLine() {
		return "  "
				+ account
				+ (amount != null ? "  $" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "")
				+ (note != null ? "  ;" + note : "");
	}

	@Override
	public String toString() {
		return postLine();
	}

	@Override
	public String getCode() {
		return code;
	}
}

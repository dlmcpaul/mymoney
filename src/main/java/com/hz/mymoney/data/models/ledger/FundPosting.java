package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;

public class FundPosting extends Posting implements IPosting {
	private final String code;

	public FundPosting(String account, Money amount, String note) {
		super(account, amount, note);
		this.code = account.substring(account.lastIndexOf(":") + 1);
	}

	public FundPosting(String account, Money amount) {
		super(account, amount, null);
		this.code = account.substring(account.lastIndexOf(":") + 1);
	}

	@Override
	public String postLine() {
		return "  "
				+ account
				+ (amount != null ? "  " + amount : "")
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

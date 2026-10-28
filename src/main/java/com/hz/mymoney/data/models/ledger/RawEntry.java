package com.hz.mymoney.data.models.ledger;

public final record RawEntry(String line) implements Entry {
	@Override
	public String toString() {
		return line;
	}
}

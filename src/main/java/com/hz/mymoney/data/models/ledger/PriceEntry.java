package com.hz.mymoney.data.models.ledger;

import org.jetbrains.annotations.NotNull;

public record PriceEntry(String line) implements Entry {
	@Override
	public @NotNull String toString() {
		return line;
	}
}

package com.hz.mymoney.data.models.ledger;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Ledger {
	private int loadErrorCount = 0;
	private List<String> preamble = new ArrayList<>();
	private List<LedgerEntry> ledgerEntries = new ArrayList<>();

	private void addAll(List<LedgerEntry> ledgerEntries) {
		this.ledgerEntries.addAll(ledgerEntries);
	}

	private void add(LocalDate date, String description, BigDecimal amount, BigDecimal price, String code, String to, String from) {
		SharePosting fromAccount = new SharePosting(from, amount, price, code, null);
		Posting toAccount = new Posting(to, amount.multiply(BigDecimal.valueOf(-1)));
		LedgerEntry ledgerEntry = new LedgerEntry(date, description, List.of(toAccount, fromAccount));
		this.add(ledgerEntry);
	}

	private void addUnique(LedgerEntry ledgerEntry) {
		if (this.hasLedgerEntry(ledgerEntry) == false) {
			this.add(ledgerEntry);
		}
	}

	public void add(LedgerEntry ledgerEntry) {
		ledgerEntries.add(ledgerEntry);
	}

	public void add(LocalDate date, String description, List<IPosting> postings) {
		this.add(new LedgerEntry(date, description, postings));
	}

	public void add(LocalDate date, String description, BigDecimal amount, String to, String from) {
		Posting fromAccount = new Posting(from, amount);
		Posting toAccount = new Posting(to, amount.multiply(BigDecimal.valueOf(-1)));
		this.add(date, description, List.of(toAccount, fromAccount));
	}

	public boolean hasLedgerEntry(LedgerEntry ledgerEntry) {
		return ledgerEntries.stream().anyMatch(t -> t.isEquivalent(ledgerEntry));
	}

	public void addPreamble(String preamble) {
		this.preamble.add(preamble);
	}

	public void addErrorCount() {
		this.loadErrorCount++;
	}

	public boolean isReadOnly() {
		return loadErrorCount != 0;
	}
}

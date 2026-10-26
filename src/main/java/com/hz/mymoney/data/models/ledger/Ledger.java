package com.hz.mymoney.data.models.ledger;

import com.hz.mymoney.data.models.Money;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Ledger {
	private int loadErrorCount = 0;
	private List<Entry> entries = new ArrayList<>();

	private void addAll(List<LedgerEntry> ledgerEntries) {
		this.entries.addAll(ledgerEntries);
	}

	private void add(LocalDate date, String description, Money amount, Money price, String code, String to, String from) {
		SharePosting fromAccount = new SharePosting(from, amount, price, code, null);
		Posting toAccount = new Posting(to, amount.negate());
		LedgerEntry ledgerEntry = new LedgerEntry(date, description, List.of(toAccount, fromAccount));
		this.add(ledgerEntry);
	}

	private void addUnique(LedgerEntry ledgerEntry) {
		if (this.hasLedgerEntry(ledgerEntry) == false) {
			this.add(ledgerEntry);
		}
	}

	public void add(Entry entry) {
		entries.add(entry);
	}

	public void add(LocalDate date, String description, List<IPosting> postings) {
		this.add(new LedgerEntry(date, description, postings));
	}

	public void add(String line) {
		this.add(new RawEntry(line));
	}

	public void add(LocalDate date, String description, Money amount, String to, String from) {
		Posting fromAccount = new Posting(from, amount);
		Posting toAccount = new Posting(to, amount.negate());
		this.add(date, description, List.of(toAccount, fromAccount));
	}

	public List<LedgerEntry> getOnlyLedgerEntries() {
		return entries.stream()
				.filter(entry -> entry instanceof LedgerEntry)
				.map(entry -> (LedgerEntry) entry)
				.toList();
	}

	public boolean hasLedgerEntry(LedgerEntry ledgerEntry) {
		return entries.stream()
				.filter(entry -> entry instanceof LedgerEntry)
				.anyMatch(t -> ((LedgerEntry)t).isEquivalent(ledgerEntry));
	}

	public void addErrorCount() {
		this.loadErrorCount++;
	}

	public boolean isReadOnly() {
		return loadErrorCount != 0;
	}
}

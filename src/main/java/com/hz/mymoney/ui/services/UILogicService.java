package com.hz.mymoney.ui.services;

import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.services.LedgerServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class UILogicService {
	private final LedgerServices dataLoaderService;

	public record JournalResult(String description, String debitAccount, String creditAccount) {}

	public List<JournalResult> smartPreFillExact(String description) {
		final Ledger ledger = dataLoaderService.getLedger();
		final LocalDate lastYear = LocalDate.now().minusYears(1);

		return ledger.getLedgerEntries().stream()
				.filter(ledgerEntry -> ledgerEntry.getDate().isAfter(lastYear))
				.filter(ledgerEntry -> ledgerEntry.getDescription().equalsIgnoreCase(description))
				.map(ledgerEntry -> new JournalResult(ledgerEntry.getDescription(), ledgerEntry.getFirstNegativePosting().getAccount(), ledgerEntry.getFirstPositivePosting().getAccount()))
				.distinct()
				.toList();
	}

	public List<JournalResult> smartPreFill(String description) {
		final Ledger ledger = dataLoaderService.getLedger();
		final LocalDate lastYear = LocalDate.now().minusYears(1);

		return ledger.getLedgerEntries().stream()
				.filter(ledgerEntry -> ledgerEntry.getDate().isAfter(lastYear))
				.filter(ledgerEntry -> ledgerEntry.getDescription().length() >= description.length())
				.filter(ledgerEntry -> ledgerEntry.getDescription().substring(0, description.length()).equalsIgnoreCase(description))
				.map(ledgerEntry -> new JournalResult(ledgerEntry.getDescription(), ledgerEntry.getFirstNegativePosting().getAccount(), ledgerEntry.getFirstPositivePosting().getAccount()))
				.distinct()
				.toList();
	}

	public String smartShortcuts(String value) {
		if (value.length() == 1 || value.length() == 2) {
			return switch (value.toLowerCase()) {
				case "a" -> "Assets:";
				case "l" -> "Liabilities:";
				case "i" -> "Income:";
				case "ex" -> "Expenses:";
				case "eq" -> "Equity:";
				default -> value;
			};
		}

		return value;
	}

	private List<String> tokenize(String line) {
		return Arrays.stream(line.split(":"))
				.map(String::trim)
				.filter(s -> s.isEmpty() == false)
				.toList();
	}

	// Extract the middle section of an account of format x:y:z
	// Alternatively extract the last section of an account of format x:y
	private String extractAccountType(String accountName) {
		List<String> tokens = tokenize(accountName);

		return switch (tokens.size()) {
			case 2 -> tokens.getLast();
			case 3 -> tokens.get(1) + ":";
			default -> "";
		};
	}

	public String smartLookahead(String value, List<String> accounts) {
		List<String> grouped = accounts.stream()
				.map(this::extractAccountType)
				.filter(s -> s.length() > 0)
				.distinct()
				.toList();
		if (grouped.size() != 1) {
			return value;
		}
		return value.substring(0, value.lastIndexOf(":") + 1) + grouped.getFirst();
	}

}

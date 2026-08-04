package com.hz.mymoney.data.utilities;

import com.hz.mymoney.data.models.ledger.*;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class LedgerParser {

	private enum LedgerEntryState {
		COMMENT,
		EMPTY,
		UNKNOWN,
		ENTRY_START,
		CASH_POSTING,
		FUND_POSTING,
		SHARE_POSTING,
		SHARE_RESET_POSTING,
		REMAINDER_POSTING
	}

	public Ledger loadLedger(InputStream inputStream) {
		Ledger ledger = new Ledger();

		try (InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			try (BufferedReader br = new BufferedReader(isr)) {
				final String lastLine = readMetaData(ledger, br);
				if (lastLine != null) {
					readLedgerEntries(ledger, br, lastLine);
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

		return ledger;
	}

	public Ledger loadLedger(Path path) {
		Ledger ledger = new Ledger();

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			final String lastLine = readMetaData(ledger, br);
			if (lastLine != null) {
				readLedgerEntries(ledger, br, lastLine);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return ledger;
	}

	private String readMetaData(Ledger ledger, BufferedReader br) throws IOException {
		String line = br.readLine();
		while (line != null && isLedgerEntryStart(line) == false) {
			ledger.addMetaData(line);
			line = br.readLine();
		}
		return line;
	}

	private void readLedgerEntries(Ledger ledger, BufferedReader br, String lastLine) throws IOException {
		// lastLine should be the start of a ledgerEntry
		String line = lastLine;
		LedgerEntry ledgerEntry = null;
		while (line != null) {
			switch (scanLine(line)) {
				case ENTRY_START -> {
					addLedgerEntry(ledger, ledgerEntry);
					ledgerEntry = newLedgerEntry(line);
				}
				case EMPTY -> {}
				case CASH_POSTING -> {
					assert ledgerEntry != null;
					addPosting(ledgerEntry, parseCashPosting(line));
				}
				case FUND_POSTING -> {
					assert ledgerEntry != null;
					addPosting(ledgerEntry, parseFundPosting(line));
				}
				case SHARE_POSTING -> {
					assert ledgerEntry != null;
					addPosting(ledgerEntry, parseSharePosting(line));
				}
				case SHARE_RESET_POSTING -> {
					assert ledgerEntry != null;
					addPosting(ledgerEntry, parseShareResetPosting(line));
				}
				case REMAINDER_POSTING -> {
					assert ledgerEntry != null;
					addPosting(ledgerEntry, parseRemainderPosting(line, ledgerEntry.getRemainingBalance()));
				}
				default -> {
					ledger.addErrorCount();
					log.error("Unsupported entry '{}'", line);
				}
			}
			line = br.readLine();
		}
		addLedgerEntry(ledger, ledgerEntry);
	}

	private void addPosting(LedgerEntry ledgerEntry, IPosting posting) {
		ledgerEntry.getPostings().add(posting);
	}

	private void addLedgerEntry(Ledger ledger, LedgerEntry ledgerEntry) {
		if (ledgerEntry != null) {
			if (ledgerEntry.isBalanced()) {
				ledger.getLedgerEntries().add(ledgerEntry);
			} else {
				ledger.addErrorCount();
				log.error("Ledger Entry is not balanced {}", ledgerEntry);
			}
		}
	}

	private LedgerEntry newLedgerEntry(String line) {
		LocalDate date = Dates.parseDate(line.substring(0, 10)); // First 10 chars is date
		String remaining = line.substring(10).trim();
		String status = null;
		String description;
		String note = null;

		if (remaining.startsWith("*") || remaining.startsWith("!")) {
			status = remaining.substring(0, 1);
			remaining = remaining.substring(1);
		}

		if (remaining.contains(";")) {
			description = remaining.substring(0, remaining.indexOf(";")).trim();
			note = remaining.substring(remaining.indexOf(";") + 1).trim();
		} else {
			description = remaining.trim();
		}

		return new LedgerEntry(date, status, description, note, new ArrayList<>());
	}

	private IPosting parseCashPosting(String line) {
		// Account  Amount [Currency]
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).getLast().trim();

		if (amount.isEmpty()) {
			log.warn("For cash line '{}' amount needs to be calculated", line);
		}

		return new Posting(account, Money.parseMoney(amount, 2), getNote(line));
	}

	private IPosting parseRemainderPosting(String line, BigDecimal remainder) {
		String account = tokenize(line).getFirst().trim();

		return new Posting(account, remainder, getNote(line));
	}

	private IPosting parseSharePosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount;
		String shares;

		if (tokenize(line).size() == 3) {
			// Follows the defined convention of 2 spaces between Account, Amount and Share Name
			// Account  Amount  Share Name @ Share Price
			amount = tokenize(line).get(1).trim();
			shares = tokenize(line).get(2).trim();
		} else {
			String misformed = tokenize(line).get(1).trim();
			amount = misformed.split(" ")[0];
			shares = misformed.substring(amount.length()).trim();
		}

		if (shares.contains("@")) {
			return new SharePosting(account, Money.parseMoney(amount, 2), Money.parseMoney(shares.split(" ")[2], 6), shares.split(" ")[0], getNote(line));
		}
		return new SharePosting(account, Money.parseMoney(amount, 2), BigDecimal.ZERO, shares.split(" ")[0], getNote(line));
	}

	private IPosting parseShareResetPosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).get(2).trim();
		String shareCode = tokenize(line).getLast().trim();

		return new SharePosting(account, Money.parseMoney(amount, 2), BigDecimal.ZERO, shareCode, true, getNote(line));
	}

	private IPosting parseFundPosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).getLast().trim();

		if (amount.isEmpty()) {
			log.warn("For fund line '{}' amount needs to be calculated", line);
		}

		return new FundPosting(account, Money.parseMoney(amount, 2), getNote(line));
	}

	private LedgerEntryState scanLine(String line) {
		if (isLedgerEntryStart(line)) {
			return LedgerEntryState.ENTRY_START;
		} else if (isCommentLine(line)) {
			return LedgerEntryState.COMMENT;
		} else if (line.contains("@")) {
			return LedgerEntryState.SHARE_POSTING;
		} else if (line.contains(":Fund:")) {
			// Quirk for my files
			return LedgerEntryState.FUND_POSTING;
		} else if (line.isEmpty()) {
			return LedgerEntryState.EMPTY;
		}

		if (countTokens(line) == 1) {
			return LedgerEntryState.REMAINDER_POSTING;
		} else if (countTokens(line) == 2) {
			return LedgerEntryState.CASH_POSTING;
		} else if (countTokens(line) == 3) {
			return LedgerEntryState.SHARE_POSTING;
		} else if (countTokens(line) == 4) {
			return LedgerEntryState.SHARE_RESET_POSTING;
		}

		return LedgerEntryState.UNKNOWN;
	}

	private boolean isCommandLine(String line) {
		return line.matches("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}.*")
				&& (line.split(" ")[1].contains("open")
				|| line.split(" ")[1].contains("balance")
				|| line.split(" ")[1].contains("custom")
				|| line.split(" ")[1].contains("query")
				|| line.split(" ")[1].contains("commodity"));
	}

	// test for date at start of line - line starts with yyyy/MM/dd or yyyy-MM-dd
	private boolean isLedgerEntryStart(String line) {
		return (line.matches("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}.*")) && isCommandLine(line) == false;
	}

	private boolean isCommentLine(String line) {
		return line.startsWith(";")
				|| line.startsWith("#")
				|| line.startsWith("*")
				|| line.startsWith("%")
				|| line.startsWith("|")
				|| isCommandLine(line);     // Treat commands as comments for now
	}

	private String getNote(String line) {
		if (line.contains(";")) {
			return line.substring(line.indexOf(";") + 1).trim();
		}
		return null;
	}

	private List<String> tokenize(String line) {
		// Remove anything after ; will handle that separately
		if (line.contains(";")) {
			line = line.substring(0, line.indexOf(";"));
		}
		// trim
		line = line.trim();

		// split into entries with 2 or more spaces as delimiter
		// remove any empty tokens
		// remove any MONEY_SYMBOL tokens
		return Arrays.stream(line.split(" {2}"))
				.map(String::trim)
				.filter(s -> s.isEmpty() == false)
				.filter(s -> s.equals(Money.MONEY_SYMBOL) == false)
				.toList();
	}

	private int countTokens(String line) {
		return tokenize(line).size();
	}

}

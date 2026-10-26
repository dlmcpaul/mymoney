package com.hz.mymoney.data.utilities;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.ledger.*;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Log4j2
public class LedgerParser {

	private static final Pattern TWO_SPACES = Pattern.compile(" {2}");
	private static final Pattern ONE_OR_MORE_SPACES = Pattern.compile("(\\s+)");
	private static final Pattern LINE_STARTS_WITH_DATE = Pattern.compile("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\s.*");

	public enum LedgerEntryState {
		COMMENT_POSTING,
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
				readLedgerEntries(ledger, br);
			} catch (IOException e) {
				ledger.addErrorCount();
				log.error(e.getMessage(), e);
			}
		} catch (IOException e) {
			ledger.addErrorCount();
			log.error(e.getMessage(), e);
		}

		return ledger;
	}

	public Ledger loadLedger(Path path) {
		Ledger ledger = new Ledger();

		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			readLedgerEntries(ledger, br);
		} catch (IOException e) {
			ledger.addErrorCount();
			log.error(e.getMessage(), e);
		}
		return ledger;
	}

	private void readLedgerEntries(Ledger ledger, BufferedReader br) throws IOException {
		String line = "";
		try {
			LedgerEntry ledgerEntry = null;
			while (	(line = br.readLine()) != null) {
				switch (scanLine(line)) {
					case ENTRY_START -> {
						addLedgerEntry(ledger, ledgerEntry);
						ledgerEntry = newLedgerEntry(line);
					}
					case CASH_POSTING -> {
						assert ledgerEntry != null : "ledgerEntry is null";
						addPosting(ledgerEntry, parseCashPosting(line));
					}
					case FUND_POSTING -> {
						assert ledgerEntry != null : "ledgerEntry is null";
						addPosting(ledgerEntry, parseFundPosting(line));
					}
					case SHARE_POSTING -> {
						assert ledgerEntry != null : "ledgerEntry is null";
						addPosting(ledgerEntry, parseSharePosting(line));
					}
					case SHARE_RESET_POSTING -> {
						assert ledgerEntry != null : "ledgerEntry is null";
						addPosting(ledgerEntry, parseShareResetPosting(line));
					}
					case REMAINDER_POSTING -> {
						assert ledgerEntry != null : "ledgerEntry is null";
						addPosting(ledgerEntry, parseRemainderPosting(line, ledgerEntry.getRemainingBalance()));
					}
					case COMMENT_POSTING -> {
						assert ledgerEntry != null : "ledgerEntry is null";
						addRawPosting(ledgerEntry, line);
					}
					case UNKNOWN, EMPTY -> {
						ledger.add(new RawEntry(line));
					}
				}
			}
			addLedgerEntry(ledger, ledgerEntry);
		} catch (Exception e) {
			ledger.addErrorCount();
			log.error("Error Parsing Line {}", line);
			throw e;
		}
	}

	private void addPosting(LedgerEntry ledgerEntry, IPosting posting) {
		ledgerEntry.getPostings().add(posting);
	}

	private void addLedgerEntry(Ledger ledger, LedgerEntry ledgerEntry) {
		if (ledgerEntry != null) {
			if (ledgerEntry.isBalanced()) {
				ledger.add(ledgerEntry);
			} else {
				ledger.addErrorCount();
				log.error("Ledger Entry is not balanced {}", ledgerEntry);
			}
		}
	}

	public LedgerEntry newLedgerEntry(String line) {
		LocalDate date = DateParser.parseDate(line.substring(0, line.indexOf(' ')));  // Everything up to first space should be a date
		String remaining = line.substring(line.indexOf(' ')).trim();
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

	private void addRawPosting(LedgerEntry ledgerEntry, String line) {
		ledgerEntry.getPostings().add(new RawPosting(line));
	}

	public IPosting parseCashPosting(String line) {
		// Account  Amount [Currency]
		List<String> tokens = tokenize(line);
		String account = tokens.getFirst().trim();
		String amount = tokens.getLast().trim();

		if (amount.isEmpty()) {
			log.warn("For cash line '{}' amount needs to be calculated", line);
		}

		return new Posting(account, MoneyParser.parseMoney(amount), getNote(line));
	}

	public IPosting parseRemainderPosting(String line, Money remainder) {
		String account = tokenize(line).getFirst().trim();

		return new Posting(account, remainder, getNote(line));
	}

	public IPosting parseSharePosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount;
		String code;
		String cost;
		boolean hasCosting = line.contains("@");

		if (tokenize(line).size() == 3 && tokenize(line).get(2).trim().contains("@")) {
			// Follows the defined convention of 2 spaces between Account, Amount and Share Definition
			// Account  Amount  code @ Cost currency
			amount = tokenize(line).get(1).trim();
			String shares = tokenize(line).get(2).trim();
			code = shares.split(" ")[0];
			if (shares.split(" ").length > 3) {
				cost = shares.split(" ")[2] + " " + shares.split(" ")[3];
			} else {
				cost = shares.split(" ")[2];
			}
		} else {
			String misformed = line.trim().substring(account.length()+1).trim();
			List<String> tokens = Arrays.stream(ONE_OR_MORE_SPACES.split(misformed))
					.map(String::trim)
					.filter(s -> s.isEmpty() == false)
					.filter(s -> s.equals(Money.DOLLAR_SYMBOL) == false)
					.filter(s -> s.equals(Money.EURO_SYMBOL) == false)
					.toList();
			amount = tokens.getFirst();
			code = tokens.get(1);
			if (tokens.size() == 2) {
				cost = "0.00";
			} else if (tokens.size() < 5) {
				cost = tokens.get(3);
			} else {
				cost = tokens.get(3) + " " + tokens.get(4);
			}
		}

		if (hasCosting) {
			return new SharePosting(account, MoneyParser.parseMoney(amount), MoneyParser.parseMoney(cost), code, getNote(line));
		}
		return new SharePosting(account, MoneyParser.parseMoney(amount), Money.ZERO, code, getNote(line));
	}

	public IPosting parseShareResetPosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).get(2).trim();
		String shareCode = tokenize(line).getLast().trim();

		return new SharePosting(account, MoneyParser.parseMoney(amount), Money.ZERO, shareCode, true, getNote(line));
	}

	public IPosting parseFundPosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).getLast().trim();

		if (amount.isEmpty()) {
			log.warn("For fund line '{}' amount needs to be calculated", line);
		}

		return new FundPosting(account, MoneyParser.parseMoney(amount), getNote(line));
	}

	private boolean isIndented(String line) {
		return line.startsWith("  ") || line.startsWith("\t");
	}

	public LedgerEntryState scanLine(String line) {

		if (isIndented(line)) {
			// Must be a Posting Line
			if (line.contains("@")) {
				return LedgerEntryState.SHARE_POSTING;
			} else if (line.contains(":Fund:")) {
				// Quirk for my files
				return LedgerEntryState.FUND_POSTING;
			} else if (line.trim().startsWith(";")) {
				return LedgerEntryState.COMMENT_POSTING;
			} else if (countTokens(line) == 1) {
				return LedgerEntryState.REMAINDER_POSTING;
			} else if (countTokens(line) == 2) {
				return LedgerEntryState.CASH_POSTING;
			} else if (countTokens(line) == 3) {
				return LedgerEntryState.SHARE_POSTING;
			} else if (countTokens(line) == 4) {
				return LedgerEntryState.SHARE_RESET_POSTING;
			}
		} else if (line.isBlank()) {
			return LedgerEntryState.EMPTY;
		} else if (isLedgerEntryStart(line)) {
			return LedgerEntryState.ENTRY_START;
		}

		return LedgerEntryState.UNKNOWN;
	}

	private boolean isCommandLine(String line) {
		return LINE_STARTS_WITH_DATE.matcher(line).find()
				&& (line.split(" ")[1].contains("open")
				|| line.split(" ")[1].contains("balance")
				|| line.split(" ")[1].contains("custom")
				|| line.split(" ")[1].contains("query")
				|| line.split(" ")[1].contains("commodity"));
	}

	// test for date at start of line - line starts with yyyy/MM/dd or yyyy-MM-dd
	private boolean isLedgerEntryStart(String line) {
		return (LINE_STARTS_WITH_DATE.matcher(line).find()) && isCommandLine(line) == false;
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
		return Arrays.stream(TWO_SPACES.split(line))
				.map(String::trim)
				.filter(s -> s.isEmpty() == false)
				.filter(s -> s.equals(Money.DOLLAR_SYMBOL) == false)
				.filter(s -> s.equals(Money.EURO_SYMBOL) == false)
				.toList();
	}

	private int countTokens(String line) {
		return tokenize(line).size();
	}

}

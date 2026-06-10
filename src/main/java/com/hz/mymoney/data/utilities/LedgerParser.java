package com.hz.mymoney.data.utilities;

import com.hz.mymoney.data.models.ledger.*;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class LedgerParser {

	private static final String DATE_FORMAT_1 = "yyyy/MM/dd";
	private static final String DATE_FORMAT_2 = "yyyy-MM-dd";

	public Ledger loadLedger(InputStream file) {
		Ledger ledger = new Ledger();

		String lastLine = null;
		try (final InputStream ledgerImportStream = file) {
			BufferedReader ledgerReader = new BufferedReader(new InputStreamReader(ledgerImportStream));
			// A ledgerEntry begins with a date followed by multiple posting lines
			String line = ledgerReader.readLine();
			LedgerEntry ledgerEntry = null;
			while (line != null) {
				line = line.trim();
				lastLine = line;
				if (isCommentLine(line) || line.isEmpty()) {
					line = ledgerReader.readLine();
					continue;
				}

				if (isNewLedgerEntry(line)) {
					if (ledgerEntry != null) {
						if (ledgerEntry.isBalanced()) {
							ledger.getLedgerEntries().add(ledgerEntry);
						} else {
							log.error("Ledger Entry is not balanced {}", ledgerEntry);
						}
					}
					ledgerEntry = makeLedgerEntryFromLine(line);
				} else if (ledgerEntry != null) {
					// Posting (cash or share)
					if (isFundPosting(line)) {
						ledgerEntry.getPostings().add(parseFundPosting(line));
					} else if (isCashPosting(line)) {
						ledgerEntry.getPostings().add(parseCashPosting(line));
					} else if (isSharePosting(line)) {
						ledgerEntry.getPostings().add(parseSharePosting(line));
					} else if (isRemainderPosting(line)) {
						ledgerEntry.getPostings().add(parseRemainderPosting(line, ledgerEntry.getRemainingBalance()));
					} else if (isShareResetPosting(line)) {
						ledgerEntry.getPostings().add(parseShareResetPosting(line));
					} else {
						log.warn("Could not parse line '{}' {}", line, countTokens(line));
					}
				} else {
					log.error("Not supported {}", line);
				}
				line = ledgerReader.readLine();
			}
			if (ledgerEntry != null) {
				if (ledgerEntry.isBalanced()) {
					ledger.getLedgerEntries().add(ledgerEntry);
				} else {
					log.error("Final Ledger Entry is not balanced {}", ledgerEntry);
				}
			}
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (Exception e) {
			log.error("({}) {}", lastLine, e.getMessage());
		}
		return ledger;
	}

	private LedgerEntry makeLedgerEntryFromLine(String line) {
		LocalDate date = getDate(line); // First 10 chars is date
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

	private boolean isCommentLine(String line) {
		return line.startsWith(";")
				|| line.startsWith("#")
				|| line.startsWith("*")
				|| line.startsWith("%")
				|| line.startsWith("|");
	}

	private BigDecimal parseMoney(String amount, int scale) {
		if (amount.contains("$-") || amount.contains("-$")) {
			// special case 1
			return parseMoney("$" + amount.substring(2), scale).multiply(BigDecimal.valueOf(-1));
		} else if (amount.contains("$ -")) {
			// special case 2
			return parseMoney("$" + amount.substring(3), scale).multiply(BigDecimal.valueOf(-1));
		} else if (amount.contains(".") == false) {
			// special case 3
			return parseMoney(amount + ".00", scale);
		} else if (amount.startsWith("$ ")) {
			// special case 4
			return parseMoney("$" + amount.substring(2), scale);
		} else if (amount.startsWith("$") == false) {
			// special case 5
			return parseMoney("$" + amount, scale);
		}

		try {
			NumberFormat currency = NumberFormat.getCurrencyInstance();

			if (currency instanceof DecimalFormat decimal) {
				decimal.setParseBigDecimal(true);
				return ((BigDecimal)decimal.parse(amount)).setScale(scale, RoundingMode.HALF_UP);
			}
		} catch (ParseException e) {
			log.error("{} {}", e.getMessage(), amount);
		}
		return BigDecimal.ZERO;
	}

	private Posting parseRemainderPosting(String line, BigDecimal remainder) {
		String account = tokenize(line).getFirst().trim();

		return new Posting(account, remainder, getNote(line));
	}

	private SharePosting parseSharePosting(String line) {
		// Account  Amount  Share Name @ Share Price
		String account = tokenize(line).get(0).trim();
		String amount = tokenize(line).get(1).trim();
		String shares = tokenize(line).get(2).trim();

		if (shares.contains("@")) {
			return new SharePosting(account, parseMoney(amount, 2), parseMoney(shares.split(" ")[2], 6), shares.split(" ")[0], getNote(line));
		}
		return new SharePosting(account, parseMoney(amount, 2), BigDecimal.ZERO, shares.split(" ")[0], getNote(line));
	}

	private SharePosting parseShareResetPosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).get(2).trim();
		String shareCode = tokenize(line).getLast().trim();

		return new SharePosting(account, parseMoney(amount, 2), BigDecimal.ZERO, shareCode, true, getNote(line));
	}

	private FundPosting parseFundPosting(String line) {
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).getLast().trim();

		if (amount.isEmpty()) {
			log.warn("for fund line '{}' amount needs to be calculated", line);
		}

		return new FundPosting(account, parseMoney(amount, 2), getNote(line));
	}

	private String getNote(String line) {
		if (line.contains(";")) {
			return line.substring(line.indexOf(";") + 1).trim();
		}
		return null;
	}

	private Posting parseCashPosting(String line) {
		// Account  Amount
		String account = tokenize(line).getFirst().trim();
		String amount = tokenize(line).getLast().trim();

		if (amount.isEmpty()) {
			log.warn("for cash line '{}' amount needs to be calculated", line);
		}

		return new Posting(account, parseMoney(amount, 2), getNote(line));
	}

	private LocalDate getDate(String line) {
		String datePart = line.substring(0, 10);
		try {
			if (datePart.contains("/")) {
				return LocalDate.parse(datePart, DateTimeFormatter.ofPattern(DATE_FORMAT_1));
			}
			return LocalDate.parse(datePart, DateTimeFormatter.ofPattern(DATE_FORMAT_2));
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	// A post line where we have to calculate the remaining amount
	private boolean isRemainderPosting(String line) {
		return countTokens(line) == 1;
	}

	private boolean isFundPosting(String line) {
		return line.contains(":Fund:");
	}

	// A cash posting
	private boolean isCashPosting(String line) {
		return countTokens(line) == 2;
	}

	// A share posting
	private boolean isSharePosting(String line) {
		return countTokens(line) == 3;
	}

	// A share reset posting
	private boolean isShareResetPosting(String line) {
		return countTokens(line) == 4;
	}

	private boolean isNewLedgerEntry(String line) {
		// test for date description - line starts with yyyy/MM/dd or yyyy-MM-dd
		return line.matches("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}.*");
	}

	private List<String> tokenize(String line) {
		// Remove anything after ;
		if (line.contains(";")) {
			line = line.substring(0, line.indexOf(";"));
		}
		// trim
		line = line.trim();

		// split into entries with 2 or more spaces as delimiter
		// remove any empty tokens
		// remove any $ tokens
		return Arrays.stream(line.split(" {2}"))
				.map(String::trim)
				.filter(s -> s.isEmpty() == false)
				.filter(s -> s.equals("$") == false)
				.toList();
	}

	private int countTokens(String line) {
		return tokenize(line).size();
	}
}

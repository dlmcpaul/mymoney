package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.coa.InvestmentHistory;
import com.hz.mymoney.data.models.coa.InvestmentHistoryEntry;
import com.hz.mymoney.data.utilities.DateParser;
import com.hz.mymoney.data.utilities.MoneyParser;
import com.hz.mymoney.exceptions.FileLoadFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Log4j2
public class PriceHistoryServices {

	private static final Pattern whitespace = Pattern.compile("\\s+");
	private static final Pattern single_space = Pattern.compile("\\s");
	private static final Pattern comma_delimited = Pattern.compile(", ");

	private String historyType;
	private String historyFileName;
	private boolean loadSuccess;
	private FileTime lastModified;

	private final ResourceLoader resourceLoader;

	@Getter
	private InvestmentHistory investmentHistory;

	public Money getInvestmentValue(String code, LocalDate asAt) {
		return investmentHistory.getInvestmentValue(code, asAt);
	}

	public Money getPreviousInvestmentValue(String code, LocalDate asAt) {
		return investmentHistory.getPreviousInvestmentValue(code, asAt);
	}

	public void startUp(@NonNull String historyType, @NotNull String historyFileName) {
		this.historyType = historyType;
		this.historyFileName = historyFileName;

		lastModified = null;
	}

	// For when Journals contain Price History entries
	public void updateFromJournal(String line) {
		parseLine(investmentHistory.commodityMap(), line);
	}

	// To include BUY/SELL entries from the journal
	public void updateFromJournal(LocalDate effective, String code, Money price) {
		addEntry(investmentHistory.commodityMap(), code, effective, price);
	}

	public void reloadCommodities() throws IOException {
		loadSuccess = false;

		switch (historyType) {
			case "internal" -> {
				// Don't reload test data
				if (investmentHistory == null || investmentHistory.commodityMap().isEmpty()) {
					loadCommoditiesFromArgs();
				}
			}
			case "commodities" -> loadCommoditiesFromArgs();
			case "quicken" -> loadCommoditiesFromQuickenFile();
			default -> throw new IllegalStateException("Unexpected value: " + historyType);
		}
		if (loadSuccess) {
			switch (historyType) {
				case "internal" -> log.info("Successfully loaded {} Commodities from TEST file {}", investmentHistory.commodityMap().size(), historyFileName);
				case "quicken" -> log.info("Successfully loaded {} Commodities from Quicken file {}", investmentHistory.commodityMap().size(), historyFileName);
				case "commodities" -> log.info("Successfully loaded {} Commodities from PTA file {}", investmentHistory.commodityMap().size(), historyFileName);
			}
		}
	}

	// Exported Quicken commodities file format
	// Basic CSV with format of Code, Value, date (dd/MM/yy)
	// VDHG, 74, 16/4/26
	private void loadCommoditiesFromQuickenFile() throws IOException {
		Path path = Path.of(historyFileName);

		if (Files.isReadable(path)) {
			if (lastModified == null || lastModified.compareTo(Files.getLastModifiedTime(path)) != 0) {
				lastModified = Files.getLastModifiedTime(path);
				Map<String, SortedSet<InvestmentHistoryEntry>> entries = new HashMap<>();
				investmentHistory = new InvestmentHistory(entries);
				try {
					try (final InputStream ledgerImportStream = Files.newInputStream(path)) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
						String line = reader.readLine();
						while (line != null) {
							List<String> tokens = Arrays.stream(comma_delimited.split(line)).toList();
							addEntry(entries, tokens.get(0), DateParser.parseQuickenDate(tokens.get(2)), parseBigDecimal(tokens.get(1)));

							line = reader.readLine();
						}
					}
					loadSuccess = true;
				} catch (RuntimeException e) {
					log.error(e.getMessage(), e);
				}
			}
		} else {
			log.error("Unable to load Quicken Commodities from file {}", historyFileName);
			throw new FileLoadFailedException("Unable to load Quicken Commodities from " + historyFileName);
		}
	}

	// Ledger Commodities file format
	private void loadCommoditiesFromArgs() throws IOException {
		Map<String, SortedSet<InvestmentHistoryEntry>> entries = new HashMap<>();
		try {
			if (historyFileName.startsWith("classpath:")) {
				investmentHistory = new InvestmentHistory(entries);
				loadCommodities(resourceLoader.getResource(historyFileName).getInputStream(), entries);
			} else {
				Path path = Path.of(historyFileName);

				if (Files.isReadable(path)) {
					if (lastModified == null || lastModified.compareTo(Files.getLastModifiedTime(path)) != 0) {
						lastModified = Files.getLastModifiedTime(path);

						investmentHistory = new InvestmentHistory(entries);
						loadCommodities(Files.newInputStream(path), entries);
					}
				} else {
					throw new FileLoadFailedException("Unable to load Commodities from " + historyFileName);
				}
			}
		} catch (FileLoadFailedException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void loadCommodities(InputStream inputStream, Map<String, SortedSet<InvestmentHistoryEntry>> entries) {
		String line = "";
		try (final BufferedInputStream ledgerImportStream = new BufferedInputStream(inputStream)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
			while ((line = reader.readLine()) != null) {
				parseLine(entries, line);
			}
		} catch (RuntimeException | IOException e) {
			log.error(e.getMessage(), e);
			throw new FileLoadFailedException("Failed to parse line '" + line + "'");
		}
		loadSuccess = true;
	}

	private void parseLine(Map<String, SortedSet<InvestmentHistoryEntry>> entries, String line) {
		// Parse as P 2025-01-01 NSC 150,25 USD
		// P date commodity value currency
		if (line.startsWith("P")) {
			String[] tokens = line.split(" ");
			addEntry(entries, tokens[2], DateParser.parseDate(tokens[1]), parseBigDecimal(tokens[3]));
		}
	}

	private Money parseBigDecimal(String value) {
		if (MoneyParser.hasCurrencySymbol(value)) {
			return MoneyParser.parseMoney(value);
		}
		return new Money(new BigDecimal(value));
	}

	private void addEntry(Map<String, SortedSet<InvestmentHistoryEntry>> entries, String commodityCode, LocalDate asAt, Money value) {
		entries.computeIfAbsent(commodityCode, k -> new TreeSet<>());
		entries.get(commodityCode).add(new InvestmentHistoryEntry(asAt, value));
	}

}

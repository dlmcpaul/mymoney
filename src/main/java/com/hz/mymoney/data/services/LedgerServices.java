package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.coa.Account;
import com.hz.mymoney.data.models.coa.ChartOfAccounts;
import com.hz.mymoney.data.models.coa.Movement;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.SharePosting;
import com.hz.mymoney.data.utilities.LedgerEntrySupport;
import com.hz.mymoney.data.utilities.LedgerParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static com.hz.mymoney.configuration.AccountConstants.FUND_ACCOUNTS;
import static com.hz.mymoney.configuration.AccountConstants.IMPUTATION_ACCOUNT;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

@Service
@Log4j2
@RequiredArgsConstructor
public class LedgerServices implements ApplicationRunner {
	private final ResourceLoader resourceLoader;
	private final SharePriceServices shareValueService;

	private String ledgerFileName;

	@Getter
	private Ledger ledger;

	@Getter
	private ChartOfAccounts coa;

	@Override
	public void run(@NonNull ApplicationArguments args) throws Exception {

		if (args.containsOption("ledger")) {
			loadLedgerFromArgs(Objects.requireNonNull(args.getOptionValues("ledger")).getFirst());
		} else {
			this.loadLedgerFromClassPath("test.ledger");
		}

		ledger.getLedgerEntries()
			.forEach(this::updateShareValues);

		makeChartOfAccounts(ledger);

		if (args.containsOption("testing")) {
			log.info("COA contains {} accounts", coa.totalAccounts());
			log.info("Assets:Cash:NAB Savings = {}", coa.getBalanceForAccount("Assets:Cash:NAB Savings", shareValueService.getInvestmentHistory()));
			log.info("Assets:Stock:IAG Shares = {}", coa.getBalanceForAccount("Assets:Stock:IAG Shares", shareValueService.getInvestmentHistory()));
			log.info("Imputation Account Source {}", coa.getAccountsOfType(IMPUTATION_ACCOUNT, false).getFirst().getFirstMovement().sourceAccount());
			log.info("Total Fund Accounts {}", coa.getZeroBalanceAccountsOfType(FUND_ACCOUNTS).size());
			log.info("Expenses:Cash:Household = {}", coa.getBalanceForAccount("Expenses:Cash:Household", shareValueService.getInvestmentHistory()));

			logEmptyCodeMovements("Income:Investment:Dividends:Franked");
			logEmptyCodeMovements("Income:Investment:Dividends:Unfranked");
			logEmptyCodeMovements("Income:Investment:MiscIncome");
			logEmptyCodeMovements("Income:Investment:Capital Gains");
			logEmptyCodeMovements("Income:Investment:Capital Return");
			logEmptyCodeMovements("Expenses:Investment:Capital Losses");
		}
	}

	private void updateShareValues(LedgerEntry entry) {
		SharePosting posting = entry.getSharePosting();
		if (posting != null) {
			shareValueService.updateFromJournal(entry.getDate(), posting.getCode(), posting.getPrice());
		}
	}

	public void makeChartOfAccounts(Ledger ledger) {
		// Convert to Chart of Accounts
		this.coa = new ChartOfAccounts(LocalDate.now().plusDays(1));
		ledger.getLedgerEntries()
			.forEach(entry -> mapLedgerEntryToPostings(coa, entry));
	}

	private Optional<IPosting> findMatching(List<IPosting> postings, IPosting posting) {
		return postings.stream().filter(p -> p.getAmount().equals(posting.getAmount().negate())).findFirst();
	}

	private List<IPosting> findBalancedPostings(LedgerEntry entry) {
		for (IPosting posting : entry.getPostings()) {
			Optional<IPosting> matched = findMatching(entry.getPostings(), posting);
			if (matched.isPresent()) {
				return List.of(posting, matched.get());
			}
		}
		return new ArrayList<>();
	}

	private void mapLedgerEntryToPostingsUnBalanced(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> matchedPostings = findBalancedPostings(entry);
		if (matchedPostings.size() == 2) {
			// Process the balanced entries and retry remaining?
			coa.addPosting(matchedPostings.getFirst(), entry.getDate(), entry.getDescription(), matchedPostings.getLast().getAccount(), commission);
			coa.addPosting(matchedPostings.getLast(), entry.getDate(), entry.getDescription(), matchedPostings.getFirst().getAccount(), commission);

			List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.removePostings(entry.getPostings(), matchedPostings.getFirst(), matchedPostings.getLast()));

			if (sortedPostings.getLast().getAmount().compareTo(Money.ZERO) > 0) {
				IPosting sourcePosting = sortedPostings.getLast();
				coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), sortedPostings.getFirst().getAccount(), commission);
				sortedPostings.subList(0, sortedPostings.size()-1).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
			} else {
				log.error("Could not determine positive source posting {}", entry);
			}
		} else {
			log.error("Unhandled Unbalanced Ledger Entry with {} postings {}", entry.getPostings().size(), entry);
		}
	}

	private void mapLedgerEntryToPostingsBalanced(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getPostings());
		List<IPosting> negativeAmounts = LedgerEntrySupport.splitAndReturnFirst(sortedPostings);
		List<IPosting> positiveAmounts = LedgerEntrySupport.splitAndReturnLast(sortedPostings);

		int i = positiveAmounts.size()-1;
		for (IPosting posting : negativeAmounts) {
			if (posting.getAmount().abs().compareTo(positiveAmounts.get(i).getAmount()) == 0) {
				// They match
				coa.addPosting(posting, entry.getDate(), entry.getDescription(), positiveAmounts.get(i).getAccount(), commission);
				coa.addPosting(positiveAmounts.get(i), entry.getDate(), entry.getDescription(), posting.getAccount(), commission);
			} else {
				log.error("Unhandled Ledger Entry with {} postings and multiple negative sources {}", entry.getPostings().size(), entry);
			}
			i--;
		}
	}

	private void mapLedgerEntryToPostingsSingleNegative(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getPostings());

		if (sortedPostings.getFirst().getAmount().compareTo(Money.ZERO) < 0) {
			IPosting sourcePosting = sortedPostings.getFirst();
			coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
			sortedPostings.subList(1, sortedPostings.size()).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
		} else {
			log.error("Could not determine negative source posting {}", entry);
		}
	}

	private void mapLedgerEntryToPostingsSinglePositive(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getPostings());

		if (sortedPostings.getLast().getAmount().compareTo(Money.ZERO) > 0) {
			IPosting sourcePosting = sortedPostings.getLast();
			coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
			sortedPostings.subList(0, sortedPostings.size()-1).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
		} else {
			log.error("Could not determine positive source posting {}", entry);
		}
	}

	public void mapLedgerEntryToPostings(ChartOfAccounts coa, LedgerEntry entry) {
		Money commission = entry.hasCommissionPosting() ? entry.getCommission() : Money.ZERO;
		List<IPosting> postings = entry.getPostings();
		if (postings.size() == 2) {
			// Simple case (does not assume order)
			postings.forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(p.getAccount()).getAccount(), commission));
		} else if (postings.size() > 2) {
			// Complex multiline posting so need to determine a primary source account (ie where the money came from (-) or goes to (+))
			// We define the source account as the negative amounts
			if (entry.totalNegativePostings() == 1) {
				mapLedgerEntryToPostingsSingleNegative(coa, entry, commission);
			} else if (entry.totalPositivePostings() == 1) {
				mapLedgerEntryToPostingsSinglePositive(coa, entry, commission);
			} else if (entry.canMatchPostings()) {
				// Complex case where there are multiple sources but for now we assume it is balanced (equal + and - amounts that match)
				mapLedgerEntryToPostingsBalanced(coa, entry, commission);
			} else {
				mapLedgerEntryToPostingsUnBalanced(coa, entry, commission);
			}
		} else {
			log.error("Unhandled Ledger Entry with {} postings", postings.size());
		}
	}

	private void logEmptyCodeMovements(String accountName) {
		Optional<Account> fd = coa.findAccount(accountName);
		if (fd.isPresent()) {
			PriorityQueue<Movement> movements = fd.get().getMovementsForCode("");
			if (movements.size() > 0) {
				log.info("Account {} has {} movements with no code", accountName, movements.size());
				movements.forEach(log::info);
			}
		}
	}

	public void saveLedger() {
		if (ledgerFileName == null || ledgerFileName.startsWith(CLASSPATH_URL_PREFIX) || ledger.isReadOnly()) {
			log.error("Cannot Save Ledger");
		} else {
			Path path = Path.of(ledgerFileName);
			log.info("Saving Ledger to file {}", path);
			try (var out = new BufferedWriter(new FileWriter(path.toFile()))) {
				for (String metaLine : ledger.getMetaData()) {
					out.write(metaLine);
				}
				for (LedgerEntry entry : ledger.getLedgerEntries()) {
					out.write(entry.toString());
				}
				log.info("Ledger saved successfully");
			} catch (IOException e) {
				log.error("Failed to Save Ledger", e);
			}
		}
	}

	private void loadLedgerFromArgs(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		Path path = Path.of(fileName);

		if (Files.isReadable(path)) {
			ledgerFileName = path.toString();
			ledger = ledgerParser.loadLedger(path);
			log.info("{}oaded {} Ledger Entries from file {} with {} errors", ledger.isReadOnly() ? "L" : "Successfully l", ledger.getLedgerEntries().size(), ledgerFileName, ledger.getLoadErrorCount());
		} else {
			log.error("Unable to load Ledger from file {}", fileName);
			throw new FileNotFoundException("Unable to load Ledger from file " + fileName);
		}
	}

	private void loadLedgerFromClassPath(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		try {
			ledgerFileName = CLASSPATH_URL_PREFIX + fileName;
			Resource resource = resourceLoader.getResource(ledgerFileName);
			ledger = ledgerParser.loadLedger(resource.getInputStream());
		} finally {
			log.info("Demo Ledger loaded successfully from classpath:{}", fileName);
		}
	}

	public void reloadLedger() throws IOException {
		if (ledgerFileName.startsWith(CLASSPATH_URL_PREFIX)) {
			this.loadLedgerFromClassPath(ledgerFileName.replace(CLASSPATH_URL_PREFIX, ""));
		} else {
			this.loadLedgerFromArgs(ledgerFileName);
		}
		makeChartOfAccounts(ledger);
	}
}

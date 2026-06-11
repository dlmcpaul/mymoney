package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.internal.Account;
import com.hz.mymoney.data.models.internal.ChartOfAccounts;
import com.hz.mymoney.data.models.internal.Movement;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
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
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static com.hz.mymoney.configuration.AccountConstants.FUNDS;
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

		makeChartOfAccounts(ledger);

		if (args.containsOption("testing")) {
			log.info("COA contains {} accounts", coa.totalAccounts());
			log.info("Assets:Cash:NAB Savings = {}", coa.getBalanceForAccount("Assets:Cash:NAB Savings", shareValueService.getInvestmentHistory()));
			log.info("Assets:Stock:IAG Shares = {}", coa.getBalanceForAccount("Assets:Stock:IAG Shares", shareValueService.getInvestmentHistory()));
			log.info("Imputation Account Source {}", coa.getAccountsOfType(IMPUTATION_ACCOUNT, false).getFirst().getFirstMovement().sourceAccount());
			log.info("Total Fund Accounts {}", coa.getZeroBalanceAccountsOfType(FUNDS).size());
			log.info("Expenses:Cash:Household = {}", coa.getBalanceForAccount("Expenses:Cash:Household", shareValueService.getInvestmentHistory()));

			logEmptyCodeMovements("Income:Investment:Dividends:Franked");
			logEmptyCodeMovements("Income:Investment:Dividends:Unfranked");
			logEmptyCodeMovements("Income:Investment:MiscIncome");
			logEmptyCodeMovements("Income:Investment:Capital Gains");
			logEmptyCodeMovements("Income:Investment:Capital Return");
			logEmptyCodeMovements("Expenses:Investment:Capital Losses");
		}
	}

	public void makeChartOfAccounts(Ledger ledger) {
		// Convert to Chart of Accounts
		this.coa = new ChartOfAccounts(LocalDate.now().plusDays(1));
		ledger.getLedgerEntries()
			.forEach(entry -> addPostings(coa, entry));
	}

	private void addPostings(ChartOfAccounts coa, LedgerEntry entry) {
		BigDecimal commission = entry.hasCommissionPosting() ? entry.getCommission() : BigDecimal.ZERO;
		List<IPosting> postings = entry.getPostings();
		if (postings.size() == 2) {
			// Simple case (does not assume order)
			postings.forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(p.getAccount()).getAccount(), commission));
		} else if (postings.size() > 2) {
			// Complex multiline posting so need to determine source account (ie where the money came from)
			if (postings.getFirst().getAmount().compareTo(BigDecimal.ZERO) < 0) {
				// First account is source
				IPosting sourcePosting = postings.getFirst();
				coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
				postings.subList(1, postings.size()).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
			} else if (postings.getLast().getAmount().compareTo(BigDecimal.ZERO) < 0) {
				// Last account is source
				IPosting sourcePosting = postings.getLast();
				coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
				postings.subList(0, postings.size()-1).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
			} else {
				// Not first or last.
				postings.sort(Comparator.comparing(IPosting::getAmount));
				if (postings.getFirst().getAmount().compareTo(BigDecimal.ZERO) < 0) {
					IPosting sourcePosting = postings.getFirst();
					coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
					postings.subList(1, postings.size()).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
				} else {
					log.error("Could not determine source posting {}", entry);
				}
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
		if (ledgerFileName == null || ledgerFileName.startsWith(CLASSPATH_URL_PREFIX)) {
			log.error("Cannot Save Ledger");
		} else {
			Path path = Path.of(ledgerFileName);
			log.info("Saving Ledger to file {}", path);
			try (var out = new BufferedWriter(new FileWriter(path.toFile()))) {
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
			try {
				ledgerFileName = path.toString();
				ledger = ledgerParser.loadLedger(Files.newInputStream(path));
			} finally {
				log.info("Ledger loaded successfully from file {}", ledgerFileName);
			}
		} else {
			log.error("Unable to load Ledger from file {}", fileName);
		}
	}

	private void loadLedgerFromClassPath(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		try {
			ledgerFileName = CLASSPATH_URL_PREFIX + fileName;
			Resource resource = resourceLoader.getResource(ledgerFileName);
			ledger = ledgerParser.loadLedger(resource.getInputStream());
		} finally {
			log.info("Ledger loaded successfully from classpath:{}", fileName);
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

package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.coa.ChartOfAccounts;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import com.hz.mymoney.data.utilities.FileSupport;
import com.hz.mymoney.data.utilities.LedgerEntrySupport;
import com.hz.mymoney.data.utilities.LedgerParser;
import com.hz.mymoney.exceptions.FileSaveFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

@Service
@RequiredArgsConstructor
@Log4j2
public class LedgerServices {
	private final ResourceLoader resourceLoader;

	private String ledgerFileName;

	@Getter
	private Ledger ledger;

	@Getter
	private ChartOfAccounts coa;

	public void startUp(@NonNull String ledgerFileName) {
		this.ledgerFileName = ledgerFileName;
	}

	public void makeChartOfAccounts(Ledger ledger) {
		// Convert to Chart of Accounts
		this.coa = new ChartOfAccounts(LocalDate.now().plusDays(1));
		ledger.getOnlyLedgerEntries()
			.forEach(entry -> mapLedgerEntryToPostings(coa, entry));
	}

	private Optional<IPosting> findMatching(List<IPosting> postings, IPosting posting) {
		return postings.stream().filter(p -> p.getAmount().equals(posting.getAmount().negate())).findFirst();
	}

	private List<IPosting> findMatchingPostings(LedgerEntry entry) {
		for (IPosting posting : entry.getPostings()) {
			Optional<IPosting> matched = findMatching(entry.getPostings(), posting);
			if (matched.isPresent()) {
				return List.of(posting, matched.get());
			}
		}
		return new ArrayList<>();
	}

	private void mapLedgerEntryToPostingsUnMatched(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> matchedPostings = findMatchingPostings(entry);
		if (matchedPostings.size() == 2) {
			// Process the balanced entries and retry remaining?
			coa.addPosting(matchedPostings.getFirst(), entry.getDate(), entry.getDescription(), matchedPostings.getLast().getAccount(), commission);
			coa.addPosting(matchedPostings.getLast(), entry.getDate(), entry.getDescription(), matchedPostings.getFirst().getAccount(), commission);

			List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.removePostings(entry.getNonRawPostings(), matchedPostings.getFirst(), matchedPostings.getLast()));

			if (sortedPostings.getLast().getAmount().compareTo(Money.ZERO) > 0) {
				IPosting sourcePosting = sortedPostings.getLast();
				coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), sortedPostings.getFirst().getAccount(), commission);
				sortedPostings.subList(0, sortedPostings.size()-1).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
			} else {
				log.error("Could not determine positive source posting {}", entry);
			}
		} else {
			// Let's just match to the largest negative account similar to the single negative amount
			List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getNonRawPostings());

			if (sortedPostings.getFirst().getAmount().compareTo(Money.ZERO) < 0) {
				IPosting sourcePosting = sortedPostings.getFirst();
				coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
				sortedPostings.subList(1, sortedPostings.size()).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
			} else {
				log.error("Could not determine negative source posting {}", entry);
			}
		}
	}

	private void mapLedgerEntryToPostingsMatched(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getNonRawPostings());
		List<IPosting> negativeAmounts = LedgerEntrySupport.splitAndReturnFirst(sortedPostings);
		List<IPosting> positiveAmounts = LedgerEntrySupport.splitAndReturnLast(sortedPostings);

		int i = positiveAmounts.size()-1;
		for (IPosting posting : negativeAmounts) {
			if (posting.getAmount().abs().compareTo(positiveAmounts.get(i).getAmount()) == 0) {
				// They match
				coa.addPosting(posting, entry.getDate(), entry.getDescription(), positiveAmounts.get(i).getAccount(), commission);
				coa.addPosting(positiveAmounts.get(i), entry.getDate(), entry.getDescription(), posting.getAccount(), commission);
			} else {
				log.error("Unhandled Ledger Entry with {} postings and multiple negative sources {}", entry.getNonRawPostings().size(), entry);
			}
			i--;
		}
	}

	private void mapLedgerEntryToPostingsSingleNegative(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getNonRawPostings());

		if (sortedPostings.getFirst().getAmount().compareTo(Money.ZERO) < 0) {
			IPosting sourcePosting = sortedPostings.getFirst();
			coa.addPosting(sourcePosting, entry.getDate(), entry.getDescription(), entry.getNonSourcePosting(sourcePosting.getAccount()).getAccount(), commission);
			sortedPostings.subList(1, sortedPostings.size()).forEach(p -> coa.addPosting(p, entry.getDate(), entry.getDescription(), sourcePosting.getAccount(), commission));
		} else {
			log.error("Could not determine negative source posting {}", entry);
		}
	}

	private void mapLedgerEntryToPostingsSinglePositive(ChartOfAccounts coa, LedgerEntry entry, Money commission) {
		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(entry.getNonRawPostings());

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
				mapLedgerEntryToPostingsMatched(coa, entry, commission);
			} else {
				mapLedgerEntryToPostingsUnMatched(coa, entry, commission);
			}
		} else {
			log.error("Unhandled Ledger Entry with {} postings", postings.size());
		}
	}

	public void saveLedger() {
		if (ledgerFileName == null) {
			log.error("Cannot Save Ledger as filename is missing");
			throw new FileSaveFailedException("Failed to Save Ledger due to missing filename");
		}

		if (ledgerFileName.startsWith(CLASSPATH_URL_PREFIX)) {
			log.error("Cannot Save Ledger as filename is internal");
			throw new FileSaveFailedException("Failed to Save Ledger file " + ledgerFileName);
		}

		if (ledger.isReadOnly()) {
			log.error("Cannot Save a Read Only Ledger");
			throw new FileSaveFailedException("Failed to Save Ledger as it is read-only");
		}

		FileSupport.writeFile(ledgerFileName, "Ledger", ledger.getEntries());
	}

	private void loadLedgerFromArgs(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		Path path = Path.of(fileName);

		if (Files.isReadable(path)) {
			ledger = ledgerParser.loadLedger(path);
			log.info("{}oaded {} Ledger Entries from file {} with {} errors", ledger.isReadOnly() ? "L" : "Successfully l", ledger.getEntries().size(), ledgerFileName, ledger.getLoadErrorCount());
		} else {
			log.error("Unable to load Ledger from file {}", fileName);
			throw new FileNotFoundException("Unable to load Ledger from file " + fileName);
		}
	}

	private void loadLedgerFromClassPath(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		try {
			Resource resource = resourceLoader.getResource(ledgerFileName);
			ledger = ledgerParser.loadLedger(resource.getInputStream());
		} finally {
			log.info("Demo Ledger loaded successfully from classpath:{}", fileName);
		}
	}

	public void reloadLedger() throws IOException {
		if (ledgerFileName.startsWith(CLASSPATH_URL_PREFIX)) {
			this.loadLedgerFromClassPath(ledgerFileName);
		} else {
			this.loadLedgerFromArgs(ledgerFileName);
		}
		log.info("Building Chart of Accounts");
		makeChartOfAccounts(ledger);
	}

	public void addLedgerEntry(LedgerEntry entry) {
		ledger.add(entry);
		this.saveLedger();
		this.makeChartOfAccounts(ledger);
	}

	public void addLedgerEntry(LocalDate date, String description, List<IPosting> postings) {
		this.addLedgerEntry(new LedgerEntry(date, description, postings));
	}

	public void addLedgerEntry(LocalDate date, String description, Money amount, String to, String from) {
		Posting fromAccount = new Posting(from, amount);
		Posting toAccount = new Posting(to, amount.negate());
		this.addLedgerEntry(date, description, List.of(toAccount, fromAccount));
	}

}

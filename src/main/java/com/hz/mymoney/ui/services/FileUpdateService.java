package com.hz.mymoney.ui.services;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.coa.Schedule;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import com.hz.mymoney.data.services.LedgerServices;
import com.hz.mymoney.data.services.PriceHistoryServices;
import com.hz.mymoney.data.services.SchedulesServices;
import com.hz.mymoney.data.services.StartupServices;
import com.hz.mymoney.ui.exceptions.ValidationException;
import com.hz.mymoney.ui.models.inputs.BasicJournalInput;
import com.hz.mymoney.ui.models.inputs.DistributionTypeInput;
import com.hz.mymoney.ui.models.inputs.DividendJournalInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.hz.mymoney.configuration.AccountConstants.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class FileUpdateService {
	private final LedgerServices ledgerServices;
	private final SchedulesServices schedulesServices;
	private final PriceHistoryServices sharePriceServices;
	private final StartupServices startupServices;

	public void addNewTransaction(BasicJournalInput basicJournalInput) {
		Money amount = new Money(basicJournalInput.getAmounts().getFirst() != null ? basicJournalInput.getAmounts().getFirst() : basicJournalInput.getAmounts().getLast());

		log.info("New Simple Journal {} {} {} {}", basicJournalInput.getJournalDate(),basicJournalInput.getDescription(), basicJournalInput.getAccounts(), amount);

		ledgerServices.addLedgerEntry(basicJournalInput.getJournalDate(),
				basicJournalInput.getDescription(),
				amount,
				basicJournalInput.getAccounts().getFirst(),
				basicJournalInput.getAccounts().getLast());
	}

	public void addNewTransaction(DistributionTypeInput distributionInputJournal) {
		log.info("New Distribution Journal Request {} {} {} {}", distributionInputJournal.getDistributionType(), distributionInputJournal.getJournalDate(), distributionInputJournal.getAccounts().getFirst(), distributionInputJournal.getAmounts().getFirst());

		Money amount = new Money(distributionInputJournal.getAmounts().getFirst() != null ? distributionInputJournal.getAmounts().getFirst() : distributionInputJournal.getAmounts().getLast());
		String shareAccount = distributionInputJournal.getAccounts().getFirst();
		String description;
		String code = shareAccount.substring(shareAccount.lastIndexOf(":") + 1);
		String sourceAccount;
		String destinationAccount = distributionInputJournal.getAccounts().getLast();

		switch (distributionInputJournal.getDistributionType()) {
			case "distribution" -> {
				description = "Distribution from " + code;
				sourceAccount = DISTRIBUTION_INCOME;
			}
			case "capital-gain" -> {
				description = "Capital Gain from " + code;
				sourceAccount = CAPITAL_GAINS_INCOME;
			}
			case "capital-return" -> {
				description = "Capital Return from " + code;
				sourceAccount = CAPITAL_RETURN_INCOME;
			}
			default ->
				throw new IllegalStateException("Unexpected value: " + distributionInputJournal.getDistributionType());
		}

		log.info("New Distribution Journal Generated {} {} {} {}", distributionInputJournal.getJournalDate(), description, sourceAccount, destinationAccount);

		ledgerServices.addLedgerEntry(distributionInputJournal.getJournalDate(),
				description,
				amount,
				sourceAccount,
				destinationAccount);
	}

	public void addNewTransaction(DividendJournalInput dividendInputJournal) {
		log.info("New Dividend Journal Request {}", dividendInputJournal);
		String shareAccount = dividendInputJournal.getAccounts().getFirst();
		String destinationAccount = dividendInputJournal.getAccounts().get(1);
		String imputationAccount =  dividendInputJournal.getAccounts().getLast();

		BigDecimal frankedAmount = dividendInputJournal.getAmounts().getFirst();
		BigDecimal unfrankedAmount = dividendInputJournal.getAmounts().get(1);
		BigDecimal imputationAmount = dividendInputJournal.getAmounts().getLast();
		BigDecimal cashAmount = frankedAmount.add(unfrankedAmount);

		String code = shareAccount.substring(shareAccount.lastIndexOf(":") + 1);
		String description = (unfrankedAmount.compareTo(BigDecimal.ZERO) == 0 ? "Franked " : "") + "Dividend from " + code;
		String imputationNote = "Tax Credit from " + code;

		// If there is a franked amount then there must be an imputation amount
		if (imputationAmount.compareTo(BigDecimal.ZERO) == 0 && frankedAmount.compareTo(BigDecimal.ZERO) != 0) {
			throw new ValidationException("Franked Amount with no Franking Credit");
		}

		// If there is no franked amount then there can be no imputation credit
		if (frankedAmount.compareTo(BigDecimal.ZERO) == 0 && imputationAmount.compareTo(BigDecimal.ZERO) != 0) {
			throw new ValidationException("Franking Credit with no Franked Amount");
		}

		// Post the income
		List<IPosting> postings = new ArrayList<>();

		postings.add(new Posting(destinationAccount, cashAmount));
		if (imputationAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(imputationAccount, imputationAmount, imputationNote));
		}
		if (frankedAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(FRANKED_DIVIDEND, frankedAmount.negate(), "Franked Dividend from " + code));
			postings.add(new Posting(TAX_CREDIT, imputationAmount.negate(), imputationNote));
		}
		if (unfrankedAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(UNFRANKED_DIVIDEND, unfrankedAmount.multiply(BigDecimal.valueOf(-1)),"Unfranked Dividend from " + code));
		}
		ledgerServices.addLedgerEntry(dividendInputJournal.getJournalDate(),
				description,
				postings);
	}

	public void saveData() {
		ledgerServices.saveLedger();
		schedulesServices.saveSchedules();
	}

	public void skipSchedule(Schedule schedule) {
		schedulesServices.scheduleRollForward(schedule);
		schedulesServices.saveSchedules();
	}

	public void postSchedule(Schedule schedule, List<BigDecimal> amountOverrides) {
		if (amountOverrides.isEmpty()) {
			log.error("No amount overrides provided");
			throw new ValidationException("No amount overrides provided");
		} else {
			if (amountOverrides.size() != schedule.ledgerEntry.getPostings().size()) {
				log.error("Override amounts counts do not match postings");
				throw new ValidationException("Override amounts counts do not match postings");
			} else {
				LocalDate today = LocalDate.now();
				LedgerEntry newLedgerEntry = new LedgerEntry(today, schedule.ledgerEntry.getDescription(), new ArrayList<>());
				log.info("Posting Scheduled Journal @ {} {}", newLedgerEntry.getDate(), newLedgerEntry.getDescription());

				schedule.ledgerEntry.getPostings().forEach(posting -> {
					newLedgerEntry.getPostings().add(new Posting(posting, new Money(amountOverrides.get(schedule.ledgerEntry.getPostings().indexOf(posting)))));
					log.info("  {} {}", newLedgerEntry.getLastPosting().getAccount(), newLedgerEntry.getLastPosting().getAmount());
				});

				if (schedule.isValid()) {
					schedulesServices.scheduleRollForward(schedule);
					schedulesServices.saveSchedules();
					ledgerServices.addLedgerEntry(newLedgerEntry);
				} else {
					log.error("Schedule did not validate");
					throw new ValidationException("Schedule did not validate " + schedule.isValid());
				}
			}
		}
	}

	public void reloadFiles() throws IOException {
		startupServices.loadFiles();
	}

	public void reloadCommodities() throws IOException {
		sharePriceServices.reloadCommodities();
	}
}

package com.hz.mymoney.ui.services;

import com.hz.mymoney.data.models.internal.Schedule;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import com.hz.mymoney.data.services.LedgerServices;
import com.hz.mymoney.data.services.SchedulesServices;
import com.hz.mymoney.data.services.SharePriceServices;
import com.hz.mymoney.exceptions.ValidationException;
import com.hz.mymoney.ui.models.inputs.BasicJournalInput;
import com.hz.mymoney.ui.models.inputs.DistributionTypeInput;
import com.hz.mymoney.ui.models.inputs.DividendJournalInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.hz.mymoney.configuration.AccountConstants.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class UIDataUpdateService {
	private final LedgerServices ledgerServices;
	private final SchedulesServices schedulesServices;
	private final SharePriceServices sharePriceServices;

	public void addNewTransaction(BasicJournalInput basicJournalInput) {
		BigDecimal amount = basicJournalInput.getAmounts().getFirst() != null ? basicJournalInput.getAmounts().getFirst() : basicJournalInput.getAmounts().getLast();

		log.info("New Simple Journal {} {} {}", basicJournalInput.getJournalDate(), basicJournalInput.getAccounts(), amount);

		Ledger ledger = ledgerServices.getLedger();
		ledger.add(basicJournalInput.getJournalDate(),
				basicJournalInput.getDescription(),
				amount,
				basicJournalInput.getAccounts().getFirst(),
				basicJournalInput.getAccounts().getLast());
		ledgerServices.makeChartOfAccounts(ledger);
		ledgerServices.saveLedger();
	}

	public void addNewTransaction(DistributionTypeInput distributionInputJournal) {
		log.info("New Distribution Journal Request {} {} {} {}", distributionInputJournal.getDistributionType(), distributionInputJournal.getJournalDate(), distributionInputJournal.getAccounts().getFirst(), distributionInputJournal.getAmounts().getFirst());
		Ledger ledger = ledgerServices.getLedger();

		BigDecimal amount = distributionInputJournal.getAmounts().getFirst() != null ? distributionInputJournal.getAmounts().getFirst() : distributionInputJournal.getAmounts().getLast();
		String shareAccount = distributionInputJournal.getAccounts().getFirst();
		String description = "";
		String code = shareAccount.substring(shareAccount.lastIndexOf(":") + 1);
		String sourceAccount = "";
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

		ledger.add(distributionInputJournal.getJournalDate(),
				description,
				amount,
				sourceAccount,
				destinationAccount);
		ledgerServices.makeChartOfAccounts(ledger);
		ledgerServices.saveLedger();
	}

	public void addNewTransaction(DividendJournalInput dividendInputJournal) {
		log.info("New Dividend Journal Request {}", dividendInputJournal);
		Ledger ledger = ledgerServices.getLedger();
		String shareAccount = dividendInputJournal.getAccounts().getFirst();
		String brokerAccount = dividendInputJournal.getAccounts().get(1);
		String destinationAccount = dividendInputJournal.getAccounts().getLast();

		BigDecimal frankedAmount = dividendInputJournal.getAmounts().getFirst();
		BigDecimal unfrankedAmount = dividendInputJournal.getAmounts().get(1);
		BigDecimal imputationAmount = dividendInputJournal.getAmounts().getLast();
		BigDecimal totalAmount = frankedAmount.add(unfrankedAmount).add(imputationAmount);

		String code = shareAccount.substring(shareAccount.lastIndexOf(":") + 1);
		String description = "Dividend from " + code;

		// If there is a franked amount then there must be an imputation amount
		if (imputationAmount.compareTo(BigDecimal.ZERO) == 0 && frankedAmount.compareTo(BigDecimal.ZERO) != 0) {
			throw new ValidationException("Franked Amount with no Imputation Credit");
		}

		// If there is no franked amount then there can be no imputation credit
		if (frankedAmount.compareTo(BigDecimal.ZERO) == 0 && imputationAmount.compareTo(BigDecimal.ZERO) != 0) {
			throw new ValidationException("Imputation Credit with no Franked Amount");
		}

		// Post the income
		List<IPosting> postings = new ArrayList<>();
		postings.add(new Posting(brokerAccount, totalAmount));
		if (frankedAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(FRANKED_DIVIDEND, frankedAmount.multiply(BigDecimal.valueOf(-1))));
		}
		if (unfrankedAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(UNFRANKED_DIVIDEND, unfrankedAmount.multiply(BigDecimal.valueOf(-1))));
		}
		if (imputationAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(IMPUTATION_INCOME, imputationAmount.multiply(BigDecimal.valueOf(-1)), "Imputation Credit from " + code));
		}
		ledger.add(dividendInputJournal.getJournalDate(),
				description,
				postings);

		// Then disburse to the accounts
		postings = new ArrayList<>();
		postings.add(new Posting(brokerAccount, totalAmount.multiply(BigDecimal.valueOf(-1))));
		postings.add(new Posting(destinationAccount, frankedAmount.add(unfrankedAmount)));
		if (imputationAmount.compareTo(BigDecimal.ZERO) > 0) {
			postings.add(new Posting(IMPUTATION_ACCOUNT, imputationAmount, code));
		}
		ledger.add(dividendInputJournal.getJournalDate(),
				description,
				postings);

		ledgerServices.makeChartOfAccounts(ledger);
		ledgerServices.saveLedger();
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
				log.info("Posting Scheduled Journal");
				schedule.ledgerEntry.getPostings().forEach(posting -> {
					posting.setAmount(amountOverrides.get(schedule.ledgerEntry.getPostings().indexOf(posting)));
					log.info("  {} {} {}", schedule.ledgerEntry.getDate(), posting.getAccount(), posting.getAmount());
				});

				if (schedule.isValid()) {
					Ledger ledger = ledgerServices.getLedger();
					// Need to clone the ledger entry
					ledger.add(new LedgerEntry(schedule.ledgerEntry.getDate(), schedule.ledgerEntry.getDescription(), schedule.ledgerEntry.getPostings()));
					schedulesServices.scheduleRollForward(schedule);

					ledgerServices.makeChartOfAccounts(ledger);
					this.saveData();
				} else {
					log.error("Schedule did not validate");
					throw new ValidationException("Schedule did not validate " + schedule.isValid());
				}
			}
		}
	}

	public void reloadFiles() throws IOException {
		sharePriceServices.reloadCommodities();
		schedulesServices.reloadSchedules();
		ledgerServices.reloadLedger();
	}
}

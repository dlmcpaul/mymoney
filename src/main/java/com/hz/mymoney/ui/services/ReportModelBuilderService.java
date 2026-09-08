package com.hz.mymoney.ui.services;

import com.hz.mymoney.configuration.AccountConstants;
import com.hz.mymoney.data.models.coa.ChartOfAccounts;
import com.hz.mymoney.data.models.coa.Movement;
import com.hz.mymoney.ui.models.Transaction;
import com.hz.mymoney.ui.models.templates.TaxYear;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static com.hz.mymoney.configuration.AccountConstants.SUPER_CONTRIBUTION_NOTE;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReportModelBuilderService {

	public TaxYear createTaxYear(LocalDate financialYearStartDate, ChartOfAccounts coa) {
		LocalDate financialYearEndDate = financialYearStartDate.plusYears(1).minusDays(1);

		List<Transaction> incomeTransactions = coa.getAccountsOfType("Income:", true).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(financialYearStartDate, financialYearEndDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.toList();

		List<Transaction> expenseTransactions = coa.getAccountsOfType("Expenses:", true).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(financialYearStartDate, financialYearEndDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.toList();

		List<Transaction> imputationTransactions = coa.getAccountsOfType(AccountConstants.IMPUTATION_ACCOUNT, false).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(financialYearStartDate, financialYearEndDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.filter(transaction -> transaction.amount().compareTo(BigDecimal.ZERO) >= 0)
				.toList();

		List<Transaction> supercontribTransactions = coa.getAccountsOfType(AccountConstants.SUPER_ACCOUNTS, false).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(financialYearStartDate, financialYearEndDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.filter(transaction -> transaction.amount().compareTo(BigDecimal.ZERO) >= 0)
				.filter(transaction -> transaction.description().equalsIgnoreCase(SUPER_CONTRIBUTION_NOTE))
				.toList();

		// PAYG Payments for the Tax Year are offset by 3 months
		LocalDate paygStartDate = financialYearStartDate.plusMonths(3);
		LocalDate paygEndDate = financialYearEndDate.plusMonths(3);

		List<Transaction> paygTransactions = coa.getAccountsOfType(AccountConstants.PAYG_DEDUCTIONS, false).stream()
				.filter(account -> account.hasMovementBetween(paygStartDate, paygEndDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(paygStartDate, paygEndDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.filter(transaction -> transaction.amount().compareTo(BigDecimal.ZERO) >= 0)
				.toList();

		return new TaxYear(incomeTransactions, expenseTransactions, imputationTransactions, paygTransactions, supercontribTransactions);
	}

	public List<Transaction> mapMovementsToTransactions(PriorityQueue<Movement> movements, String destinationAccount, boolean isShareAccount) {
		return movements.stream()
				.map(movement -> new Transaction(movement, destinationAccount, isShareAccount))
				.sorted(Comparator.comparing(Transaction::asAt))
				.toList();
	}

}

package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.ui.models.Transaction;
import lombok.Data;

import java.util.Comparator;
import java.util.List;

@Data
public class TaxTemplateData {
	public final TaxYear currentYear;
	public final TaxYear lastYear;

	public TaxTemplateData(TaxYear currentYear, TaxYear lastYear) {
		this.currentYear = currentYear;
		this.lastYear = lastYear;
	}

	public List<Transaction> getTransactionsFor(String filter, TaxYear taxYear) {
		List<Transaction> transactions = switch (filter) {
			case "Income" -> taxYear.getEarnedIncomeTransactions();
			case "SalaryTaxPaid" -> taxYear.getSalaryTaxTransactions();
			case "PAYGTaxPaid" -> taxYear.getPAYGTransactions();
			case "TaxDeduction" -> taxYear.getTaxDeductionTransactions();
			case "Donations" -> taxYear.getDonationTransactions();
			case "Investment" -> taxYear.getInvestmentIncomeTransactions();
			case "FrankingCredits" -> taxYear.getImputationTransactions();
			default -> throw new IllegalStateException("Unexpected value: " + filter);
		};

		transactions.sort(Comparator.comparing(Transaction::asAt));
		return transactions;
	}

}

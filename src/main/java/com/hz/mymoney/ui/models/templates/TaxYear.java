package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.Transaction;
import lombok.Getter;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.hz.mymoney.configuration.AccountConstants.*;

public class TaxYear {

	private final List<Transaction> incomeTransactions = new ArrayList<>();
	private final List<Transaction> expenseTransactions = new ArrayList<>();
	private final List<Transaction> paygTransactions = new ArrayList<>();
	private final List<Transaction> supercontribTransactions = new ArrayList<>();
	@Getter
	private final List<Transaction> imputationTransactions = new ArrayList<>();

	public TaxYear(List<Transaction> incomeTransactions, List<Transaction> expenseTransactions, List<Transaction> imputationTransactions, List<Transaction> paygTransactions, List<Transaction> supercontribTransactions) {
		this.incomeTransactions.addAll(incomeTransactions);
		this.expenseTransactions.addAll(expenseTransactions);
		this.imputationTransactions.addAll(imputationTransactions);
		this.paygTransactions.addAll(paygTransactions);
		this.supercontribTransactions.addAll(supercontribTransactions);
	}

	// Income = Salary Less Super
	public Money getEarnedIncome() {
		return sum(this.getEarnedIncomeTransactions()).abs()
				.subtract(sum(this.supercontribTransactions).abs());
	}

	// Investment income is dividends and capitals gains but also franking credits and Interest earned
	public Money getInvestmentIncomeAmount() {
		return sum(this.getInvestmentIncomeTransactions()).abs().add(this.getImputationCreditAmount());
	}

	public Money getTaxDeductionsAmount() {
		return sum(this.getTaxDeductionTransactions()).abs();
	}

	public Money getDonationsAmount() {
		return sum(this.getDonationTransactions()).abs();
	}

	public Money getSalaryTaxPaidAmount() {
		return this.sum(this.getSalaryTaxTransactions()).abs();
	}

	public Money getPAYGPaidAmount() {
		return this.sum(this.paygTransactions).abs();
	}

	// This applies to both sides of the tax equation (income and tax paid)
	public Money getImputationCreditAmount() {
		return sum(imputationTransactions).abs();
	}

	public Money getSuperContribAmount() {
		return sum(supercontribTransactions).abs();
	}

	private Money sum(List<Transaction> transactions) {
		return transactions.stream()
				.map(Transaction::amount)
				.reduce(Money.ZERO, Money::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private List<Transaction> filter(List<Transaction> list, String account) {
		return list.stream()
				.filter(income -> income.destinationAccount().toLowerCase().startsWith(account.toLowerCase()))
				.toList();
	}

	List<Transaction> getEarnedIncomeTransactions() {
		return new ArrayList<>(this.filter(incomeTransactions, SALARY_INCOME));
	}

	List<Transaction> getInvestmentIncomeTransactions() {
		List<Transaction> result = new ArrayList<>();

		result.addAll(this.filter(incomeTransactions, INTEREST_INCOME));
		result.addAll(this.filter(incomeTransactions, INVESTMENT_INCOME));

//		result.addAll(this.filter(incomeTransactions, DIVIDEND_INCOME));
//		result.addAll(this.filter(incomeTransactions, CAPITAL_GAINS_INCOME));
//		result.addAll(this.filter(incomeTransactions, CAPITAL_RETURN_INCOME));
//		result.addAll(this.filter(incomeTransactions, DISTRIBUTION_INCOME));

		return result;
	}

	List<Transaction> getTaxDeductionTransactions() {
		return new ArrayList<>(this.filter(expenseTransactions, TAX_DEDUCTIONS));
	}

	List<Transaction> getDonationTransactions() {
		return new ArrayList<>(this.filter(expenseTransactions, DONATIONS));
	}

	List<Transaction> getSalaryTaxTransactions() {
		return new ArrayList<>(this.filter(expenseTransactions, EMPLOYMENT_TAXES));
	}

	List<Transaction> getPAYGTransactions() {
		return this.paygTransactions;
	}

}
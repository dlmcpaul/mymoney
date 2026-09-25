package com.hz.mymoney.configuration;

// Account names used that should be extracted as configuration options
public class AccountConstants {
	private AccountConstants() {}

	// Taxes
	public static final String EMPLOYMENT_TAXES = "Expenses:Employment:Taxes";

	// Income
	public static final String INCOME_PREFIX = "Income:";
	public static final String SALARY_INCOME = "Income:Employment:Salary";
	public static final String INTEREST_INCOME = "Income:Cash:Int Paid";
	public static final String INVESTMENT_INCOME = "Income:Investment:";

	public static final String DIVIDEND_INCOME = INVESTMENT_INCOME + "Dividends";
	public static final String FRANKED_DIVIDEND = DIVIDEND_INCOME + ":Franked";
	public static final String UNFRANKED_DIVIDEND = DIVIDEND_INCOME + ":Unfranked";
	public static final String TAX_CREDIT = INVESTMENT_INCOME + "Tax Credit";
	public static final String DISTRIBUTION_INCOME = "Income:Investment:Distribution";
	public static final String REINVESTMENT_INCOME = "Income:Investment:Reinvestment";
	public static final String CAPITAL_GAINS_INCOME = "Income:Investment:Capital Gains";
	public static final String CAPITAL_LOSSES_INCOME = "Income:Investment:Capital Losses";
	public static final String CAPITAL_RETURN_INCOME = "Income:Investment:Capital Return";
	public static final String INCOME_OTHER = "Income:Investment:Miscellaneous Income";

	// Expenses
	public static final String DONATIONS = "Expenses:Cash:Donations";
	public static final String TAX_DEDUCTIONS = "Expenses:Tax";
	public static final String PAYG_DEDUCTIONS = "Expenses:Cash:Tax";
	public static final String COMMISSION = "Expenses:Broker:Commission";

	// Super
	public static final String SUPER_ACCOUNTS = "Assets:Superannuation";     // Where Super accounts are tracked
	public static final String SUPER_RETURNS = "Income:Superannuation:Returns";
	public static final String SUPER_LOSSES = "Expenses:Superannuation:Losses";
	public static final String SUPER_TAXES = "Expenses:Superannuation:Taxes";
	public static final String SUPER_FEES = "Expenses:Superannuation:Fees";
	public static final String SUPER_INSURANCE = "Expenses:Superannuation:Insurance";

	// Investment Assets
	public static final String SHARE_ACCOUNTS = "Assets:Shares:";       // Where shares are tracked
	public static final String STOCKBROKER_ACCOUNTS = "Assets:Broker:"; // Where stockbroker accounts are tracked
	public static final String FUND_ACCOUNTS = "Assets:Fund:";          // Where Fund accounts are tracked
	public static final String IMPUTATION_ACCOUNT = "Assets:Tax Credit:Imputation Credit";

	// Opening Balances locations
	public static final String EQUITY_FUND_OPENING_BALANCE = "Equity:Opening Balances:Fund";
	public static final String EQUITY_SUPER_OPENING_BALANCE = "Equity:Opening Balances:Superannuation"; // Where to find super opening balances

	// Magic Strings
	public static final String SUPER_CONTRIBUTION_NOTE = "Super Contribution Transfer";  // To identify Super Contributions
	public static final String OPENING_BALANCE_DESCRIPTION = "Opening Balance";
	public static final String SHARES_SOLD_DESCRIPTION = "Shares Sold from";
}
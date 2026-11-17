package com.hz.mymoney.configuration;

// Account names used that should be extracted as configuration options
public class AccountConstants {
	private AccountConstants() {}

	// Standard prefixes
	public static final String INCOME_PREFIX = "Income:";
	public static final String EXPENSES_PREFIX = "Expenses:";
	public static final String ASSETS_PREFIX = "Assets:";
	public static final String LIABILITIES_PREFIX = "Liabilities:";
	public static final String EQUITY_PREFIX = "Equity:";

	// Income
	public static final String SALARY_INCOME = INCOME_PREFIX + "Employment:Salary";
	public static final String INTEREST_INCOME = INCOME_PREFIX +"Cash:Int Paid";
	public static final String INVESTMENT_INCOME = INCOME_PREFIX +"Investment:";

	public static final String DIVIDEND_INCOME = INVESTMENT_INCOME + "Dividends";
	public static final String FRANKED_DIVIDEND = DIVIDEND_INCOME + ":Franked";
	public static final String UNFRANKED_DIVIDEND = DIVIDEND_INCOME + ":Unfranked";
	public static final String TAX_CREDIT = INCOME_PREFIX + "Imputation:Tax Credit";
	public static final String DISTRIBUTION_INCOME = INCOME_PREFIX + "Investment:Distribution";
	public static final String REINVESTMENT_INCOME = INCOME_PREFIX + "Investment:Reinvestment";
	public static final String CAPITAL_GAINS_INCOME = INCOME_PREFIX + "Investment:Capital Gains";
	public static final String CAPITAL_LOSSES_INCOME = INCOME_PREFIX + "Investment:Capital Losses";
	public static final String CAPITAL_RETURN_INCOME = INCOME_PREFIX + "Investment:Capital Return";
	public static final String INCOME_OTHER = INCOME_PREFIX + "Investment:Miscellaneous Income";

	// Taxes
	public static final String EMPLOYMENT_TAXES = EXPENSES_PREFIX + "Employment:Taxes";
	public static final String DONATIONS = EXPENSES_PREFIX + "Cash:Donations";
	public static final String TAX_DEDUCTIONS = EXPENSES_PREFIX + "Tax";
	public static final String PAYG_DEDUCTIONS = EXPENSES_PREFIX + "Cash:Tax";

	// Super
	public static final String SUPER_ACCOUNTS = "Assets:Superannuation";     // Where Super accounts are tracked
	public static final String SUPER_RETURNS = INCOME_PREFIX + "Superannuation:Returns";
	public static final String SUPER_LOSSES = EXPENSES_PREFIX + "Superannuation:Losses";
	public static final String SUPER_TAXES = EXPENSES_PREFIX + "Superannuation:Taxes";
	public static final String SUPER_FEES = EXPENSES_PREFIX + "Superannuation:Fees";
	public static final String SUPER_INSURANCE = EXPENSES_PREFIX + "Superannuation:Insurance";

	// Investment Assets
	public static final String SHARE_ACCOUNTS = ASSETS_PREFIX + "Shares:";       // Where shares are tracked
	public static final String STOCKBROKER_ACCOUNTS = ASSETS_PREFIX + "Broker:"; // Where stockbroker accounts are tracked
	public static final String FUND_ACCOUNTS = ASSETS_PREFIX + "Fund:";          // Where Fund accounts are tracked
	public static final String IMPUTATION_ACCOUNT = ASSETS_PREFIX + "Tax:Franking Credit";
	public static final String COMMISSION = EXPENSES_PREFIX + "Broker:Commission";

	// Opening Balances locations
	public static final String EQUITY_FUND_OPENING_BALANCE = "Equity:Opening Balances:Fund";
	public static final String EQUITY_SUPER_OPENING_BALANCE = "Equity:Opening Balances:Superannuation"; // Where to find super opening balances

	// Scheduled Reporting
	public static final String INCOMING = INCOME_PREFIX;
	public static final String OUTGOING = EXPENSES_PREFIX;

	// Magic Strings
	public static final String SUPER_CONTRIBUTION_NOTE = "Super Contribution Transfer";  // Identifies the transfer is a Super Contribution
	public static final String OPENING_BALANCE_DESCRIPTION = "Opening Balance";
	public static final String SHARES_SOLD_DESCRIPTION = "Shares Sold from";
}
package com.hz.mymoney.ui.services;

import com.hz.mymoney.configuration.AccountConstants;
import com.hz.mymoney.data.models.coa.ChartOfAccounts;
import com.hz.mymoney.data.models.coa.Movement;
import com.hz.mymoney.data.models.coa.Schedule;
import com.hz.mymoney.data.services.LedgerServices;
import com.hz.mymoney.data.services.SchedulesServices;
import com.hz.mymoney.data.services.SharePriceServices;
import com.hz.mymoney.ui.models.*;
import com.hz.mymoney.ui.models.charts.YearlyIncomeExpense;
import com.hz.mymoney.ui.models.templates.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.hz.mymoney.configuration.AccountConstants.*;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Log4j2
public class ModelBuilderService {
	private final LedgerServices dataLoaderService;
	private final SharePriceServices shareValueService;
	private final SchedulesServices schedulesLoaderService;
	private final ReportModelBuilderService reportModelBuilderService;

	public boolean isLedgerReadOnly() {
		return dataLoaderService.getLedger().isReadOnly();
	}

	public TrendsTemplateData createTrendsTemplateData(String accountName, String trendPeriod) {
		ChartOfAccounts coa = dataLoaderService.getCoa();

		var account = coa.findAccount(accountName);

		if (account.isPresent() && accountName.endsWith(":") == false) {
			return new TrendsTemplateData(accountName, trendPeriod, account.get().getMovementsAsList());
		}

		if (accountName.isEmpty()) {
			return new TrendsTemplateData(accountName, trendPeriod, new ArrayList<>());
		}

		// Merge all the accounts found into single set of movements
		List<Movement> allMovements = coa.getAccountsOfType(accountName, true)
				.stream()
				.map(com.hz.mymoney.data.models.coa.Account::getMovementsAsList)
				.flatMap(Collection::stream)
				.toList();

		return new TrendsTemplateData(accountName, trendPeriod, allMovements);
	}

	public List<ScheduledTransaction> getScheduledTransactions() {
		List<Schedule> schedules = schedulesLoaderService.getScheduledTransactions();
		return schedules.stream()
				.map(this::createScheduledTransaction).toList();
	}

	private ScheduledTransaction createScheduledTransaction(Schedule schedule) {
		List<Journal> journals = schedule.ledgerEntry.getPostings()
				.stream()
				.map(p -> new Journal(p.getNote() == null ? schedule.ledgerEntry.getDescription() : p.getNote(), p.getAccount(), p.getAmount()))
				.toList();
		return new ScheduledTransaction(schedule.ledgerEntry.getDescription(),
				schedule.ledgerEntry.getFirstPosting().isIncomePosting(),
				schedule.ledgerEntry.getDate(),
				schedule.ledgerEntry.getFirstAmount().abs(),
				schedule.recurrenceAmount(),
				schedule.recurrenceType(),
				journals);
	}

	public NetAssetLiabilityPosition createCurrentPosition() {
		return this.createNetAssetLiabilityPosition(dataLoaderService.getCoa(), true);
	}

	public List<String> searchAccounts(String searchText) {
		ChartOfAccounts coa = dataLoaderService.getCoa();

		return coa.getAccountsOfType(searchText, false)
				.stream()
				.map(com.hz.mymoney.data.models.coa.Account::getName)
				.toList();
	}

	public MonthlyIncomeExpense createMonthlyIncomeExpense(LocalDate asAt) {
		return this.createMonthlyIncomeExpense(asAt, asAt.with(TemporalAdjusters.lastDayOfMonth()));
	}

	public MonthlyIncomeExpense createMonthlyIncomeExpense(LocalDate startDate, LocalDate endDate) {
		ChartOfAccounts coa = dataLoaderService.getCoa();

		List<SummedTransaction> monthlyTransactions = new ArrayList<>();
		List<Transaction> incomeTransactions = coa.getAccountsOfType("Income", true).stream()
				.filter(account -> account.hasMovementBetween(startDate, endDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(startDate, endDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.toList();
		List<Transaction> expensesTransactions = coa.getAccountsOfType("Expenses", true).stream()
				.filter(account -> account.hasMovementBetween(startDate, endDate))
				.map(account -> mapMovementsToTransactions(account.getMovementsBetween(startDate, endDate), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.toList();

		BigDecimal income = incomeTransactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add).abs();
		BigDecimal expenses = expensesTransactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add).multiply(BigDecimal.valueOf(-1));

		monthlyTransactions.addAll(incomeTransactions.stream()
				.collect(groupingBy(Transaction::description, Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)))
				.entrySet().stream()
				.map(stringBigDecimalEntry -> new SummedTransaction(stringBigDecimalEntry.getKey(), stringBigDecimalEntry.getValue().abs()))
				.sorted((o1, o2) -> o2.amount().compareTo(o1.amount()))
				.toList());
		monthlyTransactions.addAll(expensesTransactions.stream()
				.collect(groupingBy(Transaction::description, Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)))
				.entrySet().stream()
				.map(entry -> new SummedTransaction(entry.getKey(), entry.getValue().multiply(BigDecimal.valueOf(-1))))
				.sorted(Comparator.comparing(SummedTransaction::amount))
				.toList());

		return new MonthlyIncomeExpense(startDate, income, expenses, monthlyTransactions);
	}

	// Current day coa then the coa at the start of the month going back to the start of data
	public NetWorthTemplateData createNetWorthTemplateData() {
		List<NetAssetLiabilityPosition> netAssetLiabilityPositions = new ArrayList<>(250);

		ChartOfAccounts coa = dataLoaderService.getCoa();
		while (coa.totalAccounts() > 0) {
			try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
				ChartOfAccounts finalCoa = coa;
				executor.submit(() -> netAssetLiabilityPositions.add(createNetAssetLiabilityPosition(finalCoa, false)));
				coa = executor.submit(() -> new ChartOfAccounts(finalCoa.getAsAt().withDayOfMonth(1).minusMonths(1), finalCoa)).get();
			} catch (ExecutionException | InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
		netAssetLiabilityPositions.add(createNetAssetLiabilityPosition(coa, false));

		return new NetWorthTemplateData(netAssetLiabilityPositions);
	}

	public List<Transaction> getInvestmentTransactionsFor(String code) {
		ChartOfAccounts coa = dataLoaderService.getCoa();
		List<Transaction> investmentTransactions = new ArrayList<>();

		investmentTransactions .addAll(coa.getAccountsOfType(AccountConstants.SHARE_ACCOUNTS, false).stream()
				.filter(account -> account.getCode().equals(code))
				.map(account -> mapMovementsToTransactions(account.getMovementsForCode(code), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.toList());

		investmentTransactions.addAll(coa.getAccountsOfType(AccountConstants.FUND_ACCOUNTS, false).stream()
				.filter(account -> account.getCode().equals(code))
				.map(account -> mapMovementsToTransactions(account.getMovementsForCode(code), account.getName(), account.isShareAccount()))
				.flatMap(List::stream)
				.toList());

		investmentTransactions.addAll(coa.getInvestmentMovementsForCode(code).stream()
				.map(m -> new Transaction(m.date(), m.description(), m.amount().multiply(BigDecimal.valueOf(-1)), "", "", false, BigDecimal.ZERO))
						.toList());

		investmentTransactions.sort(Comparator.comparing(Transaction::asAt));

		return investmentTransactions;
	}

	public List<Transaction> mapMovementsToTransactions(PriorityQueue<Movement> movements, String destinationAccount, boolean isShareAccount) {
		return movements.stream()
				.map(movement -> new Transaction(movement, destinationAccount, isShareAccount))
				.sorted(Comparator.comparing(Transaction::asAt))
				.toList();
	}

	public InvestmentsTemplateData createCurrentInvestmentsTemplateData() {
		InvestmentsTemplateData shareTemplateData = new InvestmentsTemplateData("Current");
		ChartOfAccounts coa = dataLoaderService.getCoa();
		shareTemplateData.investmentSummaries.addAll(coa.getAccountsOfType(AccountConstants.SHARE_ACCOUNTS, true).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		shareTemplateData.investmentSummaries.addAll(coa.getAccountsOfType(AccountConstants.FUND_ACCOUNTS, true).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		return shareTemplateData;
	}

	public InvestmentsTemplateData createPriorInvestmentsTemplateData() {
		InvestmentsTemplateData shareTemplateData = new InvestmentsTemplateData("Historical");
		ChartOfAccounts coa = dataLoaderService.getCoa();
		shareTemplateData.investmentSummaries.addAll(coa.getZeroBalanceAccountsOfType(AccountConstants.SHARE_ACCOUNTS).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		shareTemplateData.investmentSummaries.addAll(coa.getZeroBalanceAccountsOfType(AccountConstants.FUND_ACCOUNTS).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		return shareTemplateData;
	}

	public AllAccountsTemplateData createAccountsTemplateData(LocalDate fyStart, LocalDate fyEnd) {
		AllAccountsTemplateData accountsTemplateData = new AllAccountsTemplateData(fyStart);

		ChartOfAccounts chartOfAccounts = new ChartOfAccounts(fyEnd, dataLoaderService.getCoa());

		// These are as at the current date
		chartOfAccounts.getAccountsOfType("Assets", true)
				.forEach(account -> accountsTemplateData.assetAccounts.add(mapAccount(account, chartOfAccounts.getAsAt(), true)));

		chartOfAccounts.getAccountsOfType("Liabilities", true)
				.forEach(account -> accountsTemplateData.liabilityAccounts.add(mapAccount(account, chartOfAccounts.getAsAt(), true)));

		// These need to be truncated prior to financial year provided
		chartOfAccounts.getAccountsOfType("Income", true).stream()
				.filter(account -> account.hasMovementBetween(accountsTemplateData.financialYear, chartOfAccounts.getAsAt()))
				.forEach(account -> accountsTemplateData.incomeAccounts.add(buildFilteredAccount(account, accountsTemplateData.financialYear, chartOfAccounts.getAsAt())));

		chartOfAccounts.getAccountsOfType("Expenses", true).stream()
				.filter(account -> account.hasMovementBetween(accountsTemplateData.financialYear, chartOfAccounts.getAsAt()))
				.forEach(account -> accountsTemplateData.expenseAccounts.add(buildFilteredAccount(account, accountsTemplateData.financialYear, chartOfAccounts.getAsAt())));

		return accountsTemplateData;
	}

	public AllAccountsTemplateData createAccountsTemplateData() {
		return this.createAccountsTemplateData(calculateStartOfFinancialYear(LocalDate.now()), LocalDate.now());
	}

	public SingleAccountTemplateData createSingleAccountData(String accountName, LocalDate financialYearStart) {
		// Assets and Liabilities we want transactions from earliest to financial year-end
		// Income and Expenses we want transactions from financial year start to year-end
		boolean allTransactions = financialYearStart == null;

		financialYearStart = financialYearStart == null ? calculateStartOfFinancialYear(LocalDate.now()) : financialYearStart;
		LocalDate financialYearEnd = financialYearStart.plusYears(1).minusDays(1);

		ChartOfAccounts chartOfAccounts = new ChartOfAccounts(financialYearEnd, dataLoaderService.getCoa());

		if (chartOfAccounts.findAccount(accountName).isEmpty()) {
			throw new IllegalArgumentException("Account " + accountName + " does not exist");
		}

		// Get the account and all it's transactions
		var acc = chartOfAccounts.getAccountsOfType(accountName, false).stream()
				.filter(a -> a.getName().equals(accountName))
				.findFirst()
				.orElseThrow();

		List<Transaction> transactionsBetween = mapMovementsToTransactions(acc.getMovementsBetween(financialYearStart, financialYearEnd), acc.getName(), acc.isShareAccount());
		List<Transaction> transactionsToYearEnd = mapMovementsToTransactions(acc.getMovements(financialYearEnd), acc.getName(), acc.isShareAccount());

		return switch (accountName.substring(0, accountName.indexOf(":")).toLowerCase()) {
			case "assets", "liabilities", "equity" -> new SingleAccountTemplateData(accountName, mapAccount(acc, transactionsToYearEnd, chartOfAccounts.getAsAt()), allTransactions ? transactionsToYearEnd : transactionsBetween, financialYearStart, allTransactions);
			case "income", "expenses" -> new SingleAccountTemplateData(accountName, mapAccount(acc, transactionsBetween, chartOfAccounts.getAsAt()), transactionsBetween, financialYearStart, allTransactions);
			default ->
				throw new IllegalStateException("Unexpected value: " + accountName.substring(accountName.indexOf(":")));
		};
	}

	public TaxTemplateData createTaxTemplateData() {
		ChartOfAccounts coa = dataLoaderService.getCoa();
		LocalDate currentFY = calculateStartOfFinancialYear(LocalDate.now());

		return new TaxTemplateData( reportModelBuilderService.createTaxYear(currentFY, coa),
									reportModelBuilderService.createTaxYear(currentFY.minusYears(1), coa));
	}

	private Account buildFilteredAccount(com.hz.mymoney.data.models.coa.Account account, LocalDate startDate, LocalDate endDate) {
		com.hz.mymoney.data.models.coa.Account filteredAccount = new com.hz.mymoney.data.models.coa.Account(account, startDate, endDate);

		return new Account(filteredAccount.getSimpleName(), filteredAccount.getName(), filteredAccount.getCategory(), filteredAccount.getBalance(endDate, shareValueService.getInvestmentHistory()).abs(), account.isShareAccount(), null);
	}

	private LocalDate calculateStartOfFinancialYear(LocalDate endDate) {
		LocalDate financialYearStartDate = endDate.withMonth(7).withDayOfMonth(1);  // July 1st
		if (financialYearStartDate.isAfter(endDate)) {
			return financialYearStartDate.minusYears(1);
		}
		return financialYearStartDate;
	}

	private InvestmentSummary mapAccountToShareSummary(com.hz.mymoney.data.models.coa.Account account, BigDecimal earnings, LocalDate asAt) {
		BigDecimal currentShareValue = account.isShareAccount() ? shareValueService.getInvestmentValue(account.getCode(), asAt) : BigDecimal.ZERO;
		BigDecimal yesterdayShareValue = account.isShareAccount() ? shareValueService.getPreviousInvestmentValue(account.getCode(), asAt) : BigDecimal.ZERO;
		BigDecimal currentValue = account.getTotalAmount().multiply(currentShareValue).setScale(2, RoundingMode.HALF_UP);
		BigDecimal netProfitLoss = currentValue.compareTo(BigDecimal.ZERO) != 0
				? currentValue.subtract(account.getCostBase().subtract(earnings).subtract(account.getSales()))
				: earnings.add(account.getSales()).subtract(account.getCostBase());

		PriorityQueue<Movement> movementsForCode = account.getMovementsForCode(account.getCode());
		if (movementsForCode.isEmpty()) {
			return new InvestmentSummary(account.getCode(), account.getTotalAmount(), currentShareValue, yesterdayShareValue, currentValue, account.getCostBase(), account.getSales(), earnings, netProfitLoss, LocalDate.of(2000,1,1), LocalDate.now(), getNextInvestmentIncomeNote(account.getCode()));
		}

		LocalDate earliestMovementDate = movementsForCode.stream().findFirst().orElseThrow().date();
		LocalDate lastMovementDate = movementsForCode.stream().toList().getLast().date();

		return new InvestmentSummary(account.getCode(), account.getTotalAmount(), currentShareValue, yesterdayShareValue, currentValue, account.getCostBase(), account.getSales(), earnings, netProfitLoss, earliestMovementDate, lastMovementDate, getNextInvestmentIncomeNote(account.getCode()));
	}

	private NetAssetLiabilityPosition createNetAssetLiabilityPosition(ChartOfAccounts chartOfAccounts, boolean includeNote) {
		NetAssetLiabilityPosition nalPosition = new NetAssetLiabilityPosition(chartOfAccounts.getAsAt());

		chartOfAccounts.getAccountsOfType("Assets:", true)
				.forEach(account -> nalPosition.addAssetAccount(mapAccount(account, nalPosition.asAt, includeNote)));

		chartOfAccounts.getAccountsOfType("Liabilities:", true)
				.forEach(account -> nalPosition.addLiabilityAccount(mapAccount(account, nalPosition.asAt, includeNote)));

		return nalPosition;
	}

	private SuperannuationAccount mapSuperannuationAccount(com.hz.mymoney.data.models.coa.Account account, LocalDate asAt) {
		LocalDate earliestDate = account.getFirstMovement().date();
		LocalDate lastDate = account.getLastMovement().date();

		return new SuperannuationAccount(account.getSimpleName(), account.getName(), account.getBalance(asAt, shareValueService.getInvestmentHistory()), earliestDate, lastDate, account.getMovements());
	}

	private Account mapEquityAccount(com.hz.mymoney.data.models.coa.Account account, LocalDate asAt) {
		if (account.isShareAccount()) {
			BigDecimal shareValue = shareValueService.getInvestmentHistory().getInvestmentValue(account.getCode(), asAt);
			return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getTotalAmount().multiply(shareValue).setScale(2, RoundingMode.HALF_UP), account.isShareAccount(), null);
		}

		return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getBalance(asAt, shareValueService.getInvestmentHistory()).multiply(BigDecimal.valueOf(-1)), false, null);
	}

	private Account mapAccount(com.hz.mymoney.data.models.coa.Account account, LocalDate asAt, boolean includeNote) {
		if (account.isShareAccount()) {
			BigDecimal shareValue = shareValueService.getInvestmentHistory().getInvestmentValue(account.getCode(), asAt);
			return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getTotalAmount().multiply(shareValue).setScale(2, RoundingMode.HALF_UP), account.isShareAccount(), includeNote ? getNextInvestmentIncomeNote(account.getCode()) : null);
		}

		return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getBalance(asAt, shareValueService.getInvestmentHistory()), false, null);
	}

	private Account mapAccount(com.hz.mymoney.data.models.coa.Account account, List<Transaction> transactions, LocalDate asAt) {
		BigDecimal total = transactions.stream()
				.map(Transaction::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		if (account.isShareAccount()) {
			BigDecimal shareValue = shareValueService.getInvestmentHistory().getInvestmentValue(account.getCode(), asAt);
			return new Account(account.getSimpleName(), account.getName(), account.getCategory(), total.multiply(shareValue).setScale(2, RoundingMode.HALF_UP), account.isShareAccount(), getNextInvestmentIncomeNote(account.getCode()));
		}

		return new Account(account.getSimpleName(), account.getName(), account.getCategory(), total, account.isShareAccount(), null);
	}

	private String getNextInvestmentIncomeNote(String code) {
		ChartOfAccounts coa = dataLoaderService.getCoa();

		LocalDate next = coa.getNextDividendDateForCode(code);
		if (next != null) {
			return "Next Dividend for " + code + " is " + next.format(DateTimeFormatter.ofPattern("MMM yyyy"));
		}

		next = coa.getNextDistributionDateForCode(code);
		if (next != null) {
			return "Next Distribution for " + code + " is " + next.format(DateTimeFormatter.ofPattern("MMM yyyy"));
		}

		return "";
	}

	public String createIncomeExpenseHistory() {
		// Generate JSON array as String containing IncomeExpenseHistory
		List<YearlyIncomeExpense> yearlyIncomeExpenseList = new ArrayList<>();
		ChartOfAccounts chartOfAccounts = dataLoaderService.getCoa();
		LocalDate financialYearStart = calculateStartOfFinancialYear(LocalDate.now());

		int year = financialYearStart.getYear();
		int yearMin = year - 10;

		while (year > yearMin) {
			BigDecimal taxes = sumBalanceForFY(chartOfAccounts, EMPLOYMENT_TAXES, financialYearStart)
					.add(sumBalanceForFY(chartOfAccounts, SUPER_TAXES, financialYearStart));
			yearlyIncomeExpenseList.add(new YearlyIncomeExpense("FY" + (year - 2000) + "/" + (year - 1999),
					sumBalanceForFY(chartOfAccounts, INCOME_PREFIX, financialYearStart),
					sumBalanceForFY(chartOfAccounts, "Expenses", financialYearStart)
						.subtract(taxes)
						.multiply(BigDecimal.valueOf(-1)),
					taxes.multiply(BigDecimal.valueOf(-1)
			)));
			year--;
			chartOfAccounts = new ChartOfAccounts(financialYearStart.minusDays(1), chartOfAccounts);
			financialYearStart = financialYearStart.minusYears(1);
		}

		JsonMapper mapper = JsonMapper.builder()
				.build();
		return mapper.writeValueAsString(yearlyIncomeExpenseList.reversed());
	}

	private BigDecimal sumBalanceForFY(ChartOfAccounts chartOfAccounts, String accountType, LocalDate fyStart) {
		return chartOfAccounts.getAccountsOfType(accountType, true).stream()
				.filter(account -> account.hasMovementBetween(fyStart, chartOfAccounts.getAsAt()))
				.map(account -> buildFilteredAccount(account, fyStart, chartOfAccounts.getAsAt()))
				.map(Account::balance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public EquityTemplateData createEquityTemplateData() {
		EquityTemplateData equityTemplateData = new EquityTemplateData();

		ChartOfAccounts chartOfAccounts = dataLoaderService.getCoa();

		// These are as at the current date
		chartOfAccounts.getAccountsOfType("Equity", true)
				.forEach(account -> equityTemplateData.equityAccounts.add(mapEquityAccount(account, chartOfAccounts.getAsAt())));

		return equityTemplateData;
	}

	public SuperTemplateData createSuperTemplateData() {
		SuperTemplateData superTemplateData = new SuperTemplateData();

		ChartOfAccounts chartOfAccounts = dataLoaderService.getCoa();

		// These are as at the current date
		chartOfAccounts.getAccountsOfType(AccountConstants.SUPER_ACCOUNTS, false)
				.forEach(account -> superTemplateData.superAccounts.add(mapSuperannuationAccount(account, chartOfAccounts.getAsAt())));

		return superTemplateData;
	}

	public Optional<Schedule> getSchedule(String scheduleDescription) {
		return this.schedulesLoaderService.findSchedule(scheduleDescription);
	}
}

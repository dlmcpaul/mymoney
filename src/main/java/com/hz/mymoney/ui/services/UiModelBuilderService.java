package com.hz.mymoney.ui.services;

import com.hz.mymoney.configuration.AccountConstants;
import com.hz.mymoney.data.models.internal.ChartOfAccounts;
import com.hz.mymoney.data.models.internal.Movement;
import com.hz.mymoney.data.models.internal.Schedule;
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

import static com.hz.mymoney.configuration.AccountConstants.SUPER_CONTRIBUTION_NOTE;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Log4j2
public class UiModelBuilderService {
	private final LedgerServices dataLoaderService;
	private final SharePriceServices shareValueService;
	private final SchedulesServices schedulesLoaderService;

	public boolean isLedgerReadOnly() {
		return dataLoaderService.getLedger().isReadOnly();
	}

	public TrendsTemplateData createTrendsTemplateData(String accountName, String trendType) {
		ChartOfAccounts coa = dataLoaderService.getCoa();

		var account = coa.findAccount(accountName);

		if (account.isPresent()) {
			return new TrendsTemplateData(accountName, trendType, account.get().getMovementsAsList());
		}

		if (accountName.isEmpty()) {
			return new TrendsTemplateData(accountName, trendType, new ArrayList<>());
		}

		// Merge all the accounts found into single set of movements
		List<Movement> allMovements = coa.getAccountsOfType(accountName, true)
				.stream()
				.map(com.hz.mymoney.data.models.internal.Account::getMovementsAsList)
				.flatMap(Collection::stream)
				.toList();

		return new TrendsTemplateData(accountName, trendType, allMovements);
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
				.map(com.hz.mymoney.data.models.internal.Account::getName)
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
				.map(account -> getFilteredTransactions(account, startDate,endDate))
				.flatMap(List::stream)
				.toList();
		List<Transaction> expensesTransactions = coa.getAccountsOfType("Expenses", true).stream()
				.filter(account -> account.hasMovementBetween(startDate, endDate))
				.map(account -> getFilteredTransactions(account, startDate,endDate))
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

	public InvestmentsTemplateData createCurrentInvestmentsTemplateData() {
		InvestmentsTemplateData shareTemplateData = new InvestmentsTemplateData("Current");
		ChartOfAccounts coa = dataLoaderService.getCoa();
		shareTemplateData.investmentSummaries.addAll(coa.getAccountsOfType(AccountConstants.SHARES, true).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		shareTemplateData.investmentSummaries.addAll(coa.getAccountsOfType(AccountConstants.FUNDS, true).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		return shareTemplateData;
	}

	public InvestmentsTemplateData createPriorInvestmentsTemplateData() {
		InvestmentsTemplateData shareTemplateData = new InvestmentsTemplateData("Historical");
		ChartOfAccounts coa = dataLoaderService.getCoa();
		shareTemplateData.investmentSummaries.addAll(coa.getZeroBalanceAccountsOfType(AccountConstants.SHARES).stream()
				.map(account -> mapAccountToShareSummary(account, coa.getTotalInvestmentIncomeForCode(account.getCode()), coa.getAsAt()))
				.toList());

		shareTemplateData.investmentSummaries.addAll(coa.getZeroBalanceAccountsOfType(AccountConstants.FUNDS).stream()
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

		// Get the account and all it's transactions
		var acc = chartOfAccounts.getAccountsOfType(accountName, false).stream()
				.filter(a -> a.getName().equals(accountName))
				.findFirst()
				.orElseThrow();

		List<Transaction> transactionsBetween = mapMovementsToTransactions(acc.getMovementsBetween(financialYearStart, financialYearEnd), acc);
		List<Transaction> transactionsToYearEnd = mapMovementsToTransactions(acc.getMovements(financialYearEnd), acc);

		return switch (accountName.substring(0, accountName.indexOf(":")).toLowerCase()) {
			case "assets", "liabilities", "equity" -> new SingleAccountTemplateData(accountName, mapAccount(acc, transactionsToYearEnd, chartOfAccounts.getAsAt()), allTransactions ? transactionsToYearEnd : transactionsBetween, financialYearStart, allTransactions);
			case "income", "expenses" -> new SingleAccountTemplateData(accountName, mapAccount(acc, transactionsBetween, chartOfAccounts.getAsAt()), transactionsBetween, financialYearStart, allTransactions);
			default ->
				throw new IllegalStateException("Unexpected value: " + accountName.substring(accountName.indexOf(":")));
		};
	}

	public List<Transaction> mapMovementsToTransactions(PriorityQueue<Movement> movements, com.hz.mymoney.data.models.internal.Account account) {
		return movements.stream()
				.map(movement -> mapTransaction(movement, account.getName(), account.isShareAccount()))
				.sorted(Comparator.comparing(Transaction::asAt))
				.toList();
	}

	public TaxTemplateData createTaxTemplateData() {
		ChartOfAccounts coa = dataLoaderService.getCoa();
		LocalDate currentFY = calculateStartOfFinancialYear(LocalDate.now());

		return new TaxTemplateData( createTaxYear(currentFY, coa),
									createTaxYear(currentFY.minusYears(1), coa));
	}

	private Account buildFilteredAccount(com.hz.mymoney.data.models.internal.Account account, LocalDate startDate, LocalDate endDate) {
		com.hz.mymoney.data.models.internal.Account filteredAccount = new com.hz.mymoney.data.models.internal.Account(account, startDate, endDate);

		return new Account(filteredAccount.getSimpleName(), filteredAccount.getName(), filteredAccount.getCategory(), filteredAccount.getBalance(endDate, shareValueService.getInvestmentHistory()).abs(), account.isShareAccount(), null);
	}

	private TaxYear createTaxYear(LocalDate financialYearStartDate, ChartOfAccounts coa) {
		LocalDate financialYearEndDate = financialYearStartDate.plusYears(1).minusDays(1);

		List<Transaction> incomeTransactions = coa.getAccountsOfType("Income:", true).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> getFilteredTransactions(account, financialYearStartDate, financialYearEndDate))
				.flatMap(List::stream)
				.toList();

		List<Transaction> expenseTransactions = coa.getAccountsOfType("Expenses:", true).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> getFilteredTransactions(account, financialYearStartDate, financialYearEndDate))
				.flatMap(List::stream)
				.toList();

		List<Transaction> imputationTransactions = coa.getAccountsOfType(AccountConstants.IMPUTATION_ACCOUNT, false).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> getFilteredTransactions(account, financialYearStartDate, financialYearEndDate))
				.flatMap(List::stream)
				.filter(transaction -> transaction.amount().compareTo(BigDecimal.ZERO) >= 0)
				.toList();

		List<Transaction> supercontribTransactions = coa.getAccountsOfType(AccountConstants.SUPER_ACCOUNT, false).stream()
				.filter(account -> account.hasMovementBetween(financialYearStartDate, financialYearEndDate))
				.map(account -> getFilteredTransactions(account, financialYearStartDate, financialYearEndDate))
				.flatMap(List::stream)
				.filter(transaction -> transaction.amount().compareTo(BigDecimal.ZERO) >= 0)
				.filter(transaction -> transaction.description().equalsIgnoreCase(SUPER_CONTRIBUTION_NOTE))
				.toList();

		// PAYG Payments for the Tax Year are offset by 3 months
		LocalDate paygStartDate = financialYearStartDate.plusMonths(3);
		LocalDate paygEndDate = financialYearEndDate.plusMonths(3);

		List<Transaction> paygTransactions = coa.getAccountsOfType(AccountConstants.PAYG_DEDUCTIONS, false).stream()
				.filter(account -> account.hasMovementBetween(paygStartDate, paygEndDate))
				.map(account -> getFilteredTransactions(account, paygStartDate, paygEndDate))
				.flatMap(List::stream)
				.filter(transaction -> transaction.amount().compareTo(BigDecimal.ZERO) >= 0)
				.toList();

		return new TaxYear(incomeTransactions, expenseTransactions, imputationTransactions, paygTransactions, supercontribTransactions);
	}

	private LocalDate calculateStartOfFinancialYear(LocalDate endDate) {
		LocalDate financialYearStartDate = endDate.withMonth(7).withDayOfMonth(1);  // July 1st
		if (financialYearStartDate.isAfter(endDate)) {
			return financialYearStartDate.minusYears(1);
		}
		return financialYearStartDate;
	}

	private InvestmentSummary mapAccountToShareSummary(com.hz.mymoney.data.models.internal.Account account, BigDecimal earnings, LocalDate asAt) {
		BigDecimal shareValue = account.isShareAccount() ? shareValueService.getInvestmentHistory().getInvestmentValue(account.getCode(), asAt) : BigDecimal.ZERO;
		BigDecimal currentValue = account.getTotalAmount().multiply(shareValue).setScale(2, RoundingMode.HALF_UP);
		BigDecimal netProfitLoss = currentValue.compareTo(BigDecimal.ZERO) != 0
				? currentValue.subtract(account.getCostBase().subtract(earnings).subtract(account.getSales()))
				: earnings.add(account.getSales()).subtract(account.getCostBase());

		PriorityQueue<Movement> movementsForCode = account.getMovementsForCode(account.getCode());
		if (movementsForCode.isEmpty()) {
			return new InvestmentSummary(account.getCode(), account.getTotalAmount().intValue(), shareValue, currentValue, account.getCostBase(), account.getSales(), earnings, netProfitLoss, LocalDate.of(2000,1,1), LocalDate.now());
		}

		LocalDate earliestMovementDate = movementsForCode.stream().findFirst().orElseThrow().date();
		LocalDate lastMovementDate = movementsForCode.stream().toList().getLast().date();

		return new InvestmentSummary(account.getCode(), account.getTotalAmount().intValue(), shareValue, currentValue, account.getCostBase(), account.getSales(), earnings, netProfitLoss, earliestMovementDate, lastMovementDate);
	}

	private NetAssetLiabilityPosition createNetAssetLiabilityPosition(ChartOfAccounts chartOfAccounts, boolean includeNote) {
		NetAssetLiabilityPosition nalPosition = new NetAssetLiabilityPosition(chartOfAccounts.getAsAt());

		chartOfAccounts.getAccountsOfType("Assets:", true)
				.forEach(account -> nalPosition.addAssetAccount(mapAccount(account, nalPosition.asAt, includeNote)));

		chartOfAccounts.getAccountsOfType("Liabilities:", true)
				.forEach(account -> nalPosition.addLiabilityAccount(mapAccount(account, nalPosition.asAt, includeNote)));

		return nalPosition;
	}

	private SuperannuationAccount mapSuperannuationAccount(com.hz.mymoney.data.models.internal.Account account, LocalDate asAt) {
		LocalDate earliestDate = account.getFirstMovement().date();
		LocalDate lastDate = account.getLastMovement().date();

		return new SuperannuationAccount(account.getSimpleName(), account.getName(), account.getBalance(asAt, shareValueService.getInvestmentHistory()), earliestDate, lastDate, account.getMovements());
	}

	private Account mapEquityAccount(com.hz.mymoney.data.models.internal.Account account, LocalDate asAt) {
		if (account.isShareAccount()) {
			BigDecimal shareValue = shareValueService.getInvestmentHistory().getInvestmentValue(account.getCode(), asAt);
			return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getTotalAmount().multiply(shareValue).setScale(2, RoundingMode.HALF_UP), account.isShareAccount(), null);
		}

		return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getBalance(asAt, shareValueService.getInvestmentHistory()).multiply(BigDecimal.valueOf(-1)), false, null);
	}

	private Account mapAccount(com.hz.mymoney.data.models.internal.Account account, LocalDate asAt, boolean includeNote) {
		if (account.isShareAccount()) {
			BigDecimal shareValue = shareValueService.getInvestmentHistory().getInvestmentValue(account.getCode(), asAt);
			return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getTotalAmount().multiply(shareValue).setScale(2, RoundingMode.HALF_UP), account.isShareAccount(), includeNote ? getNextInvestmentIncomeNote(account.getCode()) : null);
		}

		return new Account(account.getSimpleName(), account.getName(), account.getCategory(), account.getBalance(asAt, shareValueService.getInvestmentHistory()), false, null);
	}

	private Account mapAccount(com.hz.mymoney.data.models.internal.Account account, List<Transaction> transactions, LocalDate asAt) {
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

	private List<Transaction> getFilteredTransactions(com.hz.mymoney.data.models.internal.Account account, LocalDate startDate, LocalDate endDate) {
		return mapMovementsToTransactions(account.getMovementsBetween(startDate, endDate), account);
	}

	private Transaction mapTransaction(Movement movement, String sourceAccount, boolean isShares) {
		BigDecimal amount = isShares ? movement.amount() : movement.getValue();
		return new Transaction(movement.date(), movement.getNote(), amount, sourceAccount, movement.sourceAccount(), isShares, movement.price());
	}

	public String createIncomeExpenseHistory() {
		// Generate JSON array as String containing IncomeExpenseHistory
		List<YearlyIncomeExpense> yearlyIncomeExpenseList = new ArrayList<>();
		ChartOfAccounts chartOfAccounts = dataLoaderService.getCoa();
		LocalDate financialYearStart = calculateStartOfFinancialYear(LocalDate.now());

		int year = financialYearStart.getYear();
		int yearMin = year - 10;

		while (year > yearMin) {
			yearlyIncomeExpenseList.add(new YearlyIncomeExpense("FY" + (year - 2000) + "/" + (year - 1999),
					sumBalanceForFY(chartOfAccounts, "Income", financialYearStart),
					sumBalanceForFY(chartOfAccounts, "Expenses", financialYearStart)
							.subtract(sumBalanceForFY(chartOfAccounts, AccountConstants.EMPLOYMENT_TAXES, financialYearStart))
							.multiply(BigDecimal.valueOf(-1))));
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
		chartOfAccounts.getAccountsOfType(AccountConstants.SUPER_ACCOUNT, false)
				.forEach(account -> superTemplateData.superAccounts.add(mapSuperannuationAccount(account, chartOfAccounts.getAsAt())));

		return superTemplateData;
	}

	public Optional<Schedule> getSchedule(String scheduleDescription) {
		return this.schedulesLoaderService.findSchedule(scheduleDescription);
	}
}

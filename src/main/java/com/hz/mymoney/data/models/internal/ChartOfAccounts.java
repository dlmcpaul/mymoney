package com.hz.mymoney.data.models.internal;

import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.SharePosting;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.hz.mymoney.configuration.AccountConstants.*;

@Log4j2
public class ChartOfAccounts {
	@Getter
	private final LocalDate asAt;
	private final Collection<Account> accounts;

	public ChartOfAccounts(LocalDate asAt) {
		this.asAt = asAt;
		this.accounts = new TreeSet<>();
	}

	// Make a coa with accounts that exist equal to or less than the asAt date
	public ChartOfAccounts(LocalDate asAt, ChartOfAccounts fromCoa) {
		this.asAt = asAt;
		this.accounts = fromCoa.accounts.stream()
				.filter(account -> account.hasMovementPrior(asAt.plusDays(1)))
				.map(account -> new Account(account, asAt))
				.filter(Account::hasMovements)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public int totalAccounts() {
		return accounts.size();
	}

	public Optional<Account> findAccount(String name) {
		for (Account account : accounts) {
			if (account.getName().equals(name)) {
				return Optional.of(account);
			}
		}
		return Optional.empty();
	}

	public SortedSet<Account> getZeroBalanceAccountsOfType(String accountType) {
		return this.accounts.stream()
				.filter(account -> account.getName().toLowerCase().startsWith(accountType.toLowerCase()))
				.filter(account -> account.getTotalAmount().compareTo(BigDecimal.ZERO) == 0)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public SortedSet<Account> getAccountsOfType(String accountType, boolean filterZeroAmounts) {
		SortedSet<Account> typedAccounts = new TreeSet<>();
		for (Account account : this.accounts) {
			if (account.getName().toLowerCase().startsWith(accountType.toLowerCase())) {
				if (filterZeroAmounts) {
					if (account.getTotalAmount().compareTo(BigDecimal.ZERO) != 0) {
						typedAccounts.add(account);
					}
				} else {
					typedAccounts.add(account);
				}
			}
		}
		return typedAccounts;
	}

	public void addPosting(IPosting posting, LocalDate date, String description, String sourceAccount, BigDecimal commission) {
		Optional<Account> account = findAccount(posting.getAccount());
		if (account.isEmpty()) {
			accounts.add(new Account(posting.getAccount(), posting instanceof SharePosting, new Movement(date, sourceAccount, description, posting.getAmount(), posting.getPrice(), posting.getCode(), posting.isSplit(), commission, posting.getNote())));
		} else {
			account.get().addMovement(new Movement(date, sourceAccount, description, posting.getAmount(), posting.getPrice(), posting.getCode(), posting.isSplit(), commission, posting.getNote()));
		}
	}

	public BigDecimal getBalanceForAccountType(String accountType, InvestmentHistory investmentHistory) {
		return this.getAccountsOfType(accountType, true).stream()
				.map(account -> account.getBalance(asAt, investmentHistory))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getBalanceForAccount(String accountName, InvestmentHistory investmentHistory) {
		return accounts.stream()
				.filter(account -> account.getName().equals(accountName))
				.map(account -> account.getBalance(asAt, investmentHistory))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private LocalDate get2ndOldestDate(PriorityQueue<Movement> movements) {
		LocalDate oneYearAgo = LocalDate.now().minusYears(1);
		Optional<Movement> oldest = movements.stream()
				.filter(movement -> movement.date().isAfter(oneYearAgo))
				.findFirst();
		return oldest.map(movement -> movement.date().plusYears(1)).orElse(null);
	}

	public LocalDate getNextDividendDateForCode(String code) {
		SortedSet<Account> dividendAccounts = this.getAccountsOfType(DIVIDEND_INCOME, true);
		return get2ndOldestDate( dividendAccounts.stream()
				.map(Account::getMovementsAsList)
				.flatMap(Collection::stream)
				.filter(movement -> movement.code().equals(code))
				.collect(Collectors.toCollection(PriorityQueue::new)));
	}

	public LocalDate getNextDistributionDateForCode(String code) {
		SortedSet<Account> distributionAccounts = this.getAccountsOfType(DISTRIBUTION_INCOME, true);
		return get2ndOldestDate( distributionAccounts.stream()
				.map(Account::getMovementsAsList)
				.flatMap(Collection::stream)
				.filter(movement -> movement.code().equals(code))
				.collect(Collectors.toCollection(PriorityQueue::new)));
	}

	public BigDecimal getTotalFrankedDividendsForCode(String code) {
		return this.sumAmountForAccountWithCode(FRANKED_DIVIDEND, code);
	}

	public BigDecimal getTotalUnFrankedDividendsForCode(String code) {
		return this.sumAmountForAccountWithCode(UNFRANKED_DIVIDEND, code);
	}

	public BigDecimal getTotalMiscIncomeForCode(String code) {
		return this.sumAmountForAccountWithCode(INCOME_OTHER, code);
	}

	public BigDecimal getTotalCapitalGainsForCode(String code) {
		return this.sumAmountForAccountWithCode(CAPITAL_GAINS_INCOME, code);
	}

	// None in conversion
	public BigDecimal getTotalCapitalLossesForCode(String code) {
		return this.sumAmountForAccountWithCode(CAPITAL_LOSSES_INCOME, code);
	}

	public BigDecimal getTotalCapitalReturnsForCode(String code) {
		return this.sumAmountForAccountWithCode(CAPITAL_RETURN_INCOME, code);
	}

	// Fund type distribution or reinvestment
	public BigDecimal getTotalDistributionsForCode(String code) {
		return this.sumAmountForAccountWithCode(DISTRIBUTION_INCOME, code)
				.add(this.sumAmountForAccountWithCode(REINVESTMENT_INCOME, code));
	}

	public BigDecimal getTotalInvestmentIncomeForCode(String code) {
		return this.getTotalFrankedDividendsForCode(code)
				.add(this.getTotalUnFrankedDividendsForCode(code))
				.add(this.getTotalMiscIncomeForCode(code))
				.add(this.getTotalCapitalGainsForCode(code))
				.add(this.getTotalCapitalReturnsForCode(code))
				.add(this.getTotalDistributionsForCode(code));
	}

	private BigDecimal sumAmountForAccountWithCode(String accountType, String code) {
		return accounts.stream()
				.filter(account -> account.getName().equalsIgnoreCase(accountType))
				.map(account -> account.getMovementsForCode(code))
				.map(this::sum)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.abs()
				.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal sum(Collection<Movement> movements) {
		return movements.stream()
				.map(Movement::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}
}

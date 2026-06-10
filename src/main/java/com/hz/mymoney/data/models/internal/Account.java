package com.hz.mymoney.data.models.internal;

import com.hz.mymoney.configuration.AccountConstants;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hz.mymoney.configuration.AccountConstants.*;

@Log4j2
public class Account implements Comparable<Account> {
	@Getter
	private final String name;
	@Getter
	private final boolean isShareAccount;
	@Getter
	private final PriorityQueue<Movement> movements;
	private BigDecimal cachedAmount = BigDecimal.ZERO;

	public Account(String name, boolean isShareAccount, Movement movement) {
		this.name = name;
		this.isShareAccount = isShareAccount;
		this.movements = new PriorityQueue<>(100);
		this.addMovement(movement);
	}

	public Account(String name, boolean isShareAccount, List<Movement> movements) {
		this.name = name;
		this.isShareAccount = isShareAccount;
		this.movements = new PriorityQueue<>(100);
		this.addMovements(movements);
	}

	public Account(Account account, LocalDate asAt) {
		this.name = account.getName();
		this.isShareAccount = account.isShareAccount();
		this.movements = account.getMovements(asAt);
		this.cachedAmount = calculateTotalAmount();
	}

	public Account(Account account, LocalDate startDate, LocalDate endDate) {
		this.name = account.getName();
		this.isShareAccount = account.isShareAccount();
		this.movements = account.getMovementsBetween(startDate, endDate);
		this.cachedAmount = calculateTotalAmount();
	}

	public void addMovement(Movement movement) {
		this.movements.add(movement);
		this.cachedAmount = addMovementAmount(movement);
	}

	public void addMovements(List<Movement> movements) {
		this.movements.addAll(movements);
		this.cachedAmount = calculateTotalAmount();
	}

	public String getCode() {
		return movements.stream()
				.findFirst()
				.orElseThrow()
				.code();
	}

	public List<Movement> getMovementsAsList() {
		return movements.stream().toList();
	}

	public Movement getFirstMovement() {
		return movements.stream().findFirst().orElse(null);
	}

	public Movement getLastMovement() {
		return movements.stream().toList().getLast();
	}

	public boolean hasMovements() {
		return movements.size() > 0;
	}

	public @NonNull BigDecimal getTotalAmount() {
		return cachedAmount;
	}

	private BigDecimal addMovementAmount(Movement movement) {
		if (isShareAccount) {
			if (movement.split()) {
				return movement.amount().setScale(0, RoundingMode.HALF_UP);
			}
			return cachedAmount.add(movement.amount()).setScale(0, RoundingMode.HALF_UP);
		}
		return cachedAmount.add(movement.amount());
	}

	// Used to get share counts not share value/balance
	private @NonNull BigDecimal calculateTotalAmount() {
		if (isShareAccount) {
			// Share accounts can have splits that make summing tricky.  Each split resets the sum
			BigDecimal sum = BigDecimal.ZERO;
			for (Movement movement : movements) {
				sum = movement.split() ? movement.amount() : sum.add(movement.amount());
			}
			return sum.setScale(0, RoundingMode.HALF_UP);
		}
		return sumBigDecimals(movements.stream()
				.map(Movement::amount));
	}

	public @NonNull BigDecimal getBalance(LocalDate endDate, InvestmentHistory investmenthistory) {
		if (isShareAccount) {
			// Share accounts can have splits that make summing tricky.  Each split resets the sum
			BigDecimal sum = BigDecimal.ZERO;
			for (Movement movement : movements) {
				if (movement.isBeforeOrEqual(endDate)) {
					sum = movement.split() ? movement.getValue(investmenthistory.getInvestmentValue(getCode(), endDate)) : sum.add(movement.getValue(investmenthistory.getInvestmentValue(getCode(), endDate)));
				}
			}
			return sum.setScale(2, RoundingMode.HALF_UP);
		}
		return sumBigDecimals(movements.stream()
				.filter(movement -> movement.isBeforeOrEqual(endDate))
				.map(Movement::amount));
	}

	private @NonNull BigDecimal sumBigDecimals(Stream<BigDecimal> bigDecimals) {
		return bigDecimals
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	// Not really cost base currently cost of purchases and commissions
	// Need to handle Capital Returns or just rename as outlay
	public @NonNull BigDecimal getCostBase() {
		if (isShareAccount) {
			return sumBigDecimals(movements.stream()
					.map(movement -> movement.amount().multiply(movement.price()).add(movement.commission()))
					.filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0));
		} else if (isManagedFund()) {
			return getManagedFundCostBase();
		}
		// Only shares and managed funds have a cost base
		return BigDecimal.ZERO;
	}

	private @NonNull BigDecimal getManagedFundCostBase() {
		return sumBigDecimals(movements.stream()
				.filter(movement -> movement.amount().compareTo(BigDecimal.ZERO) > 0
						&& (movement.sourceAccount().toLowerCase().startsWith(EQUITY_FUND_OPENING_BALANCE.toLowerCase())  // Opening balances prior to tracking
						|| movement.sourceAccount().toLowerCase().startsWith(REINVESTMENT_INCOME.toLowerCase()) // Reinvestment schemes
						|| movement.description().toLowerCase().startsWith(OPENING_BALANCE_DESCRIPTION.toLowerCase())))   // Opening balances after tracking (MEF)
				.map(Movement::amount)
				);
	}

	// Value of all share and managed fund sales less commission
	public @NonNull BigDecimal getSales() {
		if (isShareAccount) {
			return sumBigDecimals(movements.stream()
					.filter(movement -> movement.amount().compareTo(BigDecimal.ZERO) < 0)
					.map(movement -> movement.amount().multiply(movement.price()).abs().subtract(movement.commission())));
		} else if (isManagedFund()) {
			return sumBigDecimals(movements.stream()
					.filter(movement -> movement.amount().compareTo(BigDecimal.ZERO) < 0 && movement.description().startsWith(SHARES_SOLD_DESCRIPTION))
					.map(movement -> movement.amount().abs()));
		}
		// Only shares and Managed Funds have sales
		return BigDecimal.ZERO;
	}

	private boolean isManagedFund() {
		return name.toLowerCase().startsWith(AccountConstants.FUNDS.toLowerCase());
	}

	public String getCategory() {
		if (name.contains(":")) {
			if (name.indexOf(":") == name.lastIndexOf(":")) {
				return name.substring(name.lastIndexOf(":") + 1);
			}
			return name.split(":")[1];
		}
		return "";
	}

	public String getSimpleName() {
		int lastColon = name.lastIndexOf(":");
		String simpleName = ("" + name.charAt(lastColon + 1)).toUpperCase() + name.substring(name.lastIndexOf(":") + 2);     // Uppercase first letter

		if (isShareAccount) {
			if (getCode().equals("CPXXMV")) {
				return getTotalAmount() + " " + simpleName + " warrants";
			}

			if ((getCode().length() == 4 && getCode().charAt(3) == 'O')) {
				// Option not share?
				return getTotalAmount() + " options (" + getCode() + ") for " + getCode().substring(0,3);
			}
			return getTotalAmount() + " shares in " + simpleName;
		} else if (isManagedFund()) {
			return simpleName + " Fund";
		}

		return simpleName;
	}

	// Inclusive of endDate
	public boolean hasMovementPrior(LocalDate endDate) {
		return movements.stream()
				.anyMatch(movement -> movement.isBeforeOrEqual(endDate));
	}

	public PriorityQueue<Movement> getMovements(LocalDate asAt) {
		PriorityQueue<Movement> movementsAsAt = new PriorityQueue<>(movements);
		movementsAsAt.removeIf(movement -> movement.isAfter(asAt));
		return movementsAsAt;
	}

	public boolean hasMovementBetween(LocalDate startDate, LocalDate endDate) {
		return movements.stream()
				.anyMatch(movement -> movement.isBetween(startDate, endDate));
	}

	public PriorityQueue<Movement> getMovementsBetween(LocalDate startDate, LocalDate endDate) {
		return movements.stream()
				.filter(movement -> movement.isBetween(startDate, endDate))
				.collect(Collectors.toCollection(PriorityQueue::new));
	}

	public PriorityQueue<Movement> getMovementsForCode(String code) {
		PriorityQueue<Movement> movementsAsAt = new PriorityQueue<>(movements);
		movementsAsAt.removeIf(movement -> movement.code().equals(code) == false);
		return movementsAsAt;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Account account) {
			return this.name.equals(account.name);
		}
		return false;
	}

	@Override
	public int compareTo(Account o) {
		return this.name.compareTo(o.name);
	}
}

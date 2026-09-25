package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.NetAssetLiabilityPosition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetWorthTemplateData {
	private final List<NetAssetLiabilityPosition> netAssetLiabilityPositions = new ArrayList<>();
	private final NetAssetLiabilityPosition current;
	private final NetAssetLiabilityPosition lastYear;

	public NetWorthTemplateData(List<NetAssetLiabilityPosition> netAssetLiabilityPositions) {
		this.netAssetLiabilityPositions.addAll(netAssetLiabilityPositions);
		current = this.netAssetLiabilityPositions.getFirst();
		lastYear = this.netAssetLiabilityPositions.stream()
				.filter(position -> (position.asAt.equals(current.getLastYear())))
				.findFirst()
				.orElse(new NetAssetLiabilityPosition(current.getLastYear()));
	}

	public Money getCurrentAssetBalance() {
		return current.getAssetBalance();
	}

	public Money getLastYearAssetBalance() {
		return lastYear.getAssetBalance();
	}

	public Money getCurrentLiabilityBalance() {
		return current.getLiabilityBalance();
	}

	public Money getLastYearLiabilityBalance() {
		return lastYear.getLiabilityBalance();
	}

	public Money getCurrentNetPosition() {
		return current.getNetPosition();
	}

	public Money getLastYearNetPosition() {
		return lastYear.getNetPosition();
	}

	public Money assetDifference() {
		return current.getAssetBalance().subtract(lastYear.getAssetBalance());
	}

	public Money liabilityDifference() {
		return current.getLiabilityBalance().abs().subtract(lastYear.getLiabilityBalance().abs());
	}

	public Money netWorthDifference() {
		return current.getNetPosition().subtract(lastYear.getNetPosition());
	}

	public LocalDate earliestDate() {
		return netAssetLiabilityPositions.getLast().asAt;
	}

	public LocalDate latestDate() {
		return current.asAt;
	}

	public List<String> xAxisLabels() {
		List<String> xAxisLabels = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
		netAssetLiabilityPositions.forEach(position -> xAxisLabels.add(formatter.format(position.asAt)));
		Collections.reverse(xAxisLabels);
		return xAxisLabels;
	}

	public List<BigDecimal> yAxisNetValues() {
		List<Money> yAxisValues = new ArrayList<>();
		netAssetLiabilityPositions.forEach(position -> yAxisValues.add(position.getNetPosition()));
		Collections.reverse(yAxisValues);
		return yAxisValues.stream().map(Money::getAmount).toList();
	}

	public List<BigDecimal> yAxisAssetValues() {
		List<Money> yAxisValues = new ArrayList<>();
		netAssetLiabilityPositions.forEach(position -> yAxisValues.add(position.getAssetBalance()));
		Collections.reverse(yAxisValues);
		return yAxisValues.stream().map(Money::getAmount).toList();
	}

	public List<BigDecimal> yAxisLiabValues() {
		List<Money> yAxisValues = new ArrayList<>();
		netAssetLiabilityPositions.forEach(position -> yAxisValues.add(position.getLiabilityBalance()));
		Collections.reverse(yAxisValues);
		return yAxisValues.stream().map(Money::getAmount).toList();
	}

}

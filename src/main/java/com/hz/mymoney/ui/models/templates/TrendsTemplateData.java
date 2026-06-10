package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.internal.Movement;
import com.hz.mymoney.ui.models.charts.TrendData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TrendsTemplateData {
	@Getter
	private final String account;
	private final String trendType;
	private final List<Movement> movements;

	private LocalDate startDate() {
		return switch (trendType) {
			case "30d" -> LocalDate.now().minusDays(30);
			case "6m" -> LocalDate.now().minusMonths(5);
			case "1y" -> LocalDate.now().minusMonths(11);
			case "2y" -> LocalDate.now().minusYears(1).minusMonths(11);
			case "5y" -> LocalDate.now().minusYears(4).minusMonths(11);
			case "10y" -> LocalDate.now().minusYears(9).minusMonths(11);
			default -> throw new IllegalStateException("Unexpected value: " + trendType);
		};
	}

	private String groupBy(LocalDate date) {
		return switch (trendType) {
			case "30d" -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			case "6m", "1y", "2y" -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
			case "5y", "10y" -> date.format(DateTimeFormatter.ofPattern("yyyy"));
			default -> throw new IllegalStateException("Unexpected value: " + trendType);
		};
	}

	private String toDisplayName(String groupedName) {
		LocalDate date = switch (trendType) {
			case "30d" -> LocalDate.parse(groupedName, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			case "6m", "1y", "2y" -> LocalDate.parse(groupedName + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			case "5y", "10y" -> LocalDate.parse(groupedName + "-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			default -> throw new IllegalStateException("Unexpected value: " + trendType);
		};

		return switch (trendType) {
			case "30d" -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			case "6m", "1y" -> date.format(DateTimeFormatter.ofPattern("MMM"));
			case "2y" -> date.format(DateTimeFormatter.ofPattern("MMM-yy"));
			case "5y", "10y" -> date.format(DateTimeFormatter.ofPattern("yyyy"));
			default -> throw new IllegalStateException("Unexpected value: " + trendType);
		};
	}

	public BigDecimal total() {
		LocalDate start = startDate();
		LocalDate end = LocalDate.now();

		if (account.startsWith("Income:")) {
			return movements.stream()
					.filter(movement -> movement.isBetween(start, end))
					.map(Movement::amount)
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.abs();
		}
		return movements.stream()
				.filter(movement -> movement.isBetween(start, end))
				.map(Movement::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private TrendData buildTrendData(String name, BigDecimal value) {
		return new TrendData(name, account.startsWith("Income:") ? value.abs() : value);
	}

	public String dataSetAsJson() {
		JsonMapper mapper = JsonMapper.builder()
				.build();

		LocalDate start = startDate();
		LocalDate end = LocalDate.now();
		var yAxisValues = movements.stream()
				.filter(movement -> movement.isBetween(start, end))
				.collect(Collectors.groupingBy(movement -> groupBy(movement.date()),
						Collectors.mapping(Movement::amount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(e -> buildTrendData(toDisplayName(e.getKey()), e.getValue()))
				.toList();

		// Convert the array to JSON
		return mapper.writeValueAsString(yAxisValues);
	}

}
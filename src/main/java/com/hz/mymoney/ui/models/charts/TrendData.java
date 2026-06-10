package com.hz.mymoney.ui.models.charts;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// Chart data classes must be a POJO not a record for the JSON to conversion to work
@Data
@AllArgsConstructor
@JsonPropertyOrder({"name", "value"})
public class TrendData {
	public String name;
	public BigDecimal value;
}

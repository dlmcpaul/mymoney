package com.hz.mymoney.ui.models.charts;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

// Chart data classes must be a POJO not a record for the JSON to conversion to work
@Data
@AllArgsConstructor
@JsonPropertyOrder({"name", "value"})
public class InvestmentTotal {
	public String name;
	public int value;
}

package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;

public record SummedTransaction(String description, Money amount) {
}

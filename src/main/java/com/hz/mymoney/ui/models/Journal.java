package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;

public record Journal(String description, String destinationAccount, Money amount) {
}

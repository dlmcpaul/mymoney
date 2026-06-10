package com.hz.mymoney.ui.models;

import lombok.Data;

@Data
public class Toast {
	public final String level;
	public final String title;
	public final String message;

	public Toast(String level, String title, String message) {
		this.level = level;
		this.title = title;
		this.message = message;
	}
}

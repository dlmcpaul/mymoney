package com.hz.mymoney.ui.utilities;

import com.hz.mymoney.ui.models.internal.Menu;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;

public class PageSupport {
	static List<Menu> menuList = List.of(new Menu("Home","","Dashboard"),
			new Menu("Accounts","Accounts","Accounts"),
			new Menu("Equity","Equity","Equity"),
			new Menu("Investments","Investments","Investments"),
			new Menu("NetWorth","NetWorth","Net Worth"),
			new Menu("Superannuation","Super","Superannuation"),
			new Menu("Recurring","RecurringTransactions","Recurring"),
			new Menu("Tax","Tax","Taxation"),
			new Menu("Trends","Trends","Trends")
	);

	private PageSupport() {}

	public static void populateDefaultModelData(Model model, String version) {
		model.addAttribute("releaseVersion", version);
		model.addAttribute("today", LocalDate.now());
		model.addAttribute("menuList", menuList);
	}
}

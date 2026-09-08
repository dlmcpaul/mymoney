package com.hz.mymoney.ui.utilities;

import com.hz.mymoney.ui.models.support.Menu;
import com.hz.mymoney.ui.services.ModelBuilderService;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;

public final class PageSupport {
	private static final List<Menu> menuList = List.of(new Menu("Home","","Dashboard"),
			new Menu("Accounts","accounts","Accounts"),
			new Menu("Equity","equity","Equity"),
			new Menu("Investments","investments","Investments"),
			new Menu("NetWorth","net-worth","Net Worth"),
			new Menu("Superannuation","super","Superannuation"),
			new Menu("Recurring","recurring-transactions","Recurring"),
			new Menu("Tax","tax","Taxation"),
			new Menu("Trends","trends","Trends")
	);

	private static final String MONTHLY_INCOME_EXPENSE_FIELD = "monthlyChange";

	private PageSupport() {}

	public static void populateDefaultPageModelData(Model model, ModelBuilderService uiModelBuilderService) {
		LocalDate pnlDate = LocalDate.now().withDayOfMonth(1);

		model.addAttribute("readOnlyLedger", uiModelBuilderService.isLedgerReadOnly());
		populateCurrentPosition(model, uiModelBuilderService);
		populateScheduledTransactions(model, uiModelBuilderService);
		populateMonthlyIncomeExpense(model, uiModelBuilderService, pnlDate);
	}

	public static void populateCurrentPosition(Model model, ModelBuilderService uiModelBuilderService) {
		model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
	}

	public static void populateScheduledTransactions(Model model, ModelBuilderService uiModelBuilderService) {
		model.addAttribute("scheduledTransactions", uiModelBuilderService.getScheduledTransactions());
	}

	public static void populateMonthlyIncomeExpense(Model model, ModelBuilderService uiModelBuilderService, LocalDate pnlDate) {
		model.addAttribute("pnlDate", pnlDate);
		model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(pnlDate));
	}

	public static void populateDefaultModelData(Model model, String version) {
		model.addAttribute("releaseVersion", version);
		model.addAttribute("today", LocalDate.now());
		model.addAttribute("menuList", menuList);
	}
}

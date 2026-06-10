package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.data.models.internal.Schedule;
import com.hz.mymoney.ui.models.Toast;
import com.hz.mymoney.ui.services.UiModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@SessionAttributes("profitLossMonth")
@RequiredArgsConstructor
@Log4j2
public class HomeTemplate {
	protected static final String PROFIT_LOSS_MONTH = "profitLossMonth";
	protected static final String MONTHLY_INCOME_EXPENSE_FIELD = "monthlyChange";

	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	@ModelAttribute(PROFIT_LOSS_MONTH)
	public LocalDate profitLossMonth() {
		return LocalDate.now().withDayOfMonth(1);
	}

	// Load initial page
	@GetMapping("/")
	public String home(Model model,
	                   @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                   @ModelAttribute(value = "message", binding = false) Toast message,
	                   HtmxResponse htmxResponse) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute("scheduledTransactions", uiModelBuilderService.getScheduledTransactions());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			if (message != null) {
				htmxResponse.addTriggerAfterSettle("showMessage", message);
			}
		} catch (Exception e) {
			log.error("index Page Generation Exception {}", e.getMessage(), e);
		}
		return "index";
	}

	@GetMapping("/showScheduleModal")
	@HxRequest
	public View showScheduleModal(Model model, @RequestParam String scheduleDescription, HtmxResponse htmxResponse) {
		try {
			Optional<Schedule> schedule = uiModelBuilderService.getSchedule(scheduleDescription);
			schedule.ifPresent(value -> model.addAttribute("schedule", value));
			htmxResponse.addTriggerAfterSwap("showScheduleModal");
			return FragmentsRendering
					.fragment("fragments/Modals :: ScheduleModal")
					.build();
		} catch (Exception e) {
			log.error("Schedule Modal Generation Exception {}", e.getMessage(), e);
			htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Could not display modal because - " + e.getMessage()));
		}
		return FragmentsRendering
				.fragment("fragments/Schedules :: ScheduleList (schedules=${scheduledTransactions})")
				.build();
	}

	@GetMapping("/nextMonth")
	@HxRequest
	public View nextMonth(Model model, @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth) {
		try {
			profitLossMonth = profitLossMonth.withDayOfMonth(1).plusMonths(1);
			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
		} catch (Exception e) {
			log.error("Next Month Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@GetMapping("/prevMonth")
	@HxRequest
	public View prevMonth(Model model, @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth) {
		try {
			profitLossMonth = profitLossMonth.withDayOfMonth(1).minusMonths(1);
			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
		} catch (Exception e) {
			log.error("Prev Month Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@GetMapping("/distributionButtonClick")
	@HxRequest
	public View distributionButtonsClick(Model model, @RequestParam String button) {
		model.addAttribute("activeButton", button);
		return FragmentsRendering
				.fragment("fragments/Journal :: DistributionButtons")
				.build();
	}
}
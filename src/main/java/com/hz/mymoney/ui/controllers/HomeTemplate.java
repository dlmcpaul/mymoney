package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.data.models.coa.Schedule;
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
	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	// Load initial page
	@GetMapping("/")
	public String home(Model model,
	                   @ModelAttribute(value = "message", binding = false) Toast message,
	                   HtmxResponse htmxResponse) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
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

	@GetMapping("/income-expenses")
	@HxRequest
	public View loadIncomeAndExpenses(Model model, @RequestParam LocalDate pnlDate) {
		try {
			PageSupport.populateMonthlyIncomeExpense(model, uiModelBuilderService, pnlDate);
		} catch (Exception e) {
			log.error("Next Month Generation Exception {}", e.getMessage(), e);
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
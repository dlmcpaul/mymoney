package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.data.models.coa.Schedule;
import com.hz.mymoney.ui.models.Toast;
import com.hz.mymoney.ui.services.FileUpdateService;
import com.hz.mymoney.ui.services.ModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Log4j2
public class HomeController {
	private final ReleaseInfoContributor release;
	private final ModelBuilderService uiModelBuilderService;
	private final FileUpdateService uiDataUpdateService;

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

	@GetMapping("/schedule-modal")
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
			log.error("Income and Expense Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	// Save the ledger and schedules
	@PostMapping("/ledger")
	@HxRequest
	public View saveData(HtmxResponse htmxResponse) {
		try {
			uiDataUpdateService.saveData();
			htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "All Data Saved Successfully" ));
		} catch (Exception e) {
			log.error("Save Ledger Exception {}", e.getMessage(), e);
			htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Failed to Save All Data - Files may be corrupt" ));
		}
		return FragmentsRendering
				.fragment("fragments/Common :: SaveButton")
				.build();
	}

	@PostMapping("/reload")
	@HxRequest
	public View reload(Model model,
	                   HtmxResponse htmxResponse) {
		try {
			uiDataUpdateService.reloadFiles();

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
			model.addAttribute("today", LocalDate.now());

			if (uiModelBuilderService.isLedgerReadOnly()) {
				htmxResponse.addTrigger("showMessage", new Toast("error", "Error", "Failed to load Ledger.  See log for errors"));
			} else {
				htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Files Reloaded Successfully"));
			}
		} catch (Exception e) {
			log.error("Reload All Files Exception {}", e.getMessage(), e);
			htmxResponse.addTrigger("showMessage", new Toast("error", "Error", "Reload All Files failed"));
		}
		return FragmentsRendering
				.fragment("fragments/Common :: RefreshButton")
				.fragment("fragments/Common :: DashboardHeader")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/Schedules :: ScheduleList (schedules=${scheduledTransactions})")
				.fragment("fragments/Journal :: JournalForms (entryDate=${today})")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

}
package com.hz.mymoney.ui.controllers;

// Handles all post requests

import com.hz.mymoney.data.models.internal.Schedule;
import com.hz.mymoney.ui.models.Toast;
import com.hz.mymoney.ui.models.inputs.BasicJournalInput;
import com.hz.mymoney.ui.models.inputs.DistributionTypeInput;
import com.hz.mymoney.ui.models.inputs.DividendJournalInput;
import com.hz.mymoney.ui.services.UIDataUpdateService;
import com.hz.mymoney.ui.services.UiModelBuilderService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.hz.mymoney.ui.controllers.HomeTemplate.MONTHLY_INCOME_EXPENSE_FIELD;
import static com.hz.mymoney.ui.controllers.HomeTemplate.PROFIT_LOSS_MONTH;

@Controller
@SessionAttributes("profitLossMonth")
@RequiredArgsConstructor
@Log4j2
public class UpdatesController {
	private final UiModelBuilderService uiModelBuilderService;
	private final UIDataUpdateService uiDataUpdateService;

	@ModelAttribute(PROFIT_LOSS_MONTH)
	public LocalDate profitLossMonth() {
		return LocalDate.now().withDayOfMonth(1);
	}

	@PostMapping("/scheduleSkip")
	@HxRequest
	public View scheduleSkip(Model model,
	                         @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                         @RequestParam String scheduleDescription,
	                         HtmxResponse htmxResponse
	) {
		try {
			Optional<Schedule> schedule = uiModelBuilderService.getSchedule(scheduleDescription);
			schedule.ifPresent(uiDataUpdateService::skipSchedule);

			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			model.addAttribute("scheduledTransactions", uiModelBuilderService.getScheduledTransactions());

			htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Schedule " + scheduleDescription + " Skipped" ));
		} catch (Exception e) {
			log.error("Schedule Skip Exception {}", e.getMessage(), e);
			htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Schedule " + scheduleDescription + " Failed to Skip " + e.getMessage() ));
		}
		return FragmentsRendering
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/Schedules :: ScheduleList (schedules=${scheduledTransactions})")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@PostMapping("/schedulePost")
	@HxRequest
	public View schedulePost(Model model,
	                         @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                         @RequestParam String scheduleDescription,
	                         @RequestParam List<BigDecimal> amounts,
	                         HtmxResponse htmxResponse
	) {
		try {
			Optional<Schedule> schedule = uiModelBuilderService.getSchedule(scheduleDescription);
			schedule.ifPresent(s -> uiDataUpdateService.postSchedule(s, amounts));

			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			model.addAttribute("scheduledTransactions", uiModelBuilderService.getScheduledTransactions());

			htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Schedule " + scheduleDescription + " Posted" ));
		} catch (Exception e) {
			log.error("Schedule Post Exception {}", e.getMessage(), e);
			htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Schedule " + scheduleDescription + " Failed to Post " + e.getMessage() ));
		}
		return FragmentsRendering
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/Schedules :: ScheduleList (schedules=${scheduledTransactions})")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@PostMapping("/newJournal")
	@HxRequest
	public View newJournal(Model model,
	                       @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                       @ModelAttribute BasicJournalInput basicJournalInput,
	                       HtmxResponse htmxResponse
	) {
		try {
			if (basicJournalInput.isValid()) {
				uiDataUpdateService.addNewTransaction(basicJournalInput);
				htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Journal Entry Saved Successfully" ));
			} else {
				log.error("Invalid Journal Entry {}", basicJournalInput);
				htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Invalid Journal Entry" ));
			}

			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			model.addAttribute("defaultDate", basicJournalInput.getJournalDate());
		} catch (Exception e) {
			log.error("newJournal Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: NewJournal")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@PostMapping("/newDividend")
	@HxRequest
	public View newDividend(Model model,
	                       @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                       @ModelAttribute DividendJournalInput dividendInputJournal,
	                       HtmxResponse htmxResponse
	) {
		try {
			if (dividendInputJournal.isValid()) {
				uiDataUpdateService.addNewTransaction(dividendInputJournal);
				htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Dividend Entry Saved Successfully" ));
			} else {
				log.error("Invalid Dividend Entry {}", dividendInputJournal);
				htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Invalid Dividend Entry" ));
			}

			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			model.addAttribute("defaultDate", dividendInputJournal.getJournalDate());
		} catch (Exception e) {
			log.error("newDividend Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: NewDividendIncome")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@PostMapping("/newDistribution")
	@HxRequest
	public View newDistribution(Model model,
	                            @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                            @ModelAttribute DistributionTypeInput distributionInputTransaction,
	                            HtmxResponse htmxResponse
	) {
		try {
			if (distributionInputTransaction.isValid()) {
				uiDataUpdateService.addNewTransaction(distributionInputTransaction);
				htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Fund Distribution Entry Saved Successfully" ));
			} else {
				log.error("Invalid Fund Distribution Entry {}", distributionInputTransaction);
				htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", "Invalid Fund Distribution Entry" ));
			}

			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			model.addAttribute("defaultDate", distributionInputTransaction.getJournalDate());
			model.addAttribute("activeButton", distributionInputTransaction.getDistributionType());
		} catch (Exception e) {
			log.error("newDistribution Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: NewDistributionIncome")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	// Save the ledger and schedules
	@PostMapping("/save")
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
	                   @ModelAttribute(PROFIT_LOSS_MONTH) LocalDate profitLossMonth,
	                   HtmxResponse htmxResponse) {

		try {
			uiDataUpdateService.reloadFiles();

			model.addAttribute(PROFIT_LOSS_MONTH, profitLossMonth);
			model.addAttribute("currentPosition", uiModelBuilderService.createCurrentPosition());
			model.addAttribute(MONTHLY_INCOME_EXPENSE_FIELD, uiModelBuilderService.createMonthlyIncomeExpense(profitLossMonth));
			model.addAttribute("scheduledTransactions", uiModelBuilderService.getScheduledTransactions());
			model.addAttribute("readOnlyLedger", uiModelBuilderService.isLedgerReadOnly());
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
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

}

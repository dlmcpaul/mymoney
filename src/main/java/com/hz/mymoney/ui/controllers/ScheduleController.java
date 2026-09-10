package com.hz.mymoney.ui.controllers;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
@Log4j2
public class ScheduleController {
	private final ModelBuilderService uiModelBuilderService;
	private final FileUpdateService uiDataUpdateService;

	@PostMapping("/skip")
	@HxRequest
	public View scheduleSkip(Model model,
	                         @RequestParam String scheduleDescription,
	                         HtmxResponse htmxResponse
	) {
		try {
			Optional<Schedule> schedule = uiModelBuilderService.getSchedule(scheduleDescription);
			schedule.ifPresent(uiDataUpdateService::skipSchedule);

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);

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

	@PostMapping("/post")
	@HxRequest
	public View schedulePost(Model model,
	                         @RequestParam String scheduleDescription,
	                         @RequestParam List<BigDecimal> amounts,
	                         HtmxResponse htmxResponse
	) {
		try {
			Optional<Schedule> schedule = uiModelBuilderService.getSchedule(scheduleDescription);
			schedule.ifPresent(s -> uiDataUpdateService.postSchedule(s, amounts));

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);

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
}

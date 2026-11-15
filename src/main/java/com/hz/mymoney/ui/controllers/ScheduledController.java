package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.ui.services.ModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Log4j2
public class ScheduledController {
	private final ReleaseInfoContributor release;
	private final ModelBuilderService uiModelBuilderService;

	@GetMapping("/scheduled")
	public String transactions(Model model) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("schedules", uiModelBuilderService.getScheduledTransactions());
			model.addAttribute("incomeExpense", uiModelBuilderService.createIncomeExpense());
		} catch (Exception e) {
			log.error("Scheduled Page Generation Exception {}", e.getMessage(), e);
		}
		return "Scheduled";
	}
}

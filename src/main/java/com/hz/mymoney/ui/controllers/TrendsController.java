package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.ui.services.ModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

@Controller
@RequiredArgsConstructor
@Log4j2
public class TrendsController {
	private final ReleaseInfoContributor release;
	private final ModelBuilderService uiModelBuilderService;

	@GetMapping("/trends")
	public String accounts(Model model) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("activeButton", "30d");
			model.addAttribute("trendsData", uiModelBuilderService.createTrendsTemplateData("", "30d"));
		} catch (Exception e) {
			log.error("Trends Page Generation Exception {}", e.getMessage(), e);
		}
		return "Trends";
	}

	@GetMapping("/click/trend-period")
	@HxRequest
	public View trendPeriodButtonClick(Model model, @RequestParam String button, @RequestParam String accounts) {
		try {
			model.addAttribute("activeButton", button);
			model.addAttribute("trendsData", uiModelBuilderService.createTrendsTemplateData(accounts, button));
		}  catch (Exception e) {
			log.error("Trends Period Button Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("Trends :: #trendPeriodButtons")
				.fragment("Trends :: #TrendsGraph")
				.fragment("Trends :: #trendsTotal")
				.build();
	}

	@GetMapping("/trends/account")
	@HxRequest
	public View accountChange(Model model, @RequestParam(name = "accounts") String accountName, @RequestParam String trendPeriod) {
		try {
			model.addAttribute("activeButton", trendPeriod);
			model.addAttribute("trendsData", uiModelBuilderService.createTrendsTemplateData(accountName, trendPeriod));
		}  catch (Exception e) {
			log.error("Trends Account Change Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("Trends :: #trendPeriodButtons")
				.fragment("Trends :: #TrendsGraph")
				.fragment("Trends :: #trendsTotal")
				.build();
	}
}

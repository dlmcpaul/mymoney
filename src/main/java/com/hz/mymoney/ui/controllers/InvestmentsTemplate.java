package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.ui.services.UiModelBuilderService;
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
public class InvestmentsTemplate {
	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	@GetMapping("/Investments")
	public String investments(Model model) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("investmentsData", uiModelBuilderService.createCurrentInvestmentsTemplateData());
			model.addAttribute("nextDisplayMode", "historical");
			model.addAttribute("header", "Current Investments");
		} catch (Exception e) {
			log.error("Investments Page Generation Exception {}", e.getMessage(), e);
		}
		return "Investments";
	}

	@GetMapping("/Investments")
	@HxRequest
	public View switchInvestments(Model model, @RequestParam("nextDisplayMode") String nextDisplayMode) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("nextDisplayMode", nextDisplayMode.equals("current") ? "historical" : "current");
			model.addAttribute("header", nextDisplayMode.equals("current") ? "Current Investments" : "Historical Investments");
			model.addAttribute("investmentsData", nextDisplayMode.equals("current") ? uiModelBuilderService.createCurrentInvestmentsTemplateData() : uiModelBuilderService.createPriorInvestmentsTemplateData());
		} catch (Exception e) {
			log.error("Switch Investments Page Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/Common :: InvestmentHeader")
				.fragment("fragments/Investment :: InvestmentTable")
				.fragment("fragments/Investment :: InvestmentChart")
				.build();
	}

}

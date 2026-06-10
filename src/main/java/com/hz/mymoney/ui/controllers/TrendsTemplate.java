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
public class TrendsTemplate {
	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	@GetMapping("/Trends")
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

	@GetMapping("/trendTypeButtonClick")
	@HxRequest
	public View distributionButtonsClick(Model model, @RequestParam String button, @RequestParam String accounts) {
		model.addAttribute("activeButton", button);
		model.addAttribute("trendsData", uiModelBuilderService.createTrendsTemplateData(accounts, button));
		return FragmentsRendering
				.fragment("Trends :: #TrendTypeButtons")
				.fragment("Trends :: #trendsGraph")
				.fragment("Trends :: #trendsTotal")
				.build();
	}

	@GetMapping("/accountChanged")
	@HxRequest
	public View accountChange(Model model, @RequestParam(name = "accounts") String accountName, @RequestParam String trendType) {
		try {
			model.addAttribute("trendsData", uiModelBuilderService.createTrendsTemplateData(accountName, trendType));
		}  catch (Exception e) {
			log.error("Trends Page Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("Trends :: #trendsGraph")
				.fragment("Trends :: #trendsTotal")
				.build();
	}
}

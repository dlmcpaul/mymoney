package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.ui.services.UiModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Log4j2
public class EquityTemplate {
	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	@GetMapping("/Equity")
	public String accounts(Model model) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("accounts", uiModelBuilderService.createEquityTemplateData());
		} catch (Exception e) {
			log.error("Equity Page Generation Exception {}", e.getMessage(), e);
		}
		return "Equity";
	}
}

package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.ui.models.RunningTotaler;
import com.hz.mymoney.ui.models.Transaction;
import com.hz.mymoney.ui.models.templates.TaxTemplateData;
import com.hz.mymoney.ui.models.templates.TaxYear;
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

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Log4j2
public class TaxTemplate {
	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	private TaxTemplateData taxTemplateData;

	@GetMapping("/Tax")
	public String tax(Model model) {
		try {
			taxTemplateData = uiModelBuilderService.createTaxTemplateData();
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("taxData", taxTemplateData);
			model.addAttribute("transactions", new ArrayList<Transaction>());
			model.addAttribute("totaler", new RunningTotaler());
		} catch (Exception e) {
			log.error("Tax Page Generation Exception {}", e.getMessage(), e);
		}
		return "TaxEstimate";
	}

	@GetMapping("/Tax/Breakdown")
	@HxRequest
	public View breakdown(Model model, @RequestParam("tax_year_code") String taxYearCode, @RequestParam("breakdown_code") String breakdownType) {
		try {
			model.addAttribute("releaseVersion", release.getVersion());

			if (taxTemplateData == null) {
				taxTemplateData = uiModelBuilderService.createTaxTemplateData();
			}

			TaxYear taxYear = null;
			if (taxYearCode != null && taxYearCode.equalsIgnoreCase("current")) {
				taxYear = taxTemplateData.currentYear;
			} else if (taxYearCode != null && taxYearCode.equalsIgnoreCase("last")) {
				taxYear = taxTemplateData.lastYear;
			}

			if (taxYear != null) {
				List<Transaction> transactions = switch (breakdownType) {
					case "income" -> taxTemplateData.getTransactionsFor("Income", taxYear);
					case "investment" -> taxTemplateData.getTransactionsFor("Investment", taxYear);
					case "tax_expenses" -> taxTemplateData.getTransactionsFor("TaxDeduction", taxYear);
					case "donations" -> taxTemplateData.getTransactionsFor("Donations", taxYear);
					case "tax_salary" -> taxTemplateData.getTransactionsFor("SalaryTaxPaid", taxYear);
					case "tax_payg" -> taxTemplateData.getTransactionsFor("PAYGTaxPaid", taxYear);
					case "franking_credits" -> taxTemplateData.getTransactionsFor("FrankingCredits", taxYear);
					default -> throw new IllegalStateException("Unexpected value: " + breakdownType);
				};
				model.addAttribute("transactions", transactions);
				model.addAttribute("totaler", new RunningTotaler());
			} else {
				log.error("Tax Page Generation Exception: Invalid tax_year");
			}
		} catch (Exception e) {
			log.error("Tax Breakdown Fragment Generation Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Tax :: Breakdown")
				.build();
	}
}

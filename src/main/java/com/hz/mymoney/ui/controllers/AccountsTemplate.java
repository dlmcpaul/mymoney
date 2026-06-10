package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.components.ReleaseInfoContributor;
import com.hz.mymoney.exceptions.UnexpectedDataException;
import com.hz.mymoney.ui.models.RunningTotaler;
import com.hz.mymoney.ui.services.UiModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@Log4j2
public class AccountsTemplate {
	private final ReleaseInfoContributor release;
	private final UiModelBuilderService uiModelBuilderService;

	@GetMapping("/Accounts")
	public String accounts(Model model) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("accounts", uiModelBuilderService.createAccountsTemplateData());
			model.addAttribute("incomeExpenseHistory", uiModelBuilderService.createIncomeExpenseHistory());
		} catch (Exception e) {
			log.error("Accounts Page Generation Exception {}", e.getMessage(), e);
		}
		return "Accounts";
	}

	@GetMapping("/Accounts/{accountName}")
	public String account(@PathVariable String accountName, Model model, @RequestParam(value = "fy", required = false) LocalDate financialYearStart) {
		try {
			PageSupport.populateDefaultModelData(model, release.getVersion());
			model.addAttribute("accountName", accountName);
			model.addAttribute("singleAccount", uiModelBuilderService.createSingleAccountData(accountName, financialYearStart));
			model.addAttribute("totaler", new RunningTotaler(accountName));
		} catch (Exception e) {
			log.error("SingleAccount Page Generation Exception {}", e.getMessage(), e);
		}
		return "SingleAccount";
	}

	@GetMapping("/Accounts/changeFY")
	@HxRequest
	public View changeFinancialYear(Model model, @RequestParam LocalDate currentFY, @RequestParam String direction) {
		try {
			if (direction.equals("prev")) {
				LocalDate fyStart = currentFY.minusYears(1);
				model.addAttribute("accounts", uiModelBuilderService.createAccountsTemplateData(fyStart, currentFY.minusDays(1)));
			} else if (direction.equals("next")) {
				LocalDate fyStart = currentFY.plusYears(1);
				model.addAttribute("accounts", uiModelBuilderService.createAccountsTemplateData(fyStart, fyStart.plusYears(1).minusDays(1)));
			} else {
				throw new UnexpectedDataException("Invalid direction passed only prev or next allowed");
			}
		} catch (Exception e) {
			log.error("prevFinancialYear Page Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/Common :: AccountsHeader")
				.fragment("fragments/Account :: AccountsBody")
				.build();
	}

}

package com.hz.mymoney.ui.controllers;

// Handles all post requests

import com.hz.mymoney.ui.models.Toast;
import com.hz.mymoney.ui.models.inputs.BasicJournalInput;
import com.hz.mymoney.ui.models.inputs.DistributionTypeInput;
import com.hz.mymoney.ui.models.inputs.DividendJournalInput;
import com.hz.mymoney.ui.services.FileUpdateService;
import com.hz.mymoney.ui.services.ModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

@Controller
@RequestMapping("/journal")
@RequiredArgsConstructor
@Log4j2
public class JournalController {
	private final ModelBuilderService uiModelBuilderService;
	private final FileUpdateService uiDataUpdateService;

	@GetMapping("/distribution-type")
	@HxRequest
	public View distributionButtonsClick(Model model, @RequestParam String button) {
		model.addAttribute("activeButton", button);
		return FragmentsRendering
				.fragment("fragments/Journal :: DistributionButtons")
				.build();
	}

	@PostMapping("/basic")
	@HxRequest
	public View newJournal(Model model,
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

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
			model.addAttribute("defaultDate", basicJournalInput.getJournalDate());
		} catch (Exception e) {
			log.error("newJournal Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: BasicJournal")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@PostMapping("/dividend")
	@HxRequest
	public View newDividend(Model model,
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

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
			model.addAttribute("defaultDate", dividendInputJournal.getJournalDate());
		} catch (Exception e) {
			log.error("newDividend Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: DividendJournal")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@PostMapping("/distribution")
	@HxRequest
	public View newDistribution(Model model,
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

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
			model.addAttribute("defaultDate", distributionInputTransaction.getJournalDate());
			model.addAttribute("activeButton", distributionInputTransaction.getDistributionType());
		} catch (Exception e) {
			log.error("newDistribution Exception {}", e.getMessage(), e);
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: DistributionJournal")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

}

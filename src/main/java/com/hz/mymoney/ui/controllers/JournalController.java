package com.hz.mymoney.ui.controllers;

// Handles all post requests

import com.hz.mymoney.ui.exceptions.InvalidJournalEntryException;
import com.hz.mymoney.ui.models.Toast;
import com.hz.mymoney.ui.models.inputs.BasicJournalInput;
import com.hz.mymoney.ui.models.inputs.DistributionTypeInput;
import com.hz.mymoney.ui.models.inputs.DividendJournalInput;
import com.hz.mymoney.ui.services.FileUpdateService;
import com.hz.mymoney.ui.services.ModelBuilderService;
import com.hz.mymoney.ui.utilities.PageSupport;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRetarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import static io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxReswap.innerHtml;

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
	                       HtmxResponse htmxResponse) {
		if (basicJournalInput.isValid()) {
			uiDataUpdateService.addNewTransaction(basicJournalInput);
			htmxResponse.addTrigger("resetForm", true);
			htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Journal Entry Saved Successfully" ));
		} else {
			log.error("Invalid Journal Entry {}", basicJournalInput);
			throw new InvalidJournalEntryException("Invalid Journal Entry");
		}

		PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
		model.addAttribute("defaultDate", basicJournalInput.getJournalDate());

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
	                       HtmxResponse htmxResponse) {
			if (dividendInputJournal.isValid()) {
				uiDataUpdateService.addNewTransaction(dividendInputJournal);
				htmxResponse.addTrigger("resetForm", true);
				htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Dividend Entry Saved Successfully" ));
			} else {
				log.error("Invalid Dividend Entry {}", dividendInputJournal);
				throw new InvalidJournalEntryException("Invalid Dividend Entry");
			}

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
			model.addAttribute("defaultDate", dividendInputJournal.getJournalDate());

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
	                            HtmxResponse htmxResponse) {
			if (distributionInputTransaction.isValid()) {
				uiDataUpdateService.addNewTransaction(distributionInputTransaction);
				htmxResponse.addTrigger("resetForm", true);
				htmxResponse.addTrigger("showMessage", new Toast("success", "Success", "Fund Distribution Entry Saved Successfully" ));
			} else {
				log.error("Invalid Fund Distribution Entry {}", distributionInputTransaction);
				throw new InvalidJournalEntryException("Invalid Fund Distribution Entry");
			}

			PageSupport.populateDefaultPageModelData(model, uiModelBuilderService);
			model.addAttribute("defaultDate", distributionInputTransaction.getJournalDate());
			model.addAttribute("activeButton", distributionInputTransaction.getDistributionType());

		return FragmentsRendering
				.fragment("fragments/Journal :: DistributionJournal")
				.fragment("fragments/Account :: BalanceSheet")
				.fragment("fragments/IncomeExpense :: IncomeExpenseHeader")
				.fragment("fragments/IncomeExpense :: IncomeExpenseBody")
				.build();
	}

	@ExceptionHandler(InvalidJournalEntryException.class)
	@HxRetarget(value = "#JournalError")
	public View handleError(InvalidJournalEntryException ex, HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		if (htmxRequest.isHtmxRequest()) {
			htmxResponse.addTrigger("showMessage", new Toast("error", "Failed", ex.getMessage() ));
			htmxResponse.setReswap(innerHtml());
		}
		return FragmentsRendering
				.fragment("fragments/Common :: JournalError")
				.build();
	}
}

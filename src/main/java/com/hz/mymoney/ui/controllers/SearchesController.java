package com.hz.mymoney.ui.controllers;

import com.hz.mymoney.configuration.AccountConstants;
import com.hz.mymoney.ui.services.UILogicService;
import com.hz.mymoney.ui.services.UiModelBuilderService;
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
public class SearchesController {
	private final UiModelBuilderService uiModelBuilderService;
	private final UILogicService uiLogicService;

	@GetMapping("/descriptionSearch")
	@HxRequest
	public View descriptionSearch(Model model, @RequestParam String description, @RequestParam String label, @RequestParam String id) {
		List<UILogicService.JournalResult> prefillResults = uiLogicService.smartPreFill(description);
		List<UILogicService.JournalResult> prefillExactResults = uiLogicService.smartPreFillExact(description);
		try {
			List<String> descriptions = prefillResults.stream()
					.map(UILogicService.JournalResult::description)
					.sorted()
					.toList();
			String descriptionValue = descriptions.size() != 1 ? description : descriptions.getFirst();

			model.addAttribute("id", id);
			model.addAttribute("label", label);
			model.addAttribute("value", descriptionValue);
			model.addAttribute("descriptions", descriptions);
			model.addAttribute("showError", descriptions.isEmpty());

			if (prefillResults.size() == 2) {
				prefillResults.forEach(log::info);
			}

			if (prefillResults.size() == 1) {
				model.addAttribute("debitAccount", prefillResults.getFirst().debitAccount());
				model.addAttribute("creditAccount", prefillResults.getFirst().creditAccount());
			} else if (prefillExactResults.size() == 1) {
				model.addAttribute("debitAccount", prefillExactResults.getFirst().debitAccount());
				model.addAttribute("creditAccount", prefillExactResults.getFirst().creditAccount());
			}
		} catch (Exception e) {
			log.error("Description Search Generation Exception {}", e.getMessage(), e);
		}

		if (prefillResults.size() == 1 || prefillExactResults.size() == 1) {
			return FragmentsRendering
					.fragment("fragments/Journal :: DescriptionInputField")
					.fragment("fragments/Journal :: AccountInputField (id='from-account', label='From Account', value=${debitAccount}, accounts=null, showError=false)")
					.fragment("fragments/Journal :: AccountInputField (id='to-account', label='To Account', value=${creditAccount}, accounts=null, showError=false)")
					.build();
		}

		return FragmentsRendering
				.fragment("fragments/Journal :: DescriptionInputField")
				.build();
	}

	@GetMapping("/investmentsSearch")
	@HxRequest
	public View investmentSearch(Model model, @RequestParam(name = "accounts") String searchValue, @RequestParam String id, @RequestParam String label) {
		try {
			List<String> investmentAccounts = uiModelBuilderService.searchAccounts(AccountConstants.SHARES + searchValue);

			if (investmentAccounts.size() == 1) {
				searchValue = investmentAccounts.getFirst();
			}

			model.addAttribute("id", id);
			model.addAttribute("label", label);
			model.addAttribute("accounts", investmentAccounts);
			model.addAttribute("value", searchValue);
			model.addAttribute("showError", investmentAccounts.isEmpty());
		} catch (Exception e) {
			log.error("Investment Search Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/Journal :: InvestmentAccountInputField")
				.build();
	}

	@GetMapping("/accountSearch")
	@HxRequest
	public View accountSearch(Model model, @RequestParam(name = "accounts") String searchValue, @RequestParam String id, @RequestParam String label) {
		try {
			boolean showError = false;

			searchValue = uiLogicService.smartShortcuts(searchValue);
			if (searchValue.length() > (searchValue.lastIndexOf(":") + 1) && searchValue.split(":").length == 2) {
				searchValue = uiLogicService.smartLookahead(searchValue, uiModelBuilderService.searchAccounts(searchValue));
			}

			List<String> predictedAccounts = uiModelBuilderService.searchAccounts(searchValue);

			if (predictedAccounts.size() == 1) {
				searchValue = uiModelBuilderService.searchAccounts(searchValue).getFirst();
				predictedAccounts = new ArrayList<>();
			} else if (predictedAccounts.size() > 20) {
				predictedAccounts = predictedAccounts.subList(0, 20);
			} else {
				showError = predictedAccounts.isEmpty();
			}

			model.addAttribute("id", id);
			model.addAttribute("label", label);
			model.addAttribute("value", searchValue);
			model.addAttribute("accounts", predictedAccounts);
			model.addAttribute("showError", showError);
		} catch (Exception e) {
			log.error("Account Search Generation Exception {}", e.getMessage(), e);
		}
		return FragmentsRendering
				.fragment("fragments/Journal :: AccountInputField")
				.build();
	}

}

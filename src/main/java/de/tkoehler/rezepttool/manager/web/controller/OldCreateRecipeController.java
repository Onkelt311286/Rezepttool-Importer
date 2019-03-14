package de.tkoehler.rezepttool.manager.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tkoehler.rezepttool.manager.services.ImporterService;
import de.tkoehler.rezepttool.manager.services.ImporterServiceException;
import de.tkoehler.rezepttool.manager.services.ManagerService;
import de.tkoehler.rezepttool.manager.services.ManagerServiceException;
import de.tkoehler.rezepttool.manager.services.ManagerServiceRecipeExistsException;
import de.tkoehler.rezepttool.manager.web.model.IngredientWebInput;
import de.tkoehler.rezepttool.manager.web.model.RecipeWebInput;
import de.tkoehler.rezepttool.manager.web.model.UrlWrapper;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class OldCreateRecipeController {

	private ImporterService importerService;
	private ManagerService managerService;
	
	public OldCreateRecipeController(ImporterService importerService, ManagerService managerService) {
		this.importerService = importerService;
		this.managerService = managerService;
	}

	@PostMapping(value = "/createRecipe", params = { "newRecipe" })
	public String initializeCreateRecipePage(final ModelMap model, final HttpSession session) {
		log.info("init createRecipe.html");
		String url = "https://www.chefkoch.de/rezepte/556631153485020/Antipasti-marinierte-Champignons.html";
		UrlWrapper urlWrapper = UrlWrapper.builder().url(url).build();
		session.setAttribute("loaded", false);
		model.addAttribute("status", urlWrapper);
		return "createRecipe";
	}

	@PostMapping(value = "/createRecipe", params = { "back" })
	public String backToCreateRecipePage(final UrlWrapper urlWrapper, final ModelMap model, final HttpSession session) {
		if ((boolean) session.getAttribute("loaded")) {
			log.info("back to createRecipe.html");
			session.setAttribute("loaded", false);
			model.addAttribute("status", urlWrapper);
			return "createRecipe";
		}
		else return "redirect:/";
	}

	@RequestMapping(value = "/createRecipe", params = { "load" })
	public String createRecipeFromChefkochURL(final UrlWrapper urlWrapper, final ModelMap model, HttpSession session) {
		log.info("Loading");
		try {
			RecipeWebInput loadedRecipe = importerService.importRecipe(urlWrapper.getUrl());
			model.addAttribute("recipe", loadedRecipe);
			session.setAttribute("loaded", true);
		}
		catch (ImporterServiceException e) {
			log.error("Fehler beim Erstellen!", e);
			model.addAttribute("status", urlWrapper);
			model.addAttribute("errortext", "Es ist ein unerwarteter Fehler aufgetreten: " + e.getMessage());
			return "createRecipe";
		}
		log.info("Loaded");
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "save" })
	public String saveRecipe(final RecipeWebInput recipe, final ModelMap model, HttpSession session) {
		log.info("saving");
		model.addAttribute("recipe", recipe);
		try {
			managerService.saveRecipe(recipe);
			session.setAttribute("saved", true);
			model.addAttribute("success", "Rezept '" + recipe.getName() + "' erfolgreich gespeichert");
		}
		catch (ManagerServiceRecipeExistsException e) {
			session.setAttribute("saved", false);
			model.addAttribute("errortext", "Ein Rezept mit dem Namen und der URL erxistiert bereits!");
			return "createRecipe";
		}
		catch (ManagerServiceException e) {
			log.error("Fehler beim Speichern!", e);
			session.setAttribute("saved", false);
			model.addAttribute("errortext", "Es ist ein unerwarteter Fehler aufgetreten: " + e.getMessage());
			return "createRecipe";
		}
		log.info("saved");
		return "redirect:/";
	}

	@RequestMapping(value = "/createRecipe", params = { "addIngredient" })
	public String addIngredient(final RecipeWebInput recipe, ModelMap model) {
		recipe.getIngredients().add(new IngredientWebInput());
		model.addAttribute("recipe", recipe);
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "removeIngredient" })
	public String removeIngredient(final RecipeWebInput recipe, final HttpServletRequest req, ModelMap model) {
		final int rowId = Integer.valueOf(req.getParameter("removeIngredient"));
		recipe.getIngredients().remove(rowId);
		model.addAttribute("recipe", recipe);
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "addCategory" })
	public String addCategroy(final RecipeWebInput recipe, ModelMap model) {
		recipe.getCategories().add("");
		model.addAttribute("recipe", recipe);
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "removeCategory" })
	public String removeCategory(final RecipeWebInput recipe, final HttpServletRequest req, ModelMap model) {
		log.info("Removing");
		final int rowId = Integer.valueOf(req.getParameter("removeCategory"));
		recipe.getCategories().remove(rowId);
		log.info(recipe.getCategories().stream().toString());
		model.addAttribute("recipe", recipe);
		return "createRecipe";
	}
}
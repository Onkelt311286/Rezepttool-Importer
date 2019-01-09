package de.tkoehler.rezepttool.manager.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tkoehler.rezepttool.manager.repositories.model.Recipe;
import de.tkoehler.rezepttool.manager.repositories.model.RecipeIngredient;
import de.tkoehler.rezepttool.manager.services.ImporterService;
import de.tkoehler.rezepttool.manager.services.ImporterServiceException;
import de.tkoehler.rezepttool.manager.web.model.UrlWrapper;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class ImportRecipeController {

	private ImporterService importerService;

	public ImportRecipeController(ImporterService importerService) {
		this.importerService = importerService;
	}

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@PostMapping("/")
	public String initializeCreateRecipePage(ModelMap model, HttpSession session) {
		log.info("init createRecipe.html");
		String url = "https://www.chefkoch.de/rezepte/556631153485020/Antipasti-marinierte-Champignons.html";
		UrlWrapper urlWrapper = UrlWrapper.builder().url(url).build();
		model.addAttribute("status", urlWrapper);
		session.setAttribute("loaded", false);
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "load" })
	public String createRecipeFromChefkochURL(final UrlWrapper urlWrapper, final BindingResult bindingResult, final ModelMap model, HttpSession session) {
		log.info("Loading");
		if (bindingResult.hasErrors()) { return "index"; }
		log.info(urlWrapper.getUrl());
		try {
			Recipe loadedRecipe = importerService.loadRecipe(urlWrapper.getUrl());
			model.addAttribute("recipe", loadedRecipe);
			session.setAttribute("loaded", true);
			log.info(loadedRecipe.toString());
			// for (RecipeIngredient ingred : loadedRecipe.getIngredients()) {
			// log.info(ingred.toString());
			// }
			log.info("Loaded");
		}
		catch (ImporterServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "createRecipe";
	}

	@RequestMapping(value = "/loading", params = { "wait" })
	public String startLoading(final Recipe recipe, ModelMap model) {
		log.info("Loading loading.html");
		log.info(recipe.getName());
		return "loading";
	}

	@GetMapping(value = "/loading")
	public String alternativeLoading(final Recipe recipe, final BindingResult bindingResult, ModelMap model) {
		log.info("Alternative Loading loading.html");
		log.info(recipe.getName());
		log.info(model.get("recipe").toString());
		model.addAttribute("recipe", recipe);
		return "loading";
	}

	@RequestMapping(value = "/createRecipe", params = { "save" })
	public String saveRecipe(final Recipe recipe, final BindingResult bindingResult, final ModelMap model) {
		log.info("saving");
		log.info(recipe.toString());
		log.info(model.get("recipe").toString());
		// for (RecipeIngredient ingred : recipe.getIngredients()) {
		// log.info(ingred.toString());
		// }
		
		for (RecipeIngredient recipeIngred : recipe.getIngredients()) {
			recipeIngred.setRecipe(recipe);
			recipeIngred.getIngredient().addRecipeIngredient(recipeIngred);
		}
		

		 try {
			importerService.saveRecipe(recipe);
		}
		catch (ImporterServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("saved");

		// return "redirect:/query?q=Thymeleaf+Is+Great!";
		return "redirect:/";
	}

	@RequestMapping(value = "/createRecipe", params = { "addIngredient" })
	public String addIngredient(final Recipe recipe, final BindingResult bindingResult) {
		recipe.addRecipeIngredient(new RecipeIngredient());
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "removeIngredient" })
	public String removeIngredient(final Recipe recipe, final BindingResult bindingResult, final HttpServletRequest req) {
		final int rowId = Integer.valueOf(req.getParameter("removeIngredient"));
		recipe.getIngredients().remove(rowId);
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "addCategory" })
	public String addCategroy(final Recipe recipe, final BindingResult bindingResult) {
		recipe.getCategories().add("");
		return "createRecipe";
	}

	@RequestMapping(value = "/createRecipe", params = { "removeCategory" })
	public String removeCategory(final Recipe recipe, final BindingResult bindingResult, final HttpServletRequest req) {
		final int rowId = Integer.valueOf(req.getParameter("removeCategory"));
		recipe.getCategories().remove(rowId);
		return "createRecipe";
	}
}
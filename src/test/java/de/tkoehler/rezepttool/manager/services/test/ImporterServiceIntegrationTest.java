package de.tkoehler.rezepttool.manager.services.test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import de.tkoehler.rezepttool.manager.repositories.IngredientRepository;
import de.tkoehler.rezepttool.manager.services.ImporterServiceException;
import de.tkoehler.rezepttool.manager.services.ImporterServiceImpl;
import de.tkoehler.rezepttool.manager.services.model.ChefkochRecipe;
import de.tkoehler.rezepttool.manager.services.recipeparser.ChefkochRecipeParserImpl;
import de.tkoehler.rezepttool.manager.services.recipeparser.RecipeParserException;
import de.tkoehler.rezepttool.manager.web.model.IngredientWebInputCreate;
import de.tkoehler.rezepttool.manager.web.model.RecipeWebInputCreate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("/test.properties")
@RunWith(SpringRunner.class)
public class ImporterServiceIntegrationTest {

	@Autowired
	private ImporterServiceImpl importerService;
	@Autowired
	private ChefkochRecipeParserImpl recipeParser;
	@Autowired
	private IngredientRepository ingredientRepository;

	@Test
	public void serviceLoadRecipe_ExistingURL_CorrectIngredCount() throws ImporterServiceException {
		String url = "https://www.chefkoch.de/rezepte/556631153485020/Antipasti-marinierte-Champignons.html";
		RecipeWebInputCreate recipe = importerService.loadRecipe(url);
		assertThat(recipe.getIngredients().size(), is(7));
	}

	@Test
	public void parserParseRecipe_ExistingURL_CorrectIngredCount() throws RecipeParserException {
		String url = "https://www.chefkoch.de/rezepte/556631153485020/Antipasti-marinierte-Champignons.html";
		de.tkoehler.rezepttool.manager.services.model.ChefkochRecipe recipe = (ChefkochRecipe) recipeParser.parseRecipe(url);
		assertThat(recipe.getPrintPageData().getIngredients().size(), is(7));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void serviceLoadRecipe_loadIfDBNotEmpty_CorrectIngredNames() throws ImporterServiceException {
		String url1 = "https://www.chefkoch.de/rezepte/2280021363771917/Knoblauch-Champignons.html";
		String url2 = "https://www.chefkoch.de/rezepte/556631153485020/Antipasti-marinierte-Champignons.html";
		RecipeWebInputCreate recipe1 = importerService.loadRecipe(url1);
		importerService.saveRecipe(recipe1);
		RecipeWebInputCreate recipe2 = importerService.loadRecipe(url2);
		assertThat(recipe2.getIngredients(), hasItems(
				hasProperty("name", is("Champignons, kleine frische")),
				hasProperty("name", is("Knoblauchzehe(n)")),
				hasProperty("name", is("Olivenöl")),
				hasProperty("name", is("Rosmarin")),
				hasProperty("name", is("Balsamico")),
				hasProperty("name", is("Salz")),
				hasProperty("name", is("Blattpetersilie, gehackte"))));
	}

	@Test
	public void saveRecipe_duplicateIngredient_savesNoDuplicate() throws ImporterServiceException {
		RecipeWebInputCreate recipe1 = RecipeWebInputCreate.builder()
				.name("Recipe1")
				.url("Recipe1Url")
				.instructions("Instructions1")
				.difficulty("simpel")
				.ingredients(Arrays.asList(
						IngredientWebInputCreate.builder()
								.name("Ingredient1")
								.department("Department1")
								.originalName("Ingredient1")
								.build(),
						IngredientWebInputCreate.builder()
								.name("Ingredient2")
								.department("Department2")
								.originalName("Ingredient")
								.build()))

				.build();
		RecipeWebInputCreate recipe2 = RecipeWebInputCreate.builder()
				.name("Recipe2")
				.url("Recipe2Url")
				.instructions("Instructions2")
				.difficulty("simpel")
				.ingredients(Arrays.asList(
						IngredientWebInputCreate.builder()
								.name("Ingredient3")
								.department("Department3")
								.originalName("Ingredient3")
								.build(),
						IngredientWebInputCreate.builder()
								.name("Ingredient2")
								.department("Department2")
								.originalName("Ingredient")
								.build()))

				.build();
		importerService.saveRecipe(recipe1);
		importerService.saveRecipe(recipe2);
		
		assertThat(ingredientRepository.count(), is(3L));
	}
}

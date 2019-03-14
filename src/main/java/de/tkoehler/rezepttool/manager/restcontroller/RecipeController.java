package de.tkoehler.rezepttool.manager.restcontroller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.tkoehler.rezepttool.manager.repositories.RecipeRepository;
import de.tkoehler.rezepttool.manager.repositories.model.Difficulty;
import de.tkoehler.rezepttool.manager.repositories.model.Ingredient;
import de.tkoehler.rezepttool.manager.repositories.model.RecipeEntity;
import de.tkoehler.rezepttool.manager.repositories.model.RecipeIngredient;
import de.tkoehler.rezepttool.manager.repositories.model.TinyRecipe;
import de.tkoehler.rezepttool.manager.services.ManagerService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/rezept")
public class RecipeController {

	private final RecipeRepository recipeRepository;
	private final ManagerService managerService;

	public RecipeController(RecipeRepository recipeRepository, ManagerService managerService) {
		this.recipeRepository = recipeRepository;
		this.managerService = managerService;
	}

	@RequestMapping(path = "/savedummy", method = RequestMethod.GET)
	public ResponseEntity<Void> saveDummy() {
		long count = recipeRepository.count();

		RecipeEntity recipe = RecipeEntity.builder()
				.id(UUID.randomUUID().toString())
				.url("dummyURL" + count)
				.name("dummyName" + count)
				.additionalInformation("dummyInfo")
				.portions("100")
				.instructions("dummyInstructions")
				.workTime("10min")
				.cookTime("20min")
				.restTime("30min")
				.difficulty(Difficulty.PFIFFIG)
				.callories("10000")
				.categories(Stream.of("cat1" + count, "cat2" + count).collect(Collectors.toSet()))
				.build();

		Ingredient ingred1 = Ingredient.builder()
				.id(UUID.randomUUID().toString())
				.name("dummyIngredName1" + count)
				.alternativeNames(Stream.of("altName1" + count).collect(Collectors.toSet()))
				.build();

		RecipeIngredient recipeIngred1 = RecipeIngredient.builder()
				.id(UUID.randomUUID().toString())
				.recipe(recipe)
				.amount("100 g")
				.ingredient(ingred1)
				.build();
//		ingred1.addRecipeIngredient(recipeIngred1);

		Ingredient ingred2 = Ingredient.builder()
				.id(UUID.randomUUID().toString())
				.name("dummyIngredName2" + count)
				.alternativeNames(Stream.of("altName2" + count).collect(Collectors.toSet()))
				.build();

		RecipeIngredient recipeIngred2 = RecipeIngredient.builder()
				.id(UUID.randomUUID().toString())
				.recipe(recipe)
				.amount("100 g")
				.ingredient(ingred2)
				.build();
//		ingred2.addRecipeIngredient(recipeIngred2);

		recipe.addRecipeIngredient(recipeIngred1);
		recipe.addRecipeIngredient(recipeIngred2);

		recipeRepository.save(recipe);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@CrossOrigin
	@RequestMapping(path="/testGet", produces =MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
	public ResponseEntity<TinyRecipe> getDataTest(){
		log.info("GET: testGet");
		return new ResponseEntity<>(new TinyRecipe(UUID.randomUUID().toString(), "TestRecipe"), HttpStatus.OK);
	}
	
	@CrossOrigin
	@RequestMapping(path="/testGetMore", produces =MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
	public ResponseEntity<List<TinyRecipe>> getMoreDataTest(){
		log.info("GET: testMoreGet");
		List<TinyRecipe> recipes = new ArrayList<>();
		recipes.add(new TinyRecipe(UUID.randomUUID().toString(), "TestRecipe1"));
		recipes.add(new TinyRecipe(UUID.randomUUID().toString(), "Bratwurst"));
		recipes.add(new TinyRecipe(UUID.randomUUID().toString(), "Kartoffel"));
		recipes.add(new TinyRecipe(UUID.randomUUID().toString(), "Blablubl"));
		return new ResponseEntity<>(recipes, HttpStatus.OK);
	}
}

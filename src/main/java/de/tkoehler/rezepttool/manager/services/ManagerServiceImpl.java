package de.tkoehler.rezepttool.manager.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tkoehler.rezepttool.manager.application.mappers.ExternalRecipeToWebInputMapper;
import de.tkoehler.rezepttool.manager.application.mappers.RecipeEntityToWebInputMapper;
import de.tkoehler.rezepttool.manager.application.mappers.WebInputToRecipeEntityMapper;
import de.tkoehler.rezepttool.manager.repositories.IngredientRepository;
import de.tkoehler.rezepttool.manager.repositories.RecipeRepository;
import de.tkoehler.rezepttool.manager.repositories.model.Ingredient;
import de.tkoehler.rezepttool.manager.repositories.model.RecipeEntity;
import de.tkoehler.rezepttool.manager.repositories.model.RecipeIngredient;
import de.tkoehler.rezepttool.manager.repositories.model.TinyRecipe;
import de.tkoehler.rezepttool.manager.services.recipeparser.RecipeParser;
import de.tkoehler.rezepttool.manager.services.recipeparser.RecipeParserException;
import de.tkoehler.rezepttool.manager.web.model.IngredientWebInput;
import de.tkoehler.rezepttool.manager.web.model.RecipeWebInput;
import lombok.extern.slf4j.Slf4j;

@Component
@Transactional
@Slf4j
public class ManagerServiceImpl implements ManagerService {

	private final RecipeRepository recipeRepository;
	private final IngredientRepository ingredientRepository;
	private final RecipeParser recipeParser;
	private final ExternalRecipeToWebInputMapper externalRecipeToWebInputMapper;
	private final WebInputToRecipeEntityMapper webInputToRecipeEntityMapper;
	private final RecipeEntityToWebInputMapper recipeEntityToWebInputMapper;

	public ManagerServiceImpl(RecipeRepository recipeRepository, IngredientRepository ingredientRepository, RecipeParser recipeParser, ExternalRecipeToWebInputMapper externalRecipeToWebInputMapper,
			WebInputToRecipeEntityMapper webInputToRecipeEntityMapper, RecipeEntityToWebInputMapper recipeEntityToWebInputMapper) {
		this.recipeRepository = recipeRepository;
		this.ingredientRepository = ingredientRepository;
		this.recipeParser = recipeParser;
		this.externalRecipeToWebInputMapper = externalRecipeToWebInputMapper;
		this.webInputToRecipeEntityMapper = webInputToRecipeEntityMapper;
		this.recipeEntityToWebInputMapper = recipeEntityToWebInputMapper;
	}

	@Override
	public RecipeWebInput importRecipe(String urlString) throws ManagerServiceException {
		checkNullParameter(urlString);
		try {
			RecipeWebInput recipe = externalRecipeToWebInputMapper.process(recipeParser.parseRecipe(urlString));
			updateWebRecipeWithKnownData(recipe);
			return recipe;
		}
		catch (RecipeParserException e) {
			throw new ManagerServiceException("Failed to parse recipe!", e);
		}
	}

	public void updateWebRecipeWithKnownData(RecipeWebInput recipe) throws ManagerServiceException {
		checkNullParameter(recipe);
		for (IngredientWebInput ingredient : recipe.getIngredients()) {
			updateWebIngredientWithKnownData(ingredient);
		}
	}

	public void updateWebIngredientWithKnownData(IngredientWebInput ingredient) throws ManagerServiceException {
		checkNullParameter(ingredient);
		List<Ingredient> ingredients = ingredientRepository.findByAlternativeName(ingredient.getOriginalName());
		if (ingredients.size() > 0) {
			StringBuilder names = new StringBuilder();
			StringBuilder departments = new StringBuilder();
			for (int i = 0; i < ingredients.size(); i++) {
				Ingredient ingredientEntity = ingredients.get(i);
				names.append(ingredientEntity.getName());
				departments.append(ingredientEntity.getDepartment());
				if (i + 1 < ingredients.size()) {
					names.append(" | ");
					departments.append(" | ");
				}
			}
			ingredient.setDepartment(departments.toString());
			ingredient.setName(names.toString());
		}
	}

	@Override
	public void saveRecipe(RecipeWebInput webRecipe) throws ManagerServiceException {
		checkNullParameter(webRecipe);
		checkForExistingRecipe(webRecipe);
		RecipeEntity recipe = webInputToRecipeEntityMapper.process(webRecipe);
		for (RecipeIngredient ingredient : recipe.getIngredients()) {
			updateKnownIngredient(ingredient.getIngredient());
		}
		recipeRepository.save(recipe);
	}

	public void updateKnownIngredient(Ingredient ingredient) throws ManagerServiceException {
		checkNullParameter(ingredient);
		Optional<Ingredient> ingredientEntity = ingredientRepository.findByNameAndDepartment(ingredient.getName(), ingredient.getDepartment());
		if (ingredientEntity.isPresent()) {
			ingredient.setId(ingredientEntity.get().getId());
			ingredient.getAlternativeNames().addAll(ingredientEntity.get().getAlternativeNames());
		}
	}

	private void checkForExistingRecipe(RecipeWebInput recipe) throws ManagerServiceRecipeExistsException {
		List<RecipeEntity> recipes = recipeRepository.findByUrlAndName(recipe.getUrl(), recipe.getName());
		if (recipes.size() > 0)
			throw new ManagerServiceRecipeExistsException("Recipe already exists!");
	}



	@Override
	public List<TinyRecipe> showRecipeList() {
		return recipeRepository.findAllTinies();
	}

	@Override
	public List<TinyRecipe> filterRecipeList() throws ManagerServiceException {
		List<TinyRecipe> recipes = recipeRepository.findAllTiniesByCategory("\"fettarm\"");
		log.info("Filtersize: " + recipes.size());
		for (TinyRecipe tinyRecipe : recipes) {
			log.info(tinyRecipe.getName());
		}
		return null;
	}

	@Override
	public RecipeWebInput verifyRecipe(RecipeWebInput webRecipe) throws ManagerServiceException {
		Optional<RecipeEntity> recipe = recipeRepository.findById(webRecipe.getId());
		if (recipe.isPresent()) {
			return processDifferences(webRecipe, recipe.get());
		}
		else throw new ManagerServiceIDNotFoundException("ID could not be found!");
	}
	
	private RecipeWebInput processDifferences(RecipeWebInput webRecipe, RecipeEntity recipe) {
		for (IngredientWebInput webIngredient : webRecipe.getIngredients()) {
			for (RecipeIngredient ingredient : recipe.getIngredients()) {
				if (webIngredient.getIngredientId().equals(ingredient.getId())) {
					if (!webIngredient.getName().equals(ingredient.getIngredient().getName()) ||
							!webIngredient.getDepartment().equals(ingredient.getIngredient().getDepartment())) {
						webIngredient.setOriginalName(ingredient.getIngredient().getName());
						webIngredient.setOriginalDepartment(ingredient.getIngredient().getDepartment());
						webIngredient.setUnequalToEntity(true);
					}
				}
			}
		}
		return webRecipe;
	}

	@Override
	public RecipeWebInput editRecipe(String recipeId) throws ManagerServiceException {
		checkNullParameter(recipeId);
		Optional<RecipeEntity> recipe = recipeRepository.findById(recipeId);
		if (recipe.isPresent()) return recipeEntityToWebInputMapper.process(recipe.get());
		else throw new ManagerServiceIDNotFoundException("ID could not be found!");
	}

	@Override
	public void deleteRecipe(String recipeId) throws ManagerServiceException {
		checkNullParameter(recipeId);
		recipeRepository.deleteById(recipeId);
	}
	
	private void checkNullParameter(Object parameter) throws ManagerServiceException {
		if (parameter == null) throw new ManagerServiceException("Parameter must not be empty!");
	}
}

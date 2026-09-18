package com.rudo.smartpantry.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A recipe from the seeded collection.
 *
 * The ingredient list is not stored on the recipe row itself. It is loaded
 * separately from the recipe_ingredient table and attached here, so a Recipe
 * object may legitimately carry an empty list when only the summary was
 * needed for a list screen.
 *
 * Preparation steps are held as a single text column with newline separators,
 * because SQLite has no array type and the steps are only ever read back as a
 * whole for display.
 */
public class Recipe {

    /** Separator used to pack preparation steps into one database column. */
    public static final String STEP_SEPARATOR = "\n";

    private int id;
    private String name;
    private String description;
    private String steps;
    private int prepMinutes;
    private List<RecipeIngredient> ingredients;

    public Recipe() {
        this.id = -1;
        this.name = "";
        this.description = "";
        this.steps = "";
        this.prepMinutes = 0;
        this.ingredients = new ArrayList<>();
    }

    public Recipe(String name, String description, int prepMinutes, String steps) {
        this();
        this.name = name;
        this.description = description;
        this.prepMinutes = prepMinutes;
        this.steps = steps;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps == null ? "" : steps;
    }

    /** Splits the packed steps column into individual numbered instructions. */
    public List<String> getStepList() {
        if (steps == null || steps.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(steps.split(STEP_SEPARATOR)));
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(int prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients == null ? new ArrayList<>() : ingredients;
    }

    public void addIngredient(RecipeIngredient ingredient) {
        this.ingredients.add(ingredient);
    }

    public int getIngredientCount() {
        return ingredients.size();
    }

    @Override
    public String toString() {
        return name;
    }
}

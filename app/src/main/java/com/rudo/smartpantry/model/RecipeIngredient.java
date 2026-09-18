package com.rudo.smartpantry.model;

import com.rudo.smartpantry.util.IngredientNormaliser;

/**
 * One ingredient required by a recipe, held in its own table row rather than
 * packed into a delimited column on the recipe. Keeping ingredients as
 * separate rows is what allows the strict-matching rule to compare a recipe
 * against the pantry one ingredient at a time.
 */
public class RecipeIngredient {

    private int id;
    private int recipeId;
    private String name;
    private String nameNormalised;
    private double quantity;
    private String unit;

    public RecipeIngredient() {
        this.id = -1;
        this.recipeId = -1;
        this.name = "";
        this.nameNormalised = "";
        this.quantity = 0d;
        this.unit = "";
    }

    public RecipeIngredient(String name, double quantity, String unit) {
        this();
        setName(name);
        this.quantity = quantity;
        this.unit = unit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
        this.nameNormalised = IngredientNormaliser.normaliseName(this.name);
    }

    public String getNameNormalised() {
        return nameNormalised;
    }

    public void setNameNormalised(String nameNormalised) {
        this.nameNormalised = nameNormalised;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit == null ? "" : unit.trim();
    }

    /** Convenience for the recipe detail screen, e.g. "2 clove" or "200 g". */
    public String getDisplayQuantity() {
        String amount = IngredientNormaliser.formatAmount(quantity);
        return unit.isEmpty() ? amount : amount + " " + unit;
    }

    @Override
    public String toString() {
        return getDisplayQuantity() + " " + name;
    }
}
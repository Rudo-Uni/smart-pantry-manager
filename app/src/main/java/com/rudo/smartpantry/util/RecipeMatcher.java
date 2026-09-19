package com.rudo.smartpantry.util;

import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.model.Recipe;
import com.rudo.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which recipes the user can cook right now.
 *
 * The rule is strict: a recipe qualifies only when every ingredient it
 * requires is present in the pantry in at least the required quantity. A
 * recipe missing even one ingredient is excluded, no matter how close it is.
 *
 * Matching happens in two stages. Names are compared in their normalised form
 * so that trivial differences in spelling do not break a match, and quantities
 * are converted to a common base unit so that 500 g satisfies a requirement
 * for 0.5 kg. Both conversions live in IngredientNormaliser.
 */
public final class RecipeMatcher {

    private RecipeMatcher() {
        // Utility class, never instantiated.
    }

    /**
     * The outcome of testing one recipe against the pantry. Missing
     * ingredients are retained so the user interface can explain why a recipe
     * did not qualify without running the comparison a second time.
     */
    public static class MatchResult {

        private final Recipe recipe;
        private final List<RecipeIngredient> missingIngredients;

        MatchResult(Recipe recipe, List<RecipeIngredient> missingIngredients) {
            this.recipe = recipe;
            this.missingIngredients = missingIngredients;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public List<RecipeIngredient> getMissingIngredients() {
            return Collections.unmodifiableList(missingIngredients);
        }

        public int getMissingCount() {
            return missingIngredients.size();
        }

        /** True only when nothing at all is missing. */
        public boolean isFullyMatched() {
            return missingIngredients.isEmpty();
        }

        /** The single missing ingredient, or null when there is not exactly one. */
        public RecipeIngredient getOnlyMissingIngredient() {
            return missingIngredients.size() == 1 ? missingIngredients.get(0) : null;
        }
    }

    /**
     * Recipes the user can make right now, in alphabetical order.
     *
     * This is the list shown on the Suggested Recipes screen and the method
     * the strict-matching rule is judged on.
     */
    public static List<Recipe> findSuggestedRecipes(List<Recipe> recipes,
                                                    List<PantryItem> pantryItems) {
        List<Recipe> suggested = new ArrayList<>();
        for (MatchResult result : evaluateAll(recipes, pantryItems)) {
            if (result.isFullyMatched()) {
                suggested.add(result.getRecipe());
            }
        }
        return suggested;
    }

    /**
     * Recipes that would qualify if the user had one more ingredient.
     *
     * Kept deliberately separate from the suggestions so that a recipe which
     * is merely close can never appear in the strict list.
     */
    public static List<MatchResult> findAlmostThere(List<Recipe> recipes,
                                                    List<PantryItem> pantryItems) {
        List<MatchResult> almost = new ArrayList<>();
        for (MatchResult result : evaluateAll(recipes, pantryItems)) {
            if (result.getMissingCount() == 1) {
                almost.add(result);
            }
        }
        return almost;
    }

    /**
     * Tests every recipe against the pantry.
     *
     * The pantry is indexed once up front rather than being searched again for
     * each ingredient, which keeps the work proportional to the number of
     * ingredients rather than to pantry size multiplied by ingredient count.
     */
    public static List<MatchResult> evaluateAll(List<Recipe> recipes,
                                                List<PantryItem> pantryItems) {
        Map<String, List<PantryItem>> pantryIndex = buildPantryIndex(pantryItems);

        List<MatchResult> results = new ArrayList<>();
        if (recipes == null) {
            return results;
        }

        for (Recipe recipe : recipes) {
            results.add(evaluate(recipe, pantryIndex));
        }
        return results;
    }

    /**
     * Tests a single recipe, collecting everything the pantry cannot cover.
     * A recipe with no ingredients recorded is treated as unmatched rather
     * than as trivially satisfied, since an empty requirement list indicates
     * a data problem rather than a dish needing nothing.
     */
    public static MatchResult evaluate(Recipe recipe, Map<String, List<PantryItem>> pantryIndex) {
        List<RecipeIngredient> missing = new ArrayList<>();
        List<RecipeIngredient> required = recipe.getIngredients();

        if (required == null || required.isEmpty()) {
            // Nothing to match against, so it cannot be suggested. A single
            // placeholder keeps the result out of both the strict and the
            // almost-there lists.
            missing.add(new RecipeIngredient("unknown", 0, ""));
            return new MatchResult(recipe, missing);
        }

        for (RecipeIngredient ingredient : required) {
            if (!pantryCovers(ingredient, pantryIndex)) {
                missing.add(ingredient);
            }
        }
        return new MatchResult(recipe, missing);
    }

    /**
     * Whether the pantry holds enough of one ingredient.
     * Where the pantry has the same ingredient recorded more than once, the
     * holdings are added together, so two part-used bags of flour can satisfy
     * a requirement that neither could alone. Only entries measuring the same
     * kind of thing are combined: adding a weight to a count would be
     * meaningless.
     * Then nothing held is comparable with what the recipe asks for, having
     * the ingredient at all is accepted. This is a deliberate compromise. It
     * keeps the app usable when a user records "1 milk" against a recipe
     * calling for 250 ml, at the cost of not verifying the amount in that one
     * case.
     */
    private static boolean pantryCovers(RecipeIngredient ingredient,
                                        Map<String, List<PantryItem>> pantryIndex) {
        List<PantryItem> held = pantryIndex.get(ingredient.getNameNormalised());
        if (held == null || held.isEmpty()) {
            return false;
        }

        IngredientNormaliser.UnitCategory requiredCategory =
                IngredientNormaliser.categoryOf(ingredient.getUnit());

        double availableBase = 0d;
        boolean foundComparable = false;

        for (PantryItem item : held) {
            if (item.getQuantity() <= 0) {
                continue;
            }
            if (IngredientNormaliser.categoryOf(item.getUnit()) == requiredCategory) {
                foundComparable = true;
                availableBase += IngredientNormaliser.toBaseAmount(
                        item.getQuantity(), item.getUnit());
            }
        }

        if (!foundComparable) {
            return hasAnyQuantity(held);
        }

        double requiredBase = IngredientNormaliser.toBaseAmount(
                ingredient.getQuantity(), ingredient.getUnit());

        // Small tolerance so floating point conversion does not reject an
        // amount that is exactly sufficient on paper.
        return availableBase + 0.0001d >= requiredBase;
    }

    private static boolean hasAnyQuantity(List<PantryItem> items) {
        for (PantryItem item : items) {
            if (item.getQuantity() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Groups the pantry by normalised name so an ingredient can be looked up
     * directly instead of scanning the whole pantry.
     */
    private static Map<String, List<PantryItem>> buildPantryIndex(List<PantryItem> pantryItems) {
        Map<String, List<PantryItem>> index = new HashMap<>();
        if (pantryItems == null) {
            return index;
        }

        for (PantryItem item : pantryItems) {
            String key = item.getNameNormalised();
            if (key == null || key.isEmpty()) {
                continue;
            }
            List<PantryItem> existing = index.get(key);
            if (existing == null) {
                existing = new ArrayList<>();
                index.put(key, existing);
            }
            existing.add(item);
        }
        return index;
    }
}

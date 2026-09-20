package com.rudo.smartpantry.util;

import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.model.Recipe;
import com.rudo.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out what to remove from the pantry when the user cooks a recipe.
 * Deducting is not simply a matter of deleting rows. A recipe calling for
 * 200 g of flour should leave 300 g behind in a 500 g bag, and a requirement
 * expressed in kilograms has to be reconciled against a pantry entry recorded
 * in grams. Both sides are therefore converted to a base unit, subtracted, and
 * converted back into whatever unit the pantry entry was recorded in.
 * Where the same ingredient appears more than once, the entry expiring
 * soonest is used first. Reducing waste is the purpose of the application, so
 * the stock closest to going off should be the stock that gets used.
 * The plan is calculated separately from being applied, so that the user can
 * be shown exactly what will change before anything is written.
 */
public final class RecipeConsumer {

    private RecipeConsumer() {
        // Utility class, never instantiated.
    }

    /** A pantry entry to be reduced to a smaller quantity. */
    public static class Reduction {

        private final int itemId;
        private final String itemName;
        private final double newQuantity;
        private final String unit;
        private final double amountUsed;

        Reduction(int itemId, String itemName, double newQuantity,
                  String unit, double amountUsed) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.newQuantity = newQuantity;
            this.unit = unit;
            this.amountUsed = amountUsed;
        }

        public int getItemId() {
            return itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public double getNewQuantity() {
            return newQuantity;
        }

        public String getUnit() {
            return unit;
        }

        /** Human-readable summary, e.g. "Flour: 200 g used, 300 g left". */
        public String describe() {
            return itemName + ": " + IngredientNormaliser.formatAmount(amountUsed)
                    + formatUnit() + " used, "
                    + IngredientNormaliser.formatAmount(newQuantity)
                    + formatUnit() + " left";
        }

        private String formatUnit() {
            return unit == null || unit.isEmpty() ? "" : " " + unit;
        }
    }

    /** A pantry entry that will be used up completely and removed. */
    public static class Removal {

        private final int itemId;
        private final String itemName;

        Removal(int itemId, String itemName) {
            this.itemId = itemId;
            this.itemName = itemName;
        }

        public int getItemId() {
            return itemId;
        }

        public String getItemName() {
            return itemName;
        }
    }

    /**
     * Everything that will change if the user confirms they cooked a recipe.
     * Ingredients that could not be adjusted are carried here rather than
     * being silently skipped, so the user can be told which entries they need
     * to correct by hand.
     */
    public static class ConsumptionPlan {

        private final List<Reduction> reductions = new ArrayList<>();
        private final List<Removal> removals = new ArrayList<>();
        private final List<String> unadjusted = new ArrayList<>();

        public List<Reduction> getReductions() {
            return Collections.unmodifiableList(reductions);
        }

        public List<Removal> getRemovals() {
            return Collections.unmodifiableList(removals);
        }

        /** Ingredients whose units could not be compared, left untouched. */
        public List<String> getUnadjusted() {
            return Collections.unmodifiableList(unadjusted);
        }

        public boolean hasChanges() {
            return !reductions.isEmpty() || !removals.isEmpty();
        }

        /** A short summary of the plan for the confirmation dialog. */
        public String describe() {
            StringBuilder summary = new StringBuilder();

            for (Removal removal : removals) {
                summary.append("• ").append(removal.getItemName())
                        .append(": used up, removed from pantry\n");
            }
            for (Reduction reduction : reductions) {
                summary.append("• ").append(reduction.describe()).append("\n");
            }
            for (String name : unadjusted) {
                summary.append("• ").append(name)
                        .append(": left unchanged, units cannot be compared\n");
            }
            return summary.toString().trim();
        }
    }

    /**
     * Calculates what cooking a recipe would take out of the pantry.
     *
     * Nothing is written here. The returned plan is applied separately once
     * the user has confirmed it.
     *
     * @param recipe      the recipe being cooked
     * @param pantryItems the user's current pantry
     */
    public static ConsumptionPlan plan(Recipe recipe, List<PantryItem> pantryItems) {
        ConsumptionPlan consumptionPlan = new ConsumptionPlan();
        if (recipe == null || pantryItems == null) {
            return consumptionPlan;
        }

        Map<String, List<PantryItem>> index = indexByName(pantryItems);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            List<PantryItem> held = index.get(ingredient.getNameNormalised());
            if (held == null || held.isEmpty()) {
                continue;
            }

            IngredientNormaliser.UnitCategory requiredCategory =
                    IngredientNormaliser.categoryOf(ingredient.getUnit());

            List<PantryItem> comparable = new ArrayList<>();
            for (PantryItem item : held) {
                if (IngredientNormaliser.categoryOf(item.getUnit()) == requiredCategory) {
                    comparable.add(item);
                }
            }

            if (comparable.isEmpty()) {
                // The match was accepted on possession alone, so there is no
                // sensible arithmetic to perform. Leave it for the user.
                consumptionPlan.unadjusted.add(ingredient.getName());
                continue;
            }

            // Use the stock closest to expiry first.
            Collections.sort(comparable, expirySoonestFirst());

            double remaining = IngredientNormaliser.toBaseAmount(
                    ingredient.getQuantity(), ingredient.getUnit());

            for (PantryItem item : comparable) {
                if (remaining <= 0.0001d) {
                    break;
                }

                double availableBase = IngredientNormaliser.toBaseAmount(
                        item.getQuantity(), item.getUnit());

                if (availableBase <= remaining + 0.0001d) {
                    // This entry is entirely consumed.
                    consumptionPlan.removals.add(new Removal(item.getId(), item.getName()));
                    remaining -= availableBase;
                } else {
                    double leftBase = availableBase - remaining;
                    double leftInOwnUnit = IngredientNormaliser.fromBaseAmount(
                            leftBase, item.getUnit());
                    double usedInOwnUnit = IngredientNormaliser.fromBaseAmount(
                            remaining, item.getUnit());

                    consumptionPlan.reductions.add(new Reduction(
                            item.getId(), item.getName(),
                            round(leftInOwnUnit), item.getUnit(),
                            round(usedInOwnUnit)));
                    remaining = 0;
                }
            }
        }
        return consumptionPlan;
    }

    /**
     * Orders pantry entries so those expiring soonest come first. Entries
     * with no expiry date sort last, since they are the least urgent to use.
     */
    private static Comparator<PantryItem> expirySoonestFirst() {
        return new Comparator<PantryItem>() {
            @Override
            public int compare(PantryItem first, PantryItem second) {
                boolean firstHasDate = first.hasExpiryDate();
                boolean secondHasDate = second.hasExpiryDate();

                if (firstHasDate && secondHasDate) {
                    return Long.compare(first.getExpiryDate(), second.getExpiryDate());
                }
                if (firstHasDate) {
                    return -1;
                }
                if (secondHasDate) {
                    return 1;
                }
                return 0;
            }
        };
    }

    private static Map<String, List<PantryItem>> indexByName(List<PantryItem> pantryItems) {
        Map<String, List<PantryItem>> index = new HashMap<>();
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

    /** Keeps stored quantities to two decimals so they stay readable. */
    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}

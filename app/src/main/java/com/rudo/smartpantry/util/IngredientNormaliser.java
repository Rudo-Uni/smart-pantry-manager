package com.rudo.smartpantry.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns messily written ingredient names and units into a canonical form so
 * that the strict-matching rule can compare them reliably.
 *
 * Real users do not type consistently. A pantry holding "2 Large Tomatoes"
 * should satisfy a recipe asking for "tomato", and 500 g of flour should
 * satisfy a recipe asking for 0.5 kg. A plain string comparison fails both
 * cases, so names are folded to a singular lower-case form with descriptive
 * filler removed, and quantities are converted to a base unit before being
 * compared.
 *
 * This deliberately stops short of natural language processing. It handles the
 * common English plural patterns and the household units a pantry app needs,
 * and nothing further.
 */
public final class IngredientNormaliser {

    /** Broad kind of measurement, used to decide whether two amounts are comparable. */
    public enum UnitCategory {
        MASS,
        VOLUME,
        COUNT
    }

    /** Descriptive words that do not change which ingredient is meant. */
    private static final Set<String> FILLER_WORDS = new HashSet<>(Arrays.asList(
            "fresh", "dried", "chopped", "sliced", "diced", "minced", "grated",
            "large", "small", "medium", "ripe", "finely", "roughly", "peeled"
    ));

    /** Plurals that do not follow the usual rules. */
    private static final Map<String, String> IRREGULAR_PLURALS = new HashMap<>();

    /** Unit spellings mapped to their canonical short form. */
    private static final Map<String, String> UNIT_ALIASES = new HashMap<>();

    /** Canonical unit mapped to how many base units it represents. */
    private static final Map<String, Double> UNIT_TO_BASE = new HashMap<>();

    /** Canonical unit mapped to the kind of thing it measures. */
    private static final Map<String, UnitCategory> UNIT_CATEGORY = new HashMap<>();

    static {
        IRREGULAR_PLURALS.put("leaves", "leaf");
        IRREGULAR_PLURALS.put("loaves", "loaf");
        IRREGULAR_PLURALS.put("knives", "knife");
        IRREGULAR_PLURALS.put("halves", "half");
        IRREGULAR_PLURALS.put("tomatoes", "tomato");
        IRREGULAR_PLURALS.put("potatoes", "potato");
        IRREGULAR_PLURALS.put("chillies", "chilli");
        IRREGULAR_PLURALS.put("chilies", "chilli");
        IRREGULAR_PLURALS.put("berries", "berry");
        IRREGULAR_PLURALS.put("cherries", "cherry");

        // Mass, base unit gram.
        registerUnit("g", UnitCategory.MASS, 1d, "g", "gram", "grams", "gr");
        registerUnit("kg", UnitCategory.MASS, 1000d, "kg", "kilogram", "kilograms", "kilo", "kilos");
        registerUnit("mg", UnitCategory.MASS, 0.001d, "mg", "milligram", "milligrams");
        registerUnit("oz", UnitCategory.MASS, 28.3495d, "oz", "ounce", "ounces");
        registerUnit("lb", UnitCategory.MASS, 453.592d, "lb", "lbs", "pound", "pounds");

        // Volume, base unit millilitre.
        registerUnit("ml", UnitCategory.VOLUME, 1d, "ml", "millilitre", "millilitres", "milliliter", "milliliters");
        registerUnit("l", UnitCategory.VOLUME, 1000d, "l", "litre", "litres", "liter", "liters");
        registerUnit("tsp", UnitCategory.VOLUME, 5d, "tsp", "teaspoon", "teaspoons");
        registerUnit("tbsp", UnitCategory.VOLUME, 15d, "tbsp", "tablespoon", "tablespoons");
        registerUnit("cup", UnitCategory.VOLUME, 250d, "cup", "cups");

        // Countable things, base unit one item.
        registerUnit("", UnitCategory.COUNT, 1d, "", "piece", "pieces", "unit", "units", "whole");
        registerUnit("clove", UnitCategory.COUNT, 1d, "clove", "cloves");
        registerUnit("slice", UnitCategory.COUNT, 1d, "slice", "slices");
        registerUnit("can", UnitCategory.COUNT, 1d, "can", "cans", "tin", "tins");
        registerUnit("pinch", UnitCategory.COUNT, 1d, "pinch", "pinches");
    }

    private IngredientNormaliser() {
        // Utility class, never instantiated.
    }

    private static void registerUnit(String canonical, UnitCategory category,
                                     double baseFactor, String... aliases) {
        UNIT_TO_BASE.put(canonical, baseFactor);
        UNIT_CATEGORY.put(canonical, category);
        for (String alias : aliases) {
            UNIT_ALIASES.put(alias, canonical);
        }
    }

    /**
     * Folds a raw ingredient name to the form stored in the name_normalised
     * column. "2 Large Ripe Tomatoes" and "tomato" both reduce to "tomato".
     */
    public static String normaliseName(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = raw.toLowerCase(Locale.ROOT).trim();

        // Drop anything in brackets, e.g. "chicken breast (skinless)".
        cleaned = cleaned.replaceAll("\\(.*?\\)", " ");

        // Strip punctuation and digits, leaving words and spaces only.
        cleaned = cleaned.replaceAll("[^a-z\\s]", " ");

        StringBuilder result = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.isEmpty() || FILLER_WORDS.contains(word)) {
                continue;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(singularise(word));
        }

        String normalised = result.toString().trim();

        // If filler removal emptied the string, fall back to the cleaned input
        // rather than returning nothing to match on.
        if (normalised.isEmpty()) {
            normalised = cleaned.replaceAll("\\s+", " ").trim();
        }
        return normalised;
    }

    /**
     * Reduces a single word to its singular form using the common English
     * patterns. Words already singular are returned unchanged.
     */
    public static String singularise(String word) {
        if (word == null || word.length() < 3) {
            return word == null ? "" : word;
        }

        String mapped = IRREGULAR_PLURALS.get(word);
        if (mapped != null) {
            return mapped;
        }

        // berries -> berry
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        // potatoes -> potato, boxes -> box, dishes -> dish
        if (word.endsWith("oes") || word.endsWith("ses") || word.endsWith("xes")
                || word.endsWith("zes") || word.endsWith("ches") || word.endsWith("shes")) {
            return word.substring(0, word.length() - 2);
        }
        // carrots -> carrot, but leave glass, hummus and similar alone
        if (word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us")
                && !word.endsWith("is")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /** Resolves any accepted spelling of a unit to its canonical short form. */
    public static String canonicalUnit(String unit) {
        if (unit == null) {
            return "";
        }
        String key = unit.toLowerCase(Locale.ROOT).trim();
        String canonical = UNIT_ALIASES.get(key);
        if (canonical != null) {
            return canonical;
        }
        // Try the singular, so "cloves" resolves even before the alias lookup.
        String singular = singularise(key);
        canonical = UNIT_ALIASES.get(singular);
        return canonical != null ? canonical : key;
    }

    /** The kind of measurement a unit represents, defaulting to COUNT. */
    public static UnitCategory categoryOf(String unit) {
        UnitCategory category = UNIT_CATEGORY.get(canonicalUnit(unit));
        return category != null ? category : UnitCategory.COUNT;
    }

    /**
     * Converts an amount to its base unit: grams for mass, millilitres for
     * volume, and individual items for anything countable.
     */
    public static double toBaseAmount(double quantity, String unit) {
        Double factor = UNIT_TO_BASE.get(canonicalUnit(unit));
        return factor != null ? quantity * factor : quantity;
    }

    /**
     * Decides whether a pantry holding is enough to cover what a recipe asks
     * for. Amounts of the same kind are converted to a common base and
     * compared. When the two units measure different kinds of thing the
     * amounts cannot be compared meaningfully, so possession of the
     * ingredient is accepted on its own.
     */
    public static boolean satisfiesQuantity(double pantryQuantity, String pantryUnit,
                                            double requiredQuantity, String requiredUnit) {
        UnitCategory pantryCategory = categoryOf(pantryUnit);
        UnitCategory requiredCategory = categoryOf(requiredUnit);

        if (pantryCategory != requiredCategory) {
            return pantryQuantity > 0;
        }

        double pantryBase = toBaseAmount(pantryQuantity, pantryUnit);
        double requiredBase = toBaseAmount(requiredQuantity, requiredUnit);

        // Small tolerance so that floating point conversion does not reject an
        // amount that is exactly equal on paper.
        return pantryBase + 0.0001d >= requiredBase;
    }

    /** Formats a quantity for display, dropping a redundant trailing ".0". */
    public static String formatAmount(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }
}
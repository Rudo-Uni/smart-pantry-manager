package com.rudo.smartpantry.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.rudo.smartpantry.util.IngredientNormaliser;

/**
 * The recipe collection loaded into the database the first time the app runs.
 *
 * Recipes deliberately share common staples such as eggs, milk, onion and rice.
 * That overlap is what makes the strict-matching rule visible in use: removing
 * a single pantry item causes several suggestions to disappear at once, which
 * is exactly the behaviour the demonstration needs to show.
 *
 * Ingredient names are normalised on insert using the same helper the pantry
 * uses, so both sides of a match are always stored in the same form.
 */
final class RecipeSeedData {

    private RecipeSeedData() {
        // Utility class, never instantiated.
    }

    /**
     * Inserts every recipe and its ingredients.
     *
     * @param db the database being created, supplied by PantryDBHelper.onCreate
     * @return the number of recipes written
     */
    static int seed(SQLiteDatabase db) {
        int count = 0;

        count += addRecipe(db, "Scrambled Eggs on Toast",
                "A quick breakfast that uses up eggs and bread.", 10,
                "Crack the eggs into a bowl and whisk with the milk.\n" +
                        "Melt half the butter in a pan over medium heat.\n" +
                        "Pour in the eggs and stir gently until just set.\n" +
                        "Toast the bread, butter it, and spoon the eggs on top.",
                new String[][]{
                        {"eggs", "3", ""},
                        {"bread", "2", "slice"},
                        {"butter", "20", "g"},
                        {"milk", "30", "ml"},
                        {"salt", "1", "pinch"}
                });

        count += addRecipe(db, "Cheese Omelette",
                "Three eggs, a handful of cheese, done in minutes.", 10,
                "Whisk the eggs with a pinch of salt.\n" +
                        "Melt the butter in a non-stick pan over medium heat.\n" +
                        "Pour in the eggs and let them set at the edges.\n" +
                        "Scatter the cheese over one half, fold, and serve.",
                new String[][]{
                        {"eggs", "3", ""},
                        {"cheddar cheese", "50", "g"},
                        {"butter", "15", "g"},
                        {"salt", "1", "pinch"}
                });

        count += addRecipe(db, "Tomato Pasta",
                "A simple pasta sauce built from tomatoes and garlic.", 25,
                "Boil the pasta in salted water until tender.\n" +
                        "Soften the chopped garlic in the olive oil.\n" +
                        "Add the chopped tomatoes and simmer for 15 minutes.\n" +
                        "Drain the pasta, fold through the sauce, and season.",
                new String[][]{
                        {"pasta", "200", "g"},
                        {"tomatoes", "4", ""},
                        {"garlic", "2", "clove"},
                        {"olive oil", "30", "ml"},
                        {"salt", "5", "g"}
                });

        count += addRecipe(db, "Garlic Butter Rice",
                "A side dish that turns plain rice into something worth eating.", 25,
                "Rinse the rice until the water runs clear.\n" +
                        "Simmer in twice its volume of water until absorbed.\n" +
                        "Gently fry the crushed garlic in the butter.\n" +
                        "Fork the garlic butter through the rice and season.",
                new String[][]{
                        {"rice", "200", "g"},
                        {"butter", "30", "g"},
                        {"garlic", "3", "clove"},
                        {"salt", "5", "g"}
                });

        count += addRecipe(db, "Mieliepap",
                "Stiff maize porridge, a staple side for almost anything.", 30,
                "Bring the water to the boil with the salt.\n" +
                        "Add the maize meal slowly while stirring.\n" +
                        "Cover and steam on low heat for 20 minutes.\n" +
                        "Stir through with a fork before serving.",
                new String[][]{
                        {"maize meal", "250", "g"},
                        {"water", "750", "ml"},
                        {"salt", "5", "g"}
                });

        count += addRecipe(db, "French Toast",
                "Stale bread rescued with egg and milk.", 15,
                "Beat the eggs with the milk and sugar.\n" +
                        "Soak each slice of bread for about ten seconds a side.\n" +
                        "Fry in butter over medium heat until golden.\n" +
                        "Serve immediately while still crisp.",
                new String[][]{
                        {"bread", "4", "slice"},
                        {"eggs", "2", ""},
                        {"milk", "100", "ml"},
                        {"sugar", "15", "g"},
                        {"butter", "20", "g"}
                });

        count += addRecipe(db, "Pancakes",
                "A basic batter using pantry staples.", 20,
                "Whisk the flour, sugar, eggs and milk into a smooth batter.\n" +
                        "Rest the batter for ten minutes.\n" +
                        "Fry thin pancakes in a buttered pan.\n" +
                        "Stack and serve with whatever you have on hand.",
                new String[][]{
                        {"flour", "200", "g"},
                        {"milk", "300", "ml"},
                        {"eggs", "2", ""},
                        {"sugar", "30", "g"},
                        {"butter", "25", "g"}
                });

        count += addRecipe(db, "Banana Smoothie",
                "Three ingredients and a blender.", 5,
                "Peel the bananas and break them into chunks.\n" +
                        "Add the milk and honey to the blender.\n" +
                        "Blend until completely smooth.\n" +
                        "Pour and drink straight away.",
                new String[][]{
                        {"bananas", "2", ""},
                        {"milk", "250", "ml"},
                        {"honey", "20", "g"}
                });

        count += addRecipe(db, "Tuna Mayo Sandwich",
                "A tin of tuna turned into lunch.", 10,
                "Drain the tuna thoroughly.\n" +
                        "Mix with the mayonnaise and finely chopped onion.\n" +
                        "Season to taste.\n" +
                        "Spread between slices of bread and cut in half.",
                new String[][]{
                        {"bread", "4", "slice"},
                        {"tuna", "1", "can"},
                        {"mayonnaise", "45", "g"},
                        {"onion", "1", ""}
                });

        count += addRecipe(db, "Vegetable Stir Fry",
                "Whatever vegetables are going soft, saved by a hot pan.", 20,
                "Slice all the vegetables into similar sized strips.\n" +
                        "Heat the oil in a wok until it shimmers.\n" +
                        "Stir fry the harder vegetables first, then the softer ones.\n" +
                        "Add the soy sauce at the end and toss to coat.",
                new String[][]{
                        {"carrots", "2", ""},
                        {"onion", "1", ""},
                        {"green pepper", "1", ""},
                        {"soy sauce", "30", "ml"},
                        {"cooking oil", "30", "ml"}
                });

        count += addRecipe(db, "Chakalaka",
                "A spiced vegetable relish that keeps well.", 30,
                "Fry the chopped onion in the oil until translucent.\n" +
                        "Add the grated carrot and diced pepper.\n" +
                        "Stir in the curry powder and cook for a minute.\n" +
                        "Add the baked beans and simmer for 15 minutes.",
                new String[][]{
                        {"onion", "1", ""},
                        {"carrots", "2", ""},
                        {"green pepper", "1", ""},
                        {"baked beans", "1", "can"},
                        {"curry powder", "10", "g"},
                        {"cooking oil", "30", "ml"}
                });

        count += addRecipe(db, "Potato Wedges",
                "Oven chips without the deep fryer.", 40,
                "Cut the potatoes into thick wedges, skin on.\n" +
                        "Toss with the oil, paprika and salt.\n" +
                        "Spread on a tray in a single layer.\n" +
                        "Roast at 200 degrees for 35 minutes, turning once.",
                new String[][]{
                        {"potatoes", "4", ""},
                        {"cooking oil", "45", "ml"},
                        {"paprika", "5", "g"},
                        {"salt", "5", "g"}
                });

        count += addRecipe(db, "Creamy Mash",
                "Mashed potato, properly done.", 30,
                "Peel and quarter the potatoes.\n" +
                        "Boil in salted water until a knife slides through easily.\n" +
                        "Drain well and let the steam escape for a minute.\n" +
                        "Mash with the butter and warmed milk until smooth.",
                new String[][]{
                        {"potatoes", "5", ""},
                        {"butter", "40", "g"},
                        {"milk", "100", "ml"},
                        {"salt", "5", "g"}
                });

        count += addRecipe(db, "Chicken Stir Fry",
                "A fast weeknight dinner in one pan.", 25,
                "Slice the chicken into thin strips.\n" +
                        "Sear in hot oil until browned, then set aside.\n" +
                        "Fry the sliced onion and pepper until just softened.\n" +
                        "Return the chicken, add the soy sauce, and toss through.",
                new String[][]{
                        {"chicken breast", "400", "g"},
                        {"onion", "1", ""},
                        {"green pepper", "1", ""},
                        {"soy sauce", "45", "ml"},
                        {"cooking oil", "30", "ml"}
                });

        count += addRecipe(db, "Beef Mince Curry",
                "A forgiving curry that improves the next day.", 45,
                "Fry the chopped onion in the oil until golden.\n" +
                        "Add the curry powder and cook until fragrant.\n" +
                        "Brown the mince, breaking up any lumps.\n" +
                        "Add the chopped tomatoes and simmer for 30 minutes.",
                new String[][]{
                        {"beef mince", "500", "g"},
                        {"onion", "1", ""},
                        {"tomatoes", "3", ""},
                        {"curry powder", "15", "g"},
                        {"cooking oil", "30", "ml"}
                });

        count += addRecipe(db, "Lentil Soup",
                "Cheap, filling and almost entirely from the cupboard.", 45,
                "Rinse the lentils and check for stones.\n" +
                        "Soften the chopped onion and carrot in a large pot.\n" +
                        "Add the lentils, water and stock cube.\n" +
                        "Simmer for 35 minutes until the lentils collapse.",
                new String[][]{
                        {"lentils", "250", "g"},
                        {"onion", "1", ""},
                        {"carrots", "2", ""},
                        {"stock cube", "1", ""},
                        {"water", "1", "l"}
                });

        count += addRecipe(db, "Toasted Cheese Sandwich",
                "The three-ingredient fallback.", 10,
                "Butter the bread on the outside of each slice.\n" +
                        "Layer the cheese between two slices.\n" +
                        "Toast in a dry pan over medium heat.\n" +
                        "Press down and turn once the underside is golden.",
                new String[][]{
                        {"bread", "4", "slice"},
                        {"cheddar cheese", "80", "g"},
                        {"butter", "20", "g"}
                });

        count += addRecipe(db, "Egg Fried Rice",
                "Leftover rice, transformed.", 20,
                "Heat the oil in a wok until very hot.\n" +
                        "Scramble the beaten eggs quickly and set aside.\n" +
                        "Fry the diced carrot and onion until softened.\n" +
                        "Add the cold rice and soy sauce, then fold the egg back in.",
                new String[][]{
                        {"rice", "300", "g"},
                        {"eggs", "2", ""},
                        {"carrots", "1", ""},
                        {"onion", "1", ""},
                        {"soy sauce", "30", "ml"},
                        {"cooking oil", "30", "ml"}
                });

        count += addRecipe(db, "Peanut Butter Oats",
                "A filling breakfast from four cupboard items.", 10,
                "Bring the milk to a gentle simmer.\n" +
                        "Stir in the oats and cook for five minutes.\n" +
                        "Take off the heat and stir through the peanut butter.\n" +
                        "Drizzle with honey before serving.",
                new String[][]{
                        {"oats", "100", "g"},
                        {"milk", "250", "ml"},
                        {"peanut butter", "30", "g"},
                        {"honey", "15", "g"}
                });

        count += addRecipe(db, "Greek Style Salad",
                "No cooking required.", 15,
                "Dice the cucumber and tomatoes into rough chunks.\n" +
                        "Slice the onion as thinly as you can.\n" +
                        "Combine in a bowl and crumble the feta over the top.\n" +
                        "Dress with the olive oil just before serving.",
                new String[][]{
                        {"cucumber", "1", ""},
                        {"tomatoes", "3", ""},
                        {"onion", "1", ""},
                        {"feta cheese", "100", "g"},
                        {"olive oil", "30", "ml"}
                });

        return count;
    }

    /**
     * Writes one recipe row and its ingredient rows.
     *
     * @param ingredients each entry is {name, quantity, unit}
     * @return 1 when the recipe was written, 0 when the insert failed
     */
    private static int addRecipe(SQLiteDatabase db, String name, String description,
                                 int prepMinutes, String steps, String[][] ingredients) {
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(PantryDBHelper.COL_RECIPE_NAME, name);
        recipeValues.put(PantryDBHelper.COL_RECIPE_DESCRIPTION, description);
        recipeValues.put(PantryDBHelper.COL_RECIPE_STEPS, steps);
        recipeValues.put(PantryDBHelper.COL_RECIPE_PREP_MINUTES, prepMinutes);

        long recipeId = db.insert(PantryDBHelper.TABLE_RECIPE, null, recipeValues);
        if (recipeId == -1) {
            return 0;
        }

        for (String[] ingredient : ingredients) {
            ContentValues ingredientValues = new ContentValues();
            ingredientValues.put(PantryDBHelper.COL_INGREDIENT_RECIPE_ID, recipeId);
            ingredientValues.put(PantryDBHelper.COL_INGREDIENT_NAME, ingredient[0]);
            ingredientValues.put(PantryDBHelper.COL_INGREDIENT_NAME_NORM,
                    IngredientNormaliser.normaliseName(ingredient[0]));
            ingredientValues.put(PantryDBHelper.COL_INGREDIENT_QUANTITY,
                    Double.parseDouble(ingredient[1]));
            ingredientValues.put(PantryDBHelper.COL_INGREDIENT_UNIT, ingredient[2]);
            db.insert(PantryDBHelper.TABLE_RECIPE_INGREDIENT, null, ingredientValues);
        }
        return 1;
    }
}
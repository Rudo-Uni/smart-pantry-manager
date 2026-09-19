package com.rudo.smartpantry;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.rudo.smartpantry.data.PantryDataSource;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.model.Recipe;
import com.rudo.smartpantry.util.RecipeMatcher;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartPantry";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        runMatcherVerification();
    }

    /** Temporary verification of the matching engine. Removed once the UI exists. */
    private void runMatcherVerification() {
        PantryDataSource dataSource = new PantryDataSource(this);
        dataSource.open();

        Log.i(TAG, "Recipes seeded: " + dataSource.getRecipeCount());

        // Start from a clean pantry so repeated runs give the same result.
        for (PantryItem existing : dataSource.getAllPantryItems()) {
            dataSource.deletePantryItem(existing.getId());
        }

        dataSource.insertPantryItem(new PantryItem("Bread", 6, "slice"));
        dataSource.insertPantryItem(new PantryItem("Cheddar Cheese", 0.1, "kg"));
        dataSource.insertPantryItem(new PantryItem("Butter", 250, "g"));
        dataSource.insertPantryItem(new PantryItem("Large Eggs", 6, ""));
        dataSource.insertPantryItem(new PantryItem("Milk", 1, "l"));
        dataSource.insertPantryItem(new PantryItem("Salt", 500, "g"));

        List<Recipe> recipes = dataSource.getAllRecipesWithIngredients();
        List<PantryItem> pantry = dataSource.getAllPantryItems();

        Log.i(TAG, "--- Pantry (" + pantry.size() + " items) ---");
        for (PantryItem item : pantry) {
            Log.i(TAG, "   " + item.getName() + " -> '" + item.getNameNormalised()
                    + "' " + item.getDisplayQuantity());
        }

        List<Recipe> suggested = RecipeMatcher.findSuggestedRecipes(recipes, pantry);
        Log.i(TAG, "--- Suggested: " + suggested.size() + " of " + recipes.size() + " ---");
        for (Recipe recipe : suggested) {
            Log.i(TAG, "   MATCH: " + recipe.getName());
        }

        List<RecipeMatcher.MatchResult> almost = RecipeMatcher.findAlmostThere(recipes, pantry);
        Log.i(TAG, "--- Almost there: " + almost.size() + " ---");
        for (RecipeMatcher.MatchResult result : almost) {
            Log.i(TAG, "   NEEDS " + result.getOnlyMissingIngredient().getName()
                    + ": " + result.getRecipe().getName());
        }

        // Remove the butter and confirm the strict rule drops what depends on it.
        for (PantryItem item : pantry) {
            if (item.getNameNormalised().equals("butter")) {
                dataSource.deletePantryItem(item.getId());
            }
        }
        List<Recipe> afterRemoval = RecipeMatcher.findSuggestedRecipes(
                recipes, dataSource.getAllPantryItems());
        Log.i(TAG, "--- After removing butter: " + afterRemoval.size() + " ---");
        for (Recipe recipe : afterRemoval) {
            Log.i(TAG, "   MATCH: " + recipe.getName());
        }

        dataSource.close();
    }
}
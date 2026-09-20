package com.rudo.smartpantry.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.data.PantryDataSource;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.model.Recipe;
import com.rudo.smartpantry.model.RecipeIngredient;
import com.rudo.smartpantry.util.RecipeConsumer;
import com.rudo.smartpantry.util.RecipeMatcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows one recipe in full: its ingredients and its method.
 * Each ingredient is marked according to whether the user currently holds it,
 * which makes the strict-matching rule visible at the level of a single
 * recipe rather than only in the suggestions list.
 * When the user has everything a recipe needs they can record that they
 * cooked it, and the ingredients are deducted from their pantry.
 * Ingredient and step rows are built in code rather than declared in the
 * layout, because their number varies from one recipe to the next.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    /** Intent extra carrying the id of the recipe to display. */
    public static final String EXTRA_RECIPE_ID = "com.rudo.smartpantry.RECIPE_ID";

    private PantryDataSource dataSource;
    private Button cookButton;
    private Recipe currentRecipe;
    private int recipeId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataSource = new PantryDataSource(this);
        recipeId = getIntent().getIntExtra(EXTRA_RECIPE_ID, -1);

        cookButton = findViewById(R.id.btnCookRecipe);
        cookButton.setOnClickListener(v -> confirmCooked());

        NavBarHelper.setup(this, NavBarHelper.Screen.RECIPES);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataSource.open();
        displayRecipe();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataSource.close();
    }

    private void displayRecipe() {
        currentRecipe = dataSource.getRecipe(recipeId);
        if (currentRecipe == null) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtDetailName)).setText(currentRecipe.getName());
        ((TextView) findViewById(R.id.txtDetailDescription))
                .setText(currentRecipe.getDescription());
        ((TextView) findViewById(R.id.txtDetailPrep))
                .setText(getString(R.string.prep_time, currentRecipe.getPrepMinutes()));

        buildIngredientRows(currentRecipe);
        buildStepRows(currentRecipe);
        updateCookButton();
    }

    /**
     * The cook button is only offered when the strict-matching rule says the
     * user can actually make this recipe. Running the matcher over a list of
     * one reuses exactly the logic behind the suggestions list, so the button
     * can never disagree with what that screen showed.
     */
    private void updateCookButton() {
        List<PantryItem> pantry = dataSource.getAllPantryItems();
        boolean canMake = !RecipeMatcher.findSuggestedRecipes(
                Collections.singletonList(currentRecipe), pantry).isEmpty();

        cookButton.setEnabled(canMake);
        cookButton.setAlpha(canMake ? 1f : 0.4f);
        cookButton.setVisibility(View.VISIBLE);
    }

    /**
     * Works out what cooking this recipe would take out of the pantry and
     * shows it to the user before anything is written.
     */
    private void confirmCooked() {
        List<PantryItem> pantry = dataSource.getAllPantryItems();

        boolean canMake = !RecipeMatcher.findSuggestedRecipes(
                Collections.singletonList(currentRecipe), pantry).isEmpty();
        if (!canMake) {
            Toast.makeText(this, R.string.cook_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        RecipeConsumer.ConsumptionPlan plan = RecipeConsumer.plan(currentRecipe, pantry);
        if (!plan.hasChanges()) {
            Toast.makeText(this, R.string.cook_nothing_to_change, Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.cook_confirm_title)
                .setMessage(getString(R.string.cook_confirm_message,
                        currentRecipe.getName(), plan.describe()))
                .setPositiveButton(R.string.cook_confirm_button, (dialog, which) -> {
                    if (dataSource.applyConsumption(plan)) {
                        Toast.makeText(this, R.string.cook_done, Toast.LENGTH_SHORT).show();
                        // Reload so the ingredient marks and the button reflect
                        // the pantry as it now stands.
                        displayRecipe();
                    } else {
                        Toast.makeText(this, R.string.cook_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Lists the ingredients, marking each as held or missing. */
    private void buildIngredientRows(Recipe recipe) {
        LinearLayout container = findViewById(R.id.containerIngredients);
        container.removeAllViews();

        Set<String> pantryNames = new HashSet<>();
        for (PantryItem item : dataSource.getAllPantryItems()) {
            pantryNames.add(item.getNameNormalised());
        }

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            boolean held = pantryNames.contains(ingredient.getNameNormalised());

            TextView row = new TextView(this);
            row.setText((held ? "\u2713  " : "\u2717  ")
                    + ingredient.getDisplayQuantity() + " " + ingredient.getName());
            row.setTextSize(15f);
            row.setPadding(0, 6, 0, 6);
            row.setTextColor(ContextCompat.getColor(this,
                    held ? R.color.primary : R.color.text_secondary));
            row.setContentDescription(getString(
                    held ? R.string.ingredient_have : R.string.ingredient_missing));

            container.addView(row);
        }
    }

    /** Lists the preparation steps, numbered in order. */
    private void buildStepRows(Recipe recipe) {
        LinearLayout container = findViewById(R.id.containerSteps);
        container.removeAllViews();

        List<String> steps = recipe.getStepList();
        for (int i = 0; i < steps.size(); i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView number = new TextView(this);
            number.setText(String.valueOf(i + 1));
            number.setTextSize(15f);
            number.setTypeface(null, Typeface.BOLD);
            number.setGravity(Gravity.CENTER);
            number.setTextColor(ContextCompat.getColor(this, R.color.accent));
            number.setLayoutParams(new LinearLayout.LayoutParams(72,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView text = new TextView(this);
            text.setText(steps.get(i));
            text.setTextSize(15f);
            text.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            text.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(number);
            row.addView(text);
            container.addView(row);
        }
    }
}

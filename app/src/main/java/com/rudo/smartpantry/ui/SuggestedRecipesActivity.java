package com.rudo.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.data.PantryDataSource;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.model.Recipe;
import com.rudo.smartpantry.util.AppPreferences;
import com.rudo.smartpantry.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the recipes the user can cook right now.
 *
 * The strict list contains only recipes where every required ingredient is
 * present in sufficient quantity. Recipes missing exactly one ingredient are
 * shown in a clearly separated Almost There list below, which the user can
 * switch off in Settings.
 *
 * Matching runs in onResume so that any change made in the pantry is
 * reflected as soon as the user returns to this screen.
 */
public class SuggestedRecipesActivity extends AppCompatActivity
        implements RecipeAdapter.OnRecipeClickListener {

    private PantryDataSource dataSource;
    private RecipeAdapter suggestedAdapter;
    private RecipeAdapter almostAdapter;

    private TextView matchCount;
    private TextView noMatches;
    private TextView headerCanMake;
    private TextView headerAlmostThere;
    private TextView almostNote;
    private RecyclerView suggestedList;
    private RecyclerView almostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_suggested_recipes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataSource = new PantryDataSource(this);

        matchCount = findViewById(R.id.txtMatchCount);
        noMatches = findViewById(R.id.txtNoMatches);
        headerCanMake = findViewById(R.id.headerCanMake);
        headerAlmostThere = findViewById(R.id.headerAlmostThere);
        almostNote = findViewById(R.id.txtAlmostNote);
        suggestedList = findViewById(R.id.recyclerSuggested);
        almostList = findViewById(R.id.recyclerAlmost);

        suggestedAdapter = new RecipeAdapter(this);
        suggestedList.setLayoutManager(new LinearLayoutManager(this));
        suggestedList.setAdapter(suggestedAdapter);

        almostAdapter = new RecipeAdapter(this);
        almostList.setLayoutManager(new LinearLayoutManager(this));
        almostList.setAdapter(almostAdapter);

        NavBarHelper.setup(this, NavBarHelper.Screen.RECIPES);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataSource.open();
        runMatching();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataSource.close();
    }

    /**
     * Loads the pantry and every recipe, applies the strict-matching rule, and
     * updates both lists.
     */
    private void runMatching() {
        List<PantryItem> pantry = dataSource.getAllPantryItems();
        List<Recipe> allRecipes = dataSource.getAllRecipesWithIngredients();

        List<Recipe> suggested = RecipeMatcher.findSuggestedRecipes(allRecipes, pantry);

        List<RecipeAdapter.RecipeRow> suggestedRows = new ArrayList<>();
        for (Recipe recipe : suggested) {
            suggestedRows.add(new RecipeAdapter.RecipeRow(recipe));
        }
        suggestedAdapter.setRows(suggestedRows);

        matchCount.setText(getString(R.string.match_count, suggested.size(), allRecipes.size()));

        boolean hasSuggestions = !suggestedRows.isEmpty();
        headerCanMake.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
        suggestedList.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
        noMatches.setVisibility(hasSuggestions ? View.GONE : View.VISIBLE);

        showAlmostThere(allRecipes, pantry);
    }

    /**
     * Builds the Almost There list, kept strictly separate from the
     * suggestions so that a recipe the user cannot actually make can never
     * appear among those they can.
     */
    private void showAlmostThere(List<Recipe> allRecipes, List<PantryItem> pantry) {
        if (!AppPreferences.isShowAlmostThere(this)) {
            headerAlmostThere.setVisibility(View.GONE);
            almostNote.setVisibility(View.GONE);
            almostList.setVisibility(View.GONE);
            return;
        }

        List<RecipeMatcher.MatchResult> almost =
                RecipeMatcher.findAlmostThere(allRecipes, pantry);

        List<RecipeAdapter.RecipeRow> almostRows = new ArrayList<>();
        for (RecipeMatcher.MatchResult result : almost) {
            almostRows.add(new RecipeAdapter.RecipeRow(
                    result.getRecipe(),
                    result.getOnlyMissingIngredient().getName()));
        }
        almostAdapter.setRows(almostRows);

        boolean hasAlmost = !almostRows.isEmpty();
        headerAlmostThere.setVisibility(hasAlmost ? View.VISIBLE : View.GONE);
        almostNote.setVisibility(hasAlmost ? View.VISIBLE : View.GONE);
        almostList.setVisibility(hasAlmost ? View.VISIBLE : View.GONE);
    }

    /** Opens the full method for a recipe, passing its id through the Intent. */
    @Override
    public void onRecipeClicked(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}

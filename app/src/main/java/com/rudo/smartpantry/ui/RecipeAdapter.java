package com.rudo.smartpantry.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.model.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays recipes in the suggestions screen.
 *
 * The same adapter drives both lists on that screen. Rows in the strict
 * suggestions list carry no missing-ingredient label, while rows in the
 * Almost There list do, which is the only visual difference between them.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    /** Reports taps back to the hosting activity. */
    public interface OnRecipeClickListener {
        void onRecipeClicked(Recipe recipe);
    }

    /**
     * A recipe as shown in a list, optionally with the one ingredient the
     * user is missing. Pairing them here avoids the adapter needing to run
     * the matching logic a second time.
     */
    public static class RecipeRow {

        private final Recipe recipe;
        private final String missingIngredient;

        public RecipeRow(Recipe recipe) {
            this(recipe, null);
        }

        public RecipeRow(Recipe recipe, String missingIngredient) {
            this.recipe = recipe;
            this.missingIngredient = missingIngredient;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public String getMissingIngredient() {
            return missingIngredient;
        }
    }

    private final List<RecipeRow> rows = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRows(List<RecipeRow> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        RecipeRow row = rows.get(position);
        Recipe recipe = row.getRecipe();

        holder.name.setText(recipe.getName());
        holder.description.setText(recipe.getDescription());
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.recipe_meta, recipe.getIngredientCount(), recipe.getPrepMinutes()));

        if (row.getMissingIngredient() != null) {
            holder.missing.setVisibility(View.VISIBLE);
            holder.missing.setText(holder.itemView.getContext().getString(
                    R.string.missing_one, row.getMissingIngredient()));
        } else {
            holder.missing.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClicked(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {

        final TextView name;
        final TextView description;
        final TextView meta;
        final TextView missing;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtRecipeName);
            description = itemView.findViewById(R.id.txtRecipeDescription);
            meta = itemView.findViewById(R.id.txtRecipeMeta);
            missing = itemView.findViewById(R.id.txtRecipeMissing);
        }
    }
}
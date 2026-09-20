package com.rudo.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.rudo.smartpantry.util.RecipeConsumer;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.model.Recipe;
import com.rudo.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens, closes and queries the Smart Pantry database.
 * Every activity that touches data goes through this class rather than talking
 * to SQLite directly, which keeps the SQL in one place and lets the screens
 * work purely with model objects.
 * Callers must call open() before use and close() when finished, normally in
 * onResume and onPause respectively.
 */
public class PantryDataSource {

    private static final String TAG = "PantryDataSource";

    private SQLiteDatabase database;
    private final PantryDBHelper dbHelper;

    public PantryDataSource(Context context) {
        dbHelper = new PantryDBHelper(context);
    }

    /** Opens a writable connection. Creates and seeds the database on first call. */
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    /** Releases the connection. Safe to call when already closed. */
    public void close() {
        dbHelper.close();
    }

    public boolean isOpen() {
        return database != null && database.isOpen();
    }

    // Pantry items: create, read, update, delete

    /**
     * Writes a new pantry item.
     *
     * @return the generated id, or -1 if the insert failed
     */
    public int insertPantryItem(PantryItem item) {
        int newId = -1;
        try {
            ContentValues values = pantryValues(item);
            long rowId = database.insert(PantryDBHelper.TABLE_PANTRY_ITEM, null, values);
            newId = (int) rowId;
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert pantry item", e);
        }
        return newId;
    }

    /**
     * Overwrites an existing pantry item, matched on its id.
     * @return true when exactly one row was changed
     */
    public boolean updatePantryItem(PantryItem item) {
        boolean didSucceed = false;
        try {
            ContentValues values = pantryValues(item);
            int rowsAffected = database.update(
                    PantryDBHelper.TABLE_PANTRY_ITEM,
                    values,
                    PantryDBHelper.COL_PANTRY_ID + " = ?",
                    new String[]{String.valueOf(item.getId())});
            didSucceed = rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update pantry item " + item.getId(), e);
        }
        return didSucceed;
    }

    /**
     * Removes a pantry item.
     *
     * @return true when a row was deleted
     */
    public boolean deletePantryItem(int itemId) {
        boolean didSucceed = false;
        try {
            int rowsAffected = database.delete(
                    PantryDBHelper.TABLE_PANTRY_ITEM,
                    PantryDBHelper.COL_PANTRY_ID + " = ?",
                    new String[]{String.valueOf(itemId)});
            didSucceed = rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete pantry item " + itemId, e);
        }
        return didSucceed;
    }

    /** All pantry items, ordered by name for display. */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PantryDBHelper.TABLE_PANTRY_ITEM,
                    null, null, null, null, null,
                    PantryDBHelper.COL_PANTRY_NAME + " COLLATE NOCASE ASC");

            while (cursor.moveToNext()) {
                items.add(pantryItemFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read pantry items", e);
        } finally {
            closeCursor(cursor);
        }
        return items;
    }

    /** A single pantry item, or null when the id is not present. */
    public PantryItem getPantryItem(int itemId) {
        PantryItem item = null;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PantryDBHelper.TABLE_PANTRY_ITEM,
                    null,
                    PantryDBHelper.COL_PANTRY_ID + " = ?",
                    new String[]{String.valueOf(itemId)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                item = pantryItemFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read pantry item " + itemId, e);
        } finally {
            closeCursor(cursor);
        }
        return item;
    }

    /** True when an ingredient with the same normalised name is already held. */
    public boolean pantryContainsName(String normalisedName, int excludingItemId) {
        boolean exists = false;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PantryDBHelper.TABLE_PANTRY_ITEM,
                    new String[]{PantryDBHelper.COL_PANTRY_ID},
                    PantryDBHelper.COL_PANTRY_NAME_NORM + " = ? AND "
                            + PantryDBHelper.COL_PANTRY_ID + " != ?",
                    new String[]{normalisedName, String.valueOf(excludingItemId)},
                    null, null, null);
            exists = cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check for duplicate ingredient", e);
        } finally {
            closeCursor(cursor);
        }
        return exists;
    }

    public int getPantryItemCount() {
        return countRows(PantryDBHelper.TABLE_PANTRY_ITEM);
    }

    // Recipes: read only, since the collection is seeded rather than edited

    /**
     * Every recipe with its ingredients attached. The matching rule needs the
     * full ingredient list for each recipe, so they are loaded together here.
     */
    public List<Recipe> getAllRecipesWithIngredients() {
        List<Recipe> recipes = getAllRecipes();
        for (Recipe recipe : recipes) {
            recipe.setIngredients(getIngredientsForRecipe(recipe.getId()));
        }
        return recipes;
    }

    /** Recipe summaries without ingredients, for list screens. */
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PantryDBHelper.TABLE_RECIPE,
                    null, null, null, null, null,
                    PantryDBHelper.COL_RECIPE_NAME + " COLLATE NOCASE ASC");

            while (cursor.moveToNext()) {
                recipes.add(recipeFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read recipes", e);
        } finally {
            closeCursor(cursor);
        }
        return recipes;
    }

    /** A single recipe with its ingredients, or null when not found. */
    public Recipe getRecipe(int recipeId) {
        Recipe recipe = null;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PantryDBHelper.TABLE_RECIPE,
                    null,
                    PantryDBHelper.COL_RECIPE_ID + " = ?",
                    new String[]{String.valueOf(recipeId)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                recipe = recipeFromCursor(cursor);
                recipe.setIngredients(getIngredientsForRecipe(recipeId));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read recipe " + recipeId, e);
        } finally {
            closeCursor(cursor);
        }
        return recipe;
    }

    /** The ingredients belonging to one recipe. */
    public List<RecipeIngredient> getIngredientsForRecipe(int recipeId) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PantryDBHelper.TABLE_RECIPE_INGREDIENT,
                    null,
                    PantryDBHelper.COL_INGREDIENT_RECIPE_ID + " = ?",
                    new String[]{String.valueOf(recipeId)},
                    null, null,
                    PantryDBHelper.COL_INGREDIENT_ID + " ASC");

            while (cursor.moveToNext()) {
                ingredients.add(ingredientFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read ingredients for recipe " + recipeId, e);
        } finally {
            closeCursor(cursor);
        }
        return ingredients;
    }

    public int getRecipeCount() {
        return countRows(PantryDBHelper.TABLE_RECIPE);
    }

    // Internal helpers

    /**
     * Applies a consumption plan after the user confirms they cooked a recipe.
     * Every removal and reduction is written inside a single transaction. A
     * recipe touches several pantry entries at once, and applying only some of
     * them would leave the pantry describing a meal that was never cooked, so
     * the whole set either succeeds or none of it does.
     * @return true when the pantry was updated
     */
    public boolean applyConsumption(RecipeConsumer.ConsumptionPlan plan) {
        if (plan == null || !plan.hasChanges()) {
            return false;
        }

        boolean didSucceed = false;
        database.beginTransaction();
        try {
            for (RecipeConsumer.Removal removal : plan.getRemovals()) {
                database.delete(
                        PantryDBHelper.TABLE_PANTRY_ITEM,
                        PantryDBHelper.COL_PANTRY_ID + " = ?",
                        new String[]{String.valueOf(removal.getItemId())});
            }

            for (RecipeConsumer.Reduction reduction : plan.getReductions()) {
                ContentValues values = new ContentValues();
                values.put(PantryDBHelper.COL_PANTRY_QUANTITY, reduction.getNewQuantity());
                database.update(
                        PantryDBHelper.TABLE_PANTRY_ITEM,
                        values,
                        PantryDBHelper.COL_PANTRY_ID + " = ?",
                        new String[]{String.valueOf(reduction.getItemId())});
            }

            database.setTransactionSuccessful();
            didSucceed = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply recipe consumption", e);
        } finally {
            // Without a successful marker this rolls the whole set back.
            database.endTransaction();
        }
        return didSucceed;
    }

    private ContentValues pantryValues(PantryItem item) {
        ContentValues values = new ContentValues();
        values.put(PantryDBHelper.COL_PANTRY_NAME, item.getName());
        values.put(PantryDBHelper.COL_PANTRY_NAME_NORM, item.getNameNormalised());
        values.put(PantryDBHelper.COL_PANTRY_QUANTITY, item.getQuantity());
        values.put(PantryDBHelper.COL_PANTRY_UNIT, item.getUnit());
        // Dates are stored as milliseconds because SQLite has no date type.
        values.put(PantryDBHelper.COL_PANTRY_EXPIRY, item.getExpiryDate());
        return values;
    }

    private PantryItem pantryItemFromCursor(Cursor cursor) {
        PantryItem item = new PantryItem();
        item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(PantryDBHelper.COL_PANTRY_ID)));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow(PantryDBHelper.COL_PANTRY_NAME)));
        item.setNameNormalised(cursor.getString(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_PANTRY_NAME_NORM)));
        item.setQuantity(cursor.getDouble(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_PANTRY_QUANTITY)));
        item.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(PantryDBHelper.COL_PANTRY_UNIT)));
        item.setExpiryDate(cursor.getLong(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_PANTRY_EXPIRY)));
        return item;
    }

    private Recipe recipeFromCursor(Cursor cursor) {
        Recipe recipe = new Recipe();
        recipe.setId(cursor.getInt(cursor.getColumnIndexOrThrow(PantryDBHelper.COL_RECIPE_ID)));
        recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(PantryDBHelper.COL_RECIPE_NAME)));
        recipe.setDescription(cursor.getString(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_RECIPE_DESCRIPTION)));
        recipe.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(PantryDBHelper.COL_RECIPE_STEPS)));
        recipe.setPrepMinutes(cursor.getInt(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_RECIPE_PREP_MINUTES)));
        return recipe;
    }

    private RecipeIngredient ingredientFromCursor(Cursor cursor) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setId(cursor.getInt(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_INGREDIENT_ID)));
        ingredient.setRecipeId(cursor.getInt(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_INGREDIENT_RECIPE_ID)));
        ingredient.setName(cursor.getString(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_INGREDIENT_NAME)));
        ingredient.setNameNormalised(cursor.getString(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_INGREDIENT_NAME_NORM)));
        ingredient.setQuantity(cursor.getDouble(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_INGREDIENT_QUANTITY)));
        ingredient.setUnit(cursor.getString(
                cursor.getColumnIndexOrThrow(PantryDBHelper.COL_INGREDIENT_UNIT)));
        return ingredient;
    }

    private int countRows(String tableName) {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to count rows in " + tableName, e);
        } finally {
            closeCursor(cursor);
        }
        return count;
    }

    /** Cursors hold native resources and leak if they are not closed. */
    private void closeCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}

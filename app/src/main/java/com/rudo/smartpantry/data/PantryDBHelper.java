package com.rudo.smartpantry.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Creates and upgrades the Smart Pantry database.
 *
 * Following the pattern taught in the module, this class does nothing except
 * define the tables and react to version changes. Opening, closing and
 * querying all live in PantryDataSource.
 *
 * Three tables are used. Recipe ingredients are held in their own table rather
 * than as a delimited column on the recipe, because the strict-matching rule
 * has to examine each required ingredient individually.
 */
public class PantryDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "PantryDBHelper";

    private static final String DATABASE_NAME = "smartpantry.db";
    private static final int DATABASE_VERSION = 1;

    // Pantry item table.
    public static final String TABLE_PANTRY_ITEM = "pantry_item";
    public static final String COL_PANTRY_ID = "_id";
    public static final String COL_PANTRY_NAME = "name";
    public static final String COL_PANTRY_NAME_NORM = "name_normalised";
    public static final String COL_PANTRY_QUANTITY = "quantity";
    public static final String COL_PANTRY_UNIT = "unit";
    public static final String COL_PANTRY_EXPIRY = "expiry_date";

    // Recipe table.
    public static final String TABLE_RECIPE = "recipe";
    public static final String COL_RECIPE_ID = "_id";
    public static final String COL_RECIPE_NAME = "name";
    public static final String COL_RECIPE_DESCRIPTION = "description";
    public static final String COL_RECIPE_STEPS = "steps";
    public static final String COL_RECIPE_PREP_MINUTES = "prep_minutes";

    // Recipe ingredient table.
    public static final String TABLE_RECIPE_INGREDIENT = "recipe_ingredient";
    public static final String COL_INGREDIENT_ID = "_id";
    public static final String COL_INGREDIENT_RECIPE_ID = "recipe_id";
    public static final String COL_INGREDIENT_NAME = "name";
    public static final String COL_INGREDIENT_NAME_NORM = "name_normalised";
    public static final String COL_INGREDIENT_QUANTITY = "quantity";
    public static final String COL_INGREDIENT_UNIT = "unit";

    private static final String CREATE_TABLE_PANTRY_ITEM =
            "CREATE TABLE " + TABLE_PANTRY_ITEM + " (" +
                    COL_PANTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PANTRY_NAME + " TEXT NOT NULL, " +
                    COL_PANTRY_NAME_NORM + " TEXT NOT NULL, " +
                    COL_PANTRY_QUANTITY + " REAL NOT NULL DEFAULT 0, " +
                    COL_PANTRY_UNIT + " TEXT, " +
                    COL_PANTRY_EXPIRY + " INTEGER DEFAULT -1)";

    private static final String CREATE_TABLE_RECIPE =
            "CREATE TABLE " + TABLE_RECIPE + " (" +
                    COL_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_RECIPE_NAME + " TEXT NOT NULL, " +
                    COL_RECIPE_DESCRIPTION + " TEXT, " +
                    COL_RECIPE_STEPS + " TEXT, " +
                    COL_RECIPE_PREP_MINUTES + " INTEGER DEFAULT 0)";

    private static final String CREATE_TABLE_RECIPE_INGREDIENT =
            "CREATE TABLE " + TABLE_RECIPE_INGREDIENT + " (" +
                    COL_INGREDIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_INGREDIENT_RECIPE_ID + " INTEGER NOT NULL, " +
                    COL_INGREDIENT_NAME + " TEXT NOT NULL, " +
                    COL_INGREDIENT_NAME_NORM + " TEXT NOT NULL, " +
                    COL_INGREDIENT_QUANTITY + " REAL NOT NULL DEFAULT 0, " +
                    COL_INGREDIENT_UNIT + " TEXT, " +
                    "FOREIGN KEY (" + COL_INGREDIENT_RECIPE_ID + ") REFERENCES " +
                    TABLE_RECIPE + "(" + COL_RECIPE_ID + ") ON DELETE CASCADE)";

    /** Speeds up the repeated lookups the matching rule performs. */
    private static final String CREATE_INDEX_PANTRY_NAME =
            "CREATE INDEX idx_pantry_name_norm ON " +
                    TABLE_PANTRY_ITEM + " (" + COL_PANTRY_NAME_NORM + ")";

    private static final String CREATE_INDEX_INGREDIENT_RECIPE =
            "CREATE INDEX idx_ingredient_recipe ON " +
                    TABLE_RECIPE_INGREDIENT + " (" + COL_INGREDIENT_RECIPE_ID + ")";

    public PantryDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // SQLite ignores foreign keys unless they are switched on per connection.
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Runs the first time the database file is opened. The recipe collection is
     * seeded here so that the app has something to suggest against on first run.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PANTRY_ITEM);
        db.execSQL(CREATE_TABLE_RECIPE);
        db.execSQL(CREATE_TABLE_RECIPE_INGREDIENT);
        db.execSQL(CREATE_INDEX_PANTRY_NAME);
        db.execSQL(CREATE_INDEX_INGREDIENT_RECIPE);

        int seeded = RecipeSeedData.seed(db);
        Log.i(TAG, "Database created and seeded with " + seeded + " recipes.");
    }

    /**
     * Rebuilds the schema when the version number is raised. Pantry contents
     * are user data and would be lost, so any future change that needs to
     * preserve them should use ALTER TABLE here instead of dropping.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion
                + ". Existing data will be discarded.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY_ITEM);
        onCreate(db);
    }
}

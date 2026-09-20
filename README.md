# Smart Pantry Manager

An Android application, written in Java, that helps reduce household food waste by
tracking the ingredients a user already has at home and suggesting only the recipes
they can cook right now — with no shopping trip required.

Mobile App Development 700 · Richfield Graduate Institute of Technology · 2026

## What the app does

Most recipe applications work the other way round: you choose a dish, then go and buy
what it needs. Smart Pantry Manager starts from what is already in the cupboard. The
user records their ingredients, and the app suggests dishes that can be made from
those ingredients alone.

The rule behind this is deliberately strict. A recipe is only suggested when every
ingredient it requires is present in the pantry in at least the required quantity. A
recipe missing even one ingredient is excluded from the suggestions, no matter how
close it is.

### Features

- Add, edit and delete pantry ingredients, each with a quantity, unit and optional expiry date
- A pantry list backed by a RecyclerView bound to the local database
- Twenty recipes seeded into the database on first run, each with ingredients and method
- A Suggested Recipes screen applying the strict-matching rule
- A separate "Almost There" list showing recipes missing exactly one ingredient
- A recipe detail screen marking each ingredient as held or missing
- A settings screen for user preferences
- Input validation on the ingredient form, including a duplicate-ingredient check
- Clear feedback when no recipes match, rather than a blank screen


## Database: SQLite via SQLiteOpenHelper

The assignment permits SQLite, Firebase or PostgreSQL. This project uses SQLite,
accessed through a subclass of `SQLiteOpenHelper`.

## Why SQLite

The application is single-user and entirely local. A pantry belongs to one person on
one device, and there is nothing to synchronise, share or serve to other users.
Introducing a cloud database would add an account, a network dependency and a
third-party service without addressing any requirement the app actually has.

SQLite also means the app works with no connectivity at all, which matters for
something a user opens while standing in their kitchen. Firebase would require a
network round trip before any suggestion could be shown, and PostgreSQL would require
building and hosting a REST backend in front of it.

Finally, SQLite is the persistence approach taught in the module, so the implementation
follows the pattern covered in the Learner Guide rather than introducing an unfamiliar
one.

## How persistence is structured

Three classes, following the pattern taught in the module:

| Class | Responsibility |
|---|---|
| `PantryDBHelper` | Extends `SQLiteOpenHelper`. Defines the tables, creates them, seeds the recipes and handles version upgrades. |
| `PantryDataSource` | Opens and closes the database and holds every query. Takes and returns model objects so the screens never touch SQL. |
| `PantryItem`, `Recipe`, `RecipeIngredient` | Plain model classes carrying data between the database and the user interface. |

## Schema


pantry_item          recipe                  recipe_ingredient
-----------          ------                  -----------------
_id (PK)             _id (PK)                _id (PK)
name                 name                    recipe_id (FK -> recipe._id)
name_normalised      description             name
quantity             steps                   name_normalised
unit                 prep_minutes            quantity
expiry_date                                  unit


Recipe ingredients are held in their own table rather than packed into a delimited
column, because the matching rule compares a recipe against the pantry one ingredient
at a time.

## How the matching works

A naive string comparison breaks on trivial differences. A pantry holding "2 Large
Tomatoes" should satisfy a recipe asking for "tomato", and 500 g of flour should
satisfy a recipe asking for 0.5 kg. Two conversions handle this.

Names are folded to a canonical form by `IngredientNormaliser.normaliseName()`:
lower-cased, stripped of bracketed notes and punctuation, cleared of descriptive filler
such as "large" or "fresh", and reduced to singular. Both "2 Large Ripe Tomatoes" and
"tomato" reduce to `tomato`. This happens when a record is written, not when a match
runs, so the comparison itself stays a straight lookup.

Quantities are converted to a base unit before comparison — grams for mass,
millilitres for volume, individual items for anything countable. Unit spellings resolve
through an alias table first, so "kg", "kilogram" and "kilos" all mean the same thing.

`RecipeMatcher` then indexes the pantry once into a map keyed on normalised name and
tests each recipe against it. Where the pantry holds the same ingredient across more
than one entry, the holdings are added together, so two part-used bags of flour can
satisfy a requirement neither could alone.

Known limitation. When the pantry and the recipe measure an ingredient in different
kinds of unit — the pantry says "1 milk" while the recipe asks for "250 ml" — the two
amounts cannot be compared meaningfully. In that case possession of the ingredient is
accepted on its own. This is a deliberate trade-off that keeps the app usable when a
user records something loosely, at the cost of not verifying the amount in that one
case.

## Setup and running

## Requirements

- Android Studio (Meerkat or later)
- JDK bundled with Android Studio
- Android SDK with API 34 installed
- Minimum supported device: API 24 (Android 7.0)

## Steps

1. Clone the repository:

   git clone https://github.com/Rudo-Uni/smart-pantry-manager.git

2. In Android Studio choose **File → Open** and select the cloned folder.
3. Wait for the initial Gradle sync to finish. Dependencies download on first sync.
4. Select a device — either a connected physical device with USB debugging enabled, or
   an emulator running API 24 or later.
5. Click **Run**.

No API keys, accounts or configuration files are required. The database is created and
seeded with twenty recipes automatically the first time the app launches.

## Resetting the database

The seeding runs in `onCreate`, which only fires when the database file does not yet
exist. To reseed, either uninstall the app from the device or raise
`DATABASE_VERSION` in `PantryDBHelper`.



## Project structure


com.rudo.smartpantry
├── MainActivity.java              Pantry list, the launcher screen
├── data
│   ├── PantryDBHelper.java        Table definitions, creation, upgrades
│   ├── PantryDataSource.java      All database queries
│   └── RecipeSeedData.java        The twenty seeded recipes
├── model
│   ├── PantryItem.java
│   ├── Recipe.java
│   └── RecipeIngredient.java
├── ui
│   ├── AddEditItemActivity.java   Add and edit form with validation
│   ├── SuggestedRecipesActivity.java
│   ├── RecipeDetailActivity.java
│   ├── SettingsActivity.java
│   ├── PantryAdapter.java         RecyclerView adapter for the pantry
│   ├── RecipeAdapter.java         RecyclerView adapter for recipes
│   └── NavBarHelper.java          Shared bottom navigation wiring
└── util
    ├── IngredientNormaliser.java  Name folding and unit conversion
    ├── RecipeMatcher.java         The strict-matching rule
    └── AppPreferences.java        SharedPreferences access



## Out of scope

The assignment brief excludes the following, and none of them appear in this project:

- Google Maps, any mapping SDK, or GPS and location services
- Payment processing
- Publication to the Google Play Store

The application requires no runtime permissions and makes no network requests.



## Author

Rudo 410402352 BSc IT (3rd Year), Richfield Graduate Institute of Technology
package com.rudo.smartpantry.model;

import com.rudo.smartpantry.util.IngredientNormaliser;

/**
 * A single ingredient currently held in the user's pantry.
 * The id defaults to -1 so that calling code can distinguish a newly created
 * item (which must be inserted) from one loaded out of the database (which
 * must be updated).
 * Two forms of the name are kept. The display name preserves whatever the user
 * typed, while the normalised name is the lower-cased, singular, filler-free
 * form used by the strict-matching rule. Normalising on write keeps the match
 * itself a straight comparison.
 */
public class PantryItem {

    /**
     * Sentinel used for expiryDate when the user did not supply one.
     */
    public static final long NO_EXPIRY = -1L;

    private int id;
    private String name;
    private String nameNormalised;
    private double quantity;
    private String unit;
    private long expiryDate;

    public PantryItem() {
        this.id = -1;
        this.name = "";
        this.nameNormalised = "";
        this.quantity = 0d;
        this.unit = "";
        this.expiryDate = NO_EXPIRY;
    }

    public PantryItem(String name, double quantity, String unit) {
        this();
        setName(name);
        this.quantity = quantity;
        this.unit = unit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    /**
     * Setting the display name always refreshes the normalised form with it.
     */
    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
        this.nameNormalised = IngredientNormaliser.normaliseName(this.name);
    }

    public String getNameNormalised() {
        return nameNormalised;
    }

    /**
     * Used by the data source when rebuilding an object from a Cursor.
     */
    public void setNameNormalised(String nameNormalised) {
        this.nameNormalised = nameNormalised;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit == null ? "" : unit.trim();
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean hasExpiryDate() {
        return expiryDate != NO_EXPIRY;
    }

    /**
     * Convenience for list rows, e.g. "500 g" or "3" when there is no unit.
     */
    public String getDisplayQuantity() {
        String amount = IngredientNormaliser.formatAmount(quantity);
        return unit.isEmpty() ? amount : amount + " " + unit;
    }

    @Override
    public String toString() {
        return name + " (" + getDisplayQuantity() + ")";
    }

}
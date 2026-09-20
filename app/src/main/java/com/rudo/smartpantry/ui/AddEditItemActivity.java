package com.rudo.smartpantry.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.data.PantryDataSource;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.util.IngredientNormaliser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Adds a new pantry item or edits an existing one.
 *
 * Which of the two it is depends on whether an item id arrives in the Intent.
 * Without one the form starts blank and saving inserts; with one the item is
 * loaded and saving updates. Using a single screen for both keeps the
 * validation rules in one place.
 */
public class AddEditItemActivity extends AppCompatActivity {

    /** Intent extra carrying the id of the item to edit. */
    public static final String EXTRA_ITEM_ID = "com.rudo.smartpantry.ITEM_ID";

    /** Units offered in the spinner, all understood by IngredientNormaliser. */
    private static final String[] UNITS = {
            "", "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "slice", "clove", "can", "pinch"
    };

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    private PantryDataSource dataSource;
    private EditText nameField;
    private EditText quantityField;
    private Spinner unitSpinner;
    private TextView expiryValue;

    private int itemId = -1;
    private long expiryDate = PantryItem.NO_EXPIRY;

    /**
     * Whether the form has already been populated from the database.
     *
     * onResume runs again whenever the user returns to this screen, for
     * instance after visiting another destination from the navigation bar.
     * Reloading at that point would silently discard anything they had
     * already typed, so the form is only filled the first time.
     */
    private boolean formLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_item);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataSource = new PantryDataSource(this);

        nameField = findViewById(R.id.editName);
        quantityField = findViewById(R.id.editQuantity);
        unitSpinner = findViewById(R.id.spinnerUnit);
        expiryValue = findViewById(R.id.txtExpiryValue);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, buildUnitLabels());
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        findViewById(R.id.btnChooseDate).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btnClearDate).setOnClickListener(v -> {
            expiryDate = PantryItem.NO_EXPIRY;
            updateExpiryLabel();
        });

        Button saveButton = findViewById(R.id.btnSaveItem);
        saveButton.setOnClickListener(v -> saveItem());

        itemId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
        NavBarHelper.setup(this, NavBarHelper.Screen.PANTRY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataSource.open();

        if (itemId != -1 && !formLoaded) {
            loadExistingItem();
            formLoaded = true;
        } else if (itemId == -1) {
            updateExpiryLabel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataSource.close();
    }

    /** Fills the form from the database when an existing item is being edited. */
    private void loadExistingItem() {
        PantryItem item = dataSource.getPantryItem(itemId);
        if (item == null) {
            // The item was deleted while this screen was open.
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtFormTitle)).setText(R.string.title_edit_item);
        nameField.setText(item.getName());
        quantityField.setText(IngredientNormaliser.formatAmount(item.getQuantity()));
        expiryDate = item.getExpiryDate();
        updateExpiryLabel();

        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equals(item.getUnit())) {
                unitSpinner.setSelection(i);
                break;
            }
        }
    }

    /**
     * Validates the form and writes the item.
     *
     * Every rule reports against the field it concerns so the user is told
     * exactly what to fix, and nothing is written until all of them pass.
     */
    private void saveItem() {
        String name = nameField.getText().toString().trim();
        String quantityText = quantityField.getText().toString().trim();

        if (name.isEmpty()) {
            nameField.setError(getString(R.string.error_name_required));
            nameField.requestFocus();
            return;
        }
        if (name.length() < 2) {
            nameField.setError(getString(R.string.error_name_too_short));
            nameField.requestFocus();
            return;
        }
        if (quantityText.isEmpty()) {
            quantityField.setError(getString(R.string.error_quantity_required));
            quantityField.requestFocus();
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityText);
        } catch (NumberFormatException e) {
            quantityField.setError(getString(R.string.error_quantity_invalid));
            quantityField.requestFocus();
            return;
        }

        if (quantity <= 0) {
            quantityField.setError(getString(R.string.error_quantity_positive));
            quantityField.requestFocus();
            return;
        }

        // Two rows for the same ingredient would split a single holding across
        // entries, so the normalised name must be unique.
        String normalised = IngredientNormaliser.normaliseName(name);
        if (dataSource.pantryContainsName(normalised, itemId)) {
            nameField.setError(getString(R.string.error_duplicate_name));
            nameField.requestFocus();
            return;
        }

        PantryItem item = new PantryItem();
        item.setId(itemId);
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnit(UNITS[unitSpinner.getSelectedItemPosition()]);
        item.setExpiryDate(expiryDate);

        boolean saved;
        if (itemId == -1) {
            saved = dataSource.insertPantryItem(item) != -1;
        } else {
            saved = dataSource.updatePantryItem(item);
        }

        if (saved) {
            Toast.makeText(this, getString(R.string.item_saved, name), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (expiryDate != PantryItem.NO_EXPIRY) {
            calendar.setTimeInMillis(expiryDate);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth, 0, 0, 0);
            chosen.set(Calendar.MILLISECOND, 0);
            expiryDate = chosen.getTimeInMillis();
            updateExpiryLabel();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateExpiryLabel() {
        if (expiryDate == PantryItem.NO_EXPIRY) {
            expiryValue.setText(R.string.no_expiry_set);
        } else {
            expiryValue.setText(DATE_FORMAT.format(new Date(expiryDate)));
        }
    }

    /** Shows a readable label for the blank unit used by countable items. */
    private String[] buildUnitLabels() {
        String[] labels = new String[UNITS.length];
        for (int i = 0; i < UNITS.length; i++) {
            labels[i] = UNITS[i].isEmpty() ? "(whole items)" : UNITS[i];
        }
        return labels;
    }
}

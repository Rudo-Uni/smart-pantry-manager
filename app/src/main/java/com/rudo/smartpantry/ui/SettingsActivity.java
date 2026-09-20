package com.rudo.smartpantry.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.data.PantryDataSource;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.util.AppPreferences;

/**
 * User preferences, stored with SharedPreferences.
 *
 * Settings are written the moment they change rather than behind a save
 * button, which is the behaviour users expect on Android and means there is
 * no unsaved state to lose if the screen is closed.
 */
public class SettingsActivity extends AppCompatActivity {

    /** Warning windows offered for expiring ingredients. */
    private static final int[] EXPIRY_DAY_OPTIONS = {3, 7, 14, 30};

    private PantryDataSource dataSource;
    private SwitchCompat almostThereSwitch;
    private SwitchCompat expiryAlertsSwitch;
    private Spinner expiryDaysSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataSource = new PantryDataSource(this);

        almostThereSwitch = findViewById(R.id.switchAlmostThere);
        expiryAlertsSwitch = findViewById(R.id.switchExpiryAlerts);
        expiryDaysSpinner = findViewById(R.id.spinnerExpiryDays);

        setUpExpirySpinner();
        loadCurrentSettings();
        attachListeners();

        Button clearButton = findViewById(R.id.btnClearPantry);
        clearButton.setOnClickListener(v -> confirmClearPantry());

        NavBarHelper.setup(this, NavBarHelper.Screen.SETTINGS);
    }

    private void setUpExpirySpinner() {
        String[] labels = new String[EXPIRY_DAY_OPTIONS.length];
        for (int i = 0; i < EXPIRY_DAY_OPTIONS.length; i++) {
            labels[i] = EXPIRY_DAY_OPTIONS[i] + " days";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        expiryDaysSpinner.setAdapter(adapter);
    }

    /** Reads the stored values so the controls reflect what is actually saved. */
    private void loadCurrentSettings() {
        almostThereSwitch.setChecked(AppPreferences.isShowAlmostThere(this));

        boolean alertsOn = AppPreferences.isExpiryAlertsEnabled(this);
        expiryAlertsSwitch.setChecked(alertsOn);
        expiryDaysSpinner.setEnabled(alertsOn);

        int savedDays = AppPreferences.getExpiryWindowDays(this);
        for (int i = 0; i < EXPIRY_DAY_OPTIONS.length; i++) {
            if (EXPIRY_DAY_OPTIONS[i] == savedDays) {
                expiryDaysSpinner.setSelection(i);
                break;
            }
        }
    }

    private void attachListeners() {
        almostThereSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            AppPreferences.setShowAlmostThere(this, isChecked);
            confirmSaved();
        });

        expiryAlertsSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            AppPreferences.setExpiryAlertsEnabled(this, isChecked);
            // The warning window is meaningless when alerts are switched off.
            expiryDaysSpinner.setEnabled(isChecked);
            confirmSaved();
        });

        expiryDaysSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int chosen = EXPIRY_DAY_OPTIONS[position];
                if (chosen != AppPreferences.getExpiryWindowDays(SettingsActivity.this)) {
                    AppPreferences.setExpiryWindowDays(SettingsActivity.this, chosen);
                    confirmSaved();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed.
            }
        });
    }

    /** Emptying the pantry cannot be undone, so it is confirmed first. */
    private void confirmClearPantry() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_pantry_title)
                .setMessage(R.string.clear_pantry_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dataSource.open();
                    for (PantryItem item : dataSource.getAllPantryItems()) {
                        dataSource.deletePantryItem(item.getId());
                    }
                    dataSource.close();
                    Toast.makeText(this, R.string.pantry_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmSaved() {
        Toast.makeText(this, R.string.setting_saved, Toast.LENGTH_SHORT).show();
    }
}

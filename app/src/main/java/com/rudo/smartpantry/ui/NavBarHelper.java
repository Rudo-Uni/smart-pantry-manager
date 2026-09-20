package com.rudo.smartpantry.ui;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.rudo.smartpantry.MainActivity;
import com.rudo.smartpantry.R;

/**
 * Wires up the bottom navigation bar that every screen includes.
 * The same three listeners would otherwise be repeated in each activity, so
 * they are set from one place here. The current screen's button is tinted and
 * does nothing when tapped, which avoids stacking a second copy of a screen
 * the user is already looking at.
 */
public final class NavBarHelper {

    /** The destinations reachable from the navigation bar. */
    public enum Screen {
        PANTRY,
        RECIPES,
        SETTINGS
    }

    private NavBarHelper() {
        // Utility class, never instantiated.
    }

    /**
     * Attaches the navigation behaviour to an activity that includes nav_bar.
     *
     * @param activity the screen being set up
     * @param current  which destination that screen represents
     */
    public static void setup(Activity activity, Screen current) {
        bind(activity, R.id.btnNavPantry, R.id.lblNavPantry,
                current == Screen.PANTRY, MainActivity.class);
        bind(activity, R.id.btnNavRecipes, R.id.lblNavRecipes,
                current == Screen.RECIPES, SuggestedRecipesActivity.class);
        bind(activity, R.id.btnNavSettings, R.id.lblNavSettings,
                current == Screen.SETTINGS, SettingsActivity.class);
    }

    private static void bind(Activity activity, int buttonId, int labelId,
                             boolean isCurrent, Class<?> destination) {
        ImageButton button = activity.findViewById(buttonId);
        TextView label = activity.findViewById(labelId);
        if (button == null) {
            return;
        }

        int colour = ContextCompat.getColor(activity,
                isCurrent ? R.color.primary : R.color.text_secondary);
        button.setColorFilter(colour);
        if (label != null) {
            label.setTextColor(colour);
        }

        if (isCurrent) {
            button.setOnClickListener(v -> { });
            return;
        }

        button.setOnClickListener(v -> {
            Intent intent = new Intent(activity, destination);
            // Reuse the existing instance instead of stacking copies of a
            // screen the user has already visited.
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
        });
    }
}
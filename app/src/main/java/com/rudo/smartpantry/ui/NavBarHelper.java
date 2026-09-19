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
 *
 * The same three listeners would otherwise be repeated in each activity, so
 * they are set from one place here. The current screen's button is tinted and
 * does nothing when tapped, which avoids stacking a second copy of a screen
 * the user is already looking at.
 *
 * The settings destination is not yet built, so its button is shown but
 * disabled and is connected once that screen exists.
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

        // Not yet built, so this is visible but inactive rather than silently
        // doing nothing when tapped.
        tint(activity, R.id.btnNavSettings, R.id.lblNavSettings, false);
        ImageButton settings = activity.findViewById(R.id.btnNavSettings);
        if (settings != null) {
            settings.setEnabled(false);
            settings.setAlpha(0.4f);
        }
    }

    private static void bind(Activity activity, int buttonId, int labelId,
                             boolean isCurrent, Class<?> destination) {
        ImageButton button = activity.findViewById(buttonId);
        if (button == null) {
            return;
        }

        tint(activity, buttonId, labelId, isCurrent);

        if (isCurrent) {
            // Already here, so the tap does nothing.
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

    private static void tint(Activity activity, int buttonId, int labelId, boolean isCurrent) {
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
    }
}
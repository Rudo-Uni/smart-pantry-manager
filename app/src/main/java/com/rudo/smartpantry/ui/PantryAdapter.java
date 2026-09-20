package com.rudo.smartpantry.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.util.ExpiryStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Binds pantry items to the rows of the pantry RecyclerView.
 * A RecyclerView creates only enough row views to fill the screen and then
 * reuses them as the user scrolls. The ViewHolder below holds the references
 * to each row's widgets so that findViewById is called once per row rather
 * than every time a row is rebound.
 * The adapter does not know how to edit or delete anything. It reports taps
 * through a listener so that the activity keeps control of what happens,
 * which keeps database work out of the list.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    /** Implemented by the hosting activity to react to row taps. */
    public interface OnItemActionListener {
        void onItemClicked(PantryItem item);

        void onDeleteClicked(PantryItem item);
    }

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    private boolean expiryAlertsEnabled = true;
    private int expiryWindowDays = 7;

    public PantryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the whole list, used after any change to the database. */
    public void setItems(List<PantryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /**
     * Applies the user's expiry preferences to the list.
     * The adapter is told about the settings rather than reading them itself,
     * so that the preference lookup happens once per refresh instead of once
     * per row.
     */
    public void setExpiryPreferences(boolean alertsEnabled, int windowDays) {
        this.expiryAlertsEnabled = alertsEnabled;
        this.expiryWindowDays = windowDays;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);

        holder.name.setText(item.getName());
        holder.quantity.setText(item.getDisplayQuantity());

        bindExpiry(holder, item);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClicked(item);
            }
        });

        holder.delete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClicked(item);
            }
        });
    }

    /**
     * Shows the expiry line, wording and colouring it according to how urgent
     * the date is. Items that are fine, or that have no date at all, show
     * nothing rather than adding noise to the row.
     */
    private void bindExpiry(PantryViewHolder holder, PantryItem item) {
        if (!item.hasExpiryDate()) {
            holder.expiry.setVisibility(View.GONE);
            return;
        }

        Context context = holder.itemView.getContext();

        // With alerts switched off the date is still shown, just not flagged.
        if (!expiryAlertsEnabled) {
            holder.expiry.setVisibility(View.VISIBLE);
            holder.expiry.setText(context.getString(R.string.expires_on,
                    DATE_FORMAT.format(new Date(item.getExpiryDate()))));
            holder.expiry.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary));
            return;
        }

        ExpiryStatus status = ExpiryStatus.of(item, expiryWindowDays);
        long days = ExpiryStatus.daysUntil(item.getExpiryDate());

        switch (status) {
            case EXPIRED:
                holder.expiry.setVisibility(View.VISIBLE);
                holder.expiry.setText(days == 0
                        ? context.getString(R.string.expired_today)
                        : context.getString(R.string.expired_days_ago, Math.abs(days)));
                holder.expiry.setTextColor(
                        ContextCompat.getColor(context, R.color.danger));
                break;

            case EXPIRING_SOON:
                holder.expiry.setVisibility(View.VISIBLE);
                if (days == 0) {
                    holder.expiry.setText(R.string.expires_today);
                } else if (days == 1) {
                    holder.expiry.setText(R.string.expires_tomorrow);
                } else {
                    holder.expiry.setText(
                            context.getString(R.string.expires_in_days, days));
                }
                holder.expiry.setTextColor(
                        ContextCompat.getColor(context, R.color.accent));
                break;

            case NONE:
            default:
                holder.expiry.setVisibility(View.VISIBLE);
                holder.expiry.setText(context.getString(R.string.expires_on,
                        DATE_FORMAT.format(new Date(item.getExpiryDate()))));
                holder.expiry.setTextColor(
                        ContextCompat.getColor(context, R.color.text_secondary));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Holds one row's widget references so they are looked up only once. */
    static class PantryViewHolder extends RecyclerView.ViewHolder {

        final TextView name;
        final TextView quantity;
        final TextView expiry;
        final ImageButton delete;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtItemName);
            quantity = itemView.findViewById(R.id.txtItemQuantity);
            expiry = itemView.findViewById(R.id.txtItemExpiry);
            delete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}

package com.rudo.smartpantry.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rudo.smartpantry.R;
import com.rudo.smartpantry.model.PantryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Binds pantry items to the rows of the pantry RecyclerView.
 *
 * A RecyclerView creates only enough row views to fill the screen and then
 * reuses them as the user scrolls. The ViewHolder below holds the references
 * to each row's widgets so that findViewById is called once per row rather
 * than every time a row is rebound.
 *
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

    public boolean isEmpty() {
        return items.isEmpty();
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

        if (item.hasExpiryDate()) {
            holder.expiry.setVisibility(View.VISIBLE);
            holder.expiry.setText("Expires " + DATE_FORMAT.format(new Date(item.getExpiryDate())));
        } else {
            holder.expiry.setVisibility(View.GONE);
        }

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

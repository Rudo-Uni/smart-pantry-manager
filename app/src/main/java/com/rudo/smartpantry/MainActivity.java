package com.rudo.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rudo.smartpantry.data.PantryDataSource;
import com.rudo.smartpantry.model.PantryItem;
import com.rudo.smartpantry.ui.AddEditItemActivity;
import com.rudo.smartpantry.ui.NavBarHelper;
import com.rudo.smartpantry.ui.PantryAdapter;
import com.rudo.smartpantry.util.AppPreferences;

import java.util.List;

/**
 * The pantry list, and the app's launcher screen.
 * Shows every ingredient the user currently has, and is the starting point for
 * adding, editing and deleting them. The list is refreshed in onResume rather
 * than only in onCreate, so returning from the add or edit screen always shows
 * current data without needing a result callback.
 */
public class MainActivity extends AppCompatActivity implements PantryAdapter.OnItemActionListener {

    private PantryDataSource dataSource;
    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TextView countLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataSource = new PantryDataSource(this);

        recyclerView = findViewById(R.id.recyclerPantry);
        emptyMessage = findViewById(R.id.txtPantryEmpty);
        countLabel = findViewById(R.id.txtPantryCount);

        adapter = new PantryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.btnAddItem);
        addButton.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditItemActivity.class)));

        NavBarHelper.setup(this, NavBarHelper.Screen.PANTRY);
    }

    /**
     * The database is opened here and closed in onPause, so the connection is
     * only held while this screen is actually in front of the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        dataSource.open();
        refreshList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataSource.close();
    }

    /** Reloads from the database and switches between the list and the empty message. */
    private void refreshList() {
        List<PantryItem> items = dataSource.getAllPantryItems();

        // Read the preferences once per refresh rather than once per row.
        adapter.setExpiryPreferences(
                AppPreferences.isExpiryAlertsEnabled(this),
                AppPreferences.getExpiryWindowDays(this));
        adapter.setItems(items);

        boolean isEmpty = items.isEmpty();
        emptyMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        countLabel.setText(getString(R.string.pantry_count, items.size()));
    }

    /** Tapping a row opens it for editing, identified by its database id. */
    @Override
    public void onItemClicked(PantryItem item) {
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra(AddEditItemActivity.EXTRA_ITEM_ID, item.getId());
        startActivity(intent);
    }

    /** Deletion is confirmed first, since it cannot be undone. */
    @Override
    public void onDeleteClicked(PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item_title)
                .setMessage(getString(R.string.delete_item_message, item.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (dataSource.deletePantryItem(item.getId())) {
                        Toast.makeText(this,
                                getString(R.string.item_deleted, item.getName()),
                                Toast.LENGTH_SHORT).show();
                        refreshList();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
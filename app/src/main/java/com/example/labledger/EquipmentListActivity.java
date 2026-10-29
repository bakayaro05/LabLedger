package com.example.labledger;

import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class EquipmentListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etSearch;
    private TextView tvEquipmentCount;
    private TextView tvEquipmentEmpty;
    private EquipmentAdapter adapter;
    private ArrayList<Equipment> allEquipment;
    private ArrayList<Equipment> visibleEquipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DatabaseHelper(this);
        allEquipment = new ArrayList<>();
        visibleEquipment = new ArrayList<>();

        etSearch = findViewById(R.id.etSearchEquipment);
        tvEquipmentCount = findViewById(R.id.tvEquipmentCount);
        tvEquipmentEmpty = findViewById(R.id.tvEquipmentEmpty);
        Button btnClearSearch = findViewById(R.id.btnClearEquipmentSearch);
        Button btnAdd = findViewById(R.id.btnAddEquipment);
        Button btnBack = findViewById(R.id.btnBackInventory);
        ListView listView = findViewById(R.id.listEquipment);
        TextView emptyView = tvEquipmentEmpty;

        adapter = new EquipmentAdapter(this, visibleEquipment);
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyView);

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EquipmentListActivity.this,
                        EquipmentFormActivity.class));
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEquipment(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Equipment item = adapter.getItem(position);
                if (item != null) {
                    showEquipmentDetails(item);
                }
            }
        });
    }

    private void loadEquipment() {
        allEquipment.clear();
        allEquipment.addAll(dbHelper.getAllEquipment());
        filterEquipment(etSearch.getText().toString());
    }

    private void filterEquipment(String text) {
        String search = text.trim().toLowerCase(Locale.ROOT);
        visibleEquipment.clear();

        for (Equipment item : allEquipment) {
            String combined = (item.getName() + " " + item.getCategory() +
                    " " + item.getLocation()).toLowerCase(Locale.ROOT);
            if (combined.contains(search)) {
                visibleEquipment.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEquipmentCount.setText("Showing " + visibleEquipment.size() +
                " of " + allEquipment.size() + " equipment types");

        if (allEquipment.isEmpty()) {
            tvEquipmentEmpty.setText(
                    "No equipment registered yet. Tap Add Equipment to create one.");
        } else {
            tvEquipmentEmpty.setText(
                    "No matches found. Try a different search or tap Clear.");
        }
    }

    private void showEquipmentDetails(final Equipment item) {
        String message = "Category: " + item.getCategory() +
                "\nLocation: " + item.getLocation() +
                "\nTotal units: " + item.getTotalQuantity() +
                "\nAvailable units: " + item.getAvailableQuantity() +
                "\nBorrowed units: " + item.getBorrowedQuantity();

        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setMessage(message)
                .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(EquipmentListActivity.this,
                                EquipmentFormActivity.class);
                        intent.putExtra("equipment_id", item.getId());
                        startActivity(intent);
                    }
                })
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmDelete(item);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDelete(final Equipment item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Equipment?")
                .setMessage("Delete '" + item.getName() + "'? " +
                        "This is only allowed when the item has no loan history.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dbHelper.deleteEquipment(item.getId())) {
                            Toast.makeText(EquipmentListActivity.this,
                                    "Equipment deleted", Toast.LENGTH_SHORT).show();
                            loadEquipment();
                        } else {
                            new AlertDialog.Builder(EquipmentListActivity.this)
                                    .setTitle("Cannot Delete")
                                    .setMessage("This equipment has borrowing history " +
                                            "or is no longer available. Its record must be preserved.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadEquipment();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
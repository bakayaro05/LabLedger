
package com.example.labledger;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EquipmentFormActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etName;
    private EditText etLocation;
    private EditText etQuantity;
    private Spinner spinnerCategory;
    private TextView tvFormTitle;
    private TextView tvStockHint;
    private int equipmentId = -1;
    private int borrowedUnits = 0;

    private final String[] categories = {
            "Select Category",
            "Electronics",
            "Tools",
            "Measuring Instruments",
            "Lab Accessories",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_form);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DatabaseHelper(this);
        tvFormTitle = findViewById(R.id.tvEquipmentFormTitle);
        tvStockHint = findViewById(R.id.tvBorrowedStockHint);
        etName = findViewById(R.id.etEquipmentName);
        spinnerCategory = findViewById(R.id.spinnerEquipmentCategory);
        etLocation = findViewById(R.id.etEquipmentLocation);
        etQuantity = findViewById(R.id.etEquipmentQuantity);
        Button btnSave = findViewById(R.id.btnSaveEquipment);
        Button btnCancel = findViewById(R.id.btnCancelEquipment);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        equipmentId = getIntent().getIntExtra("equipment_id", -1);
        if (equipmentId != -1) {
            loadExistingEquipment();
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEquipment();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadExistingEquipment() {
        Equipment item = dbHelper.getEquipmentById(equipmentId);
        if (item == null) {
            Toast.makeText(this, "Equipment not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvFormTitle.setText("Edit Equipment");
        etName.setText(item.getName());
        etLocation.setText(item.getLocation());
        etQuantity.setText(String.valueOf(item.getTotalQuantity()));
        borrowedUnits = item.getBorrowedQuantity();

        for (int i = 1; i < categories.length; i++) {
            if (categories[i].equals(item.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        tvStockHint.setVisibility(View.VISIBLE);
        tvStockHint.setText("Currently borrowed: " + borrowedUnits +
                " units. Total quantity cannot be less than this.");
    }

    private void saveEquipment() {
        String name = etName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();
        int selectedCategory = spinnerCategory.getSelectedItemPosition();

        if (name.isEmpty()) {
            etName.setError("Enter an equipment name");
            etName.requestFocus();
            return;
        }

        if (selectedCategory == 0) {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (location.isEmpty()) {
            etLocation.setError("Enter the storage location");
            etLocation.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            etQuantity.setError("Enter a valid whole number");
            etQuantity.requestFocus();
            return;
        }

        if (quantity <= 0) {
            etQuantity.setError("Quantity must be greater than zero");
            etQuantity.requestFocus();
            return;
        }

        if (equipmentId != -1 && quantity < borrowedUnits) {
            etQuantity.setError("Cannot be less than " + borrowedUnits +
                    " currently borrowed units");
            etQuantity.requestFocus();
            return;
        }

        String category = categories[selectedCategory];

        if (equipmentId == -1) {
            long newId = dbHelper.addEquipment(name, category, location, quantity);
            if (newId != -1) {
                Toast.makeText(this, "Equipment added successfully",
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Unable to add equipment",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            int result = dbHelper.updateEquipment(equipmentId,
                    name, category, location, quantity);
            if (result == 1) {
                Toast.makeText(this, "Equipment updated successfully",
                        Toast.LENGTH_SHORT).show();
                finish();
            } else if (result == -1) {
                etQuantity.setError("Quantity is below the borrowed amount");
                etQuantity.requestFocus();
            } else {
                Toast.makeText(this, "Equipment no longer exists",
                        Toast.LENGTH_LONG).show();
            }
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

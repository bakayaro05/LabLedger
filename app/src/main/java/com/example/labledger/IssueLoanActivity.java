package com.example.labledger;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class IssueLoanActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Spinner spinnerEquipment;
    private EditText etStudentName;
    private EditText etRegisterNumber;
    private EditText etLoanQuantity;
    private TextView tvAvailableStock;
    private TextView tvSelectedDate;
    private Button btnIssueLoan;

    private final ArrayList<Equipment> equipmentOptions = new ArrayList<>();
    private String dueDate = null;
    private Calendar chosenDueDate = null;
    private boolean loanSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_loan);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DatabaseHelper(this);

        spinnerEquipment = findViewById(R.id.spinnerIssueEquipment);
        etStudentName = findViewById(R.id.etLoanStudentName);
        etRegisterNumber = findViewById(R.id.etLoanRegisterNumber);
        etLoanQuantity = findViewById(R.id.etLoanQuantity);
        tvAvailableStock = findViewById(R.id.tvIssueAvailable);
        tvSelectedDate = findViewById(R.id.tvSelectedDueDate);
        Button btnChooseDate = findViewById(R.id.btnChooseDueDate);
        btnIssueLoan = findViewById(R.id.btnConfirmIssue);
        Button btnCancel = findViewById(R.id.btnCancelIssue);

        spinnerEquipment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                updateAvailableStock();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateAvailableStock();
            }
        });

        btnChooseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseDueDate();
            }
        });

        btnIssueLoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndConfirm();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadEquipment() {
        int previousId = -1;
        int oldPosition = spinnerEquipment.getSelectedItemPosition();
        if (oldPosition > 0 && oldPosition <= equipmentOptions.size()) {
            previousId = equipmentOptions.get(oldPosition - 1).getId();
        }

        equipmentOptions.clear();
        equipmentOptions.addAll(dbHelper.getAllEquipment());

        ArrayList<String> labels = new ArrayList<>();
        labels.add("Select equipment");
        for (Equipment item : equipmentOptions) {
            labels.add(item.getName() + " (" + item.getAvailableQuantity() +
                    " available)");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerEquipment.setAdapter(adapter);

        int restorePosition = 0;
        for (int i = 0; i < equipmentOptions.size(); i++) {
            if (equipmentOptions.get(i).getId() == previousId) {
                restorePosition = i + 1;
                break;
            }
        }
        spinnerEquipment.setSelection(restorePosition);
        updateAvailableStock();
    }

    private Equipment selectedEquipment() {
        int position = spinnerEquipment.getSelectedItemPosition();
        if (position <= 0 || position > equipmentOptions.size()) {
            return null;
        }
        return equipmentOptions.get(position - 1);
    }

    private void updateAvailableStock() {
        Equipment selected = selectedEquipment();
        if (equipmentOptions.isEmpty()) {
            tvAvailableStock.setText("No equipment found. Add equipment in Manage Equipment first.");
            tvAvailableStock.setTextColor(Color.parseColor("#B45309"));
            btnIssueLoan.setEnabled(false);
        } else if (selected == null) {
            tvAvailableStock.setText("Select an equipment item to view stock.");
            tvAvailableStock.setTextColor(Color.parseColor("#64748B"));
            btnIssueLoan.setEnabled(true);
        } else {
            int available = selected.getAvailableQuantity();
            tvAvailableStock.setText("Available: " + available + " of " +
                    selected.getTotalQuantity() + " units");
            tvAvailableStock.setTextColor(Color.parseColor(
                    available > 0 ? "#15803D" : "#DC2626"));
            btnIssueLoan.setEnabled(available > 0 && !loanSaved);
        }
    }

    private void chooseDueDate() {
        Calendar initial = chosenDueDate == null ?
                Calendar.getInstance() : (Calendar) chosenDueDate.clone();

        DatePickerDialog picker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int month, int dayOfMonth) {
                        chosenDueDate = Calendar.getInstance();
                        chosenDueDate.set(year, month, dayOfMonth);
                        dueDate = String.format(Locale.US,
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        tvSelectedDate.setText(new SimpleDateFormat(
                                "dd MMM yyyy", Locale.US)
                                .format(chosenDueDate.getTime()));
                    }
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));

        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        picker.getDatePicker().setMinDate(todayStart.getTimeInMillis());
        picker.show();
    }

    private void validateAndConfirm() {
        if (loanSaved) {
            return;
        }

        final Equipment item = selectedEquipment();
        if (item == null) {
            new AlertDialog.Builder(this)
                    .setMessage("Please select equipment first.")
                    .setPositiveButton("OK", null).show();
            return;
        }

        final String studentName = etStudentName.getText().toString().trim();
        final String registerNumber = etRegisterNumber.getText().toString().trim();
        String quantityText = etLoanQuantity.getText().toString().trim();

        if (studentName.isEmpty()) {
            etStudentName.setError("Enter the student name");
            etStudentName.requestFocus();
            return;
        }
        if (registerNumber.isEmpty()) {
            etRegisterNumber.setError("Enter the register number");
            etRegisterNumber.requestFocus();
            return;
        }

        final int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            etLoanQuantity.setError("Enter a valid whole number");
            etLoanQuantity.requestFocus();
            return;
        }
        if (quantity <= 0) {
            etLoanQuantity.setError("Quantity must be greater than zero");
            etLoanQuantity.requestFocus();
            return;
        }
        if (quantity > item.getAvailableQuantity()) {
            etLoanQuantity.setError("Only " + item.getAvailableQuantity() +
                    " units are available");
            etLoanQuantity.requestFocus();
            return;
        }
        if (dueDate == null) {
            new AlertDialog.Builder(this)
                    .setMessage("Please choose the return due date.")
                    .setPositiveButton("OK", null).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().getTime());
        if (dueDate.compareTo(today) < 0) {
            new AlertDialog.Builder(this)
                    .setMessage("Due date cannot be earlier than today. Choose another date.")
                    .setPositiveButton("OK", null).show();
            return;
        }

        final String confirmedDueDate = dueDate;
        String summary = "Equipment: " + item.getName() +
                "\nStudent: " + studentName +
                "\nRegister No.: " + registerNumber +
                "\nQuantity: " + quantity +
                "\nDue date: " + confirmedDueDate;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Equipment Issue")
                .setMessage(summary)
                .setPositiveButton("Confirm Issue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveConfirmedLoan(item.getId(), studentName,
                                registerNumber, quantity, confirmedDueDate);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveConfirmedLoan(int equipmentId, String studentName,
                                   String registerNumber, int quantity,
                                   String confirmedDueDate) {
        if (loanSaved) {
            return;
        }

        long result = dbHelper.issueEquipment(equipmentId, studentName,
                registerNumber, quantity, confirmedDueDate);

        if (result > 0) {
            loanSaved = true;
            btnIssueLoan.setEnabled(false);
            new AlertDialog.Builder(this)
                    .setTitle("Equipment Issued Successfully")
                    .setMessage("Loan ID: " + result +
                            "\nThe equipment has been issued to " +
                            studentName + ".\n\nThe loan is saved in SQLite.")
                    .setCancelable(false)
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        } else if (result == -1) {
            loadEquipment();
            new AlertDialog.Builder(this)
                    .setTitle("Insufficient Stock")
                    .setMessage("The selected equipment is missing or no longer has enough " +
                            "units available. Please check its stock and try again.")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (result == -2) {
            new AlertDialog.Builder(this)
                    .setTitle("Check Loan Details")
                    .setMessage("The information or due date is invalid. " +
                            "Please check your entries and try again.")
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Save Failed")
                    .setMessage("The loan could not be saved. Please try again.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null && !loanSaved) {
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
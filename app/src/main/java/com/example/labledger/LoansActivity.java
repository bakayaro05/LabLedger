package com.example.labledger;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class LoansActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Spinner spinnerStatus;
    private EditText etSearch;
    private TextView tvLoanCount;
    private TextView tvEmpty;
    private LoanAdapter adapter;

    private final ArrayList<Loan> allLoans = new ArrayList<>();
    private final ArrayList<Loan> visibleLoans = new ArrayList<>();
    private final String[] filters = {"Active", "Overdue", "Returned", "All"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loans);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DatabaseHelper(this);
        spinnerStatus = findViewById(R.id.spinnerLoanStatus);
        etSearch = findViewById(R.id.etSearchLoans);
        Button btnClearSearch = findViewById(R.id.btnClearLoansSearch);
        tvLoanCount = findViewById(R.id.tvLoanCount);
        tvEmpty = findViewById(R.id.tvLoansEmpty);
        ListView listLoans = findViewById(R.id.listLoans);
        Button btnBack = findViewById(R.id.btnBackLoans);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(filterAdapter);

        adapter = new LoanAdapter(this, visibleLoans);
        listLoans.setAdapter(adapter);
        listLoans.setEmptyView(tvEmpty);

        // Active includes overdue loans, because they are not returned yet.
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                filterLoans();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterLoans();
            }
        });

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                filterLoans();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        listLoans.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Loan selected = adapter.getItem(position);
                if (selected != null) {
                    showLoanDetails(selected);
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadLoans() {
        allLoans.clear();
        allLoans.addAll(dbHelper.getAllLoans());
        filterLoans();
    }

    private void filterLoans() {
        if (adapter == null) {
            return;
        }

        int selectedFilter = spinnerStatus.getSelectedItemPosition();
        String search = etSearch.getText().toString().trim()
                .toLowerCase(Locale.ROOT);
        visibleLoans.clear();

        for (Loan loan : allLoans) {
            boolean statusMatches;
            switch (selectedFilter) {
                case 0: // Active: all outstanding, including overdue
                    statusMatches = !loan.isReturned();
                    break;
                case 1: // Overdue only
                    statusMatches = loan.isOverdue();
                    break;
                case 2: // Returned only
                    statusMatches = loan.isReturned();
                    break;
                default: // All
                    statusMatches = true;
                    break;
            }

            String searchable = (loan.getEquipmentName() + " " +
                    loan.getStudentName() + " " + loan.getRegisterNumber() +
                    " " + loan.getLoanId()).toLowerCase(Locale.ROOT);

            if (statusMatches && searchable.contains(search)) {
                visibleLoans.add(loan);
            }
        }

        adapter.notifyDataSetChanged();
        tvLoanCount.setText("Showing " + visibleLoans.size() +
                " of " + allLoans.size() + " loan records");

        if (allLoans.isEmpty()) {
            tvEmpty.setText("No loans yet. Use Issue Equipment to create one.");
        } else {
            tvEmpty.setText("No matching loans. Try another status or search term.");
        }
    }

    private void showLoanDetails(final Loan loan) {
        String details = "Equipment: " + loan.getEquipmentName() +
                "\nLoan ID: " + loan.getLoanId() +
                "\nStudent: " + loan.getStudentName() +
                "\nRegister Number: " + loan.getRegisterNumber() +
                "\nQuantity: " + loan.getQuantity() +
                "\nIssue Date: " + loan.getIssueDate() +
                "\nDue Date: " + loan.getDueDate() +
                "\nStatus: " + loan.getStatusLabel();

        if (loan.isReturned()) {
            details += "\nReturn Date: " + loan.getReturnDate();
            new AlertDialog.Builder(this)
                    .setTitle("Loan Details")
                    .setMessage(details)
                    .setPositiveButton("Close", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Loan Details")
                    .setMessage(details)
                    .setPositiveButton("Mark Returned", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            confirmReturn(loan);
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
        }
    }

    private void confirmReturn(final Loan loan) {
        String message = "Return " + loan.getQuantity() + " unit(s) of " +
                loan.getEquipmentName() + " borrowed by " +
                loan.getStudentName() + "?\n\n" +
                "All units in this loan will be returned together.";

        new AlertDialog.Builder(this)
                .setTitle("Confirm Equipment Return")
                .setMessage(message)
                .setPositiveButton("Confirm Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        completeReturn(loan.getLoanId());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeReturn(int loanId) {
        int result = dbHelper.markLoanReturned(loanId);
        loadLoans();

        if (result == 1) {
            new AlertDialog.Builder(this)
                    .setTitle("Equipment Returned")
                    .setMessage("Return recorded successfully. Available stock " +
                            "has been restored, and the transaction is saved " +
                            "in Returned history.")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (result == 0) {
            Toast.makeText(this, "This loan was already returned or was not found.",
                    Toast.LENGTH_LONG).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Return Failed")
                    .setMessage("The return could not be saved. " +
                            "Please try again.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadLoans();
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
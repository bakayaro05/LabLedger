package com.example.labledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnEquipment;
    private Button btnIssue;
    private Button btnLoans;
    private Button btnHelpAbout;

    private TextView tvTotalEquipment;
    private TextView tvAvailable;
    private TextView tvBorrowed;
    private TextView tvOverdue;
    private TextView tvDashboardHint;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new DatabaseHelper(this);

        tvTotalEquipment = findViewById(R.id.tvTotalEquipment);
        tvAvailable = findViewById(R.id.tvAvailable);
        tvBorrowed = findViewById(R.id.tvBorrowed);
        tvOverdue = findViewById(R.id.tvOverdue);
        tvDashboardHint = findViewById(R.id.tvDashboardHint);

        btnEquipment = findViewById(R.id.btnEquipment);
        btnIssue = findViewById(R.id.btnIssue);
        btnLoans = findViewById(R.id.btnLoans);
        btnHelpAbout = findViewById(R.id.btnHelpAbout);

        updateDashboard();

        // Phase 3: Equipment inventory
        btnEquipment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,
                        EquipmentListActivity.class));
            }
        });

        // Phase 4: Issue equipment to a student
        btnIssue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,
                        IssueLoanActivity.class));
            }
        });

        // Phase 5: Open active loans, overdue records and return history
        btnLoans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,
                        LoansActivity.class));
            }
        });

        // Phase 6: Visible in-app help; no separate Activity required.
        btnHelpAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelpAndAbout();
            }
        });
    }

    private void updateDashboard() {
        int types = dbHelper.getEquipmentTypeCount();
        int available = dbHelper.getAvailableUnits();
        int borrowed = dbHelper.getBorrowedUnits();
        int overdue = dbHelper.getOverdueLoanCount();

        tvTotalEquipment.setText(String.valueOf(types));
        tvAvailable.setText(String.valueOf(available));
        tvBorrowed.setText(String.valueOf(borrowed));
        tvOverdue.setText(String.valueOf(overdue));

        // Clear, actionable feedback instead of numbers and colours alone.
        if (types == 0) {
            tvDashboardHint.setText(
                    "No equipment registered yet. Tap Manage Equipment to add your first item.");
        } else if (overdue > 0) {
            tvDashboardHint.setText(overdue + " overdue loan(s) need attention. " +
                    "Open Loans and History to review them.");
        } else if (borrowed > 0) {
            tvDashboardHint.setText(borrowed + " unit(s) currently borrowed. " +
                    "There are no overdue loans.");
        } else {
            tvDashboardHint.setText("All recorded equipment is available. " +
                    "Choose Issue Equipment to lend an item.");
        }
    }

    private void showHelpAndAbout() {
        String guide = "1. Manage Equipment: add, search, edit or delete equipment.\n\n" +
                "2. Issue Equipment: choose an item, enter the student's details, " +
                "quantity and due date, then confirm.\n\n" +
                "3. Loans and History: view active or overdue loans, record returns " +
                "and review completed loans.\n\n" +
                "Stock updates automatically when items are issued or returned. " +
                "Active includes overdue loans.\n\n" +
                "LabLedger v1.1 — Offline laboratory equipment manager.";

        new AlertDialog.Builder(this)
                .setTitle("LabLedger — Help & About")
                .setMessage(guide)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null) {
            updateDashboard();
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
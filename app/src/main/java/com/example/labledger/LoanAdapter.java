package com.example.labledger;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class LoanAdapter extends ArrayAdapter<Loan> {

    public LoanAdapter(Context context, List<Loan> loans) {
        super(context, 0, loans);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_loan, parent, false);
        }

        Loan loan = getItem(position);
        TextView tvEquipment = row.findViewById(R.id.tvLoanRowEquipment);
        TextView tvStatus = row.findViewById(R.id.tvLoanRowStatus);
        TextView tvStudent = row.findViewById(R.id.tvLoanRowStudent);
        TextView tvDates = row.findViewById(R.id.tvLoanRowDates);
        TextView tvQuantity = row.findViewById(R.id.tvLoanRowQuantity);

        if (loan != null) {
            tvEquipment.setText(loan.getEquipmentName());
            tvStatus.setText(loan.getStatusLabel());
            tvStudent.setText(loan.getStudentName() + " (" +
                    loan.getRegisterNumber() + ")");
            tvQuantity.setText("Quantity: " + loan.getQuantity() +
                    "   |   Loan #" + loan.getLoanId());

            if (loan.isReturned()) {
                tvDates.setText("Returned: " + loan.getReturnDate() +
                        "   |   Due: " + loan.getDueDate());
                tvStatus.setTextColor(Color.parseColor("#15803D"));
            } else {
                tvDates.setText("Issued: " + loan.getIssueDate() +
                        "   |   Due: " + loan.getDueDate());
                tvStatus.setTextColor(Color.parseColor(
                        loan.isOverdue() ? "#DC2626" : "#B45309"));
            }
        }

        return row;
    }
}
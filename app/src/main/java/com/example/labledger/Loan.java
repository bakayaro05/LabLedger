package com.example.labledger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Represents one borrowing transaction in the loans table.
public class Loan {

    private final int loanId;
    private final int equipmentId;
    private final String equipmentName;
    private final String studentName;
    private final String registerNumber;
    private final int quantity;
    private final String issueDate;
    private final String dueDate;
    private final String returnDate;

    public Loan(int loanId, int equipmentId, String equipmentName,
                String studentName, String registerNumber, int quantity,
                String issueDate, String dueDate, String returnDate) {
        this.loanId = loanId;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.quantity = quantity;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public int getLoanId() { return loanId; }
    public int getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getStudentName() { return studentName; }
    public String getRegisterNumber() { return registerNumber; }
    public int getQuantity() { return quantity; }
    public String getIssueDate() { return issueDate; }
    public String getDueDate() { return dueDate; }
    public String getReturnDate() { return returnDate; }

    public boolean isReturned() {
        return returnDate != null && !returnDate.isEmpty();
    }

    public boolean isOverdue() {
        if (isReturned()) {
            return false;
        }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
        return dueDate.compareTo(today) < 0;
    }

    public String getStatusLabel() {
        if (isReturned()) {
            return "RETURNED";
        }
        return isOverdue() ? "OVERDUE" : "BORROWED";
    }
}
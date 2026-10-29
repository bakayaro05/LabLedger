package com.example.labledger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "LabLedgerDB.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createEquipment = "CREATE TABLE equipment (" +
                "equipment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "total_quantity INTEGER NOT NULL " +
                "CHECK(total_quantity > 0))";
        db.execSQL(createEquipment);

        String createLoans = "CREATE TABLE loans (" +
                "loan_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "equipment_id INTEGER NOT NULL, " +
                "student_name TEXT NOT NULL, " +
                "register_number TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL CHECK(quantity > 0), " +
                "issue_date TEXT NOT NULL, " +
                "due_date TEXT NOT NULL, " +
                "return_date TEXT, " +
                "FOREIGN KEY(equipment_id) " +
                "REFERENCES equipment(equipment_id) " +
                "ON DELETE RESTRICT, " +
                "CHECK(due_date >= issue_date), " +
                "CHECK(return_date IS NULL OR " +
                "return_date >= issue_date))";
        db.execSQL(createLoans);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep version 1 during this project. Never erase existing user data.
        throw new SQLiteException("Database upgrade not implemented");
    }

    private int queryNumber(SQLiteDatabase db, String sql, String[] args) {
        Cursor cursor = db.rawQuery(sql, args);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private int getNumber(String sql, String[] args) {
        return queryNumber(getReadableDatabase(), sql, args);
    }

    // Dashboard methods from Phase 2
    public int getEquipmentTypeCount() {
        return getNumber("SELECT COUNT(*) FROM equipment", null);
    }

    public int getTotalUnits() {
        return getNumber("SELECT COALESCE(SUM(total_quantity), 0) FROM equipment", null);
    }

    public int getBorrowedUnits() {
        return getNumber("SELECT COALESCE(SUM(quantity), 0) " +
                "FROM loans WHERE return_date IS NULL", null);
    }

    public int getAvailableUnits() {
        return getTotalUnits() - getBorrowedUnits();
    }

    public int getOverdueLoanCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
        return getNumber("SELECT COUNT(*) FROM loans " +
                        "WHERE return_date IS NULL AND due_date < ?",
                new String[]{today});
    }

    // Phase 3: Add equipment
    public long addEquipment(String name, String category,
                             String location, int totalQuantity) {
        if (totalQuantity <= 0) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("category", category);
        values.put("location", location.trim());
        values.put("total_quantity", totalQuantity);
        return getWritableDatabase().insert("equipment", null, values);
    }

    // Phase 3: Fetch equipment with calculated available quantity
    public ArrayList<Equipment> getAllEquipment() {
        ArrayList<Equipment> equipmentList = new ArrayList<>();
        String sql = "SELECT e.equipment_id, e.name, e.category, " +
                "e.location, e.total_quantity, " +
                "e.total_quantity - COALESCE((SELECT SUM(l.quantity) " +
                "FROM loans l WHERE l.equipment_id = e.equipment_id " +
                "AND l.return_date IS NULL), 0) AS available_quantity " +
                "FROM equipment e ORDER BY e.name COLLATE NOCASE, e.equipment_id";

        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                equipmentList.add(readEquipment(cursor));
            }
        } finally {
            cursor.close();
        }
        return equipmentList;
    }

    // Phase 3: Fetch one item for editing
    public Equipment getEquipmentById(int id) {
        String sql = "SELECT e.equipment_id, e.name, e.category, " +
                "e.location, e.total_quantity, " +
                "e.total_quantity - COALESCE((SELECT SUM(l.quantity) " +
                "FROM loans l WHERE l.equipment_id = e.equipment_id " +
                "AND l.return_date IS NULL), 0) AS available_quantity " +
                "FROM equipment e WHERE e.equipment_id = ?";

        Cursor cursor = getReadableDatabase().rawQuery(
                sql, new String[]{String.valueOf(id)});
        try {
            return cursor.moveToFirst() ? readEquipment(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    private Equipment readEquipment(Cursor cursor) {
        return new Equipment(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getInt(4),
                cursor.getInt(5)
        );
    }

    // Returns 1 if updated, 0 if missing, -1 if below borrowed quantity
    public int updateEquipment(int id, String name, String category,
                               String location, int totalQuantity) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int borrowed = queryNumber(db,
                    "SELECT COALESCE(SUM(quantity), 0) FROM loans " +
                            "WHERE equipment_id = ? AND return_date IS NULL",
                    new String[]{String.valueOf(id)});

            if (totalQuantity <= 0 || totalQuantity < borrowed) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put("name", name.trim());
            values.put("category", category);
            values.put("location", location.trim());
            values.put("total_quantity", totalQuantity);

            int updated = db.update("equipment", values,
                    "equipment_id = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return updated;
        } finally {
            db.endTransaction();
        }
    }

    // Do not delete equipment with ANY previous or current loan records
    public boolean deleteEquipment(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int loanCount = queryNumber(db,
                    "SELECT COUNT(*) FROM loans WHERE equipment_id = ?",
                    new String[]{String.valueOf(id)});
            if (loanCount > 0) {
                return false;
            }

            int deleted = db.delete("equipment", "equipment_id = ?",
                    new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return deleted > 0;
        } finally {
            db.endTransaction();
        }
    }

    // Phase 4: Safely issue equipment within one database transaction.
    // Positive return value: saved loan ID.
    // -1: equipment not found or insufficient stock.
    // -2: invalid student, quantity or due date.
    // -3: database insert failed.
    public long issueEquipment(int equipmentId, String studentName,
                               String registerNumber, int quantity,
                               String dueDate) {
        String issueDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());

        if (equipmentId <= 0 || studentName == null ||
                studentName.trim().isEmpty() || registerNumber == null ||
                registerNumber.trim().isEmpty() || quantity <= 0 ||
                dueDate == null || !dueDate.matches("\\d{4}-\\d{2}-\\d{2}") ||
                dueDate.compareTo(issueDate) < 0) {
            return -2;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String stockSql = "SELECT e.total_quantity - " +
                    "COALESCE((SELECT SUM(l.quantity) FROM loans l " +
                    "WHERE l.equipment_id = e.equipment_id " +
                    "AND l.return_date IS NULL), 0) " +
                    "FROM equipment e WHERE e.equipment_id = ?";
            Cursor stockCursor = db.rawQuery(stockSql,
                    new String[]{String.valueOf(equipmentId)});
            int available;
            try {
                if (!stockCursor.moveToFirst()) {
                    return -1;
                }
                available = stockCursor.getInt(0);
            } finally {
                stockCursor.close();
            }

            if (quantity > available) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put("equipment_id", equipmentId);
            values.put("student_name", studentName.trim());
            values.put("register_number", registerNumber.trim());
            values.put("quantity", quantity);
            values.put("issue_date", issueDate);
            values.put("due_date", dueDate);
            values.putNull("return_date");

            long loanId = db.insertOrThrow("loans", null, values);
            db.setTransactionSuccessful();
            return loanId;
        } catch (SQLiteException e) {
            return -3;
        } finally {
            db.endTransaction();
        }
    }


    // Phase 5: Read all loan records together with current equipment names.
    public ArrayList<Loan> getAllLoans() {
        ArrayList<Loan> loanList = new ArrayList<>();
        String sql = "SELECT l.loan_id, l.equipment_id, e.name, " +
                "l.student_name, l.register_number, l.quantity, " +
                "l.issue_date, l.due_date, l.return_date " +
                "FROM loans l INNER JOIN equipment e " +
                "ON e.equipment_id = l.equipment_id " +
                "ORDER BY CASE WHEN l.return_date IS NULL THEN 0 ELSE 1 END, " +
                "CASE WHEN l.return_date IS NULL THEN l.due_date END ASC, " +
                "CASE WHEN l.return_date IS NOT NULL THEN l.return_date END DESC, " +
                "l.loan_id DESC";

        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                loanList.add(new Loan(
                        cursor.getInt(0),      // loan ID
                        cursor.getInt(1),      // equipment ID
                        cursor.getString(2),   // equipment name
                        cursor.getString(3),   // student name
                        cursor.getString(4),   // register number
                        cursor.getInt(5),      // quantity
                        cursor.getString(6),   // issue date
                        cursor.getString(7),   // due date
                        cursor.isNull(8) ? null : cursor.getString(8)
                ));
            }
        } finally {
            cursor.close();
        }
        return loanList;
    }

    // Phase 5: Mark a single outstanding loan as returned.
    // Return 1 = successfully returned, 0 = already returned or not found,
    // -1 = database error. Available stock automatically increases because
    // stock calculations count only loans whose return_date is NULL.
    public int markLoanReturned(int loanId) {
        if (loanId <= 0) {
            return 0;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("return_date", today);

            // A conditional UPDATE prevents accidentally returning twice.
            // The issued-on-or-before check satisfies the Phase 2 table rule.
            return db.update("loans", values,
                    "loan_id = ? AND return_date IS NULL AND issue_date <= ?",
                    new String[]{String.valueOf(loanId), today});
        } catch (SQLiteException e) {
            return -1;
        }
    }

}
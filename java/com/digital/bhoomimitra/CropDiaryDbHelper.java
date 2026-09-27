package com.digital.bhoomimitra;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CropDiaryDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "crop_diary.db";
    private static final int DB_VERSION = 1;

    // Diary table
    private static final String TABLE_DIARY = "diary_entries";
    private static final String COL_D_ID = "id";
    private static final String COL_D_DATE = "date";
    private static final String COL_D_CROP = "crop";
    private static final String COL_D_ACTIVITY = "activity";
    private static final String COL_D_NOTES = "notes";

    // Expense table
    private static final String TABLE_EXPENSE = "expenses";
    private static final String COL_E_ID = "id";
    private static final String COL_E_DATE = "date";
    private static final String COL_E_CATEGORY = "category";
    private static final String COL_E_NOTE = "note";
    private static final String COL_E_AMOUNT = "amount";

    public CropDiaryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createDiary = "CREATE TABLE " + TABLE_DIARY + " (" +
                COL_D_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_D_DATE + " TEXT, " +
                COL_D_CROP + " TEXT, " +
                COL_D_ACTIVITY + " TEXT, " +
                COL_D_NOTES + " TEXT" +
                ");";

        String createExpense = "CREATE TABLE " + TABLE_EXPENSE + " (" +
                COL_E_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_E_DATE + " TEXT, " +
                COL_E_CATEGORY + " TEXT, " +
                COL_E_NOTE + " TEXT, " +
                COL_E_AMOUNT + " REAL" +
                ");";

        db.execSQL(createDiary);
        db.execSQL(createExpense);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DIARY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSE);
        onCreate(db);
    }

    // -------- DIARY METHODS --------

    public long insertDiaryEntry(String date, String crop, String activity, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_D_DATE, date);
        values.put(COL_D_CROP, crop);
        values.put(COL_D_ACTIVITY, activity);
        values.put(COL_D_NOTES, notes);
        return db.insert(TABLE_DIARY, null, values);
    }

    public List<DiaryEntry> getAllDiaryEntries() {
        List<DiaryEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(TABLE_DIARY,
                null,
                null,
                null,
                null,
                null,
                COL_D_DATE + " DESC, " + COL_D_ID + " DESC");

        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(COL_D_ID));
                String date = c.getString(c.getColumnIndexOrThrow(COL_D_DATE));
                String crop = c.getString(c.getColumnIndexOrThrow(COL_D_CROP));
                String activity = c.getString(c.getColumnIndexOrThrow(COL_D_ACTIVITY));
                String notes = c.getString(c.getColumnIndexOrThrow(COL_D_NOTES));
                list.add(new DiaryEntry(id, date, crop, activity, notes));
            }
            c.close();
        }
        return list;
    }

    public void deleteDiaryEntry(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DIARY, COL_D_ID + "=?", new String[]{String.valueOf(id)});
    }

    // -------- EXPENSE METHODS --------

    public long insertExpense(String date, String category, String note, double amount) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_E_DATE, date);
        values.put(COL_E_CATEGORY, category);
        values.put(COL_E_NOTE, note);
        values.put(COL_E_AMOUNT, amount);
        return db.insert(TABLE_EXPENSE, null, values);
    }

    public List<ExpenseEntry> getAllExpenses() {
        List<ExpenseEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(TABLE_EXPENSE,
                null,
                null,
                null,
                null,
                null,
                COL_E_DATE + " DESC, " + COL_E_ID + " DESC");

        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(COL_E_ID));
                String date = c.getString(c.getColumnIndexOrThrow(COL_E_DATE));
                String category = c.getString(c.getColumnIndexOrThrow(COL_E_CATEGORY));
                String note = c.getString(c.getColumnIndexOrThrow(COL_E_NOTE));
                double amount = c.getDouble(c.getColumnIndexOrThrow(COL_E_AMOUNT));
                list.add(new ExpenseEntry(id, date, category, note, amount));
            }
            c.close();
        }
        return list;
    }

    public double getTotalExpense() {
        double total = 0.0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COL_E_AMOUNT + ") FROM " + TABLE_EXPENSE, null);
        if (c != null) {
            if (c.moveToFirst()) {
                total = c.getDouble(0);
            }
            c.close();
        }
        return total;
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSE, COL_E_ID + "=?", new String[]{String.valueOf(id)});
    }
}

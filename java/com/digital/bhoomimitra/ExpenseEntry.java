package com.digital.bhoomimitra;

public class ExpenseEntry {
    public int id;
    public String date;
    public String category;
    public String note;
    public double amount;

    public ExpenseEntry(int id, String date, String category, String note, double amount) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.note = note;
        this.amount = amount;
    }
}

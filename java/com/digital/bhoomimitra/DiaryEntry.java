package com.digital.bhoomimitra;

public class DiaryEntry {
    public int id;
    public String date;
    public String crop;
    public String activity;
    public String notes;

    public DiaryEntry(int id, String date, String crop, String activity, String notes) {
        this.id = id;
        this.date = date;
        this.crop = crop;
        this.activity = activity;
        this.notes = notes;
    }
}

package com.digital.bhoomimitra;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class AddDiaryFragment extends Fragment {

    private EditText etDiaryDate, etDiaryCrop, etDiaryActivity, etDiaryNotes;
    private Button btnCancel, btnSave;
    private CropDiaryDbHelper dbHelper;

    public AddDiaryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_diary, container, false);

        dbHelper = new CropDiaryDbHelper(requireContext());

        etDiaryDate     = view.findViewById(R.id.etDiaryDate);
        etDiaryCrop     = view.findViewById(R.id.etDiaryCrop);
        etDiaryActivity = view.findViewById(R.id.etDiaryActivity);
        etDiaryNotes    = view.findViewById(R.id.etDiaryNotes);
        btnCancel       = view.findViewById(R.id.btnCancelDiary);
        btnSave         = view.findViewById(R.id.btnSaveDiary);

        etDiaryDate.setOnClickListener(v -> showDatePicker());

        btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        btnSave.setOnClickListener(v -> saveDiaryEntry());

        return view;
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
            etDiaryDate.setText(dateStr);
        }, y, m, d).show();
    }

    private void saveDiaryEntry() {
        String date = etDiaryDate.getText().toString().trim();
        String crop = etDiaryCrop.getText().toString().trim();
        String activity = etDiaryActivity.getText().toString().trim();
        String notes = etDiaryNotes.getText().toString().trim();

        if (TextUtils.isEmpty(date)) {
            etDiaryDate.setError("Select date");
            etDiaryDate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(crop)) {
            etDiaryCrop.setError("Enter crop");
            etDiaryCrop.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(activity)) {
            etDiaryActivity.setError("Enter activity");
            etDiaryActivity.requestFocus();
            return;
        }

        long id = dbHelper.insertDiaryEntry(date, crop, activity, notes);
        if (id != -1) {
            Toast.makeText(requireContext(), "Diary entry saved", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), "Failed to save diary entry", Toast.LENGTH_SHORT).show();
        }
    }
}

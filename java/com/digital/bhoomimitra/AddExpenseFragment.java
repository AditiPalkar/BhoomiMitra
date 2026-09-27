package com.digital.bhoomimitra;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class AddExpenseFragment extends Fragment {

    private EditText etExpenseDate, etExpenseCrop, etExpenseAmount, etExpenseNote;
    private Spinner spinnerCategory;
    private Button btnCancel, btnSave;
    private CropDiaryDbHelper dbHelper;

    public AddExpenseFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        dbHelper = new CropDiaryDbHelper(requireContext());

        etExpenseDate   = view.findViewById(R.id.etExpenseDate);
        etExpenseCrop   = view.findViewById(R.id.etExpenseCrop);
        etExpenseAmount = view.findViewById(R.id.etExpenseAmount);
        etExpenseNote   = view.findViewById(R.id.etExpenseNote);
        spinnerCategory = view.findViewById(R.id.spinnerExpenseCategory);
        btnCancel       = view.findViewById(R.id.btnCancelExpense);
        btnSave         = view.findViewById(R.id.btnSaveExpense);

        etExpenseDate.setOnClickListener(v -> showDatePicker());
        setupCategorySpinner();

        btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        btnSave.setOnClickListener(v -> saveExpense());

        return view;
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
            etExpenseDate.setText(dateStr);
        }, y, m, d).show();
    }

    private void setupCategorySpinner() {
        String[] categories = new String[]{
                "Seeds", "Fertilizer", "Pesticides", "Labor", "Equipment", "Transport", "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void saveExpense() {
        String date = etExpenseDate.getText().toString().trim();
        String crop = etExpenseCrop.getText().toString().trim(); // optional
        String amountStr = etExpenseAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "Other";
        String note = etExpenseNote.getText().toString().trim();

        if (TextUtils.isEmpty(date)) {
            etExpenseDate.setError("Select date");
            etExpenseDate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(amountStr)) {
            etExpenseAmount.setError("Enter amount");
            etExpenseAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etExpenseAmount.setError("Invalid amount");
            etExpenseAmount.requestFocus();
            return;
        }

        // If you want to include crop in note:
        if (!TextUtils.isEmpty(crop)) {
            if (!TextUtils.isEmpty(note)) {
                note = crop + " – " + note;
            } else {
                note = crop;
            }
        }

        long id = dbHelper.insertExpense(date, category, note, amount);
        if (id != -1) {
            Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show();
        }
    }
}

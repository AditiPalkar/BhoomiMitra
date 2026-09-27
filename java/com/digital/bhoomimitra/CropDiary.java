package com.digital.bhoomimitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CropDiary extends Fragment {

    private View layoutDiarySection, layoutExpensesSection;
    private TextView tabDiary, tabExpenses, tvDiarySummary, tvExpenseSummary;
    private RecyclerView rvDiary, rvExpenses;
    private FloatingActionButton fabAdd;

    private CropDiaryDbHelper dbHelper;
    private DiaryAdapter diaryAdapter;
    private ExpenseAdapter expenseAdapter;

    public CropDiary() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_crop_diary, container, false);

        dbHelper = new CropDiaryDbHelper(requireContext());

        // Tabs & sections
        tabDiary          = view.findViewById(R.id.tabDiary);
        tabExpenses       = view.findViewById(R.id.tabExpenses);
        layoutDiarySection    = view.findViewById(R.id.layoutDiarySection);
        layoutExpensesSection = view.findViewById(R.id.layoutExpensesSection);

        // Lists & summaries
        rvDiary          = view.findViewById(R.id.rvDiaryEntries);
        rvExpenses       = view.findViewById(R.id.rvExpenseEntries);
        tvDiarySummary   = view.findViewById(R.id.tvDiarySummary);
        tvExpenseSummary = view.findViewById(R.id.tvExpenseSummary);

        // FAB
        fabAdd = view.findViewById(R.id.fabAdd);

        setupTabs();
        setupRecyclerViews();
        loadData();

        fabAdd.setOnClickListener(v -> {
            if (layoutDiarySection.getVisibility() == View.VISIBLE) {
                // Add Diary
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AddDiaryFragment())
                        .addToBackStack("add_diary")
                        .commit();
            } else {
                // Add Expense
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AddExpenseFragment())
                        .addToBackStack("add_expense")
                        .commit();
            }
        });

        return view;
    }

    private void setupTabs() {
        showDiaryTab();

        tabDiary.setOnClickListener(v -> showDiaryTab());
        tabExpenses.setOnClickListener(v -> showExpensesTab());
    }

    private void showDiaryTab() {
        layoutDiarySection.setVisibility(View.VISIBLE);
        layoutExpensesSection.setVisibility(View.GONE);

        tabDiary.setBackgroundColor(0xFFFFFFFF);
        tabDiary.setTextColor(0xFF2E7D32);

        tabExpenses.setBackgroundColor(0x00000000);
        tabExpenses.setTextColor(0xFF555555);
    }

    private void showExpensesTab() {
        layoutDiarySection.setVisibility(View.GONE);
        layoutExpensesSection.setVisibility(View.VISIBLE);

        tabExpenses.setBackgroundColor(0xFFFFFFFF);
        tabExpenses.setTextColor(0xFF2E7D32);

        tabDiary.setBackgroundColor(0x00000000);
        tabDiary.setTextColor(0xFF555555);
    }

    private void setupRecyclerViews() {
        rvDiary.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));

        diaryAdapter = new DiaryAdapter(dbHelper.getAllDiaryEntries(), entry -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete entry")
                    .setMessage("Delete this diary entry?")
                    .setPositiveButton("Yes", (d, w) -> {
                        dbHelper.deleteDiaryEntry(entry.id);
                        loadDiary();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
        rvDiary.setAdapter(diaryAdapter);

        expenseAdapter = new ExpenseAdapter(dbHelper.getAllExpenses(), entry -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete expense")
                    .setMessage("Delete this expense record?")
                    .setPositiveButton("Yes", (d, w) -> {
                        dbHelper.deleteExpense(entry.id);
                        loadExpenses();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
        rvExpenses.setAdapter(expenseAdapter);
    }

    private void loadData() {
        loadDiary();
        loadExpenses();
    }

    private void loadDiary() {
        List<DiaryEntry> diaryList = dbHelper.getAllDiaryEntries();
        diaryAdapter.updateData(diaryList);
        tvDiarySummary.setText("Total Entries: " + diaryList.size());
    }

    private void loadExpenses() {
        List<ExpenseEntry> expenseList = dbHelper.getAllExpenses();
        expenseAdapter.updateData(expenseList);

        double total = dbHelper.getTotalExpense();
        tvExpenseSummary.setText("Total Expense: ₹" + String.format("%.2f", total));
    }

    @Override
    public void onResume() {
        super.onResume();
        // When coming back from AddDiary/AddExpense, refresh lists
        loadData();
    }
}

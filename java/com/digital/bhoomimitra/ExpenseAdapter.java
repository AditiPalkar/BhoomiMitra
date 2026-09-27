// ExpenseAdapter.java
package com.digital.bhoomimitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnItemLongClickListener {
        void onItemLongClick(ExpenseEntry entry);
    }

    private List<ExpenseEntry> list;
    private OnItemLongClickListener longClickListener;

    public ExpenseAdapter(List<ExpenseEntry> list, OnItemLongClickListener l) {
        this.list = list;
        this.longClickListener = l;
    }

    public void updateData(List<ExpenseEntry> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense_entry, parent, false); // your XML
        return new ExpenseViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseEntry item = list.get(position);

        holder.tvDate.setText(item.date);
        holder.tvCrop.setText(item.category);  // using category in place of "crop – category"
        holder.tvNote.setText(item.note);
        holder.tvAmount.setText("₹" + String.format("%.2f", item.amount));

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvCrop, tvNote, tvAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate   = itemView.findViewById(R.id.tvExpenseDate);
            tvCrop   = itemView.findViewById(R.id.tvExpenseCrop);
            tvNote   = itemView.findViewById(R.id.tvExpenseNote);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
        }
    }
}

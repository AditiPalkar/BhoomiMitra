// DiaryAdapter.java
package com.digital.bhoomimitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    public interface OnItemLongClickListener {
        void onItemLongClick(DiaryEntry entry);
    }

    private List<DiaryEntry> list;
    private OnItemLongClickListener longClickListener;

    public DiaryAdapter(List<DiaryEntry> list, OnItemLongClickListener l) {
        this.list = list;
        this.longClickListener = l;
    }

    public void updateData(List<DiaryEntry> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diary_entry, parent, false); // <== your XML name
        return new DiaryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        DiaryEntry item = list.get(position);

        holder.tvDate.setText(item.date);
        holder.tvCrop.setText(item.crop);
        holder.tvActivity.setText(item.activity);

        if (item.notes != null && !item.notes.trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(item.notes);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

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

    static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvCrop, tvActivity, tvNotes;

        DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate     = itemView.findViewById(R.id.tvDiaryDate);
            tvCrop     = itemView.findViewById(R.id.tvDiaryCrop);
            tvActivity = itemView.findViewById(R.id.tvDiaryActivity);
            tvNotes    = itemView.findViewById(R.id.tvDiaryNotes);
        }
    }
}

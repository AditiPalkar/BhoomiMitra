package com.digital.bhoomimitra;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder> {

    public interface OnMessageLongClickListener {
        void onLongClick(ChatMessage msg);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserName;
    private final OnMessageLongClickListener longClickListener;

    public ChatMessageAdapter(String currentUserName, OnMessageLongClickListener listener) {
        this.currentUserName = currentUserName == null ? "" : currentUserName;
        this.longClickListener = listener;
    }

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeMessageById(String id) {
        if (id == null) return;
        for (int i = 0; i < messages.size(); i++) {
            if (id.equals(messages.get(i).id)) {
                messages.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public ChatMessage getItem(int position) {
        return messages.get(position);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage m = messages.get(position);

        holder.tvSender.setText(m.sender);
        holder.tvMessage.setText(m.content);

        // --- REPLY UI ---
        if (m.replyToSender != null && !m.replyToSender.isEmpty()) {
            holder.layoutReplyPreview.setVisibility(View.VISIBLE);
            holder.tvReplySender.setText(m.replyToSender);
            holder.tvReplyContent.setText(m.replyToContent);
        } else {
            holder.layoutReplyPreview.setVisibility(View.GONE);
        }

        // --- TIME ---
        String timeStr = "";
        if (m.timestamp > 0) {
            Date d = new Date(m.timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            timeStr = sdf.format(d);
        }
        holder.tvTime.setText(timeStr);

        // --- ALIGNMENT FIX ---
        // Using trim() ensures "Farmer " equals "Farmer"
        boolean isMe = m.sender != null && m.sender.trim().equalsIgnoreCase(currentUserName.trim());

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.cardBubble.getLayoutParams();

        if (isMe) {
            params.gravity = Gravity.END;
            holder.cardBubble.setCardBackgroundColor(0xFFDCF8C6); // Light Green
            // Left margin 80dp, Right margin 4dp
            params.setMargins(180, 4, 4, 4);
        } else {
            params.gravity = Gravity.START;
            holder.cardBubble.setCardBackgroundColor(0xFFFFFFFF); // White
            // Left margin 4dp, Right margin 80dp
            params.setMargins(4, 4, 180, 4);
        }
        holder.cardBubble.setLayoutParams(params);

        holder.cardBubble.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(m);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTime;
        CardView cardBubble;
        LinearLayout layoutReplyPreview;
        TextView tvReplySender, tvReplyContent;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBubble = itemView.findViewById(R.id.cardBubble);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutReplyPreview = itemView.findViewById(R.id.layoutReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyContent = itemView.findViewById(R.id.tvReplyContent);
        }
    }
}
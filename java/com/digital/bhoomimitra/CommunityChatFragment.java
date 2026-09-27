package com.digital.bhoomimitra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CommunityChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;

    // Reply UI Variables
    private LinearLayout layoutReplyContext;
    private TextView tvReplySenderName, tvReplyBody;
    private ImageButton btnCloseReply;
    private ChatMessage replyingToMessage = null; // Stores message being replied to

    private ChatMessageAdapter adapter;
    private DatabaseReference chatRef;
    private ChildEventListener messagesListener;

    private String currentUserName;
    private String groupName = "global_chat";

    public CommunityChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_community_chat, container, false);

        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);

        // Init Reply Views
        layoutReplyContext = view.findViewById(R.id.layoutReplyContext);
        tvReplySenderName = view.findViewById(R.id.tvReplySenderName);
        tvReplyBody = view.findViewById(R.id.tvReplyBody);
        btnCloseReply = view.findViewById(R.id.btnCloseReply);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("bhoomimitra_user", Context.MODE_PRIVATE);
        currentUserName = prefs.getString("farmer_name", "Farmer");

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);

        adapter = new ChatMessageAdapter(currentUserName, msg -> {
            if (!currentUserName.equalsIgnoreCase(msg.sender)) {
                Toast.makeText(requireContext(), "You can delete only your messages", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete message")
                    .setMessage("Delete this message?")
                    .setPositiveButton("Delete", (d, w) -> {
                        if (msg.id != null) chatRef.child(msg.id).removeValue();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvMessages.setAdapter(adapter);

        setupSwipeReply();


        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(groupName).child("messages");
        attachMessagesListener();

        btnSend.setOnClickListener(v -> sendMessage());

        btnCloseReply.setOnClickListener(v -> closeReplyLayout());


        return view;
    }

    // ... inside CommunityChatFragment ...

    private void setupSwipeReply() {
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatMessage msg = adapter.getItem(position);
                showReplyLayout(msg);
                adapter.notifyItemChanged(position); // Snap back immediately
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.2f;
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                float maxSwipe = 200f;
                float newDX = Math.min(dX, maxSwipe);

                super.onChildDraw(c, recyclerView, viewHolder, newDX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(rvMessages);
    }

    private void showReplyLayout(ChatMessage msg) {
        replyingToMessage = msg;
        layoutReplyContext.setVisibility(View.VISIBLE);
        tvReplySenderName.setText("Replying to " + msg.sender);
        tvReplyBody.setText(msg.content);
        etMessage.requestFocus();
    }

    private void closeReplyLayout() {
        layoutReplyContext.setVisibility(View.GONE);
        replyingToMessage = null;
    }

    private void sendMessage() {
        if (getContext() == null) return;

        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");

        long now = System.currentTimeMillis();
        ChatMessage msg = new ChatMessage(currentUserName, text, groupName, now);

        // Attach Reply info if replying
        if (replyingToMessage != null) {
            msg.replyToId = replyingToMessage.id;
            msg.replyToSender = replyingToMessage.sender;
            msg.replyToContent = replyingToMessage.content;
            closeReplyLayout();
        }

        chatRef.push().setValue(msg)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send", Toast.LENGTH_SHORT).show());
    }

    private void attachMessagesListener() {
        if (messagesListener != null) return;

        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.id = snapshot.getKey();
                    adapter.addMessage(msg);
                    rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                adapter.removeMessageById(snapshot.getKey());
            }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        chatRef.addChildEventListener(messagesListener);
    }

    private void startNotificationService() {
        Intent intent = new Intent(requireContext(), ChatNotificationService.class);
        intent.putExtra("currentUser", currentUserName);
        requireContext().startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // User is looking at the chat -> STOP the notification service
        Intent serviceIntent = new Intent(requireContext(), ChatNotificationService.class);
        serviceIntent.putExtra("stop_service", true);
        requireContext().startService(serviceIntent);
    }

    @Override
    public void onPause() {
        super.onPause();
        // User left the screen (Minimized or Back button) -> START the notification service
        startBackgroundService();
    }

    private void startBackgroundService() {
        if (getContext() == null) return;

        Intent intent = new Intent(requireContext(), ChatNotificationService.class);
        intent.putExtra("currentUser", currentUserName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove local listener
        if (messagesListener != null) {
            chatRef.removeEventListener(messagesListener);
        }
    }
}
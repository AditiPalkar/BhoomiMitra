package com.digital.bhoomimitra;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatNotificationService extends Service {

    private DatabaseReference chatRef;
    private ChildEventListener listener;
    private String currentUser;
    private static final String CHANNEL_ID = "chat_service_channel";
    private static final int NOTIFICATION_ID = 123;
    private static final String TAG = "ChatService";

    // NEW: Variable to track when service started
    private long serviceStartTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Capture the time when the user LEFT the chat screen
        serviceStartTime = System.currentTimeMillis();

        if (intent != null) {
            if (intent.hasExtra("currentUser")) {
                currentUser = intent.getStringExtra("currentUser");
            }
            boolean shouldStop = intent.getBooleanExtra("stop_service", false);
            if (shouldStop) {
                stopForeground(true);
                stopSelf();
                Log.d(TAG, "Service stopped by user request");
                return START_NOT_STICKY;
            }
        }

        if (currentUser == null) {
            SharedPreferences prefs = getSharedPreferences("bhoomimitra_user", MODE_PRIVATE);
            currentUser = prefs.getString("farmer_name", null);
        }

        createNotificationChannel();

        try {
            // This is the "Silent" notification required to keep the service running
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Bhoomi Mitra Chat")
                    .setContentText("Running in background...")
                    .setSmallIcon(R.drawable.app_icon_bhoomi)
                    .setPriority(NotificationCompat.PRIORITY_MIN) // Min priority = No sound/popup
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground: " + e.getMessage());
        }

        if (chatRef == null) {
            chatRef = FirebaseDatabase.getInstance().getReference("chats").child("global_chat").child("messages");
            setupListener();
        }

        return START_STICKY;
    }

    private void setupListener() {
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);

                if (msg == null) return;
                if (currentUser == null) return;

                // Skip my own messages
                if (msg.sender.equalsIgnoreCase(currentUser)) return;

                // CRITICAL FIX: Only notify if the message arrived AFTER service started
                // This prevents notifying for the message you just read
                if (msg.timestamp > serviceStartTime) {
                    sendNewMessageNotification(msg.sender, msg.content);
                } else {
                    Log.d(TAG, "Skipping old message.");
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Listen to NEW items added after connection
        chatRef.limitToLast(1).addChildEventListener(listener);
    }

    private void sendNewMessageNotification(String sender, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String msgChannelId = "new_message_alert";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(msgChannelId, "New Messages", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, Dashboard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, msgChannelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(sender)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Chat Background Service", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), ChatNotificationService.class);
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmService != null) {
            alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        if (chatRef != null && listener != null) chatRef.removeEventListener(listener);
    }
}
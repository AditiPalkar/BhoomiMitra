package com.digital.bhoomimitra;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OfflineVideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int resId = getIntent().getIntExtra("video_res_id", -1);
        if (resId == -1) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            // 1) Copy raw video to cache folder
            File videoFile = copyVideoFromRawToCache(resId);

            // 2) Get content:// URI via FileProvider
            Uri videoUri = FileProvider.getUriForFile(
                    this,
                    "com.digital.bhoomimitra.fileprovider",   // same authority as in Manifest
                    videoFile
            );

            // 3) Open with system video player
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(videoUri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);
            finish(); // close this activity, external player is now open

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No video player app found.", Toast.LENGTH_LONG).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening video.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private File copyVideoFromRawToCache(int resId) throws IOException {
        // Open raw resource as InputStream
        InputStream inputStream = getResources().openRawResource(resId);

        // Cache dir: /cache/videos
        File cacheDir = new File(getCacheDir(), "videos");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        // Name it something like "video_<id>.mp4"
        // (extension helps some players, even though it's not strictly required)
        String fileName = "video_" + resId + ".mp4";
        File outFile = new File(cacheDir, fileName);

        // Copy only if not already present
        if (!outFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } else {
            inputStream.close();
        }

        return outFile;
    }
}

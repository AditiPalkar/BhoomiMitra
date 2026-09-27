package com.digital.bhoomimitra;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VideoView videoView = new VideoView(this);
        setContentView(videoView);

        int resId = getIntent().getIntExtra("videoRes", 0);

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
        videoView.setVideoURI(uri);
        videoView.start();
    }
}


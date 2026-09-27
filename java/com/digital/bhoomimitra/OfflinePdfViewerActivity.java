package com.digital.bhoomimitra;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetManager;
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

public class OfflinePdfViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String assetPath = getIntent().getStringExtra("pdf_asset_path");  // e.g. "pdfs/farm_guide.pdf"
        if (assetPath == null || assetPath.isEmpty()) {
            Toast.makeText(this, "PDF not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            // 1. Copy PDF from assets to cache
            File pdfFile = copyPdfFromAssetsToCache(assetPath);

            // 2. Get content:// URI using FileProvider
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    "com.digital.bhoomimitra.fileprovider",   // must match manifest authority
                    pdfFile
            );

            // 3. Launch system PDF viewer
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
            finish();

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer app found on this device.", Toast.LENGTH_LONG).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening PDF.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private File copyPdfFromAssetsToCache(String assetPath) throws IOException {
        AssetManager assetManager = getAssets();

        // assetPath looks like: "pdfs/abc.pdf"
        String fileName = assetPath.substring(assetPath.lastIndexOf('/') + 1);

        File cacheDir = new File(getCacheDir(), "pdfs");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File outFile = new File(cacheDir, fileName);

        // Copy only if not already copied
        if (!outFile.exists()) {
            InputStream inputStream = assetManager.open(assetPath);
            FileOutputStream outputStream = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();
        }

        return outFile;
    }
}

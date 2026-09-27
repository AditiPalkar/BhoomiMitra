package com.digital.bhoomimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.io.File;

public class OfflineLibrary extends Fragment {

    private CardView cardOfflineVideos, cardOfflinePdfs;
    private LinearLayout layoutVideosList, layoutPdfsList;
    private TextView arrowVideos, arrowPdfs;

    private boolean videosLoaded = false;
    private boolean pdfsLoaded = false;

    // Simple models
    static class VideoItem {
        String title;
        int resId;

        VideoItem(String title, int resId) {
            this.title = title;
            this.resId = resId;
        }
    }

    static class PdfItem {
        String title;
        String assetPath; // e.g. "pdfs/guide.pdf"

        PdfItem(String title, String assetPath) {
            this.title = title;
            this.assetPath = assetPath;
        }
    }

    // Your offline content
    private final VideoItem[] videoItems = new VideoItem[]{
//            new VideoItem("Soil Preparation", R.raw.video_1),
//            new VideoItem("How to use Urea ", R.raw.video_2),
//            new VideoItem("Soil Health ", R.raw.video_3),
//            new VideoItem("Vermicomposting", R.raw.video_4),
//            new VideoItem("Intercropping", R.raw.video_5),
//            new VideoItem("Water Conservation Technique", R.raw.video_6)


    };

    private final PdfItem[] pdfItems = new PdfItem[]{
//            new PdfItem("Farmer's Handbook English", "pdfs/farmers_handbook_on_basic_agriculture(english).pdf"),
//            new PdfItem("Farmer's Handbook Hindi", "pdfs/farmers_handbook_on_basic_agriculture(hindi).pdf"),
//            new PdfItem("Farmer's Handbook Marathi", "pdfs/farmers_handbook_on_basic_agriculture(marathi).pdf"),
//            new PdfItem("Organic Farming", "pdfs/organic_agriculture.pdf"),
//            new PdfItem("Natural Farming", "pdfs/technical_manual_on_natural_farming.pdf"),
//            new PdfItem("Maharashtra Government Schemes", "pdfs/schemes.pdf")

    };

    public OfflineLibrary() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline_library, container, false);

        cardOfflineVideos = view.findViewById(R.id.card_offline_videos);
        cardOfflinePdfs   = view.findViewById(R.id.card_offline_pdfs);
        layoutVideosList  = view.findViewById(R.id.layout_videos_list);
        layoutPdfsList    = view.findViewById(R.id.layout_pdfs_list);
        arrowVideos       = view.findViewById(R.id.arrow_videos);
        arrowPdfs         = view.findViewById(R.id.arrow_pdfs);

        // Click: videos card
        cardOfflineVideos.setOnClickListener(v -> {
            toggleSection(layoutVideosList, arrowVideos);
            if (!videosLoaded) {
                populateVideos();
                videosLoaded = true;
            }
        });

        // Click: pdfs card
        cardOfflinePdfs.setOnClickListener(v -> {
            toggleSection(layoutPdfsList, arrowPdfs);
            if (!pdfsLoaded) {
                populatePdfs();
                pdfsLoaded = true;
            }
        });

        return view;
    }

    private void toggleSection(LinearLayout sectionLayout, TextView arrow) {
        if (sectionLayout.getVisibility() == View.GONE) {
            sectionLayout.setVisibility(View.VISIBLE);
            if (arrow != null) arrow.setText("▲");
        } else {
            sectionLayout.setVisibility(View.GONE);
            if (arrow != null) arrow.setText("▼");
        }
    }

    private void populateVideos() {
        if (getContext() == null) return;
        layoutVideosList.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (VideoItem item : videoItems) {
            // inflate row layout
            View row = inflater.inflate(R.layout.item_offline_video, layoutVideosList, false);

            TextView tvTitle = row.findViewById(R.id.tvVideoTitle);
            tvTitle.setText(item.title);

            // whole row clickable
            row.setOnClickListener(v -> openVideo(item.resId));

            layoutVideosList.addView(row);
        }
    }

    private void populatePdfs() {
        if (getContext() == null) return;
        layoutPdfsList.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (PdfItem item : pdfItems) {
            View row = inflater.inflate(R.layout.item_offline_pdf, layoutPdfsList, false);

            TextView tvTitle = row.findViewById(R.id.tvPdfTitle);
            tvTitle.setText(item.title);

            row.setOnClickListener(v -> openPdf(item.assetPath));

            layoutPdfsList.addView(row);
        }
    }

    private void openVideo(int resId) {
        if (getContext() == null) return;
        Intent i = new Intent(getContext(), OfflineVideoPlayerActivity.class);
        i.putExtra("video_res_id", resId);
        startActivity(i);
    }

    private void openPdf(String assetPath) {
        if (getContext() == null) return;
        Intent i = new Intent(getContext(), OfflinePdfViewerActivity.class);
        i.putExtra("pdf_asset_path", assetPath);
        startActivity(i);
    }

}

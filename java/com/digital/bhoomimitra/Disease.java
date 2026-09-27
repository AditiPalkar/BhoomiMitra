package com.digital.bhoomimitra;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.DataType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;

public class Disease extends Fragment {

    // --- Configuration ---
    private static final String MODEL_FILE = "plant_model_dynamic_2_16.tflite";
    private static final String LABEL_FILE = "labels.txt";
    private static final int INPUT_SIZE = 128;

    // --- Request Codes ---
    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int REQUEST_CROP = 102; // New Request Code for Cropping
    private static final int PERMISSION_CAMERA = 200;

    // --- Views ---
    private ImageView cameraIcon;
    private ImageView ivResultImage;
    private TextView uploadTitle, uploadSubtitle;
    private Button btnTakePhoto, btnUploadGallery;
    private CardView resultCard, howItWorksCard, uploadCard;
    private TextView tvDiseaseName, tvConfidence, tvTreatment;

    // --- AI Variables ---
    private Interpreter tflite;
    private List<String> associatedAxisLabels;

    public Disease() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_disease, container, false);

        // 1. Initialize Views
        cameraIcon = view.findViewById(R.id.cameraIcon);
        ivResultImage = view.findViewById(R.id.ivResultImage);

        uploadTitle = view.findViewById(R.id.uploadTitle);
        uploadSubtitle = view.findViewById(R.id.uploadSubtitle);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnUploadGallery = view.findViewById(R.id.btnUploadGallery);
        resultCard = view.findViewById(R.id.resultCard);
        howItWorksCard = view.findViewById(R.id.howItWorksCard);
        uploadCard = view.findViewById(R.id.uploadCard);

        tvDiseaseName = view.findViewById(R.id.tvDiseaseName);
        tvConfidence = view.findViewById(R.id.tvConfidence);
        tvTreatment = view.findViewById(R.id.tvTreatment);

        // 2. Load the Model & Labels immediately
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(requireContext(), MODEL_FILE);
            tflite = new Interpreter(tfliteModel);
            associatedAxisLabels = FileUtil.loadLabels(requireContext(), LABEL_FILE);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error loading AI Model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // 3. Set Button Listeners
        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        btnUploadGallery.setOnClickListener(v -> openGallery());

        return view;
    }

    // --- PERMISSIONS & CAMERAS ---
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    // --- HANDLE IMAGE RESULT ---
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            // Case 1: Camera Result -> Send to Crop
            if (requestCode == REQUEST_CAMERA && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                // We need a URI to crop, so write the bitmap to a temp file
                Uri tempUri = getImageUri(requireContext(), photo);
                performCrop(tempUri);
            }

            // Case 2: Gallery Result -> Send to Crop
            else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                performCrop(selectedImage);
            }

            // Case 3: Crop Result -> Send to AI
            else if (requestCode == REQUEST_CROP && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    // The system cropper returns the image as a Bitmap in "data"
                    Bitmap croppedImage = extras.getParcelable("data");
                    if (croppedImage != null) {
                        handleFinalImage(croppedImage);
                    }
                }
            }
        }
    }

    // --- SYSTEM CROP FUNCTION ---
    private void performCrop(Uri picUri) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // Indentify data type
            cropIntent.setDataAndType(picUri, "image/*");
            // Set crop properties
            cropIntent.putExtra("crop", "true");
            // Aspect Ratio 1:1 (Square)
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            // Output Size (Matches your AI Model Input)
            cropIntent.putExtra("outputX", INPUT_SIZE);
            cropIntent.putExtra("outputY", INPUT_SIZE);
            // Return data as Bitmap
            cropIntent.putExtra("return-data", true);

            startActivityForResult(cropIntent, REQUEST_CROP);

        } catch (ActivityNotFoundException anfe) {
            // If the device doesn't support system cropping, show error and use original
            Toast.makeText(getContext(), "Device doesn't support system cropping", Toast.LENGTH_SHORT).show();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), picUri);
                handleFinalImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper: Convert Bitmap to Uri (Needed for Camera Crop)
    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Temp_Crop", null);
        return Uri.parse(path);
    }

    // --- DISPLAY & PROCESS IMAGE ---
    private void handleFinalImage(Bitmap image) {
        // Show Processing UI
        cameraIcon.setImageResource(R.drawable.ic_processing); // Make sure you have this or use a generic icon
        cameraIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        uploadTitle.setText("Analyzing...");
        uploadSubtitle.setVisibility(View.GONE);

        // Run Diagnosis
        if (tflite != null) {
            processImage(image);
        } else {
            Toast.makeText(getContext(), "Model not loaded yet", Toast.LENGTH_SHORT).show();
        }
    }

    // --- CORE AI LOGIC ---
    private void processImage(Bitmap bitmap) {
        // 1. Resize ONLY (Do NOT Normalize)
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        // 2. Load Image
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // 3. Output Buffer [1, 38]
        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, associatedAxisLabels.size()}, DataType.FLOAT32);

        // 4. Run
        if (tflite != null) {
            tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
        }

        // 5. Map Results (Match highest probability to Label)
        TensorProcessor probabilityProcessor = new TensorProcessor.Builder().build();
        Map<String, Float> labeledProbability = new TensorLabel(associatedAxisLabels, probabilityProcessor.process(probabilityBuffer)).getMapWithFloatValue();

        // 6. Find the highest confidence
        float maxConfidence = 0;
        String bestLabel = "Unknown";

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue() > maxConfidence) {
                maxConfidence = entry.getValue();
                bestLabel = entry.getKey();
            }
        }

        // 7. Show Results (Pass bitmap here)
        showResult(bestLabel, maxConfidence, bitmap);
    }

    // === UPDATED RESULT DISPLAY ===
    private void showResult(String diseaseRawName, float confidence, Bitmap image) {
        resultCard.setVisibility(View.VISIBLE);
        howItWorksCard.setVisibility(View.GONE);

        // Display the image in the result card
        ivResultImage.setImageBitmap(image);

        if (confidence < 0.70) {
            tvDiseaseName.setText("Could not identify");
            tvDiseaseName.setTextColor(Color.GRAY);
            tvConfidence.setText("Confidence: Low");
            tvTreatment.setText("The image is unclear or does not look like a known crop leaf.\n\n" +
                    "• Please get closer to the leaf.\n" +
                    "• Ensure there is good lighting.\n" +
                    "• Focus on the affected spot.");
            return;
        }

        // Clean up label name (e.g. "Corn_(maize)___healthy" -> "Corn: Healthy")
        String displayName = diseaseRawName.replace("_", " ").replace("  ", " ");
        tvDiseaseName.setText(displayName);

        // Confidence
        String percentage = String.format("%.1f%%", confidence * 100);
        tvConfidence.setText("Confidence: " + percentage);

        // Treatment Logic
        boolean isHealthy = diseaseRawName.toLowerCase().contains("healthy");

        if (isHealthy) {
            tvDiseaseName.setTextColor(Color.parseColor("#2E7D32")); // Green
            tvTreatment.setText("Good news! Your crop looks healthy.\n\n" +
                    "• Keep monitoring water levels.\n" +
                    "• Ensure soil nutrients are balanced.");
        } else {
            tvDiseaseName.setTextColor(Color.parseColor("#C62828")); // Red
            tvTreatment.setText("Alert: Potential Disease Detected.\n\n" +
                    "• Isolate this plant immediately.\n" +
                    "• Check for similar signs in nearby plants.\n" +
                    "• Consult an agricultural expert or apply recommended organic fungicide.");
        }

        // Scroll to result
        resultCard.getParent().requestChildFocus(resultCard, resultCard);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }
}
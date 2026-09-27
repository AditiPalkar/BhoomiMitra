package com.digital.bhoomimitra;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class PlantDiseaseClassifier {

    // ===== MODEL CONFIG =====
    private static final String TAG = "MODEL_DEBUG";
    private static final int IMAGE_SIZE = 128;
    private static final int CHANNELS = 3;

    private Interpreter interpreter;
    private List<String> labels;

    // ===== CONSTRUCTOR =====
    public PlantDiseaseClassifier(Context context) throws Exception {
        Log.d(TAG, "Initializing TensorFlow Lite Interpreter...");

        interpreter = new Interpreter(loadModel(context));

        Log.d(TAG, "Interpreter created successfully");

        labels = loadLabels(context);
        Log.d(TAG, "Labels loaded. Count = " + labels.size());

        // Detect input shape
        int[] shape = interpreter.getInputTensor(0).shape();
        Log.d(TAG, "Model input shape: "
                + shape[0] + " x "
                + shape[1] + " x "
                + shape[2] + " x "
                + shape[3]);
    }


    // ===== LOAD MODEL =====
    private MappedByteBuffer loadModel(Context context) throws Exception {
        String modelName = "plant_model_dynamic_2_16.tflite";

        Log.d(TAG, "Trying to load model from assets: " + modelName);

        AssetFileDescriptor afd = context.getAssets().openFd(modelName);
        Log.d(TAG, "Model file found. Size: " + afd.getDeclaredLength());

        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        MappedByteBuffer buffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.getStartOffset(),
                afd.getDeclaredLength()
        );

        Log.d(TAG, "Model mapped into memory successfully");

        return buffer;
    }


    // ===== LOAD LABELS =====
    private List<String> loadLabels(Context context) throws Exception {
        List<String> labelList = new ArrayList<>();
        InputStream is = context.getAssets().open("labels.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            labelList.add(line);
        }

        br.close();
        Log.d(TAG, "Labels loaded: " + labelList.size());
        return labelList;
    }


    private ByteBuffer preprocess(Bitmap bitmap) {

        Bitmap resized = Bitmap.createScaledBitmap(
                bitmap,
                IMAGE_SIZE,
                IMAGE_SIZE,
                true
        );

        ByteBuffer buffer = ByteBuffer.allocateDirect(
                4 * IMAGE_SIZE * IMAGE_SIZE * CHANNELS
        );
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixel : pixels) {
            // Normalize RGB to [0,1]
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            buffer.putFloat((pixel & 0xFF) / 255.0f);
        }

        return buffer;
    }


    public Result classify(Bitmap bitmap) {

        Log.d(TAG, "Running inference...");

        ByteBuffer input = preprocess(bitmap);

        float[][] output = new float[1][labels.size()];
        interpreter.run(input, output);

        Log.d(TAG, "Inference completed");

        int bestIndex = 0;
        float bestConfidence = 0f;

        for (int i = 0; i < labels.size(); i++) {
            if (output[0][i] > bestConfidence) {
                bestConfidence = output[0][i];
                bestIndex = i;
            }
        }

        Log.d(TAG, "Prediction: " + labels.get(bestIndex)
                + " (" + bestConfidence + ")");

        return new Result(labels.get(bestIndex), bestConfidence);
    }


    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }

    public static class Result {
        public final String label;
        public final float confidence;

        public Result(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}

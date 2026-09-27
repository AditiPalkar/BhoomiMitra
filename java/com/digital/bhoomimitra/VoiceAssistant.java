package com.digital.bhoomimitra;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceAssistant extends Fragment {

    private static final String CEREBRAS_API_KEY = "csk-mhmfjmt39632865d9ph4325w4dvv2hhwwepffwwew2jjpnx4";

    // UI Variables
    private TextView stateTitle, stateSubtitle, selectedLanguageText;
    private FrameLayout micContainer;
    private LinearLayout resultContainer, languageSelector;
    private TextView tvUserQuestion, tvAiAnswer;
    private MaterialButton btnClear;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent speechIntent;

    // Default to English
    private String currentLangCode = "en";
    private String currentLangName = "English";

    private final OkHttpClient client = new OkHttpClient();

    // Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), isGranted -> {

                        if (isGranted) {
                            startListening();
                        } else {
                            if (!shouldShowRequestPermissionRationale(
                                    Manifest.permission.RECORD_AUDIO)) {
                                showSettingsDialog();
                            } else {
                                Toast.makeText(
                                        requireContext(),
                                        "Microphone permission is required to use voice assistant",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    }
            );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voice_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind UI Elements
        stateTitle = view.findViewById(R.id.stateTitle);
        stateSubtitle = view.findViewById(R.id.stateSubtitle);
        micContainer = view.findViewById(R.id.micCircleContainer);
        languageSelector = view.findViewById(R.id.languageSelector);
        selectedLanguageText = view.findViewById(R.id.selectedLanguageText);
        resultContainer = view.findViewById(R.id.resultContainer);
        tvUserQuestion = view.findViewById(R.id.tvUserQuestion);
        tvAiAnswer = view.findViewById(R.id.tvAiAnswer);
        btnClear = view.findViewById(R.id.btnClear);

        // Listeners
        micContainer.setOnClickListener(v -> checkPermissionAndListen());
        languageSelector.setOnClickListener(v -> showLanguageDialog());

        btnClear.setOnClickListener(v -> {
            resultContainer.setVisibility(View.GONE);
            stateTitle.setText("Tap to speak");
            stateSubtitle.setText("I am here to help");
            if(textToSpeech != null) textToSpeech.stop();
        });

        // Initialize TTS
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        setupPills(view);
    }

    private void checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }


    private void showSettingsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission Required")
                .setMessage("Please enable the Microphone permission in Settings so I can hear you.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(),
                    "Speech recognition not available on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (speechRecognizer == null) {
            setupSpeechRecognizer();
        }
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLangCode);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechRecognizer.startListening(speechIntent);

        stateTitle.setText("Listening...");
        stateSubtitle.setText("Speak now...");
        resultContainer.setVisibility(View.GONE);
    }

    private void showLanguageDialog() {
        // Supported Languages
        String[] languages = {"English", "Hindi", "Marathi", "Tamil", "Telugu", "Kannada", "Gujarati", "Bengali", "Malayalam", "Punjabi"};
        String[] codes =     {"en",      "hi",    "mr",      "ta",    "te",     "kn",      "gu",       "bn",      "ml",        "pa"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    currentLangName = languages[which];
                    currentLangCode = codes[which];
                    selectedLanguageText.setText(currentLangName);

                    setTtsLanguage(currentLangCode);
                })
                .show();
    }

    private void setTtsLanguage(String code) {
        if (textToSpeech != null) {
            Locale locale = new Locale(code);
            int result = textToSpeech.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(requireContext(), "Voice for this language is not installed on your phone.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSpeechRecognizer() {
        if(getContext() == null) return;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() { stateTitle.setText("Processing..."); }
            @Override
            public void onError(int error) {
                stateTitle.setText("Tap to speak");
                stateSubtitle.setText("Try again");
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) handleUserQuery(matches.get(0));
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void handleUserQuery(String query) {
        resultContainer.setVisibility(View.VISIBLE);
        tvUserQuestion.setText(query);
        tvAiAnswer.setText("Thinking...");
        askCerebras(query);
    }

    private void askCerebras(String query) {
        String url = "https://api.cerebras.ai/v1/chat/completions";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "llama-3.3-70b");
            JSONObject message = new JSONObject();
            message.put("role", "user");

            String prompt = "You are a farming expert for India. " +
                    "The user has selected the language: " + currentLangName + ". " +
                    "You MUST reply ONLY in " + currentLangName + " script. " +
                    "Do not use English characters unless necessary. " +
                    "Keep the answer short, practical, and helpful (max 4 sentences). " +
                    "User Question: " + query;

            message.put("content", prompt);

            JSONArray messagesArray = new JSONArray();
            messagesArray.put(message);
            jsonBody.put("messages", messagesArray);
        } catch (JSONException e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + CEREBRAS_API_KEY).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(isAdded()) requireActivity().runOnUiThread(() -> tvAiAnswer.setText("Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(!isAdded()) return;
                try {
                    String respBody = response.body().string();
                    JSONObject json = new JSONObject(respBody);
                    String answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

                    requireActivity().runOnUiThread(() -> {
                        tvAiAnswer.setText(answer);
                        stateTitle.setText("Tap to speak");
                        speak(answer);
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> tvAiAnswer.setText("Error: " + e.getMessage()));
                }
            }
        });
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            // Ensure language is set before speaking
            setTtsLanguage(currentLangCode);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void setupPills(View view) {
        View.OnClickListener listener = v -> {
            MaterialButton b = (MaterialButton) v;
            handleUserQuery(b.getText().toString());
        };
        if(view.findViewById(R.id.pill1) != null) view.findViewById(R.id.pill1).setOnClickListener(listener);
        if(view.findViewById(R.id.pill2) != null) view.findViewById(R.id.pill2).setOnClickListener(listener);
        if(view.findViewById(R.id.pill3) != null) view.findViewById(R.id.pill3).setOnClickListener(listener);
        if(view.findViewById(R.id.pill4) != null) view.findViewById(R.id.pill4).setOnClickListener(listener);
        if(view.findViewById(R.id.pill5) != null) view.findViewById(R.id.pill5).setOnClickListener(listener);
        if(view.findViewById(R.id.pill6) != null) view.findViewById(R.id.pill6).setOnClickListener(listener);
    }

    @Override
    public void onDestroyView() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) textToSpeech.stop();
        super.onDestroyView();
    }
}
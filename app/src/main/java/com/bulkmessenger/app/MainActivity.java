package com.bulkmessenger.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText etNumbers, etMessage, etDelay;
    private Button btnStart, btnPause, btnResume;
    private TextView tvStatus, tvProgress, tvCurrentNumber;
    private ProgressBar progressBar;
    private LinearLayout llLog;
    private ScrollView svLog;

    private Handler handler = new Handler(Looper.getMainLooper());
    private List<String> phoneNumbers = new ArrayList<>();
    private String message = "";
    private int delayMs = 3000;
    private int currentIndex = 0;
    private boolean isPaused = false;
    private boolean isRunning = false;
    private Runnable sendRunnable;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupButtons();
    }

    private void initViews() {
        etNumbers = findViewById(R.id.etNumbers);
        etMessage = findViewById(R.id.etMessage);
        etDelay = findViewById(R.id.etDelay);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnResume = findViewById(R.id.btnResume);
        tvStatus = findViewById(R.id.tvStatus);
        tvProgress = findViewById(R.id.tvProgress);
        tvCurrentNumber = findViewById(R.id.tvCurrentNumber);
        progressBar = findViewById(R.id.progressBar);
        llLog = findViewById(R.id.llLog);
        svLog = findViewById(R.id.svLog);
        btnPause.setEnabled(false);
        btnResume.setEnabled(false);
    }

    private void setupButtons() {
        btnStart.setOnClickListener(v -> startSending());
        btnPause.setOnClickListener(v -> pauseSending());
        btnResume.setOnClickListener(v -> resumeSending());
    }

    private void startSending() {
        String numbersRaw = etNumbers.getText().toString().trim();
        message = etMessage.getText().toString().trim();
        String delayStr = etDelay.getText().toString().trim();

        if (TextUtils.isEmpty(numbersRaw)) { showToast("Phone numbers daalo"); return; }
        if (TextUtils.isEmpty(message)) { showToast("Message daalo"); return; }

        try {
            delayMs = TextUtils.isEmpty(delayStr) ? 3000 : Integer.parseInt(delayStr);
            if (delayMs < 1000) delayMs = 1000;
        } catch (NumberFormatException e) { delayMs = 3000; }

        phoneNumbers.clear();
        llLog.removeAllViews();
        for (String p : numbersRaw.split(",")) {
            String num = p.trim().replaceAll("[^\\d+]", "");
            if (!num.isEmpty()) phoneNumbers.add(num);
        }

        if (phoneNumbers.isEmpty()) { showToast("Valid numbers nahi mile"); return; }

        currentIndex = 0;
        isRunning = true;
        isPaused = false;
        progressBar.setMax(phoneNumbers.size());
        progressBar.setProgress(0);
        tvProgress.setText("0 / " + phoneNumbers.size());
        tvStatus.setText("Starting...");
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnResume.setEnabled(false);
        addLog("Loaded " + phoneNumbers.size() + " numbers", "#25D366");
        scheduleNext();
    }

    private void scheduleNext() {
        if (!isRunning || isPaused) return;
        if (currentIndex >= phoneNumbers.size()) { finishSending(); return; }
        int totalDelay = (currentIndex == 0) ? 500 : (delayMs + random.nextInt(2000));
        sendRunnable = () -> {
            if (!isRunning || isPaused) return;
            String number = phoneNumbers.get(currentIndex);
            openWhatsApp(number);
            currentIndex++;
            progressBar.setProgress(currentIndex);
            tvProgress.setText(currentIndex + " / " + phoneNumbers.size());
            tvCurrentNumber.setText("Sending to: " + number);
            addLog("Sent to " + number, "#FFFFFF");
            tvStatus.setText("Sending " + currentIndex + " of " + phoneNumbers.size());
            scheduleNext();
        };
        handler.postDelayed(sendRunnable, totalDelay);
    }

    private void openWhatsApp(String number) {
        try {
            String url = "https://wa.me/" + number.replaceAll("\\+", "") + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            String url = "https://wa.me/" + number.replaceAll("\\+", "") + "?text=" + Uri.encode(message);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private void pauseSending() {
        isPaused = true;
        if (sendRunnable != null) handler.removeCallbacks(sendRunnable);
        tvStatus.setText("Paused at " + currentIndex + " / " + phoneNumbers.size());
        btnPause.setEnabled(false);
        btnResume.setEnabled(true);
        addLog("Paused", "#FFC107");
    }

    private void resumeSending() {
        isPaused = false;
        btnPause.setEnabled(true);
        btnResume.setEnabled(false);
        tvStatus.setText("Resuming...");
        addLog("Resumed", "#25D366");
        scheduleNext();
    }

    private void finishSending() {
        isRunning = false;
        tvStatus.setText("All messages sent!");
        tvCurrentNumber.setText("Complete");
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnResume.setEnabled(false);
        addLog("Done! Sent to " + phoneNumbers.size() + " contacts", "#25D366");
        showToast("All messages sent!");
    }

    private void addLog(String text, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(android.graphics.Color.parseColor(colorHex));
        tv.setTextSize(13f);
        tv.setPadding(8, 4, 8, 4);
        llLog.addView(tv);
        svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sendRunnable != null) handler.removeCallbacks(sendRunnable);
    }
}

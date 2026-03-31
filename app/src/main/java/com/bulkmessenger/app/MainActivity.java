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
    private RadioButton rbBusiness, rbNormal;

    private Handler handler = new Handler(Looper.getMainLooper());
    private List<String> phoneNumbers = new ArrayList<>();
    private String message = "";
    private int delayMs = 5000;
    private int currentIndex = 0;
    private boolean isPaused = false;
    private boolean isRunning = false;
    private boolean waitingForReturn = false;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupButtons();
    }

    private void initViews() {
        etNumbers       = findViewById(R.id.etNumbers);
        etMessage       = findViewById(R.id.etMessage);
        etDelay         = findViewById(R.id.etDelay);
        btnStart        = findViewById(R.id.btnStart);
        btnPause        = findViewById(R.id.btnPause);
        btnResume       = findViewById(R.id.btnResume);
        tvStatus        = findViewById(R.id.tvStatus);
        tvProgress      = findViewById(R.id.tvProgress);
        tvCurrentNumber = findViewById(R.id.tvCurrentNumber);
        progressBar     = findViewById(R.id.progressBar);
        llLog           = findViewById(R.id.llLog);
        svLog           = findViewById(R.id.svLog);
        rbBusiness      = findViewById(R.id.rbBusiness);
        rbNormal        = findViewById(R.id.rbNormal);
        btnPause.setEnabled(false);
        btnResume.setEnabled(false);
    }

    private void setupButtons() {
        btnStart.setOnClickListener(v -> startSending());
        btnPause.setOnClickListener(v -> pauseSending());
        btnResume.setOnClickListener(v -> resumeSending());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning && !isPaused && waitingForReturn) {
            waitingForReturn = false;
            int totalDelay = delayMs + random.nextInt(2000);
            handler.postDelayed(this::sendCurrent, totalDelay);
        }
    }

    private void startSending() {
        String numbersRaw = etNumbers.getText().toString().trim();
        message           = etMessage.getText().toString().trim();
        String delayStr   = etDelay.getText().toString().trim();

        if (TextUtils.isEmpty(numbersRaw)) { showToast("Phone numbers daalo"); return; }
        if (TextUtils.isEmpty(message))    { showToast("Message daalo");        return; }

        try {
            delayMs = TextUtils.isEmpty(delayStr) ? 5000 : Integer.parseInt(delayStr);
            if (delayMs < 2000) delayMs = 2000;
        } catch (NumberFormatException e) { delayMs = 5000; }

        phoneNumbers.clear();
        llLog.removeAllViews();
        for (String p : numbersRaw.split(",")) {
            String num = p.trim().replaceAll("[^\\d+]", "");
            if (!num.isEmpty()) phoneNumbers.add(num);
        }

        if (phoneNumbers.isEmpty()) { showToast("Valid numbers nahi mile"); return; }

        currentIndex     = 0;
        isRunning        = true;
        isPaused         = false;
        waitingForReturn = false;

        progressBar.setMax(phoneNumbers.size());
        progressBar.setProgress(0);
        tvProgress.setText("0 / " + phoneNumbers.size());
        tvStatus.setText("Starting...");
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnResume.setEnabled(false);

        addLog("📋 " + phoneNumbers.size() + " numbers load hue", "#25D366");
        handler.postDelayed(this::sendCurrent, 1000);
    }

    private void sendCurrent() {
        if (!isRunning || isPaused) return;
        if (currentIndex >= phoneNumbers.size()) {
            finishSending();
            return;
        }

        String number = phoneNumbers.get(currentIndex);
        tvCurrentNumber.setText("📱 " + number);
        tvStatus.setText("Sending " + (currentIndex + 1) + " / " + phoneNumbers.size());
        addLog("✅ Sending to: " + number, "#FFFFFF");

        openWhatsApp(number);

        currentIndex++;
        progressBar.setProgress(currentIndex);
        tvProgress.setText(currentIndex + " / " + phoneNumbers.size());
        waitingForReturn = true;

        handler.postDelayed(() -> {
            if (isRunning && !isPaused && waitingForReturn) {
                waitingForReturn = false;
                sendCurrent();
            }
        }, delayMs + 8000);
    }

    private void openWhatsApp(String number) {
        String clean = number.replaceAll("\\+", "").replaceAll("\\s", "");
        String url   = "https://wa.me/" + clean + "?text=" + Uri.encode(message);

        String waPackage = (rbBusiness != null && rbBusiness.isChecked())
                ? "com.whatsapp.w4b"
                : "com.whatsapp";

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(waPackage);
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ex) {
                addLog("❌ Error: " + number, "#FF5555");
            }
        }
    }

    private void pauseSending() {
        isPaused = true;
        handler.removeCallbacksAndMessages(null);
        tvStatus.setText("⏸ Paused at " + currentIndex + " / " + phoneNumbers.size());
        btnPause.setEnabled(false);
        btnResume.setEnabled(true);
        addLog("⏸ Paused", "#FFC107");
    }

    private void resumeSending() {
        isPaused = false;
        btnPause.setEnabled(true);
        btnResume.setEnabled(false);
        tvStatus.setText("▶ Resuming...");
        addLog("▶ Resumed", "#25D366");
        handler.postDelayed(this::sendCurrent, 1000);
    }

    private void finishSending() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        tvStatus.setText("🎉 Sab messages send ho gaye!");
        tvCurrentNumber.setText("✅ Complete");
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnResume.setEnabled(false);
        addLog("🎉 Done! " + phoneNumbers.size() + " contacts ko message hua", "#25D366");
        showToast("🎉 All messages sent!");
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
        handler.removeCallbacksAndMessages(null);
    }
}

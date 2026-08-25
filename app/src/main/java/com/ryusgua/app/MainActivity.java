package com.ryusgua.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Activity shell. Rendering and interaction live in {@link GuaView}. */
public class MainActivity extends Activity implements SensorEventListener {
    private GuaView guaView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeAt = 0L;
    private AudioEngine audioEngine;

    @android.annotation.TargetApi(28)
    private static final class Api28Window {
        static void enableCutout(Window window) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Api28Window.enableCutout(window);
        } catch (Throwable ignored) {}
        enterImmersive(window);
        audioEngine = new AudioEngine(this);
        guaView = new GuaView(this, audioEngine);
        setContentView(guaView);
        new Handler(Looper.getMainLooper()).postDelayed(() -> UpdateChecker.check(this, false), 2600L);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        float g = (float) Math.sqrt(x * x + y * y + z * z);
        long now = System.currentTimeMillis();
        if (g > 18.5f && now - lastShakeAt > 1400L) {
            lastShakeAt = now;
            if (guaView != null) guaView.onShake();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void enterImmersive(Window window) {
        try {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } catch (Throwable ignored) {}
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive(getWindow());
    }

    @Override protected void onDestroy() {
        if (audioEngine != null) audioEngine.release();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (guaView != null && guaView.handleBack()) return;
        super.onBackPressed();
    }
}

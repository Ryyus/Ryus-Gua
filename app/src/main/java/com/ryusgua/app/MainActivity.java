package com.ryusgua.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Window.enableCutout(window);
            }
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
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } catch (Throwable ignored) {
            // Fullscreen is cosmetic. A ROM-specific failure must never stop 柳之卦 from opening.
        }
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

    private static final class GuaView extends View {
        private static final int BG = Color.rgb(7, 9, 7);
        private static final int FG = Color.rgb(231, 226, 207);
        private static final int MUTED = Color.rgb(125, 132, 119);
        private static final int GOLD = Color.rgb(201, 168, 91);
        private static final int RED = Color.rgb(210, 82, 62);
        private static final int PANEL = Color.rgb(17, 20, 17);
        private static final int GRID = Color.rgb(29, 35, 29);

        private enum State { BOOT, IDLE, CASTING, RESULT, DETAIL, HISTORY, OFFLINE, AI }

        private static final class CoinBody {
            float x, y, vx, vy, angle, angular, targetX, targetY, scale = 1f;
            String targetFace = "字";
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
        private final Typeface monoBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        private final int[] lines = new int[6]; // bottom -> top
        private final RectF primaryButton = new RectF();
        private final RectF leftButton = new RectF();
        private final RectF rightButton = new RectF();
        private final RectF auxLeftButton = new RectF();
        private final RectF auxRightButton = new RectF();
        private final RectF historyButton = new RectF();
        private final RectF settingsButton = new RectF();
        private final RectF experienceButton = new RectF();
        private final RectF backButton = new RectF();
        private final RectF clearButton = new RectF();
        private final RectF reasoningToggleButton = new RectF();
        private final String[] currentCoins = {"·", "·", "·"};
        private final ZhouYiRepository zhouYi;
        private final ArrayList<HistoryHit> historyHits = new ArrayList<>();

        private State state = State.BOOT;
        private int castCount = 0;
        private String toastLine = "";
        private float coinPhase = 0f;
        private float scrollOffset = 0f;
        private float maxScroll = 0f;
        private float downY = 0f;
        private float lastY = 0f;
        private boolean dragging = false;
        private boolean loadedFromHistory = false;
        private String currentHistoryId = "";
        private boolean aiLoading = false;
        private String aiText = "";
        private String aiReasoning = "";
        private boolean aiReasoningExpanded = false;
        private boolean aiReasoningSummaryOnly = true;
        private String aiError = "";
        private String aiModel = "";
        private int bootStep = 0;
        private float bootSweep = 0f;
        private final AudioEngine audio;
        private final Vibrator vibrator;
        private boolean soundEnabled = true;
        private boolean hapticEnabled = true;
        private boolean shakeEnabled = true;
        private boolean manualCasting = false;
        private boolean verticalFlipEnabled = false;
        private boolean formalCastingActive = false;
        private boolean formalAwaitingManual = false;
        private long formalStartEpoch = 0L;
        private final Runnable formalClockTicker = new Runnable() {
            @Override public void run() {
                if (!formalCastingActive || state != State.CASTING) return;
                postInvalidateOnAnimation();
                handler.postDelayed(this, 250L);
            }
        };
        private static final String ANIM_CLASSIC = "classic";
        private static final String ANIM_PHYSICS = "physics";
        private String coinAnimationMode = ANIM_CLASSIC;
        private boolean lineAnimating = false;
        private boolean physicsActive = false;
        private long physicsStartedAt = 0L;
        private long physicsLastFrameAt = 0L;
        private int pendingPhysicsLine = 0;
        private int physicsSoundMask = 0;
        private final CoinBody[] physicsCoins = {new CoinBody(), new CoinBody(), new CoinBody()};
        private float safeInsetLeft = 0f;
        private float safeInsetTop = 0f;
        private float safeInsetRight = 0f;
        private float safeInsetBottom = 0f;

        GuaView(Context context, AudioEngine audio) {
            super(context);
            this.audio = audio;
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            zhouYi = new ZhouYiRepository(context);
            paint.setTypeface(mono);
            paint.setDither(true);
            setBackgroundColor(BG);
            setFocusable(true);
            loadExperienceSettings();
            try {
                setOnApplyWindowInsetsListener((v, insets) -> {
                    try { return applySafeInsets(insets); }
                    catch (Throwable ignored) { return insets; }
                });
                requestApplyInsets();
            } catch (Throwable ignored) {}
            startBootAnimation();
        }

        private void startBootAnimation() {
            final long started = SystemClock.uptimeMillis();
            handler.postDelayed(() -> {
                if (soundEnabled) audio.boot();
                pulse(18, 90);
            }, 120L);
            Runnable animator = new Runnable() {
                @Override public void run() {
                    float t = Math.min(1f, (SystemClock.uptimeMillis() - started) / 1250f);
                    float inv = 1f - t;
                    bootSweep = 1f - inv * inv * inv;
                    bootStep = t < .18f ? 0 : t < .38f ? 1 : t < .58f ? 2 : t < .78f ? 3 : 4;
                    postInvalidateOnAnimation();
                    if (t < 1f) {
                        postOnAnimation(this);
                    } else {
                        state = State.IDLE;
                        pulse(28, 120);
                        postInvalidateOnAnimation();
                    }
                }
            };
            postOnAnimation(animator);
        }

        private WindowInsets applySafeInsets(WindowInsets insets) {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    int[] values = Api30Insets.read(insets);
                    left = values[0]; top = values[1]; right = values[2]; bottom = values[3];
                    if (bottom > 0) bottom += (int) dp(4);
                } catch (Throwable ignored) {}
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        int[] cut = Api28Insets.readCutout(insets);
                        left = Math.max(left, cut[0]); top = Math.max(top, cut[1]);
                        right = Math.max(right, cut[2]); bottom = Math.max(bottom, cut[3]);
                    } catch (Throwable ignored) {}
                }
                left = Math.max(left, insets.getSystemWindowInsetLeft());
                right = Math.max(right, insets.getSystemWindowInsetRight());
                bottom = Math.max(bottom, insets.getSystemWindowInsetBottom());
            }
            safeInsetLeft = left;
            safeInsetTop = top;
            safeInsetRight = right;
            safeInsetBottom = bottom;
            postInvalidateOnAnimation();
            return insets;
        }

        @android.annotation.TargetApi(30)
        private static final class Api30Insets {
            static int[] read(WindowInsets insets) {
                Insets nav = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars());
                Insets gestures = insets.getInsets(WindowInsets.Type.systemGestures());
                Insets mandatory = insets.getInsets(WindowInsets.Type.mandatorySystemGestures());
                Insets cut = insets.getInsets(WindowInsets.Type.displayCutout());
                return new int[]{
                        Math.max(Math.max(nav.left, gestures.left), Math.max(mandatory.left, cut.left)),
                        Math.max(nav.top, cut.top),
                        Math.max(Math.max(nav.right, gestures.right), Math.max(mandatory.right, cut.right)),
                        Math.max(Math.max(nav.bottom, gestures.bottom), Math.max(mandatory.bottom, cut.bottom))};
            }
        }

        @android.annotation.TargetApi(28)
        private static final class Api28Insets {
            static int[] readCutout(WindowInsets insets) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout == null) return new int[]{0, 0, 0, 0};
                return new int[]{cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                        cutout.getSafeInsetRight(), cutout.getSafeInsetBottom()};
            }
        }

        private void loadExperienceSettings() {
            SharedPreferences pref = getContext().getSharedPreferences("ryusgua_experience", Context.MODE_PRIVATE);
            soundEnabled = pref.getBoolean("sound", true);
            hapticEnabled = pref.getBoolean("haptic", true);
            shakeEnabled = pref.getBoolean("shake", true);
            manualCasting = pref.getBoolean("manual_cast", false);
            verticalFlipEnabled = pref.getBoolean("vertical_flip", false);
            coinAnimationMode = pref.getString("coin_animation", ANIM_CLASSIC);
            if (!ANIM_PHYSICS.equals(coinAnimationMode)) coinAnimationMode = ANIM_CLASSIC;
        }

        private void saveExperienceSettings() {
            getContext().getSharedPreferences("ryusgua_experience", Context.MODE_PRIVATE).edit()
                    .putBoolean("sound", soundEnabled)
                    .putBoolean("haptic", hapticEnabled)
                    .putBoolean("shake", shakeEnabled)
                    .putBoolean("manual_cast", manualCasting)
                    .putBoolean("vertical_flip", verticalFlipEnabled)
                    .putString("coin_animation", coinAnimationMode)
                    .apply();
        }

        boolean handleBack() {
            if (state == State.BOOT) return true;
            if (state == State.DETAIL) { state = State.RESULT; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.HISTORY) { state = State.IDLE; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.OFFLINE) { state = State.RESULT; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.AI) { state = State.OFFLINE; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.RESULT) { state = State.IDLE; loadedFromHistory = false; postInvalidateOnAnimation(); return true; }
            if (state == State.CASTING && formalCastingActive) {
                cancelFormalCasting();
                state = State.IDLE;
                postInvalidateOnAnimation();
                Toast.makeText(getContext(), "正式起卦已取消", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }

        void onShake() {
            if (!shakeEnabled) return;
            if (state == State.IDLE) {
                haptic(HapticFeedbackConstants.CONFIRM);
                pulse(24, 130);
                Toast.makeText(getContext(), manualCasting ? "摇动 · 第一爻" : "摇卦", Toast.LENGTH_SHORT).show();
                startCasting();
                return;
            }
            if (state == State.CASTING && manualCasting && !lineAnimating && castCount < 6) {
                if (formalCastingActive && !formalAwaitingManual) {
                    Toast.makeText(getContext(), "正式起卦 · 请等待定时提示", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (formalCastingActive) formalAwaitingManual = false;
                haptic(HapticFeedbackConstants.CLOCK_TICK);
                pulse(15, 90);
                castNext();
            }
        }

        private float dp(float n) { return n * getResources().getDisplayMetrics().density; }

        private void pulse(long ms, int amplitude) {
            if (!hapticEnabled || vibrator == null) return;
            try {
                if (!vibrator.hasVibrator()) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Api26Vibration.oneShot(vibrator, ms, amplitude);
                } else {
                    vibrator.vibrate(ms);
                }
            } catch (Throwable ignored) {}
        }

        private void ritualPulse() {
            if (!hapticEnabled || vibrator == null) return;
            try {
                if (!vibrator.hasVibrator()) return;
                long[] timings = {0, 18, 52, 18, 70, 34};
                int[] amps = {0, 85, 0, 115, 0, 165};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Api26Vibration.wave(vibrator, timings, amps);
                } else {
                    vibrator.vibrate(timings, -1);
                }
            } catch (Throwable ignored) {}
        }

        @android.annotation.TargetApi(26)
        private static final class Api26Vibration {
            static void oneShot(Vibrator vibrator, long ms, int amplitude) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, Math.max(1, Math.min(255, amplitude))));
            }
            static void wave(Vibrator vibrator, long[] timings, int[] amps) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
            }
        }

        private void haptic(int constant) {
            if (!hapticEnabled) return;
            try { performHapticFeedback(constant); }
            catch (Throwable ignored) {}
        }

        private void text(Canvas c, String s, float x, float y, float size, int color, Paint.Align align, boolean bold) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextSize(dp(size));
            paint.setTextAlign(align);
            paint.setTypeface(bold ? monoBold : mono);
            c.drawText(s, x, y, paint);
        }

        private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(width));
            paint.setStrokeCap(Paint.Cap.SQUARE);
            paint.setColor(color);
            c.drawLine(x1, y1, x2, y2, paint);
        }

        private void panel(Canvas c, RectF r) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PANEL);
            c.drawRoundRect(r, dp(5), dp(5), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(GRID);
            c.drawRoundRect(r, dp(5), dp(5), paint);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float contentW = Math.max(dp(240), getWidth() - safeInsetLeft - safeInsetRight);
            float contentH = Math.max(dp(360), getHeight() - safeInsetTop - safeInsetBottom);
            c.save();
            c.translate(safeInsetLeft, safeInsetTop);
            drawGrid(c, contentW, contentH);
            if (state != State.BOOT) drawHeader(c, contentW);
            switch (state) {
                case BOOT: drawBoot(c, contentW, contentH); break;
                case IDLE: drawIdle(c, contentW, contentH); break;
                case CASTING: drawCasting(c, contentW, contentH); break;
                case RESULT: drawResult(c, contentW, contentH); break;
                case DETAIL: drawDetail(c, contentW, contentH); break;
                case HISTORY: drawHistory(c, contentW, contentH); break;
                case OFFLINE: drawOffline(c, contentW, contentH); break;
                case AI: drawAi(c, contentW, contentH); break;
            }
            c.restore();
        }

        private void drawGrid(Canvas c, float w, float h) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(4, 5, 4));
            c.drawRect(0, 0, w, h, paint);
            paint.setColor(Color.argb(26, 150, 165, 140));
            for (float y = 0; y < h; y += dp(4)) c.drawRect(0, y, w, y + dp(.45f), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(.8f));
            paint.setColor(Color.rgb(29, 33, 28));
            RectF bezel = new RectF(dp(8), dp(8), w - dp(8), h - dp(8));
            c.drawRoundRect(bezel, dp(11), dp(11), paint);
            paint.setColor(Color.argb(80, 201, 168, 91));
            c.drawLine(dp(15), dp(18), dp(38), dp(18), paint);
            c.drawLine(w-dp(38), h-dp(18), w-dp(15), h-dp(18), paint);
        }

        private void drawBoot(Canvas c, float w, float h) {
            float cx = w / 2f;
            text(c, "M5://STICKS3", dp(22), dp(44), 8.5f, MUTED, Paint.Align.LEFT, true);
            text(c, "HEX TERMINAL", w-dp(22), dp(44), 8.5f, MUTED, Paint.Align.RIGHT, false);
            line(c, dp(22), dp(58), w-dp(22), dp(58), GRID, 1);
            float y = h * .37f;
            if (bootStep >= 1) text(c, "柳", cx-dp(22), y, 42, FG, Paint.Align.CENTER, true);
            if (bootStep >= 2) text(c, "卦", cx+dp(22), y, 42, GOLD, Paint.Align.CENTER, true);
            if (bootStep >= 3) {
                text(c, "RYU\'S GUA", cx, y+dp(31), 10, MUTED, Paint.Align.CENTER, true);
                text(c, "电子蓍筮终端", cx, y+dp(52), 9.5f, FG, Paint.Align.CENTER, false);
            }
            float barL=dp(30), barR=w-dp(30), barY=h*.70f;
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(GRID);
            c.drawRect(barL, barY, barR, barY+dp(8), paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(GOLD);
            c.drawRect(barL+dp(2), barY+dp(2), barL+dp(2)+(barR-barL-dp(4))*bootSweep, barY+dp(6), paint);
            text(c, bootStep < 4 ? "INITIALIZING..." : "READY", cx, barY+dp(28), 8, bootStep<4?MUTED:GOLD, Paint.Align.CENTER, true);
            text(c, "v" + BuildConfig.VERSION_NAME + " / Ryu\'s Gua", cx, h-dp(36), 7.5f, MUTED, Paint.Align.CENTER, false);
        }

        private void drawHeader(Canvas c, float w) {
            text(c, "柳之卦", dp(20), dp(41), 26, FG, Paint.Align.LEFT, true);
            text(c, "RYU\'S GUA / " + BuildConfig.VERSION_NAME, dp(20), dp(61), 9, MUTED, Paint.Align.LEFT, false);
            text(c, "易", w - dp(22), dp(43), 25, GOLD, Paint.Align.RIGHT, true);
            line(c, dp(20), dp(75), w - dp(20), dp(75), GOLD, 1);
        }

        private void drawIdle(Canvas c, float w, float h) {
            float cx = w / 2f;
            float topLeft = dp(20), topRight = w - dp(20), gap = dp(8);
            float cell = (topRight - topLeft - gap) / 2f;
            experienceButton.set(topLeft, dp(88), topLeft + cell, dp(120));
            historyButton.set(topLeft + cell + gap, dp(88), topRight, dp(120));
            settingsButton.setEmpty();
            button(c, experienceButton, "设置", FG, false, 10);
            button(c, historyButton, "历史", MUTED, false, 10);

            text(c, "默 念 一 事", cx, h * 0.30f, 24, FG, Paint.Align.CENTER, true);
            text(c, "ONE QUESTION / ONE CAST", cx, h * 0.30f + dp(27), 8, MUTED, Paint.Align.CENTER, true);
            text(c, "静心 · 持念 · 起卦", cx, h * 0.30f + dp(47), 10.5f, GOLD, Paint.Align.CENTER, false);

            RectF seal = new RectF(cx - dp(43), h * 0.47f - dp(43), cx + dp(43), h * 0.47f + dp(43));
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(RED);
            c.drawRect(seal, paint);
            text(c, "卦", cx, h * 0.47f + dp(19), 48, RED, Paint.Align.CENTER, true);

            primaryButton.set(dp(28), h - dp(122), w - dp(28), h - dp(64));
            button(c, primaryButton, manualCasting ? "按下 · 掷第一爻" : "按下成卦", GOLD, true, 13);
            text(c, shakeEnabled ? "KEY / SHAKE" : "KEY", cx, h - dp(41), 9, GOLD, Paint.Align.CENTER, true);
            text(c, manualCasting ? "逐爻手动 · 一次一掷" : "三钱六掷 · 自动成卦", cx, h - dp(21), 8.2f, MUTED, Paint.Align.CENTER, false);
        }

        private void drawCasting(Canvas c, float w, float h) {
            float cx = w / 2f;
            int shown = Math.min(castCount + 1, 6);
            if (formalCastingActive) {
                text(c, "正式起卦 · " + formatClock(System.currentTimeMillis()), cx, dp(104), 10.5f, GOLD, Paint.Align.CENTER, true);
                text(c, String.format(Locale.CHINA, "第 %d / 6 爻", shown), cx, dp(126), 12, FG, Paint.Align.CENTER, true);
            } else {
                text(c, String.format(Locale.CHINA, "第 %d / 6 爻", shown), cx, dp(112), 13, GOLD, Paint.Align.CENTER, true);
            }
            float coinY = formalCastingActive ? dp(183) : dp(177);
            if (!(ANIM_PHYSICS.equals(coinAnimationMode) && physicsActive)) {
                for (int i = 0; i < 3; i++) drawCoin(c, cx + dp((i - 1) * 72), coinY, currentCoins[i], i);
            }

            float reserved = manualCasting ? dp(145) : dp(80);
            RectF frame = new RectF(dp(35), dp(234), w - dp(35), h - reserved);
            panel(c, frame);
            String castModeText = formalCastingActive ? (manualCasting ? "六爻 / 正式定时 · 手动确认" : "六爻 / 正式定时 · 自动")
                    : (manualCasting ? "六爻 / 点击或摇动逐爻投掷" : "六爻 / 自动投掷");
            text(c, castModeText, frame.left + dp(14), frame.top + dp(24), 9.5f, MUTED, Paint.Align.LEFT, false);
            drawStack(c, frame.centerX(), frame.bottom - dp(24), lines, castCount, false, dp(78), dp(32));

            if (manualCasting || ANIM_PHYSICS.equals(coinAnimationMode)) {
                if (!toastLine.isEmpty()) text(c, toastLine, cx, h - dp(94), 10.5f, RED, Paint.Align.CENTER, true);
                primaryButton.set(dp(28), h - dp(72), w - dp(28), h - dp(20));
                String label;
                if (castCount >= 6) label = "成卦中…";
                else if (lineAnimating) label = "投掷中…";
                else if (formalCastingActive && manualCasting) label = formalAwaitingManual ? "点击 · 掷此爻" : "等待定时…";
                else label = manualCasting ? "点击 · 下一爻" : "自动 · 下一爻";
                button(c, primaryButton, label, lineAnimating ? MUTED : GOLD, true, 11.5f);
            } else {
                if (!toastLine.isEmpty()) text(c, toastLine, cx, h - dp(42), 11, RED, Paint.Align.CENTER, true);
            }
            // Physics coins are drawn last so they visibly fly out over the launch button and panel.
            if (ANIM_PHYSICS.equals(coinAnimationMode) && physicsActive) drawPhysicsCoins(c);
        }

        private void drawCoin(Canvas c, float x, float y, String face, int index) {
            // v0.7: keep every coin circular at all times. Previous perspective squash
            // made the second/third coin look progressively thinner or malformed.
            float phase = coinPhase + index * 0.22f;
            float lift = (float) Math.abs(Math.sin(phase)) * dp(4);
            float pulse = 1f + 0.025f * (float) Math.sin(phase * 2f);
            float r = dp(26) * pulse;
            float cy = y - lift;
            float flipScale = 1f;
            if (verticalFlipEnabled && lineAnimating) {
                flipScale = Math.max(0.14f, Math.abs((float) Math.cos(phase)));
            }
            c.save();
            c.scale(1f, flipScale, x, cy);
            RectF outer = new RectF(x - r, cy - r, x + r, cy + r);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(39, 34, 23));
            c.drawOval(outer, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(GOLD);
            c.drawOval(outer, paint);

            float inner = r * 0.72f;
            RectF ring = new RectF(x - inner, cy - inner, x + inner, cy + inner);
            paint.setStrokeWidth(dp(0.8f));
            paint.setColor(Color.rgb(118, 92, 45));
            c.drawOval(ring, paint);

            float hole = dp(6.5f);
            RectF square = new RectF(x - hole, cy - hole, x + hole, cy + hole);
            paint.setStrokeWidth(dp(1));
            paint.setColor(GOLD);
            c.drawRect(square, paint);
            text(c, face, x, cy + dp(5), 10, FG, Paint.Align.CENTER, true);
            c.restore();
        }

        private void drawPhysicsCoins(Canvas c) {
            for (int i = 0; i < physicsCoins.length; i++) {
                CoinBody b = physicsCoins[i];
                float shadowAlpha = Math.max(0.12f, Math.min(0.55f, 0.55f - (Math.abs(b.y - b.targetY) / Math.max(dp(260), 1f)) * 0.4f));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (shadowAlpha * 255), 0, 0, 0));
                RectF shadow = new RectF(b.x - dp(23) * b.scale, b.targetY + dp(30), b.x + dp(23) * b.scale, b.targetY + dp(38));
                c.drawOval(shadow, paint);

                String shownFace = Math.cos(b.angle) >= 0 ? b.targetFace : oppositeFace(b.targetFace);
                drawPhysicsCoin(c, b.x, b.y, shownFace, b.angle, b.scale);
            }
        }

        private void drawPhysicsCoin(Canvas c, float x, float y, String face, float angle, float scale) {
            float r = dp(26) * Math.max(0.72f, Math.min(1.22f, scale));
            float flipScale = verticalFlipEnabled ? Math.max(0.14f, Math.abs((float) Math.cos(angle))) : 1f;
            c.save();
            c.scale(1f, flipScale, x, y);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(39, 34, 23));
            c.drawCircle(x, y, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(GOLD);
            c.drawCircle(x, y, r, paint);

            float inner = r * 0.72f;
            paint.setStrokeWidth(dp(0.8f));
            paint.setColor(Color.rgb(118, 92, 45));
            c.drawCircle(x, y, inner, paint);

            c.save();
            c.rotate((float) Math.toDegrees(angle), x, y);
            float hole = Math.max(dp(5.5f), r * 0.24f);
            RectF square = new RectF(x - hole, y - hole, x + hole, y + hole);
            paint.setStrokeWidth(dp(1));
            paint.setColor(GOLD);
            c.drawRect(square, paint);
            c.restore();
            text(c, face, x, y + dp(5), 10, FG, Paint.Align.CENTER, true);
            c.restore();
        }

        private String oppositeFace(String face) {
            return "背".equals(face) ? "字" : "背";
        }

        private void drawResult(Canvas c, float w, float h) {
            HexagramEngine.Hexagram base = HexagramEngine.lookup(lines, false);
            HexagramEngine.Hexagram changed = HexagramEngine.lookup(lines, true);
            List<String> moving = HexagramEngine.movingLineLabels(lines);
            float cx = w / 2f;

            text(c, String.format(Locale.CHINA, "%02d", base.number), dp(21), dp(111), 10, RED, Paint.Align.LEFT, true);
            text(c, base.name, dp(48), dp(112), 18, FG, Paint.Align.LEFT, true);
            text(c, "本卦", w-dp(22), dp(109), 8.5f, GOLD, Paint.Align.RIGHT, true);
            line(c, dp(20), dp(123), w-dp(20), dp(123), GRID, 1);

            // 主卦使用更接近小尺寸 LCD 的大号六爻排版。
            float stackCx = cx - dp(39);
            drawStack(c, stackCx, dp(315), lines, 6, false, dp(63), dp(33));
            text(c, "上" + base.upper + " / 下" + base.lower, stackCx, dp(345), 9.5f, MUTED, Paint.Align.CENTER, true);

            // 之卦缩在右侧，模拟掌上设备中“一屏看本卦，旁注之卦”的布局。
            text(c, "之", w-dp(62), dp(166), 9, MUTED, Paint.Align.CENTER, true);
            drawMiniStack(c, w-dp(62), dp(226), changedLines());
            text(c, String.format(Locale.CHINA, "%02d", changed.number), w-dp(62), dp(280), 8, RED, Paint.Align.CENTER, true);
            text(c, changed.name, w-dp(62), dp(299), 9.5f, FG, Paint.Align.CENTER, true);

            String movingText = moving.isEmpty() ? "静卦 / NO CHANGE" : "动爻 / " + join(moving, " · ");
            text(c, movingText, dp(22), dp(382), 9.5f, moving.isEmpty() ? MUTED : RED, Paint.Align.LEFT, true);
            line(c, dp(20), dp(396), w-dp(20), dp(396), GRID, 1);

            text(c, "KEY MAP", dp(22), dp(421), 7.5f, MUTED, Paint.Align.LEFT, true);
            float y1 = h - dp(154), y2 = h - dp(99), y3 = h - dp(44);
            auxLeftButton.set(dp(20), y1, w/3f-dp(4), y2-dp(7));
            rightButton.set(w/3f+dp(4), y1, w*2f/3f-dp(4), y2-dp(7));
            auxRightButton.set(w*2f/3f+dp(4), y1, w-dp(20), y2-dp(7));
            leftButton.set(dp(20), y2, w/2f-dp(5), y3-dp(7));
            settingsButton.set(w/2f+dp(5), y2, w-dp(20), y3-dp(7));
            primaryButton.set(dp(20), y3, w-dp(20), h-dp(8));
            button(c, auxLeftButton, "经文", GOLD, false, 11);
            button(c, rightButton, "解卦", GOLD, true, 11);
            button(c, auxRightButton, "历史", MUTED, false, 11);
            button(c, leftButton, "复制", FG, false, 10);
            button(c, settingsButton, "设置", MUTED, false, 10);
            button(c, primaryButton, "再起一卦 / RECAST", MUTED, false, 9);
        }

        private int[] changedLines() {
            int[] out = new int[6];
            for (int i = 0; i < 6; i++) {
                int v = lines[i];
                if (v == 6) out[i] = 7;
                else if (v == 9) out[i] = 8;
                else out[i] = v;
            }
            return out;
        }

        private void drawDetail(Canvas c, float w, float h) {
            backButton.set(w - dp(78), dp(86), w - dp(20), dp(116));
            button(c, backButton, "返回", MUTED, false, 9);
            text(c, "经文", dp(20), dp(108), 15, GOLD, Paint.Align.LEFT, true);
            c.save();
            c.clipRect(0, dp(126), w, h);
            float y = dp(151) - scrollOffset;
            HexagramEngine.Hexagram base = HexagramEngine.lookup(lines, false);
            HexagramEngine.Hexagram changed = HexagramEngine.lookup(lines, true);
            ZhouYiRepository.TextEntry baseText = zhouYi.get(base.name);
            ZhouYiRepository.TextEntry changedText = zhouYi.get(changed.name);

            y = detailTitle(c, "本卦 · 第" + base.number + "卦 · " + base.name, y, GOLD, w);
            y = wrapped(c, "卦辞：" + baseText.guaCi, dp(22), y, w - dp(44), 12, FG, dp(21), false) + dp(15);
            text(c, "爻辞 / 自下而上", dp(22), y, 10, MUTED, Paint.Align.LEFT, true); y += dp(25);
            for (int i = 0; i < 6; i++) {
                boolean yang = HexagramEngine.isYang(lines[i]);
                String label = ZhouYiRepository.traditionalLineLabel(i, yang);
                boolean moving = HexagramEngine.isMoving(lines[i]);
                int color = moving ? RED : FG;
                String prefix = moving ? "● " : "  ";
                y = wrapped(c, prefix + label + "：" + zhouYi.lineText(base.name, i, yang), dp(22), y,
                        w - dp(44), 11, color, dp(19), moving) + dp(9);
            }
            if (baseText.yao.containsKey("用九")) {
                y = wrapped(c, "  用九：" + baseText.yao.get("用九"), dp(22), y,
                        w - dp(44), 11, FG, dp(19), false) + dp(9);
            }
            if (baseText.yao.containsKey("用六")) {
                y = wrapped(c, "  用六：" + baseText.yao.get("用六"), dp(22), y,
                        w - dp(44), 11, FG, dp(19), false) + dp(9);
            }
            y += dp(12);
            y = detailTitle(c, "之卦 · 第" + changed.number + "卦 · " + changed.name, y, GOLD, w);
            y = wrapped(c, "卦辞：" + changedText.guaCi, dp(22), y, w - dp(44), 12, FG, dp(21), false) + dp(20);
            y = wrapped(c, "注：经文用于传统文化阅读与娱乐参考，不代表对现实结果的确定预测。", dp(22), y,
                    w - dp(44), 9.5f, MUTED, dp(17), false) + dp(40);
            maxScroll = Math.max(0, y + scrollOffset - h + dp(30));
            c.restore();
        }

        private float detailTitle(Canvas c, String s, float y, int color, float w) {
            text(c, s, dp(22), y, 14, color, Paint.Align.LEFT, true);
            line(c, dp(22), y + dp(10), w - dp(22), y + dp(10), GRID, 1);
            return y + dp(35);
        }

        private void drawHistory(Canvas c, float w, float h) {
            backButton.set(w - dp(78), dp(86), w - dp(20), dp(116));
            button(c, backButton, "返回", MUTED, false, 9);
            clearButton.set(w - dp(158), dp(86), w - dp(88), dp(116));
            button(c, clearButton, "清空未固定", RED, false, 7.8f);
            text(c, "历史", dp(20), dp(108), 15, GOLD, Paint.Align.LEFT, true);

            List<HistoryStore.Entry> entries = HistoryStore.load(getContext());
            historyHits.clear();
            c.save(); c.clipRect(0, dp(126), w, h);
            float y = dp(143) - scrollOffset;
            if (entries.isEmpty()) {
                text(c, "尚无卦象", w / 2f, dp(240), 13, MUTED, Paint.Align.CENTER, false);
                maxScroll = 0;
                c.restore(); return;
            }
            SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);
            for (int i = 0; i < entries.size(); i++) {
                HistoryStore.Entry e = entries.get(i);
                HexagramEngine.Hexagram base = HexagramEngine.lookup(e.lines, false);
                HexagramEngine.Hexagram changed = HexagramEngine.lookup(e.lines, true);
                RectF card = new RectF(dp(20), y, w - dp(20), y + dp(116));
                panel(c, card);
                String timeLine = fmt.format(new Date(e.timeMillis)) + " · " + (e.formal ? "正式起卦" : "普通起卦");
                text(c, timeLine, card.left + dp(12), card.top + dp(18), 8.2f, e.formal ? GOLD : MUTED, Paint.Align.LEFT, false);
                text(c, base.name, card.left + dp(12), card.top + dp(44), 13, FG, Paint.Align.LEFT, true);
                String moving = HexagramEngine.movingLineLabels(e.lines).isEmpty() ? "静卦" : "→ " + changed.name;
                text(c, moving, card.left + dp(12), card.top + dp(65), 9.3f, moving.equals("静卦") ? MUTED : RED, Paint.Align.LEFT, false);
                String badges = (e.hasAi() ? "AI 已保存" : "") + (e.hasAi() && e.hasNote() ? " · " : "") + (e.hasNote() ? "有备注" : "");
                if (!badges.isEmpty()) text(c, badges, card.left + dp(12), card.top + dp(86), 8.5f, e.hasAi() ? GOLD : MUTED, Paint.Align.LEFT, true);
                if (e.hasNote()) {
                    String note = e.note.replace('\n', ' ').trim();
                    if (note.length() > 18) note = note.substring(0, 18) + "…";
                    text(c, "备注：" + note, card.left + dp(12), card.top + dp(105), 8.2f, MUTED, Paint.Align.LEFT, false);
                }

                RectF pinRect = new RectF(card.right - dp(119), card.top + dp(8), card.right - dp(68), card.top + dp(34));
                RectF noteRect = new RectF(card.right - dp(119), card.top + dp(40), card.right - dp(68), card.top + dp(66));
                button(c, pinRect, e.pinned ? "已固定" : "固定", e.pinned ? GOLD : MUTED, false, 7.5f);
                button(c, noteRect, "备注", MUTED, false, 7.5f);
                drawMiniStack(c, card.right - dp(35), card.centerY() + dp(4), e.lines);
                historyHits.add(new HistoryHit(new RectF(card), pinRect, noteRect, e));
                y += dp(128);
            }
            maxScroll = Math.max(0, y + scrollOffset - h + dp(20));
            c.restore();
        }

        private void loadHistoryEntry(HistoryStore.Entry entry) {
            if (entry == null) return;
            System.arraycopy(entry.lines, 0, lines, 0, 6);
            loadedFromHistory = true;
            currentHistoryId = entry.id;
            aiText = entry.aiText;
            aiReasoning = entry.aiReasoning;
            aiReasoningSummaryOnly = entry.aiReasoningSummaryOnly;
            aiReasoningExpanded = false;
            aiModel = entry.aiModel;
            aiError = "";
            aiLoading = false;
            state = State.RESULT;
            scrollOffset = 0;
            haptic(HapticFeedbackConstants.CLOCK_TICK);
            postInvalidateOnAnimation();
        }

        private void showHistoryNoteDialog(HistoryStore.Entry entry) {
            if (entry == null) return;
            final EditText input = new EditText(getContext());
            input.setHint("给这次起卦写备注（最多 300 字）");
            input.setText(entry.note);
            input.setSelection(input.getText().length());
            input.setMinLines(2);
            input.setMaxLines(5);
            new AlertDialog.Builder(getContext())
                    .setTitle(entry.pinned ? "固定记录 · 备注" : "历史备注")
                    .setView(input)
                    .setPositiveButton("保存", (d, which) -> {
                        String note = input.getText().toString();
                        if (note.length() > 300) note = note.substring(0, 300);
                        HistoryStore.updateNote(getContext(), entry.id, note);
                        postInvalidateOnAnimation();
                    })
                    .setNeutralButton(entry.note.isEmpty() ? null : "清除备注", (d, which) -> {
                        HistoryStore.updateNote(getContext(), entry.id, "");
                        postInvalidateOnAnimation();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }


        private void drawOffline(Canvas c, float w, float h) {
            backButton.set(w - dp(78), dp(86), w - dp(20), dp(116));
            button(c, backButton, "返回", MUTED, false, 9);
            text(c, "离线解卦", dp(20), dp(108), 15, GOLD, Paint.Align.LEFT, true);
            text(c, "OFFLINE / LOCAL RULES", dp(20), dp(124), 7.5f, MUTED, Paint.Align.LEFT, true);

            c.save();
            c.clipRect(0, dp(136), w, h - dp(82));
            float y = dp(160) - scrollOffset;
            y = wrapped(c, offlineReadingText(), dp(22), y, w - dp(44), 11.2f, FG, dp(20), false) + dp(24);
            maxScroll = Math.max(0, y + scrollOffset - (h - dp(82)) + dp(20));
            c.restore();

            auxLeftButton.set(dp(20), h - dp(69), w / 2f - dp(5), h - dp(15));
            auxRightButton.set(w / 2f + dp(5), h - dp(69), w - dp(20), h - dp(15));
            button(c, auxLeftButton, "复制离线解读", FG, false, 9.5f);
            boolean hasSavedAi = aiText != null && !aiText.trim().isEmpty()
                    && currentHistoryId != null && !currentHistoryId.isEmpty();
            button(c, auxRightButton, hasSavedAi ? "查看 AI 解卦" : "AI 解卦", GOLD, true, 10.5f);
        }

        private String offlineReadingText() {
            return OfflineInterpreter.interpret(lines, zhouYi);
        }

        private void drawAi(Canvas c, float w, float h) {
            backButton.set(w - dp(78), dp(86), w - dp(20), dp(116));
            clearButton.set(w - dp(148), dp(86), w - dp(88), dp(116));
            button(c, backButton, "返回", MUTED, false, 9);
            button(c, clearButton, "设置", GOLD, false, 9);
            text(c, "AI 解卦", dp(20), dp(108), 15, GOLD, Paint.Align.LEFT, true);

            c.save();
            c.clipRect(0, dp(126), w, h - dp(82));
            float y = dp(151) - scrollOffset;
            String statusTitle;
            if (aiLoading) {
                if (!aiText.isEmpty()) statusTitle = "AI 正在解卦 · 流式输出";
                else if (!aiReasoning.isEmpty()) statusTitle = "AI 正在思考…";
                else statusTitle = "正在连接 AI……";
                text(c, statusTitle, dp(22), y, 13, GOLD, Paint.Align.LEFT, true);
                y += dp(27);
            }
            text(c, "模型：" + aiModel, dp(22), y, 9.5f, MUTED, Paint.Align.LEFT, false);
            y += dp(27);

            y = drawAiReasoning(c, w, y);

            if (aiLoading) {
                if (aiText.isEmpty()) {
                    String waiting = aiReasoning.isEmpty()
                            ? "正在等待首段内容。请求会直接从此设备发往你设置的 API 服务。"
                            : "思考过程已收到，正在等待最终解读。思考默认折叠，可点击上方查看。";
                    y = wrapped(c, waiting, dp(22), y, w - dp(44), 10, MUTED, dp(18), false) + dp(20);
                } else {
                    y = wrapped(c, aiText, dp(22), y, w - dp(44), 11.5f, FG, dp(21), false) + dp(20);
                    y = wrapped(c, "最终解读仍在生成中…", dp(22), y, w - dp(44), 9.2f, MUTED, dp(17), false) + dp(18);
                }
            } else if (!aiError.isEmpty()) {
                text(c, "请求失败", dp(22), y, 13, RED, Paint.Align.LEFT, true);
                y += dp(28);
                y = wrapped(c, aiError, dp(22), y, w - dp(44), 11, FG, dp(20), false) + dp(20);
                y = wrapped(c, "可检查 API Key、模型 ID、接口模式与余额/额度后重试。", dp(22), y,
                        w - dp(44), 9.5f, MUTED, dp(18), false) + dp(20);
            } else {
                y = wrapped(c, aiText.isEmpty() ? "暂无解读" : aiText, dp(22), y,
                        w - dp(44), 11.5f, FG, dp(21), false) + dp(24);
                y = wrapped(c, "AI 解读与卦象均只作传统文化与娱乐参考，不构成医疗、法律、投资等专业建议。", dp(22), y,
                        w - dp(44), 9.2f, MUTED, dp(17), false) + dp(24);
            }
            maxScroll = Math.max(0, y + scrollOffset - (h - dp(82)) + dp(20));
            c.restore();

            auxLeftButton.set(dp(20), h - dp(69), w / 2f - dp(5), h - dp(15));
            auxRightButton.set(w / 2f + dp(5), h - dp(69), w - dp(20), h - dp(15));
            button(c, auxLeftButton, "复制解读", FG, false, 10.5f);
            button(c, auxRightButton, aiLoading ? "请求中…" : "重新解卦", GOLD, true, 10.5f);
        }

        private float drawAiReasoning(Canvas c, float w, float y) {
            if (aiReasoning == null || aiReasoning.trim().isEmpty()) {
                reasoningToggleButton.setEmpty();
                return y;
            }
            String kind = aiReasoningSummaryOnly ? "思考过程（摘要）" : "思考过程";
            reasoningToggleButton.set(dp(22), y - dp(4), w - dp(22), y + dp(30));
            button(c, reasoningToggleButton, kind + (aiReasoningExpanded ? "  ▾" : "  ▸"), MUTED, false, 9.2f);
            y += dp(43);
            if (aiReasoningExpanded) {
                y = wrapped(c, aiReasoning, dp(26), y, w - dp(52), 9.5f, MUTED, dp(18), false) + dp(17);
            }
            return y;
        }

        private void drawMiniStack(Canvas c, float cx, float cy, int[] source) {
            for (int i = 0; i < 6; i++) {
                float y = cy + dp(25) - i * dp(10);
                boolean yang = HexagramEngine.isYang(source[i]);
                int color = HexagramEngine.isMoving(source[i]) ? RED : FG;
                if (yang) line(c, cx - dp(24), y, cx + dp(24), y, color, 2.5f);
                else {
                    line(c, cx - dp(24), y, cx - dp(5), y, color, 2.5f);
                    line(c, cx + dp(5), y, cx + dp(24), y, color, 2.5f);
                }
            }
        }

        private void drawStack(Canvas c, float cx, float bottomY, int[] source, int count, boolean changed, float halfWidth, float step) {
            for (int i = 0; i < 6; i++) {
                float y = bottomY - i * step;
                if (i >= count) { line(c, cx - halfWidth, y, cx + halfWidth, y, GRID, 5); continue; }
                int v = source[i];
                boolean yang = changed ? HexagramEngine.changedYang(v) : HexagramEngine.isYang(v);
                boolean moving = HexagramEngine.isMoving(v) && !changed;
                int color = moving ? RED : FG;
                if (yang) line(c, cx - halfWidth, y, cx + halfWidth, y, color, 6);
                else {
                    float g = dp(12);
                    line(c, cx - halfWidth, y, cx - g, y, color, 6);
                    line(c, cx + g, y, cx + halfWidth, y, color, 6);
                }
                if (moving) text(c, "×", cx + halfWidth + dp(10), y + dp(4), 10, RED, Paint.Align.CENTER, true);
            }
        }

        private void button(Canvas c, RectF r, String label, int color, boolean filled, float textSize) {
            paint.setStrokeWidth(dp(1.2f)); paint.setColor(color); paint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
            if (filled) {
                paint.setColor(Color.rgb(39, 34, 23)); c.drawRoundRect(r, dp(4), dp(4), paint);
                paint.setStyle(Paint.Style.STROKE); paint.setColor(color); c.drawRoundRect(r, dp(4), dp(4), paint);
            } else c.drawRoundRect(r, dp(4), dp(4), paint);
            text(c, label, r.centerX(), r.centerY() + dp(4), textSize, color, Paint.Align.CENTER, true);
        }

        private float wrapped(Canvas c, String s, float x, float y, float maxWidth, float size, int color, float lineHeight, boolean bold) {
            paint.setTextSize(dp(size)); paint.setTypeface(bold ? monoBold : mono);
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\n') {
                    text(c, row.toString(), x, y, size, color, Paint.Align.LEFT, bold); row.setLength(0); y += lineHeight; continue;
                }
                String candidate = row.toString() + ch;
                if (paint.measureText(candidate) > maxWidth && row.length() > 0) {
                    text(c, row.toString(), x, y, size, color, Paint.Align.LEFT, bold);
                    row.setLength(0); row.append(ch); y += lineHeight;
                } else row.append(ch);
            }
            if (row.length() > 0) text(c, row.toString(), x, y, size, color, Paint.Align.LEFT, bold);
            return y + lineHeight;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX() - safeInsetLeft, y = e.getY() - safeInsetTop;
            if ((state == State.DETAIL || state == State.HISTORY || state == State.OFFLINE || state == State.AI)) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    downY = lastY = y; dragging = false; return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    float dy = y - lastY;
                    if (Math.abs(y - downY) > dp(5)) dragging = true;
                    if (dragging) {
                        scrollOffset = clamp(scrollOffset - dy, 0, maxScroll);
                        postInvalidateOnAnimation();
                    }
                    lastY = y; return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (!dragging && backButton.contains(x, y)) {
                        if (state == State.DETAIL || state == State.OFFLINE) state = State.RESULT;
                        else if (state == State.AI) state = State.OFFLINE;
                        else state = State.IDLE;
                        scrollOffset = 0; postInvalidateOnAnimation(); return true;
                    }
                    if (!dragging && state == State.OFFLINE && auxLeftButton.contains(x, y)) {
                        copyOfflineReading(); return true;
                    }
                    if (!dragging && state == State.OFFLINE && auxRightButton.contains(x, y)) {
                        startAiOrConfigure(); return true;
                    }
                    if (!dragging && state == State.AI && reasoningToggleButton.contains(x, y) && !aiReasoning.isEmpty()) {
                        aiReasoningExpanded = !aiReasoningExpanded;
                        haptic(HapticFeedbackConstants.CLOCK_TICK);
                        postInvalidateOnAnimation();
                        return true;
                    }
                    if (!dragging && state == State.AI && clearButton.contains(x, y)) {
                        showSettingsDialog(false); return true;
                    }
                    if (!dragging && state == State.AI && auxLeftButton.contains(x, y)) {
                        copyAiResult(); return true;
                    }
                    if (!dragging && state == State.AI && auxRightButton.contains(x, y)) {
                        if (!aiLoading) rerunAiReading();
                        return true;
                    }
                    if (!dragging && state == State.HISTORY && clearButton.contains(x, y)) {
                        HistoryStore.clearUnpinned(getContext()); scrollOffset = 0;
                        Toast.makeText(getContext(), "未固定历史已清空", Toast.LENGTH_SHORT).show(); postInvalidateOnAnimation(); return true;
                    }
                    if (!dragging && state == State.HISTORY) {
                        for (HistoryHit hit : historyHits) {
                            if (hit.pinRect.contains(x, y)) {
                                boolean pinned = HistoryStore.togglePin(getContext(), hit.entry.id);
                                Toast.makeText(getContext(), pinned ? "已固定 · 不再自动删除" : "已取消固定", Toast.LENGTH_SHORT).show();
                                postInvalidateOnAnimation(); return true;
                            }
                            if (hit.noteRect.contains(x, y)) {
                                showHistoryNoteDialog(hit.entry); return true;
                            }
                            if (hit.rect.contains(x, y)) {
                                loadHistoryEntry(HistoryStore.find(getContext(), hit.entry.id));
                                return true;
                            }
                        }
                    }
                    return true;
                }
                return true;
            }

            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            if (state == State.IDLE) {
                if (primaryButton.contains(x, y)) { haptic(HapticFeedbackConstants.CONFIRM); pulse(22, 120); startCasting(); return true; }
                if (experienceButton.contains(x, y)) { showSettingsDialog(false); return true; }
                if (historyButton.contains(x, y)) { state = State.HISTORY; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            }
            if (state == State.CASTING && manualCasting) {
                if (primaryButton.contains(x, y) && !lineAnimating && castCount < 6) {
                    if (formalCastingActive && !formalAwaitingManual) {
                        Toast.makeText(getContext(), "正式起卦 · 请等待定时提示", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (formalCastingActive) formalAwaitingManual = false;
                    haptic(HapticFeedbackConstants.CLOCK_TICK);
                    pulse(15, 90);
                    castNext();
                }
                return true;
            }
            if (state == State.RESULT) {
                if (auxLeftButton.contains(x, y)) { pulse(10, 60); state = State.DETAIL; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
                if (auxRightButton.contains(x, y)) { pulse(10, 60); state = State.HISTORY; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
                if (leftButton.contains(x, y)) { copyResult(); return true; }
                if (rightButton.contains(x, y)) { state = State.OFFLINE; scrollOffset = 0; haptic(HapticFeedbackConstants.CONFIRM); postInvalidateOnAnimation(); return true; }
                if (settingsButton.contains(x, y)) { showSettingsDialog(false); return true; }
                if (primaryButton.contains(x, y)) { pulse(15, 75); state = State.IDLE; loadedFromHistory = false; postInvalidateOnAnimation(); return true; }
            }
            return true;
        }

        private void startCasting() {
            handler.removeCallbacksAndMessages(null);
            formalCastingActive = false;
            formalAwaitingManual = false;
            formalStartEpoch = 0L;
            Arrays.fill(lines, 0);
            castCount = 0;
            toastLine = "";
            coinPhase = 0f;
            lineAnimating = false;
            physicsActive = false;
            loadedFromHistory = false;
            currentHistoryId = "";
            aiText = ""; aiReasoning = ""; aiError = ""; aiModel = "";
            state = State.CASTING;
            postInvalidateOnAnimation();
            castNext();
        }

        private void startFormalCasting() {
            handler.removeCallbacksAndMessages(null);
            Arrays.fill(lines, 0);
            castCount = 0;
            toastLine = "";
            coinPhase = 0f;
            lineAnimating = false;
            physicsActive = false;
            loadedFromHistory = false;
            currentHistoryId = "";
            aiText = ""; aiReasoning = ""; aiError = ""; aiModel = "";
            formalCastingActive = true;
            formalAwaitingManual = false;
            long now = System.currentTimeMillis();
            formalStartEpoch = ((now / 60000L) + 1L) * 60000L;
            state = State.CASTING;
            toastLine = "正式起卦 · 首爻 " + formatClock(formalStartEpoch);
            postInvalidateOnAnimation();
            handler.post(formalClockTicker);
            for (int i = 0; i < 6; i++) {
                final int index = i;
                final long target = formalStartEpoch + i * 10000L;
                handler.postDelayed(() -> onFormalCastTime(index, target), Math.max(0L, target - System.currentTimeMillis()));
            }
        }

        private void onFormalCastTime(int index, long target) {
            if (!formalCastingActive || state != State.CASTING || castCount > index) return;
            if (castCount < index || lineAnimating) {
                handler.postDelayed(() -> onFormalCastTime(index, target), 120L);
                return;
            }
            if (manualCasting) {
                formalAwaitingManual = true;
                toastLine = "第 " + (index + 1) + " 爻 · 请掷爻 · 3秒后自动";
                pulse(24, 135);
                haptic(HapticFeedbackConstants.CONFIRM);
                postInvalidateOnAnimation();
                handler.postDelayed(() -> {
                    if (!formalCastingActive || state != State.CASTING || !formalAwaitingManual) return;
                    if (castCount != index || lineAnimating) return;
                    formalAwaitingManual = false;
                    toastLine = "第 " + (index + 1) + " 爻 · 自动补掷";
                    castNext();
                }, 3000L);
            } else {
                toastLine = "第 " + (index + 1) + " 爻 · " + formatClock(target);
                castNext();
            }
        }

        private void cancelFormalCasting() {
            formalCastingActive = false;
            formalAwaitingManual = false;
            formalStartEpoch = 0L;
            handler.removeCallbacksAndMessages(null);
        }

        private String formatClock(long epochMs) {
            return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(epochMs));
        }

        private void castNext() {
            if (state != State.CASTING || lineAnimating) return;
            if (castCount >= 6) {
                finishCasting();
                return;
            }
            if (ANIM_PHYSICS.equals(coinAnimationMode)) castNextPhysics();
            else castNextClassic();
        }

        private void castNextClassic() {
            lineAnimating = true;
            final long started = SystemClock.uptimeMillis();
            final int[] lastBucket = {-1};
            Runnable flip = new Runnable() {
                @Override public void run() {
                    float t = Math.min(1f, (SystemClock.uptimeMillis() - started) / 1080f);
                    coinPhase = t * (float) (Math.PI * 8.0);
                    int bucket = Math.min(11, (int) (t * 12f));
                    if (bucket != lastBucket[0]) {
                        for (int i = 0; i < 3; i++) currentCoins[i] = Math.random() > 0.5 ? "字" : "背";
                        if ((bucket == 1 || bucket == 5 || bucket == 9) && soundEnabled) audio.coin();
                        if (bucket == 6) pulse(10, 65);
                        lastBucket[0] = bucket;
                    }
                    postInvalidateOnAnimation();
                    if (t < 1f) postOnAnimation(this); else settleLine(HexagramEngine.castLine());
                }
            };
            postOnAnimation(flip);
        }

        private void castNextPhysics() {
            lineAnimating = true;
            physicsActive = true;
            pendingPhysicsLine = HexagramEngine.castLine();
            String[] finalFaces = facesForValue(pendingPhysicsLine);
            float cx = getWidth() / 2f;
            float startY = Math.max(dp(285), getHeight() - safeInsetBottom - dp(84));
            float targetY = dp(177);
            float[] vxDp = {-315f, 22f, 300f};
            float[] vyDp = {-1040f, -1120f, -1000f};
            float[] angular = {11.8f, -14.2f, 13.1f};
            for (int i = 0; i < physicsCoins.length; i++) {
                CoinBody b = physicsCoins[i];
                b.x = cx + dp((i - 1) * 5f);
                b.y = startY + dp(i == 1 ? -3f : 2f);
                b.vx = dp(vxDp[i] + (float) (Math.random() * 44f - 22f));
                b.vy = dp(vyDp[i] + (float) (Math.random() * 80f - 40f));
                b.targetX = cx + dp((i - 1) * 72f);
                b.targetY = targetY;
                b.angle = (float) (Math.random() * Math.PI * 2.0);
                b.angular = angular[i] + (float) (Math.random() * 2.2f - 1.1f);
                b.scale = 0.78f;
                b.targetFace = finalFaces[i];
            }
            physicsStartedAt = SystemClock.uptimeMillis();
            physicsLastFrameAt = physicsStartedAt;
            physicsSoundMask = 0;
            if (soundEnabled) { audio.coin(); handler.postDelayed(audio::coin, 95L); }
            pulse(16, 90);
            postOnAnimation(this::stepPhysics);
        }

        private void stepPhysics() {
            if (!physicsActive || state != State.CASTING) return;
            long now = SystemClock.uptimeMillis();
            float elapsed = (now - physicsStartedAt) / 1000f;
            float dt = Math.min(0.032f, Math.max(0.008f, (now - physicsLastFrameAt) / 1000f));
            physicsLastFrameAt = now;

            boolean springPhase = elapsed > 0.52f;
            for (CoinBody b : physicsCoins) {
                if (!springPhase) {
                    b.vy += dp(1850f) * dt;
                    b.vx *= (float) Math.pow(0.985, dt * 60f);
                } else {
                    float k = 22f;
                    float damp = 8.0f;
                    float ax = (b.targetX - b.x) * k - b.vx * damp;
                    float ay = (b.targetY - b.y) * k - b.vy * damp;
                    b.vx += ax * dt;
                    b.vy += ay * dt;
                }
                b.x += b.vx * dt;
                b.y += b.vy * dt;
                b.angle += b.angular * dt;
                b.angular *= (float) Math.pow(springPhase ? 0.955 : 0.992, dt * 60f);
                float pop = Math.min(1f, elapsed / 0.22f);
                b.scale = springPhase ? 1f + 0.08f * (float) Math.exp(-(elapsed - 0.52f) * 5f) : 0.78f + 0.34f * pop;
            }
            resolveCoinCollisions();

            if (soundEnabled && elapsed > 0.42f && (physicsSoundMask & 1) == 0) { audio.coin(); physicsSoundMask |= 1; }
            if (soundEnabled && elapsed > 0.72f && (physicsSoundMask & 2) == 0) { audio.coin(); physicsSoundMask |= 2; }
            postInvalidateOnAnimation();
            if (elapsed < 1.08f) {
                postOnAnimation(this::stepPhysics);
            } else {
                for (CoinBody b : physicsCoins) { b.x = b.targetX; b.y = b.targetY; b.scale = 1f; }
                physicsActive = false;
                settleLine(pendingPhysicsLine);
            }
        }

        private void resolveCoinCollisions() {
            float minDist = dp(44);
            for (int i = 0; i < physicsCoins.length; i++) {
                for (int j = i + 1; j < physicsCoins.length; j++) {
                    CoinBody a = physicsCoins[i], b = physicsCoins[j];
                    float dx = b.x - a.x, dy = b.y - a.y;
                    float d2 = dx * dx + dy * dy;
                    if (d2 <= 1f || d2 >= minDist * minDist) continue;
                    float d = (float) Math.sqrt(d2);
                    float nx = dx / d, ny = dy / d;
                    float push = (minDist - d) * 0.5f;
                    a.x -= nx * push; a.y -= ny * push;
                    b.x += nx * push; b.y += ny * push;
                    float rel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
                    if (rel < 0) {
                        float impulse = -rel * 0.34f;
                        a.vx -= nx * impulse; a.vy -= ny * impulse;
                        b.vx += nx * impulse; b.vy += ny * impulse;
                    }
                }
            }
        }

        private void settleLine(int value) {
            setCoinFacesForValue(value);
            lines[castCount] = value;
            toastLine = HexagramEngine.lineText(value);
            if (soundEnabled) audio.settle();
            pulse(HexagramEngine.isMoving(value) ? 30 : 17, HexagramEngine.isMoving(value) ? 155 : 95);
            haptic(HapticFeedbackConstants.CLOCK_TICK);
            castCount++;
            lineAnimating = false;
            physicsActive = false;
            if (formalCastingActive && castCount < 6) {
                long nextAt = formalStartEpoch + castCount * 10000L;
                toastLine = HexagramEngine.lineText(value) + " · 待 " + formatClock(nextAt);
            }
            postInvalidateOnAnimation();
            if (castCount >= 6) {
                handler.postDelayed(this::finishCasting, manualCasting ? 300L : 240L);
            } else if (!manualCasting && !formalCastingActive) {
                handler.postDelayed(this::castNext, 300L);
            }
        }

        private void finishCasting() {
            if (state != State.CASTING) return;
            boolean wasFormal = formalCastingActive;
            formalCastingActive = false;
            formalAwaitingManual = false;
            handler.removeCallbacks(formalClockTicker);
            state = State.RESULT;
            HistoryStore.Entry savedEntry = HistoryStore.add(getContext(), lines, wasFormal);
            currentHistoryId = savedEntry.id;
            if (soundEnabled) audio.complete();
            ritualPulse();
            haptic(HapticFeedbackConstants.CONFIRM);
            postInvalidateOnAnimation();
        }

        private String[] facesForValue(int value) {
            if (value == 6) return new String[]{"字", "字", "字"};
            if (value == 7) return new String[]{"背", "字", "字"};
            if (value == 8) return new String[]{"背", "背", "字"};
            return new String[]{"背", "背", "背"};
        }

        private void setCoinFacesForValue(int value) {
            // 约定“字=2、背=3”，总数 6/7/8/9 对应老阴/少阳/少阴/老阳。
            if (value == 6) { currentCoins[0] = "字"; currentCoins[1] = "字"; currentCoins[2] = "字"; }
            else if (value == 7) { currentCoins[0] = "背"; currentCoins[1] = "字"; currentCoins[2] = "字"; }
            else if (value == 8) { currentCoins[0] = "背"; currentCoins[1] = "背"; currentCoins[2] = "字"; }
            else { currentCoins[0] = "背"; currentCoins[1] = "背"; currentCoins[2] = "背"; }
        }
        private String resultText() {
            HexagramEngine.Hexagram base = HexagramEngine.lookup(lines, false);
            HexagramEngine.Hexagram changed = HexagramEngine.lookup(lines, true);
            List<String> moving = HexagramEngine.movingLineLabels(lines);
            ZhouYiRepository.TextEntry text = zhouYi.get(base.name);
            StringBuilder sb = new StringBuilder();
            sb.append("【柳之卦】\n");
            sb.append("本卦：").append(base.compact()).append("（上").append(base.upper).append("下").append(base.lower).append("）\n");
            sb.append("卦辞：").append(text.guaCi).append("\n");
            sb.append("变卦：").append(changed.compact()).append("\n");
            sb.append("动爻：").append(moving.isEmpty() ? "无" : join(moving, "、")).append("\n");
            if (!moving.isEmpty()) {
                for (int i = 0; i < 6; i++) if (HexagramEngine.isMoving(lines[i])) {
                    boolean yang = HexagramEngine.isYang(lines[i]);
                    sb.append(ZhouYiRepository.traditionalLineLabel(i, yang)).append("：")
                            .append(zhouYi.lineText(base.name, i, yang)).append("\n");
                }
            }
            sb.append("六爻（自下而上）：");
            for (int i = 0; i < 6; i++) { if (i > 0) sb.append(" / "); sb.append(lines[i]); }
            return sb.toString();
        }

        private String aiPrompt() {
            return resultText() + "\n\n【本机离线解卦】\n" + offlineReadingText()
                    + "\n\n请以上述卦象事实与本机离线取用为基础生成最终解读，不要重新计算卦象，也不要复述全部原始数据。"
                    + "按“卦意 / 动爻（如有） / 变卦 / 建议”四部分输出；全文控制在300至450个中文字符，最多不超过500个中文字符。"
                    + "最终回答中不要包含思考过程、分析草稿、reasoning、thinking 或 <think> 标签。";
        }

        private void copyResult() {
            ClipboardManager cb = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("柳之卦结果", resultText()));
            haptic(HapticFeedbackConstants.CLOCK_TICK);
            Toast.makeText(getContext(), "卦象已复制", Toast.LENGTH_SHORT).show();
        }


        private void copyOfflineReading() {
            ClipboardManager cb = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("柳之卦离线解读", offlineReadingText()));
            haptic(HapticFeedbackConstants.CLOCK_TICK);
            Toast.makeText(getContext(), "离线解读已复制", Toast.LENGTH_SHORT).show();
        }

        private void startAiOrConfigure() {
            if (currentHistoryId != null && !currentHistoryId.isEmpty()) {
                HistoryStore.Entry saved = HistoryStore.find(getContext(), currentHistoryId);
                if (saved != null && saved.hasAi()) {
                    aiText = saved.aiText;
                    aiReasoning = saved.aiReasoning;
                    aiReasoningSummaryOnly = saved.aiReasoningSummaryOnly;
                    aiReasoningExpanded = false;
                    aiModel = saved.aiModel;
                    aiError = "";
                    aiLoading = false;
                    state = State.AI;
                    scrollOffset = 0;
                    postInvalidateOnAnimation();
                    return;
                }
            }
            rerunAiReading();
        }

        private void rerunAiReading() {
            AiSettingsStore.Settings settings = AiSettingsStore.load(getContext());
            if (!settings.isConfigured()) {
                showSettingsDialog(true);
                return;
            }
            requestAiReading(settings);
        }

        private void requestAiReading(AiSettingsStore.Settings settings) {
            if (aiLoading) return;
            aiLoading = true;
            aiError = "";
            aiText = "";
            aiReasoning = "";
            aiReasoningExpanded = false;
            aiReasoningSummaryOnly = true;
            reasoningToggleButton.setEmpty();
            aiModel = settings.model;
            state = State.AI;
            scrollOffset = 0;
            haptic(HapticFeedbackConstants.CONFIRM);
            postInvalidateOnAnimation();

            new Thread(() -> {
                final StringBuilder streamedAnswer = new StringBuilder();
                final StringBuilder streamedReasoning = new StringBuilder();
                try {
                    String text = AiClient.interpretStream(settings, aiPrompt(), new AiClient.StreamListener() {
                        @Override public void onAnswerDelta(String delta) {
                            synchronized (streamedAnswer) { appendCapped(streamedAnswer, delta, 12000); }
                            handler.post(() -> {
                                synchronized (streamedAnswer) { aiText = streamedAnswer.toString(); }
                                aiError = "";
                                postInvalidateOnAnimation();
                            });
                        }

                        @Override public void onReasoningDelta(String delta, boolean summaryOnly) {
                            synchronized (streamedReasoning) { appendCapped(streamedReasoning, delta, 12000); }
                            handler.post(() -> {
                                synchronized (streamedReasoning) { aiReasoning = streamedReasoning.toString(); }
                                if (!summaryOnly) aiReasoningSummaryOnly = false;
                                aiError = "";
                                postInvalidateOnAnimation();
                            });
                        }
                    });
                    handler.post(() -> {
                        aiLoading = false;
                        aiText = text;
                        synchronized (streamedReasoning) { aiReasoning = streamedReasoning.toString(); }
                        aiError = "";
                        if (currentHistoryId != null && !currentHistoryId.isEmpty()) {
                            HistoryStore.updateAi(getContext(), currentHistoryId, aiText, aiReasoning, aiReasoningSummaryOnly, aiModel);
                        }
                        haptic(HapticFeedbackConstants.CONFIRM);
                        postInvalidateOnAnimation();
                    });
                } catch (Exception ex) {
                    String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    handler.post(() -> {
                        aiLoading = false;
                        synchronized (streamedAnswer) { if (!streamedAnswer.toString().trim().isEmpty()) aiText = streamedAnswer.toString(); }
                        synchronized (streamedReasoning) { aiReasoning = streamedReasoning.toString(); }
                        aiError = message;
                        postInvalidateOnAnimation();
                    });
                }
            }, "ryus-gua-ai-stream").start();
        }

        private static void appendCapped(StringBuilder target, String delta, int maxChars) {
            if (delta == null || delta.isEmpty() || target.length() >= maxChars) return;
            int room = maxChars - target.length();
            if (delta.length() <= room) target.append(delta);
            else target.append(delta, 0, room).append("\n…思考内容过长，已截断显示");
        }

        private void copyAiResult() {
            if (aiText == null || aiText.trim().isEmpty()) {
                Toast.makeText(getContext(), "当前没有可复制的 AI 解读", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cb = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("柳之卦 AI 解读", aiText));
            haptic(HapticFeedbackConstants.CLOCK_TICK);
            Toast.makeText(getContext(), "AI 解读已复制", Toast.LENGTH_SHORT).show();
        }

        private void showSettingsDialog(boolean startAiAfterSave) {
            if (startAiAfterSave) {
                showAiSettingsPanel(true);
                return;
            }
            Context ctx = getContext();
            AiSettingsStore.Settings saved = AiSettingsStore.load(ctx);
            float density = getResources().getDisplayMetrics().density;

            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding((int)(16*density), (int)(12*density), (int)(16*density), (int)(14*density));
            root.setBackgroundColor(BG);

            TextView heading = new TextView(ctx);
            heading.setText("柳之卦设置"); heading.setTextSize(22); heading.setTextColor(FG);
            heading.setTypeface(Typeface.DEFAULT_BOLD); root.addView(heading);
            TextView sub = new TextView(ctx);
            sub.setText("RYU'S GUA / SETTINGS / v" + BuildConfig.VERSION_NAME); sub.setTextSize(10); sub.setTextColor(MUTED);
            root.addView(sub);
            TextView author = new TextView(ctx);
            author.setText("作者 · Ryyus"); author.setTextSize(9.5f); author.setTextColor(MUTED);
            author.setPadding(0, (int)(2*density), 0, (int)(12*density)); root.addView(author);

            final AlertDialog[] parentDialog = {null};
            LinearLayout interaction = settingsCard(ctx, "交互与动画", interactionSummary());
            root.addView(interaction);
            TextView interactionSummaryView = (TextView) interaction.getChildAt(1);
            interaction.setOnClickListener(v -> showInteractionSettingsDialog(
                    () -> interactionSummaryView.setText(interactionSummary()),
                    () -> { if (parentDialog[0] != null) parentDialog[0].dismiss(); }));

            LinearLayout ai = settingsCard(ctx, "AI 解卦", aiSettingsSummary(AiSettingsStore.load(ctx)));
            root.addView(ai);
            TextView aiSummaryView = (TextView) ai.getChildAt(1);
            ai.setOnClickListener(v -> showAiSettingsPanel(false, () -> aiSummaryView.setText(aiSettingsSummary(AiSettingsStore.load(ctx)))));

            LinearLayout update = settingsCard(ctx, "版本更新", "当前 v" + BuildConfig.VERSION_NAME + " · 点击检查新版本");
            root.addView(update);
            update.setOnClickListener(v -> UpdateChecker.check((Activity) ctx, true));

            AlertDialog dialog = new AlertDialog.Builder(ctx)
                    .setView(root)
                    .setNegativeButton("关闭", null)
                    .create();
            parentDialog[0] = dialog;
            dialog.setOnShowListener(d -> {
                try {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(GOLD);
                } catch (Throwable ignored) {}
            });
            dialog.show();
        }

        private String interactionSummary() {
            String base = (ANIM_PHYSICS.equals(coinAnimationMode) ? "物理飞出" : "经典浮动")
                    + " · " + (manualCasting ? "逐爻" : "自动")
                    + " · " + (shakeEnabled ? "可摇动" : "仅点击");
            return verticalFlipEnabled ? base + " · 垂直翻转" : base;
        }

        private String aiSettingsSummary(AiSettingsStore.Settings saved) {
            return providerLabel(saved.provider) + " · " + (saved.isConfigured() ? saved.model : "未配置 API Key");
        }

        private LinearLayout settingsCard(Context ctx, String titleText, String summaryText) {
            float density = getResources().getDisplayMetrics().density;
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding((int)(14*density), (int)(12*density), (int)(14*density), (int)(12*density));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, (int)(9*density));
            card.setLayoutParams(lp);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(PANEL); bg.setCornerRadius(12*density); bg.setStroke(Math.max(1, (int)density), GRID);
            card.setBackground(bg);
            card.setClickable(true); card.setFocusable(true);

            TextView title = new TextView(ctx);
            title.setText(titleText + "   ›"); title.setTextSize(15); title.setTextColor(FG);
            title.setTypeface(Typeface.DEFAULT_BOLD); card.addView(title);
            TextView summary = new TextView(ctx);
            summary.setText(summaryText); summary.setTextSize(11.5f); summary.setTextColor(MUTED);
            summary.setPadding(0, (int)(4*density), 0, 0); card.addView(summary);
            return card;
        }

        private void showInteractionSettingsDialog() { showInteractionSettingsDialog(null, null); }

        private void showInteractionSettingsDialog(Runnable onSaved) { showInteractionSettingsDialog(onSaved, null); }

        private void showInteractionSettingsDialog(Runnable onSaved, Runnable onFormalStarted) {
            Context ctx = getContext();
            float density = getResources().getDisplayMetrics().density;
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding((int)(16*density), (int)(8*density), (int)(16*density), (int)(8*density));

            TextView modeTitle = new TextView(ctx); modeTitle.setText("投币动画"); modeTitle.setTextSize(14); modeTitle.setTextColor(GOLD); box.addView(modeTitle);
            RadioGroup animation = new RadioGroup(ctx); animation.setOrientation(RadioGroup.VERTICAL);
            RadioButton classic = new RadioButton(ctx); classic.setText("经典浮动 · 稳定清晰"); classic.setId(View.generateViewId());
            RadioButton physics = new RadioButton(ctx); physics.setText("物理飞出 · 从按钮抛出三枚铜钱"); physics.setId(View.generateViewId());
            animation.addView(classic); animation.addView(physics);
            animation.check(ANIM_PHYSICS.equals(coinAnimationMode) ? physics.getId() : classic.getId()); box.addView(animation);

            CheckBox sound = new CheckBox(ctx); sound.setText("音效"); sound.setChecked(soundEnabled); box.addView(sound);
            CheckBox haptic = new CheckBox(ctx); haptic.setText("震动 / 触感反馈"); haptic.setChecked(hapticEnabled); box.addView(haptic);
            CheckBox shake = new CheckBox(ctx); shake.setText("摇动起卦 / 继续投掷"); shake.setChecked(shakeEnabled); box.addView(shake);
            CheckBox manual = new CheckBox(ctx); manual.setText("逐爻手动投掷"); manual.setChecked(manualCasting); box.addView(manual);
            CheckBox verticalFlip = new CheckBox(ctx); verticalFlip.setText("垂直翻转 · 铜钱沿水平轴翻面"); verticalFlip.setChecked(verticalFlipEnabled); box.addView(verticalFlip);

            TextView formalTitle = new TextView(ctx); formalTitle.setText("正式起卦"); formalTitle.setTextSize(14); formalTitle.setTextColor(GOLD);
            formalTitle.setPadding(0, (int)(12*density), 0, 0); box.addView(formalTitle);
            TextView formalClock = new TextView(ctx); formalClock.setTextSize(16); formalClock.setTextColor(FG); formalClock.setTypeface(Typeface.MONOSPACE); box.addView(formalClock);
            TextView formalInfo = new TextView(ctx);
            formalInfo.setText("下一整分起初爻，随后在 +10s / +20s / +30s / +40s / +50s 起后五爻。逐爻手动开启时，每个节点先提醒；3秒未操作则自动补掷。");
            formalInfo.setTextSize(10.5f); formalInfo.setTextColor(MUTED); formalInfo.setPadding(0, (int)(4*density), 0, (int)(6*density)); box.addView(formalInfo);
            Button formalStart = new Button(ctx); formalStart.setText("正式起卦 · 等待整分"); box.addView(formalStart);

            final Handler clockHandler = new Handler(Looper.getMainLooper());
            final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            final Runnable[] clockUpdate = new Runnable[1];
            clockUpdate[0] = () -> {
                long now = System.currentTimeMillis();
                long next = ((now / 60000L) + 1L) * 60000L;
                formalClock.setText("当前 " + clockFormat.format(new Date(now)) + "  ·  整分 " + clockFormat.format(new Date(next)));
                clockHandler.postDelayed(clockUpdate[0], 250L);
            };

            AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle("交互与动画").setView(box)
                    .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
            dialog.setOnShowListener(d -> {
                clockHandler.post(clockUpdate[0]);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    soundEnabled = sound.isChecked(); hapticEnabled = haptic.isChecked(); shakeEnabled = shake.isChecked(); manualCasting = manual.isChecked();
                    verticalFlipEnabled = verticalFlip.isChecked();
                    coinAnimationMode = animation.getCheckedRadioButtonId() == physics.getId() ? ANIM_PHYSICS : ANIM_CLASSIC;
                    saveExperienceSettings();
                    if (onSaved != null) onSaved.run();
                    dialog.dismiss(); postInvalidateOnAnimation();
                    Toast.makeText(ctx, "交互设置已保存", Toast.LENGTH_SHORT).show();
                });
            });
            formalStart.setOnClickListener(v -> {
                soundEnabled = sound.isChecked(); hapticEnabled = haptic.isChecked(); shakeEnabled = shake.isChecked(); manualCasting = manual.isChecked();
                verticalFlipEnabled = verticalFlip.isChecked();
                coinAnimationMode = animation.getCheckedRadioButtonId() == physics.getId() ? ANIM_PHYSICS : ANIM_CLASSIC;
                saveExperienceSettings();
                if (onSaved != null) onSaved.run();
                dialog.dismiss();
                if (onFormalStarted != null) onFormalStarted.run();
                startFormalCasting();
            });
            dialog.setOnDismissListener(d -> clockHandler.removeCallbacksAndMessages(null));
            dialog.show();
        }

        private void showAiSettingsPanel(boolean startAiAfterSave) { showAiSettingsPanel(startAiAfterSave, null); }

        private void showAiSettingsPanel(boolean startAiAfterSave, Runnable onSaved) {
            final Context ctx = getContext();
            final AiSettingsStore.Settings[] current = {AiSettingsStore.load(ctx)};
            final float density = getResources().getDisplayMetrics().density;
            ScrollView scroll = new ScrollView(ctx);
            LinearLayout box = new LinearLayout(ctx); box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding((int)(16*density), (int)(6*density), (int)(16*density), (int)(10*density)); scroll.addView(box);

            TextView warning = new TextView(ctx);
            warning.setText("每个服务商现在独立保存 API Key、接口、模型与协议。切换服务商会读取该服务商自己的配置；API Key 仍使用 Android Keystore 加密。");
            warning.setTextColor(MUTED); warning.setTextSize(11.5f); warning.setPadding(0,0,0,(int)(8*density)); box.addView(warning);

            Spinner provider = new Spinner(ctx);
            String[] providerLabels = {"OpenAI", "DeepSeek", "Gemini", "通义千问", "Kimi", "自定义"};
            String[] providerValues = {AiSettingsStore.PROVIDER_OPENAI, AiSettingsStore.PROVIDER_DEEPSEEK,
                    AiSettingsStore.PROVIDER_GEMINI, AiSettingsStore.PROVIDER_QWEN,
                    AiSettingsStore.PROVIDER_KIMI, AiSettingsStore.PROVIDER_CUSTOM};
            provider.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, providerLabels));
            provider.setSelection(providerIndex(current[0].provider)); box.addView(provider);

            TextView endpointInfo = new TextView(ctx);
            endpointInfo.setTextSize(9.5f); endpointInfo.setTextColor(MUTED);
            endpointInfo.setPadding(0, (int)(5*density), 0, (int)(5*density)); box.addView(endpointInfo);
            EditText endpoint = new EditText(ctx); endpoint.setHint("API 地址（仅自定义服务商可编辑）"); endpoint.setSingleLine(true); endpoint.setText(current[0].endpoint); box.addView(endpoint);
            boolean initialCustom = AiSettingsStore.PROVIDER_CUSTOM.equals(current[0].provider);
            endpoint.setVisibility(initialCustom ? View.VISIBLE : View.GONE);
            endpointInfo.setVisibility(initialCustom ? View.GONE : View.VISIBLE);
            endpointInfo.setText("API 地址 · " + AiSettingsStore.providerEndpoint(current[0].provider));
            EditText key = new EditText(ctx); key.setHint(current[0].apiKey.isEmpty() ? "API Key" : "API Key（此服务商已保存；留空保持不变）");
            key.setSingleLine(true); key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); box.addView(key);
            EditText model = new EditText(ctx); model.setHint("模型 ID"); model.setSingleLine(true); model.setText(current[0].model); box.addView(model);
            Spinner mode = new Spinner(ctx);
            mode.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, new String[]{"Responses API", "Chat Completions（兼容模式）"}));
            mode.setSelection(AiSettingsStore.MODE_CHAT.equals(current[0].mode) ? 1 : 0); box.addView(mode);

            provider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                boolean first = true;
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selected = providerValues[Math.max(0, Math.min(position, providerValues.length - 1))];
                    if (first) { first = false; return; }
                    current[0] = AiSettingsStore.loadProvider(ctx, selected);
                    boolean custom = AiSettingsStore.PROVIDER_CUSTOM.equals(selected);
                    endpoint.setVisibility(custom ? View.VISIBLE : View.GONE);
                    endpointInfo.setVisibility(custom ? View.GONE : View.VISIBLE);
                    if (custom) endpoint.setText(current[0].endpoint);
                    else endpointInfo.setText("API 地址 · " + AiSettingsStore.providerEndpoint(selected));
                    model.setText(current[0].model);
                    mode.setSelection(AiSettingsStore.MODE_CHAT.equals(current[0].mode) ? 1 : 0);
                    key.setText("");
                    key.setHint(current[0].apiKey.isEmpty() ? "API Key" : "API Key（此服务商已保存；留空保持不变）");
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            LinearLayout row = new LinearLayout(ctx); row.setOrientation(LinearLayout.HORIZONTAL);
            Button models = new Button(ctx); models.setText("读取模型"); Button clear = new Button(ctx); clear.setText("清除当前 Key");
            row.addView(models, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(clear, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(row);

            AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle("AI 解卦设置").setView(scroll)
                    .setPositiveButton(startAiAfterSave ? "保存并解卦" : "保存", null).setNegativeButton("取消", null).create();

            models.setOnClickListener(v -> {
                String selected = providerValues[provider.getSelectedItemPosition()];
                AiSettingsStore.Settings stored = AiSettingsStore.loadProvider(ctx, selected);
                String enteredKey = key.getText().toString().trim();
                String effectiveKey = enteredKey.isEmpty() ? stored.apiKey : enteredKey;
                String endpointValue = aiEndpointValue(selected, endpoint);
                if (effectiveKey.isEmpty()) { Toast.makeText(ctx, "请先填写此服务商的 API Key", Toast.LENGTH_SHORT).show(); return; }
                if (!endpointValue.startsWith("https://")) { Toast.makeText(ctx, "接口地址必须使用 HTTPS", Toast.LENGTH_SHORT).show(); return; }
                String modeValue = mode.getSelectedItemPosition() == 1 ? AiSettingsStore.MODE_CHAT : AiSettingsStore.MODE_RESPONSES;
                AiSettingsStore.Settings temp = new AiSettingsStore.Settings(endpointValue, effectiveKey, model.getText().toString().trim(), modeValue, selected);
                models.setEnabled(false); models.setText("读取中…");
                new Thread(() -> {
                    try {
                        List<String> list = AiClient.listModels(temp);
                        ((Activity)ctx).runOnUiThread(() -> { models.setEnabled(true); models.setText("读取模型");
                            if (list.isEmpty()) { Toast.makeText(ctx, "接口未返回可选文本模型", Toast.LENGTH_SHORT).show(); return; }
                            String[] items=list.toArray(new String[0]); new AlertDialog.Builder(ctx).setTitle("选择模型（"+items.length+"）")
                                    .setItems(items,(d,which)->model.setText(items[which])).setNegativeButton("取消",null).show(); });
                    } catch(Exception ex) {
                        String message=ex.getMessage()==null?ex.toString():ex.getMessage();
                        ((Activity)ctx).runOnUiThread(() -> { models.setEnabled(true); models.setText("读取模型"); Toast.makeText(ctx,"读取失败："+message,Toast.LENGTH_LONG).show(); });
                    }
                }, "ryusgua-models").start();
            });

            clear.setOnClickListener(v -> {
                String selected = providerValues[provider.getSelectedItemPosition()];
                AiSettingsStore.clearApiKey(ctx, selected);
                current[0] = AiSettingsStore.loadProvider(ctx, selected);
                key.setText(""); key.setHint("API Key（当前服务商已清除）");
                if (onSaved != null) onSaved.run();
                Toast.makeText(ctx,"已清除 " + providerLabel(selected) + " 的 API Key",Toast.LENGTH_SHORT).show();
            });

            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String selected = providerValues[provider.getSelectedItemPosition()];
                String modeValue=mode.getSelectedItemPosition()==1?AiSettingsStore.MODE_CHAT:AiSettingsStore.MODE_RESPONSES;
                try {
                    AiSettingsStore.save(ctx, aiEndpointValue(selected, endpoint), key.getText().toString(), model.getText().toString(), modeValue, selected);
                    AiSettingsStore.Settings now=AiSettingsStore.load(ctx);
                    if (!selected.equals(now.provider)
                            || !aiEndpointValue(selected, endpoint).equals(now.endpoint)
                            || !model.getText().toString().trim().equals(now.model)
                            || !modeValue.equals(now.mode)) {
                        throw new IllegalStateException("保存后校验失败，请重试");
                    }
                    if(startAiAfterSave&&!now.isConfigured()){ Toast.makeText(ctx,"请填写并保存 " + providerLabel(selected) + " 的 API Key",Toast.LENGTH_SHORT).show(); return; }
                    dialog.dismiss();
                    if (onSaved != null) onSaved.run();
                    Toast.makeText(ctx,providerLabel(selected) + " 设置已保存",Toast.LENGTH_SHORT).show();
                    if(startAiAfterSave) requestAiReading(now);
                    postInvalidateOnAnimation();
                } catch(Exception ex){ Toast.makeText(ctx,ex.getMessage()==null?"保存失败":ex.getMessage(),Toast.LENGTH_LONG).show(); }
            }));
            dialog.show();
        }

        private String aiEndpointValue(String provider, EditText endpoint) {
            if (!AiSettingsStore.PROVIDER_CUSTOM.equals(provider)) return AiSettingsStore.providerEndpoint(provider);
            return AiSettingsStore.normalizeEndpoint(endpoint == null ? "" : endpoint.getText().toString());
        }

        private String providerLabel(String provider) {
            if (AiSettingsStore.PROVIDER_OPENAI.equals(provider)) return "OpenAI";
            if (AiSettingsStore.PROVIDER_DEEPSEEK.equals(provider)) return "DeepSeek";
            if (AiSettingsStore.PROVIDER_GEMINI.equals(provider)) return "Gemini";
            if (AiSettingsStore.PROVIDER_QWEN.equals(provider)) return "通义千问";
            if (AiSettingsStore.PROVIDER_KIMI.equals(provider)) return "Kimi";
            return "自定义";
        }

        private int providerIndex(String provider) {
            if (AiSettingsStore.PROVIDER_OPENAI.equals(provider)) return 0;
            if (AiSettingsStore.PROVIDER_DEEPSEEK.equals(provider)) return 1;
            if (AiSettingsStore.PROVIDER_GEMINI.equals(provider)) return 2;
            if (AiSettingsStore.PROVIDER_QWEN.equals(provider)) return 3;
            if (AiSettingsStore.PROVIDER_KIMI.equals(provider)) return 4;
            return 5;
        }

        private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
        private static String join(List<String> list, String separator) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) { if (i > 0) sb.append(separator); sb.append(list.get(i)); }
            return sb.toString();
        }

        private static final class HistoryHit {
            final RectF rect;
            final RectF pinRect;
            final RectF noteRect;
            final HistoryStore.Entry entry;
            HistoryHit(RectF rect, RectF pinRect, RectF noteRect, HistoryStore.Entry entry) {
                this.rect = new RectF(rect);
                this.pinRect = new RectF(pinRect);
                this.noteRect = new RectF(noteRect);
                this.entry = entry;
            }
        }
    }
}

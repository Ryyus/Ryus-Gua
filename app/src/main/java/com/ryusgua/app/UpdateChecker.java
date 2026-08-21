package com.ryusgua.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Multi-source update checker: jsDelivr first, GitHub fallback. */
final class UpdateChecker {
    private static final String[] META_URLS = {
            "https://cdn.jsdelivr.net/gh/Ryyus/Ryus-Gua@main/update/latest.json",
            "https://raw.githubusercontent.com/Ryyus/Ryus-Gua/main/update/latest.json"
    };
    private static final String PREF = "ryusgua_updates";
    private static final long AUTO_INTERVAL_MS = 12L * 60L * 60L * 1000L;

    private UpdateChecker() {}

    static void check(Activity activity, boolean manual) {
        if (activity == null || activity.isFinishing()) return;
        SharedPreferences p = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!manual && now - p.getLong("last_check", 0L) < AUTO_INTERVAL_MS) return;
        p.edit().putLong("last_check", now).apply();
        if (manual) Toast.makeText(activity, "正在检查版本更新", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                JSONObject json = fetchMetadata();
                int remoteCode = json.optInt("versionCode", 0);
                String remoteName = json.optString("versionName", "");
                String title = json.optString("title", "发现版本更新");
                String notes = json.optString("notes", "");
                String releasePage = json.optString("releasePage", "https://github.com/Ryyus/Ryus-Gua/releases");
                boolean legacy = "legacy".equalsIgnoreCase(BuildConfig.FLAVOR);
                String mirror = json.optString(legacy ? "legacyMirrorApkUrl" : "mirrorApkUrl", "");
                String direct = json.optString(legacy ? "legacyApkUrl" : "apkUrl", "");
                String downloadUrl = firstReachable(mirror, direct, releasePage);
                int localCode = currentVersionCode(activity);

                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
                    if (remoteCode > localCode) {
                        String message = "v" + remoteName + " 可用\n\n" + notes;
                        new AlertDialog.Builder(activity)
                                .setTitle(title)
                                .setMessage(message.trim())
                                .setPositiveButton("立即更新", (d, w) -> open(activity, downloadUrl))
                                .setNeutralButton("发布页面", (d, w) -> open(activity, releasePage))
                                .setNegativeButton("稍后再说", null)
                                .show();
                    } else if (manual) {
                        Toast.makeText(activity, "当前已经是最新版", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ex) {
                if (manual) activity.runOnUiThread(() -> {
                    if (!activity.isFinishing()) Toast.makeText(activity, "检查更新暂不可用", Toast.LENGTH_SHORT).show();
                });
            }
        }, "ryus-gua-update-check").start();
    }

    private static JSONObject fetchMetadata() throws Exception {
        Exception last = null;
        for (String url : META_URLS) {
            HttpURLConnection conn = null;
            try {
                conn = openConnection(url, 4500, 5500);
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                return new JSONObject(readAll(conn.getInputStream()));
            } catch (Exception ex) {
                last = ex;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw last == null ? new IllegalStateException("No update source") : last;
    }

    private static String firstReachable(String... urls) {
        for (String url : urls) {
            if (url == null || url.trim().isEmpty()) continue;
            if (isReachable(url.trim())) return url.trim();
        }
        for (String url : urls) if (url != null && !url.trim().isEmpty()) return url.trim();
        return "https://github.com/Ryyus/Ryus-Gua/releases";
    }

    private static boolean isReachable(String url) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(url, 3500, 3500);
            conn.setRequestProperty("Range", "bytes=0-0");
            int code = conn.getResponseCode();
            return (code >= 200 && code < 400) || code == 416;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String url, int connectMs, int readMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(connectMs);
        conn.setReadTimeout(readMs);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Accept", "application/json, application/octet-stream, */*");
        conn.setRequestProperty("User-Agent", "Ryus-Gua-Android/1.3");
        return conn;
    }

    private static int currentVersionCode(Context context) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        long code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
        return (int) Math.min(Integer.MAX_VALUE, code);
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void open(Context context, String url) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ex) {
            Toast.makeText(context, "无法打开更新页面", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.zhanggua.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores non-secret AI settings in SharedPreferences and encrypts the API key at rest
 * with a device-bound Android Keystore AES/GCM key.
 *
 * This protects the key from casual extraction from app preferences, but a mobile app
 * can never provide the same secret isolation as a trusted backend.
 */
final class AiSettingsStore {
    static final String DEFAULT_ENDPOINT = "https://api.openai.com";
    static final String DEFAULT_MODEL = "gpt-5.6-terra";
    static final String MODE_RESPONSES = "responses";
    static final String MODE_CHAT = "chat";

    static final String PROVIDER_OPENAI = "openai";
    static final String PROVIDER_DEEPSEEK = "deepseek";
    static final String PROVIDER_GEMINI = "gemini";
    static final String PROVIDER_QWEN = "qwen";
    static final String PROVIDER_KIMI = "kimi";
    static final String PROVIDER_CUSTOM = "custom";

    static final class Settings {
        final String endpoint;
        final String apiKey;
        final String model;
        final String mode;
        final String provider;

        Settings(String endpoint, String apiKey, String model, String mode) {
            this(endpoint, apiKey, model, mode, inferProvider(endpoint));
        }

        Settings(String endpoint, String apiKey, String model, String mode, String provider) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.model = model;
            this.mode = mode;
            this.provider = provider == null ? PROVIDER_CUSTOM : provider;
        }

        boolean isConfigured() {
            return apiKey != null && !apiKey.trim().isEmpty()
                    && model != null && !model.trim().isEmpty()
                    && endpoint != null && endpoint.startsWith("https://");
        }
    }

    private static final String PREFS = "ai_settings_v1";
    private static final String KEY_ALIAS = "zhanggua_ai_api_key_v1";
    private static final String K_ENDPOINT = "endpoint";
    private static final String K_MODEL = "model";
    private static final String K_MODE = "mode";
    private static final String K_PROVIDER = "provider";
    private static final String K_KEY_CT = "key_ct";
    private static final String K_KEY_IV = "key_iv";

    private AiSettingsStore() {}

    static Settings load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String endpoint = p.getString(K_ENDPOINT, DEFAULT_ENDPOINT);
        String model = p.getString(K_MODEL, DEFAULT_MODEL);
        String mode = p.getString(K_MODE, MODE_RESPONSES);
        String provider = p.getString(K_PROVIDER, null);
        String apiKey = decryptApiKey(p);
        return new Settings(endpoint == null ? DEFAULT_ENDPOINT : endpoint,
                apiKey == null ? "" : apiKey,
                model == null ? DEFAULT_MODEL : model,
                MODE_CHAT.equals(mode) ? MODE_CHAT : MODE_RESPONSES,
                provider == null ? inferProvider(endpoint) : provider);
    }

    static void save(Context context, String endpoint, String apiKey, String model, String mode) throws Exception {
        save(context, endpoint, apiKey, model, mode, inferProvider(endpoint));
    }

    static void save(Context context, String endpoint, String apiKey, String model, String mode, String provider) throws Exception {
        endpoint = normalizeEndpoint(endpoint);
        model = model == null ? "" : model.trim();
        if (!endpoint.startsWith("https://")) throw new IllegalArgumentException("接口地址必须使用 HTTPS");
        if (model.isEmpty()) throw new IllegalArgumentException("模型 ID 不能为空");

        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit()
                .putString(K_ENDPOINT, endpoint)
                .putString(K_MODEL, model)
                .putString(K_MODE, MODE_CHAT.equals(mode) ? MODE_CHAT : MODE_RESPONSES)
                .putString(K_PROVIDER, normalizeProvider(provider));

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] ct = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
            e.putString(K_KEY_CT, Base64.encodeToString(ct, Base64.NO_WRAP));
            e.putString(K_KEY_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP));
        }
        e.apply();
    }

    static void clearApiKey(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(K_KEY_CT).remove(K_KEY_IV).apply();
    }

    private static String decryptApiKey(SharedPreferences p) {
        String ct64 = p.getString(K_KEY_CT, null);
        String iv64 = p.getString(K_KEY_IV, null);
        if (ct64 == null || iv64 == null) return "";
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            SecretKey key = (SecretKey) ks.getKey(KEY_ALIAS, null);
            if (key == null) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = Base64.decode(iv64, Base64.NO_WRAP);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] clear = cipher.doFinal(Base64.decode(ct64, Base64.NO_WRAP));
            return new String(clear, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        SecretKey existing = (SecretKey) ks.getKey(KEY_ALIAS, null);
        if (existing != null) return existing;

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    static String normalizeProvider(String provider) {
        if (PROVIDER_OPENAI.equals(provider) || PROVIDER_DEEPSEEK.equals(provider)
                || PROVIDER_GEMINI.equals(provider) || PROVIDER_QWEN.equals(provider)
                || PROVIDER_KIMI.equals(provider)) return provider;
        return PROVIDER_CUSTOM;
    }

    static String inferProvider(String endpoint) {
        String e = normalizeEndpoint(endpoint).toLowerCase();
        if (e.contains("api.openai.com")) return PROVIDER_OPENAI;
        if (e.contains("api.deepseek.com")) return PROVIDER_DEEPSEEK;
        if (e.contains("generativelanguage.googleapis.com")) return PROVIDER_GEMINI;
        if (e.contains("dashscope.aliyuncs.com") || e.contains("maas.aliyuncs.com")) return PROVIDER_QWEN;
        if (e.contains("api.moonshot.cn") || e.contains("api.moonshot.ai")) return PROVIDER_KIMI;
        return PROVIDER_CUSTOM;
    }

    static String providerEndpoint(String provider) {
        switch (normalizeProvider(provider)) {
            case PROVIDER_OPENAI: return "https://api.openai.com";
            case PROVIDER_DEEPSEEK: return "https://api.deepseek.com";
            case PROVIDER_GEMINI: return "https://generativelanguage.googleapis.com/v1beta/openai";
            case PROVIDER_QWEN: return "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case PROVIDER_KIMI: return "https://api.moonshot.cn/v1";
            default: return DEFAULT_ENDPOINT;
        }
    }

    static String providerModel(String provider) {
        switch (normalizeProvider(provider)) {
            case PROVIDER_OPENAI: return "gpt-5.6-terra";
            case PROVIDER_DEEPSEEK: return "deepseek-v4-flash";
            case PROVIDER_GEMINI: return "gemini-3.6-flash";
            case PROVIDER_QWEN: return "qwen3.7-flash";
            case PROVIDER_KIMI: return "kimi-k2.6";
            default: return DEFAULT_MODEL;
        }
    }

    static String providerMode(String provider) {
        return PROVIDER_OPENAI.equals(normalizeProvider(provider)) ? MODE_RESPONSES : MODE_CHAT;
    }

    static String normalizeEndpoint(String endpoint) {
        String v = endpoint == null ? "" : endpoint.trim();
        if (v.isEmpty()) v = DEFAULT_ENDPOINT;
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }
}

package com.ryusgua.app;

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
 * Provider-scoped AI settings store.
 *
 * Non-secret settings live in SharedPreferences. Each provider keeps its own endpoint,
 * model, protocol mode and encrypted API key. API keys are encrypted with a single
 * device-bound Android Keystore AES/GCM master key, but ciphertext/IV pairs are stored
 * separately per provider.
 */
final class AiSettingsStore {
    static final String DEFAULT_ENDPOINT = "https://api.openai.com";
    static final String DEFAULT_MODEL = "gpt-5.6-terra";
    static final String MODE_RESPONSES = "responses";
    static final String MODE_CHAT = "chat";
    static final String REASONING_DEFAULT = "default";
    static final String REASONING_LOW = "low";
    static final String REASONING_MEDIUM = "medium";
    static final String REASONING_HIGH = "high";

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
        final String reasoningEffort;

        Settings(String endpoint, String apiKey, String model, String mode) {
            this(endpoint, apiKey, model, mode, inferProvider(endpoint), REASONING_LOW);
        }

        Settings(String endpoint, String apiKey, String model, String mode, String provider) {
            this(endpoint, apiKey, model, mode, provider, REASONING_LOW);
        }

        Settings(String endpoint, String apiKey, String model, String mode, String provider,
                 String reasoningEffort) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.model = model;
            this.mode = mode;
            this.provider = provider == null ? PROVIDER_CUSTOM : provider;
            this.reasoningEffort = normalizeReasoningEffort(reasoningEffort);
        }

        boolean isConfigured() {
            return apiKey != null && !apiKey.trim().isEmpty()
                    && model != null && !model.trim().isEmpty()
                    && endpoint != null && endpoint.startsWith("https://");
        }
    }

    private static final String PREFS = "ryusgua_ai_settings_v1";
    private static final String KEY_ALIAS = "ryusgua_ai_api_key_v1";
    private static final String K_ACTIVE_PROVIDER = "active_provider";

    private static final String F_ENDPOINT = "endpoint";
    private static final String F_MODEL = "model";
    private static final String F_MODE = "mode";
    private static final String F_REASONING = "reasoning_effort";
    private static final String F_KEY_CT = "key_ct";
    private static final String F_KEY_IV = "key_iv";


    private AiSettingsStore() {}

    static Settings load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String provider = normalizeProvider(p.getString(K_ACTIVE_PROVIDER, PROVIDER_OPENAI));
        return loadProviderInternal(context, provider);
    }

    static Settings loadProvider(Context context, String provider) {
        return loadProviderInternal(context, normalizeProvider(provider));
    }

    private static Settings loadProviderInternal(Context context, String provider) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String endpoint = PROVIDER_CUSTOM.equals(provider)
                ? p.getString(key(provider, F_ENDPOINT), providerEndpoint(provider))
                : providerEndpoint(provider);
        String model = p.getString(key(provider, F_MODEL), providerModel(provider));
        String mode = p.getString(key(provider, F_MODE), providerMode(provider));
        String reasoningEffort = p.getString(key(provider, F_REASONING), REASONING_LOW);
        String apiKey = decryptApiKey(p, provider);
        return new Settings(
                endpoint == null ? providerEndpoint(provider) : endpoint,
                apiKey == null ? "" : apiKey,
                model == null ? providerModel(provider) : model,
                MODE_CHAT.equals(mode) ? MODE_CHAT : MODE_RESPONSES,
                provider,
                reasoningEffort);
    }

    static void save(Context context, String endpoint, String apiKey, String model, String mode) throws Exception {
        save(context, endpoint, apiKey, model, mode, inferProvider(endpoint));
    }

    static void save(Context context, String endpoint, String apiKey, String model, String mode, String provider) throws Exception {
        save(context, endpoint, apiKey, model, mode, provider, REASONING_LOW);
    }

    static void save(Context context, String endpoint, String apiKey, String model, String mode,
                     String provider, String reasoningEffort) throws Exception {
        provider = normalizeProvider(provider);
        endpoint = PROVIDER_CUSTOM.equals(provider)
                ? normalizeEndpoint(endpoint) : providerEndpoint(provider);
        model = model == null ? "" : model.trim();
        if (!endpoint.startsWith("https://")) throw new IllegalArgumentException("接口地址必须使用 HTTPS");
        if (model.isEmpty()) throw new IllegalArgumentException("模型 ID 不能为空");

        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit()
                .putString(K_ACTIVE_PROVIDER, provider)
                .putString(key(provider, F_ENDPOINT), endpoint)
                .putString(key(provider, F_MODEL), model)
                .putString(key(provider, F_MODE), MODE_CHAT.equals(mode) ? MODE_CHAT : MODE_RESPONSES)
                .putString(key(provider, F_REASONING), normalizeReasoningEffort(reasoningEffort));

        // Empty input means "keep this provider's saved key". It no longer falls through
        // to another provider because every provider has its own ciphertext/IV pair.
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] ct = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
            e.putString(key(provider, F_KEY_CT), Base64.encodeToString(ct, Base64.NO_WRAP));
            e.putString(key(provider, F_KEY_IV), Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP));
        }

        if (!e.commit()) throw new IllegalStateException("AI 设置写入失败");
    }

    static void setActiveProvider(Context context, String provider) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(K_ACTIVE_PROVIDER, normalizeProvider(provider)).commit();
    }

    static void clearApiKey(Context context) {
        Settings current = load(context);
        clearApiKey(context, current.provider);
    }

    static void clearApiKey(Context context, String provider) {
        provider = normalizeProvider(provider);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(key(provider, F_KEY_CT))
                .remove(key(provider, F_KEY_IV))
                .commit();
    }

    private static String decryptApiKey(SharedPreferences p, String provider) {
        String ct64 = p.getString(key(provider, F_KEY_CT), null);
        String iv64 = p.getString(key(provider, F_KEY_IV), null);
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


    private static String key(String provider, String field) {
        return "provider." + normalizeProvider(provider) + "." + field;
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

    static String normalizeReasoningEffort(String effort) {
        if (REASONING_LOW.equals(effort) || REASONING_MEDIUM.equals(effort)
                || REASONING_HIGH.equals(effort)) return effort;
        return REASONING_DEFAULT;
    }

    static String normalizeEndpoint(String endpoint) {
        String v = endpoint == null ? "" : endpoint.trim();
        if (v.isEmpty()) v = DEFAULT_ENDPOINT;
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }
}

package io.github.jingtuo.biometric.ui.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.github.jingtuo.biometric.R;

/**
 * 加密管理类
 *
 * @author JingTuo
 */
public class CryptoManager {

    private static final String TAG = "Crypto";

    private static final String PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * 生物识别不匹配
     */
    public static final int ERROR_BIOMETRIC_MISMATCH = 0x0227;

    /**
     * 未配置加密算法
     */
    public static final int ERROR_NO_CRYPTO = 0x0228;

    /**
     * 无生物识别硬件
     */
    public static final int ERROR_NO_HARDWARE = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;

    /**
     * 生物识别硬件不可用
     */
    public static final int ERROR_HW_UNAVAILABLE = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE;

    /**
     * 未注册生物识别
     */
    public static final int ERROR_NONE_ENROLLED = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;

    /**
     *
     */
    public static final int ERROR_SECURITY_UPDATE_REQUIRED = BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED;

    public static final int ERROR_UNKNOWN = BiometricManager.BIOMETRIC_STATUS_UNKNOWN;

    /**
     * 加密初始化错误
     */
    public static final int ERROR_CRYPTO_INIT_ERROR = 0x0229;

    /**
     * 加密异常
     */
    public static final int ERROR_ENCRYPT_EXCEPTION = 0x0229;

    /**
     * 解密异常
     */
    public static final int ERROR_DECRYPT_EXCEPTION = 0x022A;

    /**
     * 账号尚未绑定
     */
    public static final int ERROR_NONE_BIND = 0x022B;


    private BiometricManager biometricManager;

    private final FragmentActivity activity;
    private final String algorithm;
    private final String blockMode;
    private final String encryptPadding;

    /**
     * 密钥别名
     */
    private final String keystoreAlias;

    private final SharedPreferences preferences;

    CryptoManager(FragmentActivity activity, String algorithm, String blockMode, String encryptPadding, String keystoreAlias) {
        this.activity = activity;
        this.algorithm = algorithm;
        this.blockMode = blockMode;
        this.encryptPadding = encryptPadding;
        this.keystoreAlias = keystoreAlias;
        String name = "BiometricLogin";
//        try {
//            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
//            preferences = EncryptedSharedPreferences.create(name,
//                    masterKeyAlias, activity.getApplicationContext(),
//                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage());
//            preferences = activity.getSharedPreferences(name, Context.MODE_PRIVATE);
//        }
        preferences = activity.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * 绑定生物识别
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void bindBiometric(String username, String pwd, BiometricListener biometricListener) {
        if (biometricManager == null) {
            biometricManager = BiometricManager.from(activity);
        }
        int flag = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (BiometricManager.BIOMETRIC_SUCCESS == flag) {
            BiometricPrompt biometricPrompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            if (biometricListener != null) {
                                biometricListener.onAuthFailure(errorCode, errString);
                            }
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            //生物识别成功, 加密密码
                            if (result.getCryptoObject() != null
                                    && result.getCryptoObject().getCipher() != null) {
                                Cipher cipher = result.getCryptoObject().getCipher();
                                try {
                                    byte[] buffer = cipher.doFinal(pwd.getBytes(StandardCharsets.UTF_8));
                                    byte[] iv = cipher.getIV();
                                    preferences.edit().putString(username, Base64.encodeToString(buffer, Base64.NO_WRAP))
                                            .putString(username + "_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                                            .apply();
                                    if (biometricListener!= null) {
                                        biometricListener.onBindSuccess();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage(), e);
                                    if (biometricListener != null) {
                                        biometricListener.onAuthFailure(ERROR_ENCRYPT_EXCEPTION,
                                                activity.getString(R.string.biometric_error_encrypt_exception));
                                    }
                                }
                            } else {
                                if (biometricListener != null) {
                                    biometricListener.onAuthFailure(ERROR_NO_CRYPTO,
                                            activity.getString(R.string.biometric_error_no_crypto));
                                }
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            if (biometricListener != null) {
                                biometricListener.onAuthFailure(ERROR_BIOMETRIC_MISMATCH,
                                        activity.getString(R.string.biometric_error_auth_failure));
                            }
                        }
                    });
            try {
                Cipher cipher = Cipher.getInstance(algorithm + "/" + blockMode + "/" + encryptPadding);
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
                biometricPrompt.authenticate(getBiometricPromptInfo(), new BiometricPrompt.CryptoObject(cipher));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                if (biometricListener != null) {
                    biometricListener.onAuthFailure(ERROR_CRYPTO_INIT_ERROR, e.getMessage());
                }
            }
        } else {
            onBiometricError(flag, biometricListener);
        }
    }

    private void onBiometricError(int biometricStatus, BiometricListener biometricListener) {
        if (BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE == biometricStatus) {
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_NO_HARDWARE,
                        activity.getString(R.string.biometric_error_no_hardware));
            }
        } else if (BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE == biometricStatus) {
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_HW_UNAVAILABLE,
                        activity.getString(R.string.biometric_error_hw_unavailable));
            }
        } else if (BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED == biometricStatus) {
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_NONE_ENROLLED,
                        activity.getString(R.string.biometric_error_none_enrolled));
            }
        } else if (BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED == biometricStatus) {
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_SECURITY_UPDATE_REQUIRED,
                        activity.getString(R.string.biometric_error_security_update_required));
            }
        } else {
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_UNKNOWN,
                        activity.getString(R.string.biometric_error_none_enrolled));
            }
        }
    }

    /**
     * 生物识别登录
     * @param username 用户名
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void biometricLogin(String username, BiometricListener biometricListener){
        String ivStr = preferences.getString(username + "_iv", "");
        if (TextUtils.isEmpty(ivStr)) {
            //尚未绑定
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_NONE_BIND, activity.getString(R.string.biometric_error_none_bind));
            }
            return;
        }
        //先判断是否支持
        if (biometricManager == null) {
            biometricManager = BiometricManager.from(activity);
        }
        int flag = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (BiometricManager.BIOMETRIC_SUCCESS == flag) {
            //支持生物认证
            BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                    ContextCompat.getMainExecutor(activity),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Log.i("Login", "Auth error: " + errorCode + ", " + errString);
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            //生物识别成功, 解密密码
                            if (result.getCryptoObject() != null
                                    && result.getCryptoObject().getCipher() != null) {
                                Cipher cipher = result.getCryptoObject().getCipher();
                                try {
                                    String cipherPwd = preferences.getString(username, "");
                                    byte[] buffer = cipher.doFinal(Base64.decode(cipherPwd, Base64.NO_WRAP));
                                    String pwd = new String(buffer, StandardCharsets.UTF_8);
                                    if (biometricListener != null) {
                                        biometricListener.onAuthSuccess(pwd);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                    if (biometricListener != null) {
                                        biometricListener.onAuthFailure(ERROR_DECRYPT_EXCEPTION,
                                                activity.getString(R.string.biometric_error_decrypt_exception));
                                    }
                                }
                            } else {
                                if (biometricListener != null) {
                                    biometricListener.onAuthFailure(ERROR_NO_CRYPTO,
                                            activity.getString(R.string.biometric_error_no_crypto));
                                }
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            if (biometricListener != null) {
                                biometricListener.onAuthFailure(ERROR_BIOMETRIC_MISMATCH,
                                        activity.getString(R.string.biometric_error_auth_failure));
                            }
                        }
                    });
            try {
                Cipher cipher = Cipher.getInstance(algorithm + "/" + blockMode + "/" + encryptPadding);
                byte[] iv = Base64.decode(ivStr, Base64.NO_WRAP);
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new IvParameterSpec(iv));
                biometricPrompt.authenticate(getBiometricPromptInfo(), new BiometricPrompt.CryptoObject(cipher));
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                if (biometricListener != null) {
                    biometricListener.onAuthFailure(ERROR_CRYPTO_INIT_ERROR, e.getMessage());
                }
            }
        } else {
            onBiometricError(flag, biometricListener);
        }
    }

    private BiometricPrompt.PromptInfo getBiometricPromptInfo() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric))
                .setDescription(activity.getString(R.string.start_biometric))
                .setNegativeButtonText(activity.getString(android.R.string.cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE);
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null);
        Key key = keyStore.getKey(keystoreAlias, null);
        if (key != null) {
            return (SecretKey) key;
        }
        KeyGenParameterSpec.Builder build =  new KeyGenParameterSpec.Builder(keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            build.setInvalidatedByBiometricEnrollment(true);
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            //将密钥存储在:StrongBox Keymaster
//            build.setIsStrongBoxBacked(true);
//        }
        //用户认证之后多长时间内密钥可用
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            build.setUserAuthenticationParameters(5, KeyProperties.AUTH_BIOMETRIC_STRONG);
//        } else {
//            build.setUserAuthenticationValidityDurationSeconds(5);
//        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                PROVIDER_ANDROID_KEY_STORE);
        keyGenerator.init(build.build());
        return keyGenerator.generateKey();
    }

    public static class Builder {
        private FragmentActivity activity;
        private String algorithm;
        private String blockMode;
        private String encryptPadding;

        /**
         * 密钥别名
         */
        private String keystoreAlias;

        public Builder() {
        }

        public Builder setActivity(FragmentActivity activity) {
            this.activity = activity;
            return this;
        }

        public Builder setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder setBlockMode(String blockMode) {
            this.blockMode = blockMode;
            return this;
        }

        public Builder setEncryptPadding(String encryptPadding) {
            this.encryptPadding = encryptPadding;
            return this;
        }

        public Builder setKeystoreAlias(String keystoreAlias) {
            this.keystoreAlias = keystoreAlias;
            return this;
        }

        public CryptoManager build() {
            return new CryptoManager(activity, algorithm, blockMode, encryptPadding, keystoreAlias);
        }
    }
}

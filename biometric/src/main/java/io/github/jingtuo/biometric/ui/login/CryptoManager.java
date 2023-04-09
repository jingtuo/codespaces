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

import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jcajce.provider.symmetric.SM4;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.jingtuo.biometric.R;

/**
 * 加密管理类
 *
 * @author JingTuo
 */
public class CryptoManager {

    private static final String TAG = "Crypto";

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private static final String PREFERENCE_NAME = "Biometric";

    /**
     * 生物识别不匹配
     */
    public static final int ERROR_BIOMETRIC_MISMATCH = 0x0227;

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
    public static final int ERROR_SECURITY_UPDATE_REQUIRED = BiometricManager
            .BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED;

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
    /**
     * 密钥别名
     */
    private final String keyAlias;
    private final String algorithm;
    private final String blockMode;
    private final String encryptPadding;
    private final int authenticators;

    private final SharedPreferences preferences;

    CryptoManager(FragmentActivity activity,String keyAlias, String algorithm, String blockMode,
                  String encryptPadding, int authenticators) {
        this.activity = activity;
        this.keyAlias = keyAlias;
        this.algorithm = algorithm;
        this.blockMode = blockMode;
        this.encryptPadding = encryptPadding;
        this.authenticators = authenticators;
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
        preferences = activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 绑定生物识别
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void bindBiometric(String username, String pwd, BiometricListener biometricListener) {
        if (biometricManager == null) {
            biometricManager = BiometricManager.from(activity);
        }
        int flag = biometricManager.canAuthenticate(authenticators);
        if (BiometricManager.BIOMETRIC_SUCCESS == flag) {
            Cipher cipher;
            try {
                cipher = Cipher.getInstance(algorithm + "/" + blockMode + "/" + encryptPadding);
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                if (biometricListener != null) {
                    biometricListener.onAuthFailure(ERROR_CRYPTO_INIT_ERROR, e.getMessage());
                }
                return;
            }
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
                            if (result.getCryptoObject() != null
                                    && result.getCryptoObject().getCipher() != null) {
                                Cipher cipher = result.getCryptoObject().getCipher();
                                savePwd(cipher, username, pwd, biometricListener);
                            } else {
                                savePwd(cipher, username, pwd, biometricListener);
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
            if (cryptoIsSupported(authenticators)) {
                biometricPrompt.authenticate(getBiometricPromptInfo(authenticators), new BiometricPrompt.CryptoObject(cipher));
            } else {
                biometricPrompt.authenticate(getBiometricPromptInfo(authenticators));
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
                        activity.getString(R.string.biometric_error_unknown));
            }
        }
    }

    /**
     * 生物识别登录
     * @param username 用户名
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void biometricLogin(String username, int authenticators, BiometricListener biometricListener) {
        String ivStr = preferences.getString(username + "_iv", "");
        Log.i(TAG, "biometricLogin: " + ivStr);
        //先判断是否支持
        if (biometricManager == null) {
            biometricManager = BiometricManager.from(activity);
        }
        int flag = biometricManager.canAuthenticate(authenticators);
        if (BiometricManager.BIOMETRIC_SUCCESS == flag) {
            //支持生物认证
            Cipher cipher;
            try {
                cipher = Cipher.getInstance(algorithm + "/" + blockMode + "/" + encryptPadding);
                AlgorithmParameters parameters = AlgorithmParameters.getInstance(algorithm);
                if (TextUtils.isEmpty(ivStr)) {
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
                } else {
                    byte[] iv = Base64.decode(ivStr, Base64.NO_WRAP);
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new IvParameterSpec(iv));
                }
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                if (biometricListener != null) {
                    biometricListener.onAuthFailure(ERROR_CRYPTO_INIT_ERROR, e.getMessage());
                }
                return;
            }
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
                                getPwd(cipher, username, biometricListener);
                            } else {
                                getPwd(cipher, username, biometricListener);
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
            if (cryptoIsSupported(authenticators)) {
                biometricPrompt.authenticate(getBiometricPromptInfo(authenticators), new BiometricPrompt.CryptoObject(cipher));
            } else {
                biometricPrompt.authenticate(getBiometricPromptInfo(authenticators));
            }
        } else {
            onBiometricError(flag, biometricListener);
        }
    }

    private BiometricPrompt.PromptInfo getBiometricPromptInfo(int authenticators) {
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric))
                .setDescription(activity.getString(R.string.start_biometric))
                .setAllowedAuthenticators(authenticators);
        if ((BiometricManager.Authenticators.DEVICE_CREDENTIAL & authenticators)
                != BiometricManager.Authenticators.DEVICE_CREDENTIAL) {
            //允许设置取消按钮
            builder.setNegativeButtonText(activity.getString(android.R.string.cancel));
        }
        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null);
        Key key = keyStore.getKey(keyAlias, null);
        if (key != null) {
            return (SecretKey) key;
        }
        KeyGenParameterSpec.Builder build = new KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(blockMode)
                .setEncryptionPaddings(encryptPadding);
        //密钥是否需要用户授权
        build.setUserAuthenticationRequired(cryptoIsSupported(authenticators));
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
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
        keyGenerator.init(build.build());
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey;
    }

    public static class Builder {
        private FragmentActivity activity;

        private String keyAlias;
        private String algorithm;
        private String blockMode;
        private String encryptPadding;

        /**
         * 身份验证方式
         *
         * 参见: {@link BiometricManager.Authenticators#BIOMETRIC_STRONG}等
         */
        private int authenticators = 0;

        public Builder() {
        }

        public Builder setActivity(FragmentActivity activity) {
            this.activity = activity;
            return this;
        }

        public Builder setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
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

        /**
         * 身份验证方式
         *
         * @param authenticators 参见: {@link BiometricManager.Authenticators#BIOMETRIC_STRONG}等
         */
        public Builder setAuthenticators(int authenticators) {
            this.authenticators = authenticators;
            return this;
        }

        public CryptoManager build() {
            return new CryptoManager(activity, keyAlias, algorithm, blockMode,
                    encryptPadding, authenticators);
        }
    }

    private void savePwd(Cipher cipher, String username, String pwd, BiometricListener biometricListener) {
        try {
            byte[] buffer = cipher.doFinal(pwd.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            Log.i(TAG, "pwd encrypt: " + Base64.encodeToString(buffer, Base64.NO_WRAP));
            SharedPreferences.Editor editor = preferences.edit()
                    .putString(username, Base64.encodeToString(buffer, Base64.NO_WRAP));
            if (iv != null) {
                //SM4 没有偏移量
                String ivStr = Base64.encodeToString(iv, Base64.NO_WRAP);
                Log.i(TAG, "ivStr: " + ivStr);
                editor.putString(username + "_iv", ivStr);
            }
            editor.apply();
            if (biometricListener != null) {
                biometricListener.onBindSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG , "Encrypt Error", e);
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_ENCRYPT_EXCEPTION,
                        activity.getString(R.string.biometric_error_encrypt_exception));
            }
        }
    }

    /**
     * 是否支持授权密钥crypto
     * @param authenticators 授权类型
     * @return true: 支持{@link BiometricPrompt.CryptoObject}
     */
    private boolean cryptoIsSupported(int authenticators) {
        if ((BiometricManager.Authenticators.BIOMETRIC_WEAK & authenticators)
                == BiometricManager.Authenticators.BIOMETRIC_WEAK) {
            //由于BIOMETRIC_WEAK是0xFF, BIOMETRIC_STRONG是OxF, 所以要先判断WEAK
            return false;
        }
        return (BiometricManager.Authenticators.BIOMETRIC_STRONG & authenticators)
                == BiometricManager.Authenticators.BIOMETRIC_STRONG;
    }


    private void getPwd(Cipher cipher, String username, BiometricListener biometricListener) {
        try {
            String cipherPwd = preferences.getString(username, "");
            Log.i(TAG, "pwd encrypt: " + cipherPwd);
            byte[] buffer = cipher.doFinal(Base64.decode(cipherPwd, Base64.NO_WRAP));
            String pwd = new String(buffer, StandardCharsets.UTF_8);
            if (biometricListener != null) {
                biometricListener.onAuthSuccess(pwd);
            }
        } catch (Exception e) {
            Log.e(TAG, "Decrypt Error", e);
            if (biometricListener != null) {
                biometricListener.onAuthFailure(ERROR_DECRYPT_EXCEPTION,
                        activity.getString(R.string.biometric_error_decrypt_exception));
            }
        }
    }
}

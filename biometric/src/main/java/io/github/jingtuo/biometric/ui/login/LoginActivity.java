package io.github.jingtuo.biometric.ui.login;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.github.jingtuo.biometric.R;
import io.github.jingtuo.biometric.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Login";

    private static final String PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore";

    private static final String KEY_NAME_BIOMETRIC_LOGIN = "BiometricLogin";

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    private BiometricPrompt biometricPrompt;

    private BiometricManager biometricManager;

    private SharedPreferences preferences;

    private static final int LOGIN_STATE_BIND_BIOMETRIC = 1;

    private static final int LOGIN_STATE_BIOMETRIC_LOGIN = 2;

    private int loginState = LOGIN_STATE_BIND_BIOMETRIC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        preferences = getSharedPreferences("BiometricLogin", Context.MODE_PRIVATE);
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button bindBiometricBtn = binding.bindBiometric;
        final Button biometricLoginBtn = binding.biometricLogin;
        final ProgressBar loadingProgressBar = binding.loading;

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                bindBiometricBtn.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    if (LOGIN_STATE_BIND_BIOMETRIC == loginState) {
                        //登录成功之后, 进行生物识别再加密
                        bindBiometric(loginResult.getSuccess().getUsername(), loginResult.getSuccess().getPassword());
                    } else {
                        //生物识别登录
                        Toast.makeText(LoginActivity.this, "生物识别登录成功", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginState = LOGIN_STATE_BIND_BIOMETRIC;
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        bindBiometricBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginState = LOGIN_STATE_BIND_BIOMETRIC;
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });

        biometricLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                biometricLogin(usernameEditText.getText().toString());
            }
        });
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private BiometricPrompt.PromptInfo getBiometricPromptInfo() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric))
                .setDescription(getString(R.string.start_biometric))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private SecretKey generateSecretKey(String keyName) throws Exception {
        KeyGenParameterSpec.Builder build =  new KeyGenParameterSpec.Builder(keyName,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            build.setInvalidatedByBiometricEnrollment(true);
        }
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private SecretKey getSecretKey(String keyName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE);
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null);
        Key key = keyStore.getKey(keyName, null);
        if (key != null) {
            Log.i(TAG, "key has exits: " + key);
            return (SecretKey) key;
        }
        return generateSecretKey(keyName);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
    }

    /**
     * 绑定生物认证, 拉起生物认证
     * @param username
     * @param password
     */
    private void bindBiometric(String username, String password) {
        //先判断是否支持
        if (biometricManager == null) {
            biometricManager = BiometricManager.from(LoginActivity.this);
        }
        int flag = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        Log.i("Login",  "can auth result: " + flag);
        if (BiometricManager.BIOMETRIC_SUCCESS == flag) {
            //支持生物认证
            if (biometricPrompt == null) {
                biometricPrompt = new BiometricPrompt(LoginActivity.this,
                        ContextCompat.getMainExecutor(LoginActivity.this),
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);
                                Log.i("Login",  "Auth error: " + errorCode + ", " + errString);
                            }

                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                //生物识别成功, 加密密码
                                if (result.getCryptoObject() != null
                                        && result.getCryptoObject().getCipher() != null) {
                                    Cipher cipher = result.getCryptoObject().getCipher();
                                    try {
                                        byte[] buffer = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
                                        byte[] iv = cipher.getIV();
                                        preferences.edit().putString(username, Base64.encodeToString(buffer, Base64.NO_WRAP))
                                                .putString(username + "_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                                                .apply();
                                        Log.i("Login",  "encrypt pwd: success");
                                    }  catch (Exception e) {
                                        Log.i("Login",  "encrypt pwd: " + e.getMessage());
                                    }
                                }
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                                Log.i("Login",  "Auth failure");
                            }
                        });
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    Cipher cipher = getCipher();
                    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(KEY_NAME_BIOMETRIC_LOGIN));
                    biometricPrompt.authenticate(getBiometricPromptInfo(), new BiometricPrompt.CryptoObject(cipher));
                } catch (Exception e) {
                    Log.i("Login", e.getMessage(), e);
                }
            }
        } else if (BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE == flag) {
            Toast.makeText(LoginActivity.this, R.string.no_biometric_hardware,
                    Toast.LENGTH_SHORT).show();
        } else if (BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE == flag) {
            Toast.makeText(LoginActivity.this, R.string.biometric_hardware_security_low,
                    Toast.LENGTH_SHORT).show();
        } else if (BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED == flag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                registerForActivityResult(new BiometricContract(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Log.i("Login", "" + result.getResultCode() + ", "
                                + result.getData());
                    }
                });
            } else {
                Toast.makeText(LoginActivity.this, R.string.no_biometric_enroll,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this, "其他错误: " + flag,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void biometricLogin(String username){
        //先判断是否支持
        if (biometricManager == null) {
            biometricManager = BiometricManager.from(LoginActivity.this);
        }
        int flag = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        Log.i("Login",  "can auth result: " + flag);
        if (BiometricManager.BIOMETRIC_SUCCESS == flag) {
            //支持生物认证
            biometricPrompt = new BiometricPrompt(LoginActivity.this,
                    ContextCompat.getMainExecutor(LoginActivity.this),
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
                                    if (TextUtils.isEmpty(cipherPwd)) {
                                        Log.i("Login", "no bind biometric");
                                        return;
                                    }
                                    byte[] buffer = cipher.doFinal(Base64.decode(cipherPwd, Base64.NO_WRAP));
                                    String password = new String(buffer, StandardCharsets.UTF_8);
                                    loginState = LOGIN_STATE_BIOMETRIC_LOGIN;
                                    loginViewModel.login(username, password);
                                } catch (Exception e) {
                                    Log.i("Login", e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Log.i("Login", "Auth failure");
                        }
                    });
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    Cipher cipher = getCipher();
                    byte[] iv = Base64.decode(preferences.getString(username + "_iv", ""), Base64.NO_WRAP);
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(KEY_NAME_BIOMETRIC_LOGIN), new IvParameterSpec(iv));
                    biometricPrompt.authenticate(getBiometricPromptInfo(), new BiometricPrompt.CryptoObject(cipher));
                } catch (Exception e) {
                    Log.i("Login", e.getMessage(), e);
                }
            }
        } else if (BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE == flag) {
            Toast.makeText(LoginActivity.this, R.string.no_biometric_hardware,
                    Toast.LENGTH_SHORT).show();
        } else if (BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE == flag) {
            Toast.makeText(LoginActivity.this, R.string.biometric_hardware_security_low,
                    Toast.LENGTH_SHORT).show();
        } else if (BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED == flag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                registerForActivityResult(new BiometricContract(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Log.i("Login", "" + result.getResultCode() + ", "
                                + result.getData());
                    }
                });
            } else {
                Toast.makeText(LoginActivity.this, R.string.no_biometric_enroll,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this, "其他错误: " + flag,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
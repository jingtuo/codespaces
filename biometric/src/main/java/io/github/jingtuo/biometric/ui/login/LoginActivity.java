package io.github.jingtuo.biometric.ui.login;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.biometric.BiometricManager;
import androidx.lifecycle.ViewModelProvider;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;
import java.util.Set;

import io.github.jingtuo.biometric.R;
import io.github.jingtuo.biometric.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private static final String ALGORITHM = "AES";
    private static final String BLOCK_MODE = "CBC";

    private static final String ENCRYPT_PADDING = "PKCS7Padding";

    private static final String TAG = "Login";

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;
    private static final int LOGIN_STATE_BIND_BIOMETRIC = 1;

    private static final int LOGIN_STATE_BIOMETRIC_LOGIN = 2;

    private int loginState = LOGIN_STATE_BIND_BIOMETRIC;

    private CryptoManager cryptoManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final EditText etKeyAlias = binding.keyAlias;
        final Button bindBiometricBtn = binding.bindBiometric;
        final Button biometricLoginBtn = binding.biometricLogin;
        final ProgressBar loadingProgressBar = binding.loading;
        final AppCompatCheckBox cbStrong = binding.strong;
        final AppCompatCheckBox cbWeak = binding.weak;
        final AppCompatCheckBox cbDc = binding.deviceCredential;

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
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
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                Toast.makeText(LoginActivity.this, loginResult.getError(), Toast.LENGTH_SHORT).show();
            }
            if (loginResult.getSuccess() != null) {
                if (LOGIN_STATE_BIND_BIOMETRIC == loginState) {
                    //登录成功之后, 进行生物识别再加密
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        bindBiometric(loginResult.getSuccess().getUsername(),
                                loginResult.getSuccess().getPassword(),
                                etKeyAlias.getText().toString(),
                                getAuthenticators(cbStrong, cbWeak, cbDc));
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.biometric_error_no_hardware, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.biometric_login_success, Toast.LENGTH_SHORT).show();
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
                        passwordEditText.getText().toString(), etKeyAlias.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        etKeyAlias.addTextChangedListener(afterTextChangedListener);

        bindBiometricBtn.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            loginState = LOGIN_STATE_BIND_BIOMETRIC;
            loginViewModel.login(usernameEditText.getText().toString(),
                    passwordEditText.getText().toString());
        });

        biometricLoginBtn.setOnClickListener(v -> {
            loginState = LOGIN_STATE_BIOMETRIC_LOGIN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                biometricLogin(usernameEditText.getText().toString(),
                        etKeyAlias.getText().toString(),
                        getAuthenticators(cbStrong, cbWeak, cbDc));
            } else {
                Toast.makeText(LoginActivity.this, R.string.biometric_error_no_hardware, Toast.LENGTH_SHORT).show();
            }
        });

        Provider[] providers = Security.getProviders();
        for (Provider item: providers) {
            Log.i(TAG, "provider: " + item.getName() + ", info: " + item.getInfo());
            Set<Provider.Service> services = item.getServices();
            for (Provider.Service service: services) {
                Log.i(TAG, "find service: " + item.getName());
                if (service.getAlgorithm().startsWith("SM")) {
                    Log.i(TAG, "find provider: " + item.getName() + ", algorithm: " +
                            service.getAlgorithm());
                }
            }
        }
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());
        providers = Security.getProviders();
        Log.i(TAG, "after modify");
        for (Provider item: providers) {
            Log.i(TAG, "provider: " + item.getInfo());
            Set<Provider.Service> services = item.getServices();
            for (Provider.Service service: services) {
                if (service.getAlgorithm().startsWith("SM")) {
                    Log.i(TAG, "find provider: " + item.getName()
                            + ", algorithm: " + service.getAlgorithm()
                            + ", type: " + service.getType()
                            + ", class: " + service.getClassName());
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void bindBiometric(String username, String pwd, String keyAlias, int authenticators) {
        new CryptoManager.Builder()
                .setActivity(this)
                .setKeyAlias(keyAlias)
                .setAlgorithm(ALGORITHM)
                .setBlockMode(BLOCK_MODE)
                .setEncryptPadding(ENCRYPT_PADDING)
                .setAuthenticators(authenticators)
                .build().bindBiometric(username, pwd, new BiometricListener() {
            @Override
            public void onBindSuccess() {
                Toast.makeText(LoginActivity.this, R.string.bind_biometric_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthSuccess(String pwd) {

            }

            @Override
            public void onAuthFailure(int code, CharSequence msg) {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void biometricLogin(String username, String keyAlias, int authenticators) {
        //先判断是否支持
        new CryptoManager.Builder()
                .setActivity(this)
                .setKeyAlias(keyAlias)
                .setAlgorithm(ALGORITHM)
                .setBlockMode(BLOCK_MODE)
                .setEncryptPadding(ENCRYPT_PADDING)
                .setAuthenticators(authenticators)
                .build().biometricLogin(username, authenticators, new BiometricListener() {
            @Override
            public void onBindSuccess() {

            }

            @Override
            public void onAuthSuccess(String pwd) {
                loginViewModel.login(username, pwd);
            }

            @Override
            public void onAuthFailure(int code, CharSequence msg) {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getAuthenticators(AppCompatCheckBox cbStrong, AppCompatCheckBox cbWeak, AppCompatCheckBox cbDc) {
        int flag = 0;
        if (cbStrong.isChecked()) {
            flag |= BiometricManager.Authenticators.BIOMETRIC_STRONG;
        }
        if (cbWeak.isChecked()) {
            flag |= BiometricManager.Authenticators.BIOMETRIC_WEAK;
        }
        if (cbDc.isChecked()) {
            flag |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        }
        return flag;
    }
}
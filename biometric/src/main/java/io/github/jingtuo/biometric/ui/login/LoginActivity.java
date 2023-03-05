package io.github.jingtuo.biometric.ui.login;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import io.github.jingtuo.biometric.R;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private static final int LOGIN_STATE_BIND_BIOMETRIC = 1;

    private static final int LOGIN_STATE_BIOMETRIC_LOGIN = 2;

    private int loginState = LOGIN_STATE_BIND_BIOMETRIC;

    private CryptoManager cryptoManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button bindBiometricBtn = findViewById(R.id.bind_biometric);
        final Button biometricLoginBtn = findViewById(R.id.biometric_login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

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
                        bindBiometric(loginResult.getSuccess().getUsername(), loginResult.getSuccess().getPassword());
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
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginState = LOGIN_STATE_BIND_BIOMETRIC;
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
            return false;
        });

        bindBiometricBtn.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            loginState = LOGIN_STATE_BIND_BIOMETRIC;
            loginViewModel.login(usernameEditText.getText().toString(),
                    passwordEditText.getText().toString());
        });

        biometricLoginBtn.setOnClickListener(v -> {
            loginState = LOGIN_STATE_BIOMETRIC_LOGIN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                biometricLogin(usernameEditText.getText().toString());
            } else {
                Toast.makeText(LoginActivity.this, R.string.biometric_error_no_hardware, Toast.LENGTH_SHORT).show();
            }
        });

        cryptoManager = new CryptoManager.Builder()
                .setActivity(this)
                .setAlgorithm("AES")
                .setBlockMode("CBC")
                .setEncryptPadding("PKCS7Padding")
                .setKeystoreAlias("BiometricLogin")
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void bindBiometric(String username, String pwd) {
        cryptoManager.bindBiometric(username, pwd, new BiometricListener() {
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
    private void biometricLogin(String username) {
        //先判断是否支持
        cryptoManager.biometricLogin(username, new BiometricListener() {
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
}
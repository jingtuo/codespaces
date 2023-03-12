package io.github.jingtuo.biometric.ui.login;

import androidx.annotation.Nullable;

/**
 * Data validation state of the login form.
 */
class LoginFormState {
    @Nullable
    private Integer usernameError;
    @Nullable
    private Integer passwordError;

    @Nullable
    private Integer keyAliasError;
    private boolean isDataValid;

    LoginFormState(boolean isDataValid) {
        this.usernameError = null;
        this.passwordError = null;
        this.isDataValid = isDataValid;
    }

    LoginFormState(@Nullable Integer usernameError, @Nullable Integer passwordError, @Nullable Integer keyAliasError) {
        this.usernameError = usernameError;
        this.passwordError = passwordError;
        this.keyAliasError = keyAliasError;
        this.isDataValid = false;
    }

    @Nullable
    Integer getUsernameError() {
        return usernameError;
    }

    @Nullable
    Integer getPasswordError() {
        return passwordError;
    }

    boolean isDataValid() {
        return isDataValid;
    }
}
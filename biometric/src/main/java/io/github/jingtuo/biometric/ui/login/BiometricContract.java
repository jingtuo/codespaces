package io.github.jingtuo.biometric.ui.login;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;

/**
 * 生物认证-跳转协议
 * @author JingTuo
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public class BiometricContract extends ActivityResultContract<Void, ActivityResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Void input) {
        Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
        intent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return intent;
    }

    @Override
    public ActivityResult parseResult(int resultCode, @Nullable Intent intent) {
        return new ActivityResult(resultCode, intent);
    }
}

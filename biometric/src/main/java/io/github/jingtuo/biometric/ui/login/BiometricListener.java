package io.github.jingtuo.biometric.ui.login;

/**
 * 生物识别监听
 *
 * @author JingTuo
 */
public interface BiometricListener {

    /**
     * 绑定成功
     */
    void onBindSuccess();

    /**
     * 生物识别成功
     */
    void onAuthSuccess(String pwd);

    /**
     * 生物识别失败
     * @param code 错误代码
     * @param msg 错误信息
     */
    void onAuthFailure(int code, CharSequence msg);
}

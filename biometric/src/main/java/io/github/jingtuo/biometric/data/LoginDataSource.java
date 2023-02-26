package io.github.jingtuo.biometric.data;

import io.github.jingtuo.biometric.data.model.LoggedInUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 *
 * @author JingTuo
 */
public class LoginDataSource {

    /**
     * 根据登录用户名的数据
     */
    private final Map<String, LoggedInUser> users = new HashMap<>();

    public Result<LoggedInUser> login(String username, String password) {
        Result<LoggedInUser> result = new Result<>();
        LoggedInUser user = users.get(username);
        if (user == null) {
            user = new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            username, password);
            users.put(username, user);
            result.setSuccess(true);
            result.setData(user);
        } else {
            if (user.getPassword() != null && user.getPassword().equals(password)) {
                //密码相同
                result.setSuccess(true);
                result.setData(user);
            } else {
                result.setSuccess(false);
                result.setError("Logging failure");
            }
        }
        return result;
    }

    public void logout() {
        // TODO: revoke authentication
    }
}
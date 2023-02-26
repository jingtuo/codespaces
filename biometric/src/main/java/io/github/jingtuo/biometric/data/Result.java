package io.github.jingtuo.biometric.data;

/**
 * A generic class that holds a result success w/ data or an error exception.
 *
 * @author JingTuo
 */
public class Result<T> {

    private boolean success;

    private String error;

    private T data;

    @Override
    public String toString() {
        if (success) {
            return "Success[data=" + data.toString() + "]";
        } else {
            return "Error[error=" + error + "]";
        }
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public T getData() {
        return data;
    }
}
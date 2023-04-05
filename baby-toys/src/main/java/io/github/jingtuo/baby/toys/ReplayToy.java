package io.github.jingtuo.baby.toys;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * 重放玩具
 * 录制宝宝声音进行重放
 * 
 * 1.0版本
 *  由于播放文件使用的是同一文件, 所以每次都要{@link MediaPlayer#setDataSource(FileDescriptor)}
 * 
 * @author JingTuo
 */
public class ReplayToy implements Toy, MediaRecorder.OnInfoListener,
        MediaRecorder.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "Replay";

    private static final String FOLDER_NAME = "replay";

    private static final String TEMP_FILE_NAME = "temp.3gp";
    private MediaRecorder recorder;

    private MediaPlayer player;

    private Context context;

    private static final int NONE = 0;

    /**
     * 与{@link MediaPlayer}的Idle状态对应
     */
    private static final int INITIAL = 1;

    /**
     * {@link MediaRecorder#setAudioSource(int)}之后变更为此状态
     * {@link MediaPlayer#setDataSource(FileDescriptor)}之后变更为此状态
     */
    private static final int INITIALIZED = 2;

    /**
     * {@link MediaRecorder}专用的状态, 设置encoder, outputFormat等之后
     */
    private static final int DATA_SOURCE_CONFIGURED = 3;

    private static final int PREPARED = 4;

    /**
     * {@link MediaPlayer}专用的状态
     */
    private static final int PREPARING = 5;

    /**
     * {@link MediaRecorder}专用的状态
     */
    private static final int RECORDING = 6;

    /**
     * {@link MediaPlayer}专用的状态
     */
    private static final int STARTED = 7;

    private int recorderStatus = NONE;

    private int playerStatus = NONE;

    private OnStatusChangedListener onStatusChangedListener;
    public ReplayToy(Context context) {
        this.context = context;
    }

    private File createOutputFile() throws IOException {
        File folder = context.getExternalCacheDir();
        folder = new File(folder, FOLDER_NAME);
        if (!folder.exists() && !folder.mkdir()) {
            throw new IOException("create directory(" + folder.getPath() + " ) failure: ");
        }
        return new File(folder, TEMP_FILE_NAME);
    }

    @Override
    public void start() {
        if (RECORDING == recorderStatus) {
            //正在录制, 忽略启动操作
            return;
        }
        if (NONE != playerStatus) {
            //有播放状态, 释放播放资源
            releasePlayer();
        }
        //更新recorder
        if (NONE == recorderStatus) {
            //无状态, 需要重建
            initRecorder();
        } else {
            if (INITIAL != recorderStatus) {
                resetRecorder();
            }
        }
        try {
            preparedRecorder();
            startRecorder();
        } catch (Exception e) {
            releaseRecorder();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * 初始化recorder
     */
    private void initRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder = new MediaRecorder(context);
        } else {
            recorder = new MediaRecorder();
        }
        recorder.setOnInfoListener(this);
        recorder.setOnErrorListener(this);
        recorderStatus = INITIAL;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void preparedRecorder() throws IOException {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorderStatus = INITIALIZED;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
        //纯音频文件, 暂时选择3GPP格式
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(createOutputFile());
        //基于音频采样率高, 音频效果好, 采用默认AAC_ELD
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD);
        recorder.setMaxDuration(5000);
        recorderStatus = DATA_SOURCE_CONFIGURED;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
        recorder.prepare();
        recorderStatus = PREPARED;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void startRecorder() {
        recorder.start();
        recorderStatus = RECORDING;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void stopRecorder() {
        recorder.stop();
        recorderStatus = INITIAL;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void resetRecorder() {
        recorder.reset();
        recorderStatus = INITIAL;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void releaseRecorder() {
        recorder.release();
        recorder = null;
        recorderStatus = NONE;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    @Override
    public void stop() {
        if (RECORDING == recorderStatus) {
            //停止录制
            stopRecorder();
        }
        if (NONE != recorderStatus) {
            releaseRecorder();
        }
        if (NONE != playerStatus) {
            releasePlayer();
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
            stopRecorder();
            //开始播放录制音频
            if (NONE == playerStatus) {
                initPlayer();
            } else {
                resetPlayer();
            }
            preparePlayer();
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {

    }

    private void initPlayer() {
        player = new MediaPlayer();
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        playerStatus = INITIAL;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void preparePlayer() {
        try(FileInputStream fis = new FileInputStream(createOutputFile())) {
            player.setDataSource(fis.getFD());
            playerStatus = INITIALIZED;
            if (onStatusChangedListener != null) {
                onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
            }
            player.prepareAsync();
            playerStatus = PREPARING;
            if (onStatusChangedListener != null) {
                onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
            }
        } catch (Exception e) {
            releasePlayer();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void resetPlayer() {
        player.reset();
        playerStatus = INITIAL;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    private void releasePlayer() {
        player.release();
        player = null;
        playerStatus = NONE;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        playerStatus = PREPARED;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
        player.start();
        playerStatus = STARTED;
        if (onStatusChangedListener != null) {
            onStatusChangedListener.onStatusChanged(recorderStatus, playerStatus);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            //播放完毕, 重新开始录制, 需要释放之前player资源
            releasePlayer();
            preparedRecorder();
            startRecorder();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    public void release() {
        if (NONE != recorderStatus) {
            releaseRecorder();
        }
        if (NONE != playerStatus) {
            releasePlayer();
        }
    }

    public boolean isRecording() {
        return RECORDING == recorderStatus;
    }

    public boolean isPlaying() {
        return STARTED == playerStatus;
    }

    public boolean isStopped() {
        return NONE == recorderStatus && NONE == playerStatus;
    }

    public interface OnStatusChangedListener {
        /**
         *
         * @param recorderStatus 录制状态
         * @param playerStatus 播放状态
         */
        void onStatusChanged(int recorderStatus, int playerStatus);
    }

    public void setOnStatusChangedListener(OnStatusChangedListener onStatusChangedListener) {
        this.onStatusChangedListener = onStatusChangedListener;
    }
}

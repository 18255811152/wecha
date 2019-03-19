package com.sk.weichat.xmpp;

import android.content.Context;
import android.util.Log;

import com.sk.weichat.MyApplication;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.UserCheckedActivity;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ReconnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

/**
 * 当网络发生改变<由无网络变成有网络>的时候，会调用该类的重连线程 {@link #setNetWorkState(boolean)}-->重连{@link #reconnect()} 或 停止重连{@link #mReconnectionThread}{@link Thread#interrupt()};
 */
public class XReconnectionManager extends AbstractConnectionListener {
    boolean mIsNetWorkActive;
    // Holds the state of the reconnection
    boolean doReconnecting = false;
    private Context mContext;
    private XMPPTCPConnection mConnection;
    private boolean isReconnectionAllowed = false;
    private Thread mReconnectionThread;
    private ReconnectionManager mReconnectionManager;

    public XReconnectionManager(Context context, XMPPTCPConnection connection, boolean reconnectionAllowed, boolean isNetWorkActive) {
        mContext = context;
        mConnection = connection;
        mConnection.addConnectionListener(this);
        isReconnectionAllowed = reconnectionAllowed;
        mIsNetWorkActive = isNetWorkActive;

        // 不自己重连，改为smack内部的自动重连
        mReconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        mReconnectionManager.enableAutomaticReconnection();
        mReconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY);
        mReconnectionManager.addReconnectionListener(new ReconnectionListener() {
            @Override
            public void reconnectingIn(int seconds) {
                Log.e("zq", "重连中..." + seconds);
            }

            @Override
            public void reconnectionFailed(Exception e) {
                Log.e("zq", "重连失败" + e.getMessage());
            }
        });
    }

    /**
     * Returns true if the reconnection mechanism is enabled.
     *
     * @return true if automatic reconnections are allowed.
     */
    private boolean isReconnectionAllowed() {
        return doReconnecting && mIsNetWorkActive && !mConnection.isConnected() && isReconnectionAllowed;
    }

    public void setNetWorkState(boolean isNetWorkActive) {
        mIsNetWorkActive = isNetWorkActive;
        if (mIsNetWorkActive) {// 网络状态变为可用
            if (isReconnectionAllowed()) {
                reconnect();
            }
        } else {// 网络不可用,断开重连线程
            if (mReconnectionThread != null && mReconnectionThread.isAlive()) {
                mReconnectionThread.interrupt();
            }
        }
    }

    public void restartConnection() {
        doReconnecting = true;
        if (this.isReconnectionAllowed()) {
            if (CoreService.DEBUG)
                this.reconnect();
        }
    }

    void release() {
        doReconnecting = false;
        if (mReconnectionThread != null && mReconnectionThread.isAlive()) {
            mReconnectionThread.interrupt();
        }
        if (mReconnectionManager != null) {// 停止重连
            mReconnectionManager.disableAutomaticReconnection();
        }
    }

    /**
     * Starts a reconnection mechanism if it was configured to do that. The algorithm is been executed when the first connection error is detected.
     * <p/>
     * The reconnection mechanism will try to reconnect periodically in this way:
     * <ol>
     * <li>First it will try 6 times every 10 seconds.
     * <li>Then it will try 10 times every 1 minute.
     * <li>Finally it will try indefinitely every 5 minutes.
     * </ol>
     */
    private synchronized void reconnect() {
        //        if (this.isReconnectionAllowed()) {
        //            // Since there is no thread running, creates a new one to attempt
        //            // the reconnection.
        //            // avoid to run duplicated reconnectionThread -- fd: 16/09/2010
        //            if (mReconnectionThread != null && mReconnectionThread.isAlive()) {
        //                return;
        //            }
        //            mReconnectionThread = new Thread() {
        //
        //                private int mRandomBase = new Random().nextInt(11) + 5; // between 5 and 15 seconds
        //                /**
        //                 * Holds the current number of reconnection attempts
        //                 */
        //                private int attempts = 0;
        //
        //                /**
        //                 * Returns the number of seconds until the next reconnection attempt.
        //                 *
        //                 * @return the number of seconds until the next reconnection attempt.
        //                 */
        //                private int timeDelay() {
        //                    attempts++;
        //                    if (attempts > 13) {
        //                        return mRandomBase * 6 * 5; // between 2.5 and 7.5
        //                        // minutes (~5 minutes)
        //                    }
        //                    if (attempts > 7) {
        //                        return mRandomBase * 6; // between 30 and 90 seconds (~1
        //                        // minutes)
        //                    }
        //                    return mRandomBase; // 10 seconds
        //                }
        //
        //                /**
        //                 * The process will try the reconnection until the connection succeed or the user cancel it
        //                 */
        //                public void run() {
        //                    Log.e("zq", "网络发生改变，开始重连");
        //                    while (isReconnectionAllowed()) {
        //                        // Makes a reconnection attempt
        //                        try {
        //                            if (isReconnectionAllowed()) {
        //                                Log.e("zq", "网络发生改变，正在重连...");
        //                                mConnection.connect();
        //                            }
        //                        } catch (Exception e) {
        //                            notifyReconnectionFailed(e);// Fires the failed reconnection notification
        //                        }
        //
        //                        int remainingSeconds = timeDelay();
        //                        while (isReconnectionAllowed() && remainingSeconds > 0) {
        //                            try {
        //                                Thread.sleep(1000);
        //                                remainingSeconds--;
        //                                notifyAttemptToReconnectIn(remainingSeconds);
        //                            } catch (InterruptedException e1) {
        //                                // Notify the reconnection has failed
        //                                notifyReconnectionFailed(e1);
        //                            }
        //                        }
        //                    }
        //                }
        //            };
        //            mReconnectionThread.setName("Smack XReconnectionManager");
        //            mReconnectionThread.setDaemon(true);
        //            mReconnectionThread.start();
        //        }
    }

    private void notifyReconnectionFailed(Exception exception) {
/*
        Log.e("zq", "网络发生改变，重连发生异常");
        if (isReconnectionAllowed()) {
            for (ConnectionListener listener : mConnection.getConnectionListeners()) {
                listener.reconnectionFailed(exception);
            }
        }
*/
    }

    private void notifyAttemptToReconnectIn(int seconds) {
/*
        if (isReconnectionAllowed()) {
            for (ConnectionListener listener : mConnection.getConnectionListeners()) {
                listener.reconnectingIn(seconds);
            }
        }
*/
    }

    private void conflict() {
        ((CoreService) mContext).logout();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
        // 弹出对话框
        UserCheckedActivity.start(mContext);
        // LoginHelper.broadcastConflict(mContext);
    }

    @Override
    public void connectionClosed() {// 连接关闭，不允许自动重连
        doReconnecting = false;
    }

    /**
     * @param e
     */
    @Override
    public void connectionClosedOnError(Exception e) {
        doReconnecting = true;
        if (e instanceof StreamErrorException) {// 重复登录
            StreamErrorException streamErrorException = (StreamErrorException) e;
            StreamError streamError = streamErrorException.getStreamError();

            if (streamError.getCondition().equals(StreamError.Condition.conflict)) {// 下线通知
                if (CoreService.DEBUG)
                    Log.d(CoreService.TAG, "异常断开，有另外设备登陆啦");
                conflict();
                doReconnecting = false;
                return;
            }
        }
        // 因为其他原因导致下线，那么就开始重连
        if (this.isReconnectionAllowed()) {
            if (CoreService.DEBUG)
                Log.d(CoreService.TAG, "异常断开，开始重连");
            this.reconnect();
        }
    }
}

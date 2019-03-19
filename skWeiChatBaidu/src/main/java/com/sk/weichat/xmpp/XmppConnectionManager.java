package com.sk.weichat.xmpp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.sk.weichat.MyApplication;
import com.sk.weichat.Reporter;
import com.sk.weichat.adapter.MessageEventBG;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.HttpUtil;
import com.sk.weichat.xmpp.util.XmppStringUtil;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.Callable;

import de.greenrobot.event.EventBus;

/**
 * XMPP连接类
 */
public class XmppConnectionManager {
    /* Handler */
    private static final int MSG_CONNECTING = 0;                 // 连接中...
    private static final int MSG_CONNECTED = 1;                   // 已连接
    private static final int MSG_AUTHENTICATED = 2;            // 已认证
    private static final int MSG_CONNECTION_CLOSED = 3;  // 连接关闭
    private static final int MSG_CONNECTION_CLOSED_ON_ERROR = 4; // 连接错误
    public static int mXMPPCurrentState;
    private Context mContext;
    private NotifyConnectionListener mNotifyConnectionListener;
    @SuppressLint("HandlerLeak")
    private Handler mNotifyConnectionHandler = new Handler() {
        public void handleMessage(Message msg) {
            mXMPPCurrentState = msg.what;
            Log.e("zq", "当前XMPP连接状态:" + mXMPPCurrentState);
            if (msg.what == MSG_CONNECTING) {
                if (mNotifyConnectionListener != null) {
                    mNotifyConnectionListener.notifyConnecting();
                }
            } else if (msg.what == MSG_CONNECTED) {
                XMPPConnection connection = (XMPPConnection) msg.obj;
                if (mNotifyConnectionListener != null) {
                    mNotifyConnectionListener.notifyConnected(connection);
                }
            } else if (msg.what == MSG_AUTHENTICATED) {
                XMPPConnection connection = (XMPPConnection) msg.obj;
                if (mNotifyConnectionListener != null) {
                    mNotifyConnectionListener.notifyAuthenticated(connection);
                }
            } else if (msg.what == MSG_CONNECTION_CLOSED) {
                if (mNotifyConnectionListener != null) {
                    mNotifyConnectionListener.notifyConnectionClosed();
                }
            } else if (msg.what == MSG_CONNECTION_CLOSED_ON_ERROR) {
                if (mNotifyConnectionListener != null) {
                    Exception e = (Exception) msg.obj;
                    mNotifyConnectionListener.notifyConnectionClosedOnError(e);
                }
            }
        }
    };
    private XMPPTCPConnection mConnection;
    private AbstractConnectionListener mAbstractConnectionListener = new AbstractConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {
            Log.e("zq", "connected：已连接");
            Message msg = mNotifyConnectionHandler.obtainMessage(MSG_CONNECTED);
            msg.obj = connection;
            msg.sendToTarget();
        }

        @Override
        public void authenticated(final XMPPConnection connection, boolean resumed) {
            Log.e("authenticated", "authenticated：认证成功");
            Log.e("authenticated", "resumed-->" + resumed);

            Message msg = mNotifyConnectionHandler.obtainMessage(MSG_AUTHENTICATED);
            msg.obj = connection;
            msg.sendToTarget();

            if (mConnection.isSmResumptionPossible()) {
                Log.e("zq", "服务端开启了流");
            } else {
                Log.e("zq", "服务端关闭了流");
                MyApplication.IS_OPEN_RECEIPT = true;// 检查服务器是否启用了流管理，如关闭本地请求回执标志位一定为true
            }
        }

        @Override
        public void connectionClosed() {
            Log.e("zq", "connectionClosed：连接关闭");
            mNotifyConnectionHandler.sendEmptyMessage(MSG_CONNECTION_CLOSED);

            EventBus.getDefault().post(new MessageEventBG(false));
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            Log.e("zq", "connectionClosedOnError：连接异常");
            Log.e("zq", "connectionClosedOnError：" + e.getMessage());
            Reporter.post("xmpp connectionClosedOnError,", e);
            Message msg = mNotifyConnectionHandler.obtainMessage(MSG_CONNECTION_CLOSED_ON_ERROR);
            msg.obj = e;
            msg.sendToTarget();

            EventBus.getDefault().post(new MessageEventBG(false));
        }
    };
    private XReconnectionManager mReconnectionManager;
    private XServerReceivedListener XServerReceivedListener;
    private boolean mIsNetWorkActive;// 当前网络是否连接上
    private boolean doLogining = false;
    private String mLoginUserId;  // 仅用于登陆失败，重新登陆用
    private String mLoginPassword;// 仅用于登陆失败，重新登陆用
    private LoginThread mLoginThread;
    private BroadcastReceiver mNetWorkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }
            Log.e("zq", "监测到网络改变");
            mIsNetWorkActive = isGprsOrWifiConnected();
            if (isAuthenticated()) {
                Log.e("zq", "XMPP已认证，Return");
                return;
            }
            if (mIsNetWorkActive) {// 有网
                if (isLoginAllowed()) {
                    Log.e("zq", "有网，开始登录");

                    login(mLoginUserId, mLoginPassword);
                }
            } else {// 无网
                Log.e("zq", "无网");
                if (mLoginThread != null && mLoginThread.isAlive()) {
                    Log.e("zq", "无网且登录线程isAlive,打断该线程");
                    mLoginThread.interrupt();
                }
            }
            mReconnectionManager.setNetWorkState(mIsNetWorkActive);
        }
    };

    public XmppConnectionManager(Context context, NotifyConnectionListener listener) {
        mContext = context;
        mNotifyConnectionListener = listener;

        mConnection = new XMPPTCPConnection(getConnectionConfiguration());
        mConnection.addConnectionListener(mAbstractConnectionListener);

        initNetWorkStatusReceiver();
        mReconnectionManager = new XReconnectionManager(mContext, mConnection, true, mIsNetWorkActive);

        XServerReceivedListener = new XServerReceivedListener();
        mConnection.addStanzaAcknowledgedListener(XServerReceivedListener);
    }

    private XMPPTCPConnectionConfiguration getConnectionConfiguration() {
        final String mXmppHost = CoreManager.requireConfig(MyApplication.getInstance()).XMPPHost;
        int mXmppPort = CoreManager.requireConfig(MyApplication.getInstance()).mXMPPPort;
        String mXmppDomain = CoreManager.requireConfig(MyApplication.getInstance()).XMPPDomain;

        DomainBareJid mDomainBareJid = null;
        try {
            mDomainBareJid = JidCreate.domainBareFrom(mXmppDomain);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        InetAddress address = AsyncUtils.forceAsync(new Callable<InetAddress>() {
            @Override
            public InetAddress call() throws Exception {
                try {
                    return InetAddress.getByName(mXmppHost);
                } catch (Exception e) {
                    return null;
                }
            }
        });

        XMPPTCPConnectionConfiguration mConnectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                .setHostAddress(address) // 服务器地址
                .setPort(mXmppPort)      // 服务器端口
                .setXmppDomain(mDomainBareJid)
                .setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.disabled) // 是否开启安全模式
                .setCompressionEnabled(false)
                .setSendPresence(false)
                .build();
        return mConnectionConfiguration;
    }

    public XMPPTCPConnection getConnection() {
        return mConnection;
    }

    public boolean isAuthenticated() {
        return mConnection != null && mConnection.isConnected() && mConnection.isAuthenticated();
    }

    private boolean isLoginAllowed() {
        return doLogining && mIsNetWorkActive && (!mConnection.isConnected() || !mConnection.isAuthenticated());
    }

    /*********************
     * 网络连接状态
     ***************/
    private void initNetWorkStatusReceiver() {
        // 获取程序启动时的网络状态
        mIsNetWorkActive = isGprsOrWifiConnected();
        // 注册网络监听广播
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetWorkChangeReceiver, intentFilter);
    }

    private boolean isGprsOrWifiConnected() {
        if (!HttpUtil.isGprsOrWifiConnected(mContext)) {
            logout();
        } else {
            if (!TextUtils.isEmpty(CoreManager.requireSelf(mContext).getUserId())
                    && !TextUtils.isEmpty(CoreManager.requireSelf(mContext).getPassword()))
                login(CoreManager.requireSelf(mContext).getUserId(), CoreManager.requireSelf(mContext).getPassword());
        }

        return true;
    }

    public synchronized void login(final String userId, final String password) {
        if (mConnection.isAuthenticated()) {
            /*// 如果已经登陆
            if (StringUtils.parseName(mConnection.getUser()).equals(userId)) {
                // 如果登陆的用户和需要在登陆的是同一个用户，赋予可能改变的用户名和密码，返回
                return;
            } else {
                mConnection.disconnect();
            }*/
            return;
        }

        if (mLoginThread != null && mLoginThread.isAlive()) {
            // 正在进行上一个用户的登陆中，或者用户密码变更，但是还在登陆中
            if (mLoginThread.isSameUser(userId, password)) {
                if (mLoginThread.getAttempts() > 13) {
                    // 当尝试次数大于13的时候，尝试的时间变得太长，果断结束点，开始一次新的尝试
                    mLoginThread.interrupt();
                    doLogining = false;
                } else {
                    return;
                }
            } else {
                // 和之前在尝试登陆的用户属性一致，结束这个登陆的线程
                mLoginThread.interrupt();
                doLogining = false;
            }
        }
        // 等待上一个登陆线程的结束，才开始下一个
        long time = System.currentTimeMillis();
        while (mLoginThread != null && mLoginThread.isAlive()) {
            if (System.currentTimeMillis() - time > 3000) {
                // 防止结束线程时异常了，卡住主线程
                break;
            }
        }
        doLogining = true;
        mLoginUserId = userId;
        mLoginPassword = password;

        mLoginThread = new LoginThread(userId, password);
        mLoginThread.start();
    }

    public void reConnection() {
        if (mReconnectionManager != null) {
            if (CoreManager.requireSelf(mContext).getUserId() != null) {
                mReconnectionManager.restartConnection();
            }
        }
    }

    void logout() {
        doLogining = false;
        if (mLoginThread != null && mLoginThread.isAlive()) {
            mLoginThread.interrupt();
        }
        if (mReconnectionManager != null) {
            mReconnectionManager.release();
        }
        if (mConnection == null) {
            return;
        }

        presenceOffline();

        if (mConnection.isConnected()) {
            Log.e("zq", "断开连接" + 3);
            mConnection.disconnect();
        }
    }

    void release() {
        mContext.unregisterReceiver(mNetWorkChangeReceiver);
        doLogining = false;
        if (mLoginThread != null && mLoginThread.isAlive()) {
            mLoginThread.interrupt();
        }
        mReconnectionManager.release();

        presenceOffline();

        if (mConnection != null && mConnection.isConnected()) {
            Log.e("zq", "断开连接" + 4);
            mConnection.disconnect();
        }
    }

    void sendOnLineMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                presenceOnline();
            }
        }).start();
    }

    private void presenceOnline() {
        Presence presence = new Presence(Presence.Type.available);
        try {
            try {
                mConnection.sendStanza(presence);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (NotConnectedException e) {
            e.printStackTrace();
        }
    }

    private void presenceOffline() {
        Presence presence = new Presence(Presence.Type.unavailable);
        try {
            try {
                mConnection.sendStanza(presence);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (NotConnectedException e) {
            e.printStackTrace();
        }
    }

    private class LoginThread extends Thread {
        private String loginUserId;
        private String loginPassword;
        private int attempts;
        private int randomBase = new Random().nextInt(11) + 5; // between 5 and 15seconds

        LoginThread(String loginUserId, String loginPassword) {
            this.loginUserId = loginUserId;
            this.loginPassword = loginPassword;
            this.setName("Xmpp Login Thread" + loginUserId);
        }

        public boolean isSameUser(String userId, String password) {
            if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(password)) {
                return false;
            }
            return loginUserId.equals(userId) && loginPassword.equals(password);
        }

        public int getAttempts() {
            return attempts;
        }

        /**
         * Returns the number of seconds until the next reconnection attempt.
         *
         * @return the number of seconds until the next reconnection attempt.
         */
        private int timeDelay() {
            attempts++;
            if (attempts > 13) {
                return randomBase * 6 * 5; // between 2.5 and 7.5 minutes
            }
            if (attempts > 7) {
                return randomBase * 6; // between 30 and 90 seconds (~1 minutes)
            }
            return randomBase; // 10 seconds
        }

        public void run() {
            while (isLoginAllowed()) {
                mNotifyConnectionHandler.sendEmptyMessage(MSG_CONNECTING);
                try {
                    if (!mConnection.isConnected()) {
                        try {
                            mConnection.connect();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                       /* TrafficStats.setThreadStatsTag(0x01);
                        try {
                            ((XMPPTCPConnection) mConnection).connectWithoutLogin();
                        } catch (Exception e) {
                            // 捕获到异常
                        } finally {
                            TrafficStats.clearThreadStatsTag();
                        }*/
                    }

                    // 登录XMPP
                    // resource 改为全局变量
                    Resourcepart mResourcepart;
                    if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {
                        mResourcepart = Resourcepart.fromOrThrowUnchecked("android");
                    } else {
                        mResourcepart = Resourcepart.fromOrThrowUnchecked("youjob");
                    }

                    try {
                        mConnection.login(loginUserId, loginPassword, mResourcepart);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mConnection.isAuthenticated()) {// 登录成功 已验证
                        PingManager.getInstanceFor(mConnection).setPingInterval(CoreManager.requireConfig(MyApplication.getInstance()).xmppPingTime);
                        PingManager.getInstanceFor(mConnection).registerPingFailedListener(new PingFailedListener() {// 注册PING机制失败回调
                            @Override
                            public void pingFailed() {
                                Log.e("zq", "ping 失败了");
                                reConnection();// PING失败了，重连
                            }
                        });
                    } else {
                        Log.e("zq", "断开连接" + 1);
                        mConnection.disconnect();
                    }
                } catch (SmackException | IOException e) {
                    // Todo if SASL Authentication failed. No know authentication mechanisims. Need import Smack-sasl-provided.jar
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                    if (!TextUtils.isEmpty(e.getMessage())
                            && e.getMessage().contains("not-authorized")) { // org.jivesoftware.smack.sasl.SASLErrorException: SASLError using PLAIN: not-authorized
                        MyApplication.getInstance().sendBroadcast(new Intent(Constants.NOT_AUTHORIZED));
                    }
                    return;
                }
                if (mConnection.isAuthenticated()) {
                    if (!XmppStringUtil.parseName(mConnection.getUser().toString()).equals(loginUserId)) {
                        Log.e("zq", "断开连接" + 2);
                        mConnection.disconnect();
                    } else {
                        doLogining = false;
                        // mAbstractConnectionListener.authenticated(mConnection);
                    }
                } else {
                    // Find how much time we should wait until the next try
                    int remainingSeconds = timeDelay();
                    Log.d("roamer", "login try delay：remainingSeconds：" + remainingSeconds);
                    while (isLoginAllowed() && remainingSeconds > 0) {
                        Log.d("roamer", "login try delay");
                        try {
                            Thread.sleep(1000);
                            remainingSeconds--;
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}

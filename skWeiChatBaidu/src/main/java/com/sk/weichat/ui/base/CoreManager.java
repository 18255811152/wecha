package com.sk.weichat.ui.base;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.ConfigBean;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.UserStatus;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.bean.redpacket.Balance;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.sp.UserSp;
import com.sk.weichat.ui.UserCheckedActivity;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.sk.weichat.xmpp.CoreService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

public class CoreManager {
    public static final String KEY_CONFIG_BEAN = "configBean";
    private static final String TAG = "CoreManager";
    private static AppConfig staticConfig = null;
    private static User staticSelf = null;
    private static UserStatus staticSelfStatus = null;
    private boolean loginRequired;
    private boolean configRequired;
    private User self = null;
    private UserStatus selfStatus = null;
    private Limit limit = new Limit(this);
    private AppConfig config = null;
    private BaseLoginActivity ctx;
    private CoreStatusListener coreStatusListener;
    // 绑定Service成功的回调，
    // TODO: 要改成登录成功才回调，
    private Runnable connectedCallback;
    private CoreService mService;
    private boolean isBind = false;
    // 当前绑定服务的连接，
    private ServiceConnection mCoreServiceConnection;

    CoreManager(BaseLoginActivity ctx, CoreStatusListener coreStatusListener) {
        this.ctx = ctx;
        this.coreStatusListener = coreStatusListener;
    }

    private static SharedPreferences getSharedPreferences(Context ctx) {
        return ctx.getSharedPreferences("core_manager", Context.MODE_PRIVATE);
    }

    // TODO: 全局调用都要去掉，
    @SuppressWarnings("WeakerAccess")
    public static AppConfig requireConfig(Context ctx) {
        if (staticConfig == null) {
            synchronized (CoreManager.class) {
                if (staticConfig == null) {
                    ConfigBean configBean = readConfigBean(ctx);
                    if (configBean == null) {
                        configBean = new ConfigBean();
                        // TODO: 第一次打开获取config前可能就调用了这个方法，外面封装的问题，
                        // 这不可能，第一次打开必须拿到config, 之后本地必须有，
                        Reporter.unreachable();
                    }
                    setStaticConfig(ctx, AppConfig.initConfig(configBean));
                }
            }
        }
        Log.d(TAG, "requireConfig() returned: " + staticConfig);
        return staticConfig;
    }

    private static ConfigBean readConfigBean(Context ctx) {
        String configBeanJson = getSharedPreferences(ctx)
                .getString(KEY_CONFIG_BEAN, null);
        if (TextUtils.isEmpty(configBeanJson)) {
            return null;
        }
        return JSON.parseObject(configBeanJson, ConfigBean.class);
    }


    // 刷新用户余额
    public static void updateMyBalance() {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", staticSelfStatus.accessToken);
        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        Balance b = result.getData();
                        if (b != null) {
                            staticSelf.setBalance(b.getBalance());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    public static void appBackstage(Context context, boolean isBack) {
        if (staticSelf == null || staticSelfStatus == null) {
            return;
        }
        String str;
        User self = staticSelf;
        if (isBack) {
            str = "程序已到后台";

            Log.e("zq", str + "，开始--》将离线时间本存至本地");
            long time = System.currentTimeMillis() / 1000;
            CoreManager.saveOfflineTime(context, time);
            UserDao.getInstance().updateUnLineTime(self.getUserId(), time);
            Log.e("zq", str + "，结束--》将离线时间本存至本地");
            Log.e("zq", "弹出了无响应？");
        } else {
            str = "XMPP连接关闭 || 异常断开";
        }

        Log.e("zq", str + "，开始--》调用outTime接口");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", staticSelfStatus.accessToken);
        params.put("userId", self.getUserId());
        StringJsonObjectRequest<User> request = new StringJsonObjectRequest<>(CoreManager.requireConfig(MyApplication.getInstance()).USER_OUTTIME,
                null, null, null, params);
        MyApplication.getInstance().getFastVolley().addShortRequest(TAG + "@", request);
    }

    // TODO: 全局调用都要去掉，
    public static void saveOfflineTime(Context context, long outTime) {
        PreferenceUtils.putLong(context, Constants.OFFLINE_TIME, outTime);
        if (staticSelf != null) {
            staticSelf.setOfflineTime(outTime);
        }
    }

    // TODO: 全局调用都要去掉，
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static User requireSelf(Context ctx) {
        if (staticSelf == null) {
            synchronized (CoreManager.class) {
                if (staticSelf == null) {
                    String userId = UserSp.getInstance(ctx).getUserId("");
                    User user = UserDao.getInstance().getUserByUserId(userId);
                    if (user == null) {
                        Reporter.post("登录的User为空，");
                        // 无论如何没有登录信息就跳到重新登录，
                        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
                        // 弹出对话框
                        UserCheckedActivity.start(ctx);
                        // 就算没有登录也不能返回null, 直接把本地过期的返回以便正常初始化，然后finish页面，
                        user = new User();
                    }
                    setStaticSelf(user);
                }
            }
        }
        Log.d(TAG, "requireSelfUser() returned: " + staticSelf);
        return staticSelf;
    }

    // TODO: 全局调用都要去掉，
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static User getSelf(Context ctx) {
        if (staticSelf == null) {
            synchronized (CoreManager.class) {
                if (staticSelf == null) {
                    String userId = UserSp.getInstance(ctx).getUserId("");
                    setStaticSelf(UserDao.getInstance().getUserByUserId(userId));
                }
            }
        }
        Log.d(TAG, "requireSelfUser() returned: " + staticSelf);
        return staticSelf;
    }

    // TODO: 全局调用都要去掉，
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static UserStatus requireSelfStatus(Context ctx) {
        if (staticSelfStatus == null) {
            synchronized (CoreManager.class) {
                if (staticSelfStatus == null) {
                    UserStatus info = new UserStatus();
                    info.accessToken = UserSp.getInstance(ctx).getAccessToken(null);

                    if (TextUtils.isEmpty(info.accessToken)) {
                        Reporter.post("登录的accessToken为空，");
                        // 无论如何没有登录信息就跳到重新登录，
                        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
                        // 弹出对话框
                        UserCheckedActivity.start(ctx);
                        // 就算没有登录也不能返回null, 直接把本地过期的返回以便正常初始化，然后finish页面，
                    }
                    setStaticSelfStatus(info);
                }
            }
        }
        return staticSelfStatus;
    }

    // TODO: 全局调用都要去掉，
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static UserStatus getSelfStatus(Context ctx) {
        if (staticSelfStatus == null) {
            synchronized (CoreManager.class) {
                if (staticSelfStatus == null) {
                    UserStatus info = new UserStatus();
                    info.accessToken = UserSp.getInstance(ctx).getAccessToken(null);
                    if (!TextUtils.isEmpty(info.accessToken)) {
                        setStaticSelfStatus(info);
                    }
                }
            }
        }
        return staticSelfStatus;
    }

    private static void setStaticConfig(Context ctx, AppConfig config) {
        staticConfig = config;
        Reporter.putUserData("configUrl", AppConfig.readConfigUrl(ctx));
        if (config != null) {
            Reporter.putUserData("apiUrl", config.apiUrl);
        }
    }

    /**
     * 登录和读取本地用户信息时会调用，
     * 顺便设置到bugly上，
     */
    private static void setStaticSelf(User self) {
        staticSelf = self;
        if (self != null) {
            Reporter.setUserId(self.getTelephone());
            Reporter.putUserData("userId", self.getUserId());
            Reporter.putUserData("telephone", self.getTelephone());
            // Reporter.putUserData("password", self.getPassword());
            Reporter.putUserData("nickName", self.getNickName());
        }
    }

    /**
     * 登录和读取本地用户信息时会调用，
     * 顺便设置到bugly上，
     */
    private static void setStaticSelfStatus(UserStatus selfStatus) {
        staticSelfStatus = selfStatus;
        if (selfStatus != null) {
            Reporter.putUserData("accessToken", selfStatus.accessToken);
        }
    }

    // 创建一个服务连接，重连时要创建新的，
    private ServiceConnection createCoreServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                // 可能后台Service断掉，然后回到前台时不会自动重新绑定，
                Log.d(TAG, "onServiceDisconnected() called with: name = [" + name + "]");
                mService = null;
                mCoreServiceConnection = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected() called with: name = [" + name + "], service = [" + service + "]");
                mService = ((CoreService.CoreServiceBinder) service).getService();
                mCoreServiceConnection = this;
                // 改成传进CoreService里面回调，
                if (connectedCallback != null) {
                    connectedCallback.run();
                }
            }
        };
    }

    public ConfigBean readConfigBean() {
        return readConfigBean(ctx);
    }

    public void saveConfigBean(ConfigBean configBean) {
        getSharedPreferences(ctx)
                .edit()
                .putString(KEY_CONFIG_BEAN, JSON.toJSONString(configBean))
                .apply();
        config = AppConfig.initConfig(configBean);
        setStaticConfig(ctx, config);
    }

    public AppConfig getConfig() {
        if (config == null) {
            config = requireConfig(ctx);
        }
        return config;
    }

    public User getSelf() {
        return self;
    }

    /**
     * 登录时会调用该方法保存用户信息，
     */
    public void setSelf(User self) {
        this.self = self;
        setStaticSelf(self);
    }

    public UserStatus getSelfStatus() {
        return selfStatus;
    }

    /**
     * 登录时会调用该方法保存用户信息，
     */
    public void setSelfStatus(UserStatus selfStatus) {
        this.selfStatus = selfStatus;
        setStaticSelfStatus(selfStatus);
    }

    public Limit getLimit() {
        return limit;
    }

    void init(boolean loginRequired, boolean configRequired) {
        Log.d(TAG, "init() called");
        this.loginRequired = loginRequired;
        this.configRequired = configRequired;
        if (loginRequired) {
            this.self = requireSelf(ctx);
            this.selfStatus = requireSelfStatus(ctx);
        } else {
            this.self = getSelf(ctx);
            this.selfStatus = getSelfStatus(ctx);
        }
        if (configRequired) {
            this.config = requireConfig(ctx);
        }
        // TODO: 判断一下User状态，是否需要跳到登录，
        if (loginRequired && !isServiceReady()) {
            ContextCompat.startForegroundService(ctx, CoreService.getIntent(ctx, self.getUserId(), self.getPassword(), self.getNickName()));
            connectedCallback = () -> {
                coreStatusListener.onCoreReady();
                // onCoreReady回调只调用一次，用于第一次初始化，
                // 重新登录不再次调用，
                connectedCallback = null;
            };
            isBind = ctx.bindService(CoreService.getIntent(), createCoreServiceConnection(), Context.BIND_AUTO_CREATE);
        }
    }

    void destroy() {
        if (isBind && mCoreServiceConnection != null) {
            try {
                ctx.unbindService(mCoreServiceConnection);
            } catch (Exception e) {
                // 以防万一，比如没绑定时会抛IllegalArgumentException，
                Reporter.unreachable(e);
            }
        }
    }

    public void logout() {
        if (isServiceReady()) {
            try {
                mService.logout();
                Log.e("zq", "logout成功");
            } catch (Exception e) {
                // 以防万一，
                Reporter.unreachable(e);
                Log.e("zq", "logout失败1");
            }
        } else {
            Log.e("zq", "logout失败2");
        }
    }

    public boolean isServiceReady() {
        return isBind && mService != null;
    }

    private void requireXmpp() {
        if (!isServiceReady()) {
            throw new IllegalStateException("xmpp服务没启动");
        }
    }

    private boolean requireXmppOrReport() {
        boolean ret = isServiceReady();
        if (!ret) {
            Reporter.post("xmpp服务没启动");
        }
        return ret;
    }

    public void disconnect() {
        Log.d(TAG, "disconnect() called");
        if (isServiceReady() && mService.getmConnectionManager() != null
                && mService.getmConnectionManager().getConnection() != null) {
            mService.getmConnectionManager().getConnection().disconnect();
        }
    }

    /**
     * 比如后台回来时的重新登录，
     */
    public void relogin() {
        Log.d(TAG, "relogin() called");
        if (!isLogin()) {
            ContextCompat.startForegroundService(ctx, CoreService.getIntent(ctx, self.getUserId(), self.getPassword(), self.getNickName()));
            connectedCallback = null;
            isBind = ctx.bindService(CoreService.getIntent(), createCoreServiceConnection(), Context.BIND_AUTO_CREATE);
        }
    }

    public void login() {
        Log.d(TAG, "login() called");
        requireXmpp();
        mService.login(getSelf().getUserId(), getSelf().getPassword());
    }

    public void joinExistGroup() {
        Log.d(TAG, "joinExistGroup() called");
        requireXmpp();
        mService.joinExistGroup();
    }

    public void loadMachineList() {
        Log.d(TAG, "loadMachineList() called");
        requireXmpp();
        mService.loadMachineList();
    }

    public String createMucRoom(String roomName) {
        Log.d(TAG, "createMucRoom() called with: roomName = [" + roomName + "]");
        requireXmpp();
        return mService.createMucRoom(roomName);
    }

    public void joinMucChat(String mRoomJid, long currentTimeSecond) {
        Log.d(TAG, "joinMucChat() called with: mRoomJid = [" + mRoomJid + "], l = [" + currentTimeSecond + "]");
        requireXmpp();
        mService.joinMucChat(mRoomJid, currentTimeSecond);
    }

    public void exitMucChat(String mRoomJid) {
        Log.d(TAG, "exitMucChat() called with: mRoomJid = [" + mRoomJid + "]");
        if (!requireXmppOrReport()) {
            return;
        }
        mService.exitMucChat(mRoomJid);
    }

    private void requirePackageId(XmppMessage message) {
        if (TextUtils.isEmpty(message.getPacketId())) {
            message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        }
    }

    public void sendChatMessage(String toUserId, ChatMessage message) {
        Log.d(TAG, "sendChatMessage() called with: call_toUser = [" + toUserId + "], message = [" + message + "]");
        requireXmpp();
        requirePackageId(message);
        mService.sendChatMessage(toUserId, message);
    }

    public void sendMucChatMessage(String mRoomJid, ChatMessage message) {
        Log.d(TAG, "sendMucChatMessage() called with: mRoomJid = [" + mRoomJid + "], message = [" + message + "]");
        requireXmpp();
        requirePackageId(message);
        mService.sendMucChatMessage(mRoomJid, message);
    }

    public void sendNewFriendMessage(String userId, NewFriendMessage message) {
        Log.d(TAG, "sendNewFriendMessage() called with: userId = [" + userId + "], message = [" + message + "]");
        requireXmpp();
        requirePackageId(message);
        mService.sendNewFriendMessage(userId, message);
    }

    public boolean isLogin() {
        Log.d(TAG, "isLogin() called");
        return isServiceReady() && mService.isAuthenticated();
    }

    public void askReconnect() {
        new AlertDialog.Builder(ctx)
                .setTitle(InternationalizationHelper.getString("JX_Tip"))
                .setMessage(ctx.getString(R.string.you_are_offline))
                .setPositiveButton(InternationalizationHelper.getString("RECONNECT"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DialogHelper.showDefaulteMessageProgressDialog(ctx);
                        AsyncUtils.doAsync(ctx, new AsyncUtils.Function<AsyncUtils.AsyncContext<Activity>>() {
                            @Override
                            public void apply(AsyncUtils.AsyncContext<Activity> activityAsyncContext) throws Exception {
                                if (!isServiceReady()) {
                                    DialogHelper.dismissProgressDialog();
                                    // ToastUtil.showErrorNet(LivePlayingActivity.this);
                                    return;
                                }
                                mService.logoutWithOutStopSelf();
                                activityAsyncContext.postDelayed(new AsyncUtils.Function<Activity>() {
                                    @Override
                                    public void apply(Activity activity) throws Exception {
                                        DialogHelper.dismissProgressDialog();
                                        mService.login(getSelf().getUserId(), getSelf().getPassword());

                                    }
                                }, 3000);
                            }
                        });
                    }
                }).setNegativeButton(InternationalizationHelper.getString("NO"), null).show();
    }
}

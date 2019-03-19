package com.sk.weichat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.util.LruCache;

import com.baidu.mapapi.SDKInitializer;
import com.danikula.videocache.HttpProxyCacheServer;
import com.fanjun.keeplive.KeepLive;
import com.fanjun.keeplive.config.ForegroundNotification;
import com.fanjun.keeplive.config.ForegroundNotificationClickListener;
import com.fanjun.keeplive.config.KeepLiveService;
import com.mob.MobSDK;
import com.sk.weichat.NetWorkObservable.NetWorkObserver;
import com.sk.weichat.adapter.MessageEventBG;
import com.sk.weichat.db.SQLiteHelper;
import com.sk.weichat.map.MapHelper;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.tool.MyFileNameGenerator;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.LocaleHelper;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ScreenShotListenManager;
import com.sk.weichat.volley.FastVolley;

import org.jetbrains.annotations.Contract;

import java.io.File;

import de.greenrobot.event.EventBus;

import static com.sk.weichat.util.Constants.IS_GOOGLE_MAP_KEY;

public class MyApplication extends Application {
    public static final String TAG = "com.sk.weichat";

    public static boolean IS_OPEN_CLUSTER = false;// 服务器是否开启集群 如开启，在登录、自动登录时需要传area，在发起音视频通话(单人)时会要调接口获取通话地址
    public static boolean IS_OPEN_RECEIPT = true;
    // 是否支持多端登录
    public static boolean IS_SUPPORT_MULTI_LOGIN;
    // 是否将消息转发给所有设备,当且仅当消息类型为上、下线消息(检测消息除外),该标志位才为true
    public static boolean IS_SEND_MSG_EVERYONE;
    public static String[] machine = new String[]{"ios", "pc", "mac", "web"};
    public static String IsRingId = "Empty";// 当前聊天对象的id/jid 用于控制消息来时是否响铃通知
    public static String mRoomKeyLastCreate = "compatible";// 本地建群时的jid(给个初始值坐下兼容) 用于防止收到服务端的907消息时本地也在建群而造成群组重复
    private static MyApplication INSTANCE = null;
    private static Context context;
    /* 文件缓存的目录 */
    public String mAppDir01;
    public String mPicturesDir01;
    public String mVoicesDir01;
    public String mVideosDir01;
    public String mFilesDir01;
    public int mActivityCount = 0;
    /* 文件缓存的目录 */
    public String mAppDir;
    public String mPicturesDir;
    public String mVoicesDir;
    public String mVideosDir;
    public String mFilesDir;
    public int mUserStatus;
    public boolean mUserStatusChecked = false;
    /*********************
     * 百度地图定位服务
     ************************/
    private BdLocationHelper mBdLocationHelper;
    /*********************
     * 提供网络全局监听
     ************************/
    private NetWorkObservable mNetWorkObservable;
    /*****************
     * 提供全局的Volley
     ***************************/

    private FastVolley mFastVolley;
    private LruCache<String, Bitmap> mMemoryCache;
    // 抖音模块缓存
    private HttpProxyCacheServer proxy;

    @Contract(pure = true)
    public static MyApplication getInstance() {
        return INSTANCE;
    }

    public static Context getContext() {
        return context;
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        MyApplication app = (MyApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        context = getApplicationContext();
        SDKInitializer.initialize(getApplicationContext());
        MobSDK.init(this, "22f8674521391", "1034357e1298736ce7d7a446ddb196ae");
        if (AppConfig.DEBUG) {
            Log.d(AppConfig.TAG, "MyApplication onCreate");
        }

        boolean isSupport = PreferenceUtils.getBoolean(this, "RESOURCE_TYPE", true);// 默认为开启
        if (isSupport) {
            IS_SUPPORT_MULTI_LOGIN = true;
        } else {
            IS_SUPPORT_MULTI_LOGIN = false;
        }

        // 在7.0的设备上，开启该模式访问相机或裁剪居然不会抛出FileUriExposedException异常，记录一下
        if (AppConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
        // 初始化网络监听
        mNetWorkObservable = new NetWorkObservable(this);
        // 初始化数据库
        SQLiteHelper.copyDatabaseFile(this);
        // 初始化定位
        getBdLocationHelper();
        // 初始化App目录
        initAppDir();
        initAppDirsecond();
        // 初始化图片加载 缓存
        initLruCache();

        // 判断前后台切换
        getAppBackground();
        // 监听屏幕截图
        ListeningScreenshots();

        int launchCount = PreferenceUtils.getInt(this, Constants.APP_LAUNCH_COUNT, 0);// 记录app启动的次数
        PreferenceUtils.putInt(this, Constants.APP_LAUNCH_COUNT, ++launchCount);

        initMap();

        initLanguage();

        initReporter();

        // 启动保活
//        initKeepLive();
    }

    private void initKeepLive() {
        // 定义前台服务的默认样式。即标题、描述和图标
        ForegroundNotification foregroundNotification = new ForegroundNotification("IM核心", "进程守护中", R.mipmap.icon,
                //定义前台服务的通知点击事件
                new ForegroundNotificationClickListener() {
                    @Override
                    public void foregroundNotificationClick(Context context, Intent intent) {
/*
                        intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
                        context.startActivity(intent);
*/
                    }
                });
        //启动保活服务
        KeepLive.startWork(this, KeepLive.RunMode.ENERGY, foregroundNotification,
                //你需要保活的服务，如socket连接、定时任务等，建议不用匿名内部类的方式在这里写
                new KeepLiveService() {
                    /**
                     * 运行中
                     * 由于服务可能会多次自动启动，该方法可能重复调用
                     */
                    @Override
                    public void onWorking() {
                        // Log.e("xuan", "onWorking: ");
                        // Intent startIntent = CoreService.getIntent(getContext(), user.getUserId(), user.getPassword(), user.getNickName());
                        // startService(startIntent);
                    }

                    /**
                     * 服务终止
                     * 由于服务可能会被多次终止，该方法可能重复调用，需同onWorking配套使用，如注册和注销broadcast
                     */
                    @Override
                    public void onStop() {
                        Log.e("xuan", "onStop: ");
                    }
                }
        );
    }

    private void initReporter() {
        Reporter.init(this);
    }

    private void initLanguage() {
        // 应用程序里设置的语言，否则程序杀死后重启又会是系统语言，
        LocaleHelper.setLocale(this, LocaleHelper.getLanguage(this));
    }

    private void initMap() {
        MapHelper.initContext(this);
        // 默认为百度地图，
        boolean isGoogleMap = PreferenceUtils.getBoolean(this, IS_GOOGLE_MAP_KEY, false);
        if (isGoogleMap) {
            MapHelper.setMapType(MapHelper.MapType.GOOGLE);
        } else {
            MapHelper.setMapType(MapHelper.MapType.BAIDU);
        }
    }

    private void getAppBackground() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (mActivityCount == 0) {
                    Log.e("zq", "程序已到前台,检查XMPP是否验证");
                    EventBus.getDefault().post(new MessageEventBG(true));
                }
                mActivityCount++;
                Log.e("APPLICATION", "onActivityStarted-->" + mActivityCount);
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                /*
                现在程序到后台，XMPP不置为离线，保持TCP的长连接，但是离线时间还是要保存
                 */
                mActivityCount--;
                Log.e("APPLICATION", "onActivityStopped-->" + mActivityCount);
                if (!AppUtils.isAppForeground(getContext())) {// 在app启动时，当启动页stop，而MainActivity还未start时，又会回调到该方法内，所以需要判断到底是不是真的处于后台
                    appBackstage(true);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public void appBackstage(boolean isBack) {
        AsyncUtils.doAsync(this, c -> {
            CoreManager.appBackstage(getApplicationContext(), isBack);
        });
    }

    /*******************
     * 初始化图片加载
     **********************//*
    // 显示的设置
    public static DisplayImageOptions mNormalImageOptions;
    public static DisplayImageOptions mAvatarRoundImageOptions;
    public static DisplayImageOptions mAvatarNormalImageOptions;

    private void initImageLoader() {
        int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 5);
        MemoryCacheAware<String, Bitmap> memoryCache;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            memoryCache = new LruMemoryCache(memoryCacheSize);
        } else {
            memoryCache = new LRULimitedMemoryCache(memoryCacheSize);
        }

        mNormalImageOptions = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheInMemory(true).cacheOnDisc(true)
                .resetViewBeforeLoading(false).showImageForEmptyUri(R.drawable.image_download_fail_icon)
                .showImageOnFail(R.drawable.image_download_fail_icon).build();

        mAvatarRoundImageOptions = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheInMemory(true).cacheOnDisc(true)
                .displayer(new RoundedBitmapDisplayer(10)).resetViewBeforeLoading(true).showImageForEmptyUri(R.drawable.avatar_normal)
                .showImageOnFail(R.drawable.avatar_normal).showImageOnLoading(R.drawable.avatar_normal).build();

        mAvatarNormalImageOptions = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheInMemory(true).cacheOnDisc(true)
                .resetViewBeforeLoading(true).showImageForEmptyUri(R.drawable.avatar_normal).showImageOnFail(R.drawable.avatar_normal)
                .showImageOnLoading(R.drawable.avatar_normal).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).defaultDisplayImageOptions(mNormalImageOptions)
                // .denyCacheImageMultipleSizesInMemory()
                .discCache(new TotalSizeLimitedDiscCache(new File(mPicturesDir), 50 * 1024 * 1024))
                // 最多缓存50M的图片
                .discCacheFileNameGenerator(new Md5FileNameGenerator()).memoryCache(memoryCache).tasksProcessingOrder(QueueProcessingType.LIFO)
                .threadPriority(Thread.NORM_PRIORITY - 2).threadPoolSize(4).build();
        // Initialize ImageLoader with configuration.
        com.nostra13.universalimageloader.utils.L.disableLogging();
        ImageLoader.getInstance().init(config);
    }*/
    private void ListeningScreenshots() {
        ScreenShotListenManager manager = ScreenShotListenManager.newInstance(this);
        manager.setListener(new ScreenShotListenManager.OnScreenShotListener() {
            @Override
            public void onShot(String imagePath) {
                PreferenceUtils.putString(getApplicationContext(), Constants.SCREEN_SHOTS, imagePath);
            }
        });
        manager.startListen();
    }

    /*
    保存某群组的部分属性
     */
    public void saveGroupPartStatus(String groupJid, int mGroupShowRead, int mGroupAllowSecretlyChat,
                                    int mGroupAllowConference, int mGroupAllowSendCourse, long mGroupTalkTime) {
        // 是否显示群消息已读人数
        PreferenceUtils.putBoolean(this, Constants.IS_SHOW_READ + groupJid, mGroupShowRead == 1);
        // 是否允许普通成员私聊
        PreferenceUtils.putBoolean(this, Constants.IS_SEND_CARD + groupJid, mGroupAllowSecretlyChat == 1);
        // 是否允许普通成员发起会议
        PreferenceUtils.putBoolean(this, Constants.IS_ALLOW_NORMAL_CONFERENCE + groupJid, mGroupAllowConference == 1);
        // 是否允许普通成员发送课程
        PreferenceUtils.putBoolean(this, Constants.IS_ALLOW_NORMAL_SEND_COURSE + groupJid, mGroupAllowSendCourse == 1);
        // 是否开启了全体禁言
        PreferenceUtils.putBoolean(this, Constants.GROUP_ALL_SHUP_UP + groupJid, mGroupTalkTime > 0);
    }

    /**
     * 初始化支付密码设置状态，
     * 登录接口返回支付密码是否设置，在这里保存起来，
     *
     * @param payPassword 支付密码是否已经设置，
     */
    public void initPayPassword(String userId, int payPassword) {
        Log.d(TAG, "initPayPassword() called with: userId = [" + userId + "], payPassword = [" + payPassword + "]");
        // 和initPrivateSettingStatus中的其他变量保存方式统一，
        PreferenceUtils.putBoolean(this, Constants.IS_PAY_PASSWORD_SET + userId, payPassword == 1);
    }

    /*
    登录时记录下用户的Settings Status
     */
    public void initPrivateSettingStatus(String userId, double chatSyncTimeLen, int isEncrypt, int isVibration, int isTyping,
                                         int isUseGoogleMap, int multipleDevices) {
        // 消息漫游时长
        PreferenceUtils.putString(MyApplication.getContext(), Constants.CHAT_SYNC_TIME_LEN + userId,
                String.valueOf(chatSyncTimeLen));

        // 需要好友验证为服务端判断 本地不做处理

        // 消息加密传输
        PreferenceUtils.putBoolean(this, Constants.IS_ENCRYPT + userId, isEncrypt == 1);
        // 消息来时振动
        PreferenceUtils.putBoolean(this, Constants.MSG_COME_VIBRATION + userId, isVibration == 1);
        // 让对方知道我正在输入...
        PreferenceUtils.putBoolean(this, Constants.KNOW_ENTER_STATUS + userId, isTyping == 1);

        // 使用谷歌地图
        PreferenceUtils.putBoolean(this, Constants.IS_GOOGLE_MAP_KEY, isUseGoogleMap == 1);
        if (isUseGoogleMap == 1) {
            MapHelper.setMapType(MapHelper.MapType.GOOGLE);
        } else {
            MapHelper.setMapType(MapHelper.MapType.BAIDU);
        }
        // 支持多设备登录
        if (multipleDevices == 1) {
            MyApplication.IS_SUPPORT_MULTI_LOGIN = true;
            PreferenceUtils.putBoolean(this, "RESOURCE_TYPE", true);
        } else {
            MyApplication.IS_SUPPORT_MULTI_LOGIN = false;
            PreferenceUtils.putBoolean(this, "RESOURCE_TYPE", false);
        }
    }

    public BdLocationHelper getBdLocationHelper() {
        if (mBdLocationHelper == null) {
            mBdLocationHelper = new BdLocationHelper(this);
        }
        return mBdLocationHelper;
    }

    public boolean isNetworkActive() {
        if (mNetWorkObservable != null) {
            return mNetWorkObservable.isNetworkActive();
        }
        return true;
    }

    public void registerNetWorkObserver(NetWorkObserver observer) {
        if (mNetWorkObservable != null) {
            mNetWorkObservable.registerObserver(observer);
        }
    }

    public void unregisterNetWorkObserver(NetWorkObserver observer) {
        if (mNetWorkObservable != null) {
            mNetWorkObservable.unregisterObserver(observer);
        }
    }

    private void initAppDirsecond() {
        File file = getExternalFilesDir(null);

        if (file == null) {
            return;
        }

        if (!file.exists()) {
            file.mkdirs();
        }
        mAppDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!file.exists()) {
            file.mkdirs();
        }
        mPicturesDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (!file.exists()) {
            file.mkdirs();
        }
        mVoicesDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (!file.exists()) {
            file.mkdirs();
        }
        mVideosDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!file.exists()) {
            file.mkdirs();
        }
        mFilesDir01 = file.getAbsolutePath();
    }

    private void initAppDir() {
        File file = getExternalFilesDir(null);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file != null) {
            mAppDir = file.getAbsolutePath();
        }

        file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file != null) {
            mPicturesDir = file.getAbsolutePath();
        }

        file = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file != null) {
            mVoicesDir = file.getAbsolutePath();
        }

        file = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file != null) {
            mVideosDir = file.getAbsolutePath();
        }

        file = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file != null) {
            mFilesDir = file.getAbsolutePath();
        }
    }

    public FastVolley getFastVolley() {
        if (mFastVolley == null) {
            synchronized (MyApplication.class) {
                if (mFastVolley == null) {
                    mFastVolley = new FastVolley(this);
                    mFastVolley.start();
                }
            }
        }
        return mFastVolley;
    }

    private void releaseFastVolley() {
        if (mFastVolley != null) {
            mFastVolley.stop();
        }
    }

    /**
     * 在程序内部关闭时，调用此方法
     */
    public void destory() {
        if (AppConfig.DEBUG) {
            Log.d(AppConfig.TAG, "MyApplication destory");
        }
        // 结束百度定位
        if (mBdLocationHelper != null) {
            mBdLocationHelper.release();
        }
        // 关闭网络状态的监听
        if (mNetWorkObservable != null) {
            mNetWorkObservable.release();
        }
        // 清除图片加载
        // ImageLoader.getInstance().destroy();
        releaseFastVolley();
        // 释放数据库
        // SQLiteHelper.release();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /***********************
     * 保存其他用户坐标信息
     ***************/

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public void initLruCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                .maxCacheSize(1024 * 1024 * 1024)       // 1 Gb for cache
                .fileNameGenerator(new MyFileNameGenerator()).build();
    }
}

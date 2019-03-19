package com.sk.weichat.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.sk.weichat.AppConfig;
import com.sk.weichat.AppConstant;
import com.sk.weichat.BuildConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.adapter.MessageLogin;
import com.sk.weichat.bean.ConfigBean;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.account.LoginActivity;
import com.sk.weichat.ui.account.LoginHistoryActivity;
import com.sk.weichat.ui.account.RegisterActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.EventBusHelper;
import com.sk.weichat.util.LogUtils;
import com.sk.weichat.util.PermissionUtil;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.util.VersionUtil;
import com.sk.weichat.view.TipDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.JsonCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 启动页
 */
public class SplashActivity extends BaseActivity implements PermissionUtil.OnRequestPermissionsResultCallbacks {
    // 声明一个数组，用来存储所有需要动态申请的权限
    private static final int REQUEST_CODE = 0;
    private final Map<String, Integer> permissionsMap = new HashMap<>();
    /*
    现在服务器要做集群，客户端需要将area上传服务器，服务器在根据area返回config
    集群主要改变配置内的4个参数:1.apiUrl、XMPPHost、liveUrl meetUrl
     */
    String area;
    private LinearLayout mSelectLv;
    private Button mSelectLoginBtn;
    private Button mSelectRegisBtn;

    // 配置是否成功
    private boolean mConfigReady = false;

    public SplashActivity() {
        // 这个页面不需要已经获取config, 也不需要已经登录，
        noConfigRequired();
        noLoginRequired();

        // 定位权限等获取到配置再看是否要请求，
        // 照相
        permissionsMap.put(Manifest.permission.CAMERA, R.string.permission_photo);
        // 麦克风
        permissionsMap.put(Manifest.permission.RECORD_AUDIO, R.string.permission_microphone);
        // 手机状态
        permissionsMap.put(Manifest.permission.READ_PHONE_STATE, R.string.permission_phone_status);
        // 存储权限
        permissionsMap.put(Manifest.permission.READ_EXTERNAL_STORAGE, R.string.permission_storage);
        permissionsMap.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_storage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.log(getIntent());
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_splash);
        // mStartTimeMs = System.currentTimeMillis();
        mSelectLv = (LinearLayout) findViewById(R.id.select_lv);
        mSelectLoginBtn = (Button) findViewById(R.id.select_login_btn);
        mSelectLoginBtn.setText(InternationalizationHelper.getString("JX_Login"));
        mSelectLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!UiUtils.isNormalClick()){
                    return;
                }
                startActivity(new Intent(mContext, LoginActivity.class));
                finish();
            }
        });
        mSelectRegisBtn = (Button) findViewById(R.id.select_register_btn);
        mSelectRegisBtn.setText(InternationalizationHelper.getString("REGISTERS"));
        mSelectRegisBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!UiUtils.isNormalClick()){
                    return;
                }
                startActivity(new Intent(mContext, RegisterActivity.class));
            }
        });
        mSelectLv.setVisibility(View.INVISIBLE);

        // 注册按钮先隐藏，如果是接口返回开放注册再显示，
        mSelectRegisBtn.setVisibility(View.GONE);

        // 初始化配置
        initConfig();
        // 同时请求定位以外的权限，
        requestPermissions();

        EventBusHelper.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms, boolean isAllGranted) {
        if (isAllGranted) {// 请求权限返回 已全部授权
            ready();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms, boolean isAllDenied) {
        if (perms != null && perms.size() > 0) {
            // 用set去重，有的复数权限
            Set<String> tipSet = new HashSet<>();
            for (int i = 0; i < perms.size(); i++) {
                tipSet.add(getString(permissionsMap.get(perms.get(i))));
            }
            String tipS = TextUtils.join(", ", tipSet);
            boolean onceAgain = PermissionUtil.deniedRequestPermissionsAgain(this, perms.toArray(new String[perms.size()]));
            TipDialog mTipDialog = new TipDialog(this);
            if (onceAgain) {// 部分 || 所有权限被拒绝且选择了（选择了不再询问 || 部分机型默认为不在询问）
                mTipDialog.setmConfirmOnClickListener(getString(R.string.tip_reject_permission_place_holder, tipS), new TipDialog.ConfirmOnClickListener() {
                    @Override
                    public void confirm() {
                        PermissionUtil.startApplicationDetailsSettings(SplashActivity.this, REQUEST_CODE);
                    }
                });
            } else {// 部分 || 所有权限被拒绝
                mTipDialog.setTip(getString(R.string.tip_permission_reboot_place_holder, tipS));
            }
            mTipDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {// 设置 手动开启权限 返回 再次判断是否获取全部权限
            ready();
        }
    }

    /**
     * 配置参数初始化
     */
    private void initConfig() {
        /*if (!MyApplication.getInstance().isNetworkActive()) {
            ToastUtil.showToast(SplashActivity.this, R.string.tip_get_config_failed);
            // 在断网的情况下，使用已经保存了的配置，
            ConfigBean configBean = coreManager.readConfigBean();
            setConfig(configBean);
            return;
        }*/

        area = PreferenceUtils.getString(SplashActivity.this, AppConstant.EXTRA_CLUSTER_AREA);
        if (TextUtils.isEmpty(area)) {// 在本地已经存在area的情况下，就不去调用获取地区的接口了，因为有时候访问这个接口需要很久才能响应
            getIpInfo();
        } else {
            getConfig();
        }
    }

    private void getIpInfo() {
        String URL = "https://ipinfo.io/geo";
        HttpUtils.get().url(URL)
                .build()
                .execute(new JsonCallback() {
                    @Override
                    public void onResponse(String result) {
                        if (!TextUtils.isEmpty(result)) {
                            JSONObject jsonObject = JSONObject.parseObject(result);
                            String city = jsonObject.getString("city");
                            String region = jsonObject.getString("region");
                            String country = jsonObject.getString("country");
                            area = country + "," + region + "," + city;
                            PreferenceUtils.putString(SplashActivity.this, AppConstant.EXTRA_CLUSTER_AREA, area);
                            Log.e("zq", area);
                        }
                        getConfig();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        getConfig();
                    }
                });

    }

    private void getConfig() {
        String mConfigApi = AppConfig.readConfigUrl(mContext);

        Map<String, String> params = new HashMap<>();
        if (!TextUtils.isEmpty(area)) {
            params.put("area", area);
        }
        Reporter.putUserData("configUrl", mConfigApi);
        HttpUtils.get().url(mConfigApi)
                .params(params)
                .build()
                .execute(new BaseCallback<ConfigBean>(ConfigBean.class) {
                    @Override
                    public void onResponse(ObjectResult<ConfigBean> result) {
                        ConfigBean configBean;
                        if (result == null || result.getData() == null || result.getResultCode() != Result.CODE_SUCCESS) {
                            Log.e("zq", "获取网络配置失败，使用已经保存了的配置");
                            // ToastUtil.showToast(SplashActivity.this, R.string.tip_get_config_failed);
                            // 获取网络配置失败，使用已经保存了的配置，
                            configBean = coreManager.readConfigBean();
                        } else {
                            Log.e("zq", "获取网络配置成功，使用服务端返回的配置并更新本地配置");
                            configBean = result.getData();
                            coreManager.saveConfigBean(configBean);
                            MyApplication.IS_OPEN_CLUSTER = configBean.getIsOpenCluster() == 1 ? true : false;
                        }
                        setConfig(configBean);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e("zq", "获取网络配置失败，使用已经保存了的配置");
                        // ToastUtil.showToast(SplashActivity.this, R.string.tip_get_config_failed);
                        // 获取网络配置失败，使用已经保存了的配置，
                        ConfigBean configBean = coreManager.readConfigBean();
                        setConfig(configBean);
                    }
                });
    }

    private void setConfig(ConfigBean configBean) {
        if (configBean == null) {

            // 如果没有保存配置，也就是第一次使用，就连不上服务器，使用默认配置
            configBean = getDefaultConfig();
            if (configBean == null) {
                // 不可到达，本地assets一定要提供默认config,
                DialogHelper.tip(this, getString(R.string.tip_get_config_failed));
                return;
            }
            coreManager.saveConfigBean(configBean);
        }
        // 初始化配置
        if (coreManager.getConfig().isOpenRegister) {
            mSelectRegisBtn.setVisibility(View.VISIBLE);
        } else {
            mSelectRegisBtn.setVisibility(View.GONE);
        }
        if (!coreManager.getConfig().disableLocationServer) {
            // 定位
            permissionsMap.put(Manifest.permission.ACCESS_COARSE_LOCATION, R.string.permission_location);
            permissionsMap.put(Manifest.permission.ACCESS_FINE_LOCATION, R.string.permission_location);
        }
        // 配置完毕
        mConfigReady = true;
        // 如果没有androidDisable字段就不判断，
        // 当前版本没被禁用才继续打开，
        if (TextUtils.isEmpty(configBean.getAndroidDisable()) || !blockVersion(configBean.getAndroidDisable(), configBean.getAndroidAppUrl())) {
            // 进入主界面
            ready();
        }
    }

    private ConfigBean getDefaultConfig() {
        if (BuildConfig.DEBUG) {
            ToastUtil.showToast(this, R.string.tip_get_config_failed);
        }
        try {
            // 手动下载一份config接口返回值放在assets里作为默认config,
            ObjectResult<ConfigBean> result = JSON.parseObject(getAssets().open("default_config"), new TypeReference<ObjectResult<ConfigBean>>() {
            }.getType());
            return result.getData();
        } catch (IOException e) {
            // 不可到达，本地assets一定要提供默认config,
            Reporter.unreachable();
            return null;
        }
    }

    /**
     * 如果当前版本被禁用，就自杀，
     *
     * @param disabledVersion 禁用该版本以下的版本，
     * @param appUrl          版本被禁用时打开的地址，
     * @return 返回是否被禁用，
     */
    private boolean blockVersion(String disabledVersion, String appUrl) {
        String currentVersion = BuildConfig.VERSION_NAME;
        if (VersionUtil.compare(currentVersion, disabledVersion) > 0) {
            // 当前版本大于被禁用版本，
            return false;
        } else {
            // 通知一下，
            DialogHelper.tip(this, getString(R.string.tip_version_disabled))
                    .setOnDismissListener(dialog -> {
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(appUrl));
                            startActivity(i);
                        } catch (Exception e) {
                            // 弹出浏览器失败的话无视，
                            // 比如没有浏览器的情况，
                            // 比如appUrl不合法的情况，
                        }
                        // 自杀，
                        finish();
                        MyApplication.getInstance().destory();
                    });
            return true;
        }
    }

    private void ready() {
        if (!mConfigReady) {// 配置失败
            return;
        }

        // 检查 || 请求权限
        boolean hasAll = requestPermissions();
        if (hasAll) {// 已获得所有权限
            jump();
        }
    }

    private boolean requestPermissions() {
        return PermissionUtil.requestPermissions(this, REQUEST_CODE, permissionsMap.keySet().toArray(new String[]{}));
    }

    @SuppressLint("NewApi")
    private void jump() {
        if (isDestroyed()) {
            return;
        }
        int userStatus = LoginHelper.prepareUser(mContext, coreManager);
        Intent intent = new Intent();
        switch (userStatus) {
            case LoginHelper.STATUS_USER_FULL:
            case LoginHelper.STATUS_USER_NO_UPDATE:
            case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
                boolean login = PreferenceUtils.getBoolean(this, Constants.LOGIN_CONFLICT, false);
                if (login) {// 登录冲突，退出app再次进入，跳转至历史登录界面
                    intent.setClass(mContext, LoginHistoryActivity.class);
                } else {
                    intent.setClass(mContext, MainActivity.class);
                }
                break;
            case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                intent.setClass(mContext, LoginHistoryActivity.class);
                break;
            case LoginHelper.STATUS_NO_USER:
            default:
                stay();
                return;// must return
        }
        startActivity(intent);
        finish();
    }

    // 第一次进入酷聊，显示登录、注册按钮
    private void stay() {
/*
        mSelectLv.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.alpha_in);
        mSelectLv.startAnimation(anim);
*/
        // 因为启动页有时会替换，无法做到按钮与图片的完美适配，干脆直接进入到登录页面
        startActivity(new Intent(mContext, LoginActivity.class));
        finish();
    }
}

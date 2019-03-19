package com.sk.weichat.ui.account;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConfig;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.MessageLogin;
import com.sk.weichat.bean.LoginRegisterResult;
import com.sk.weichat.bean.WXUploadResult;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.helper.UsernameHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.me.SetConfigActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.EventBusHelper;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.wxapi.WXEntryActivity;
import com.sk.weichat.wxapi.WXHelper;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 登陆界面
 *
 * @author Dean Tao
 * @version 1.0
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private EditText mPhoneNumberEdit;
    private EditText mPasswordEdit;
    private TextView tv_prefix;
    private int mobilePrefix = 86;
    private String thirdToken;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    public LoginActivity() {
        noLoginRequired();
    }

    public static void bindThird(Context ctx, WXUploadResult thirdToken) {
        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.putExtra("thirdToken", JSON.toJSONString(thirdToken));
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        thirdToken = getIntent().getStringExtra("thirdToken");
        initActionBar();
        initView();

        IntentFilter filter = new IntentFilter();
        filter.addAction("CHANGE_CONFIG");
        registerReceiver(broadcastReceiver, filter);

        if (!TextUtils.isEmpty(thirdToken)) {
            // 第三方进来直接登录，
            // 清空手机号以标记是第三方登录，
            mPhoneNumberEdit.setText("");
            login(true);
        }
        EventBusHelper.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果没有保存用户定位信息，那么去地位用户当前位置
        if (!MyApplication.getInstance().getBdLocationHelper().isLocationUpdate()) {
            MyApplication.getInstance().getBdLocationHelper().requestLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.login));
        TextView tvRight = (TextView) findViewById(R.id.tv_title_right);
        // 定制包隐藏设置服务器按钮，
        if (!AppConfig.isShiku()) {
            // 为方便测试，留个启用方法，adb shell命令运行"setprop log.tag.ShikuServer D"启用，
            if (!Log.isLoggable("ShikuServer", Log.DEBUG)) {
                tvRight.setVisibility(View.GONE);
            }
        }
        tvRight.setText(R.string.settings_server_address);
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SetConfigActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initView() {
        mPhoneNumberEdit = (EditText) findViewById(R.id.phone_numer_edit);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);
        tv_prefix = (TextView) findViewById(R.id.tv_prefix);
        if (coreManager.getConfig().registerUsername) {
            tv_prefix.setVisibility(View.GONE);
        } else {
            tv_prefix.setOnClickListener(this);
        }
        mobilePrefix = PreferenceUtils.getInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tv_prefix.setText("+" + mobilePrefix);
        Button forgetPasswordBtn, registerBtn, loginBtn;
        // 登陆账号
        loginBtn = (Button) findViewById(R.id.login_btn);
        // loginBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        loginBtn.setOnClickListener(this);
        // 注册账号
        registerBtn = (Button) findViewById(R.id.register_account_btn);
        if (TextUtils.isEmpty(thirdToken)) {
            registerBtn.setOnClickListener(this);
        } else {
            // 第三方登录的不需要这个注册按钮，登录后没有账号直接跳到注册，
            registerBtn.setVisibility(View.GONE);
        }
        if (coreManager.getConfig().isOpenRegister) {
            registerBtn.setVisibility(View.VISIBLE);
        } else {
            registerBtn.setVisibility(View.GONE);
        }
        // 忘记密码
        forgetPasswordBtn = (Button) findViewById(R.id.forget_password_btn);
        if (coreManager.getConfig().registerUsername) {
            forgetPasswordBtn.setVisibility(View.GONE);
        }
/*
        forgetPasswordBtn.setTextColor(SkinUtils.getSkin(this).getAccentColor());
*/
        forgetPasswordBtn.setOnClickListener(this);
        UsernameHelper.initEditText(mPhoneNumberEdit, coreManager.getConfig().registerUsername);
        // mPasswordEdit.setHint(InternationalizationHelper.getString("JX_InputPassWord"));
        loginBtn.setText(InternationalizationHelper.getString("JX_Login"));
        registerBtn.setText(InternationalizationHelper.getString("JX_Register"));
        forgetPasswordBtn.setText(InternationalizationHelper.getString("JX_ForgetPassWord"));

//        if (TextUtils.isEmpty(thirdToken)) {
//            findViewById(R.id.wx_login_btn).setOnClickListener(this);
//        } else {
//            findViewById(R.id.wx_login_btn).setVisibility(View.GONE);
//        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_prefix:
                // 选择国家区号
                Intent intent = new Intent(this, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.login_btn:
                // 登陆
                login(false);
                break;
//            case R.id.wx_login_btn:
//                WXEntryActivity.wxLogin(this);
//                break;
            case R.id.register_account_btn:
                // 注册
                register();
                break;
            case R.id.forget_password_btn:
                // 忘记密码
                startActivity(new Intent(mContext, FindPwdActivity.class));
                break;
        }
    }

    private void register() {
        RegisterActivity.registerFromThird(
                this,
                mobilePrefix,
                mPhoneNumberEdit.getText().toString(),
                mPasswordEdit.getText().toString()
                , thirdToken
        );
    }

    /**
     * @param third 第三方自动登录，
     */
    private void login(boolean third) {
        PreferenceUtils.putInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        String password = mPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(thirdToken)) {
            // 第三方登录的不处理账号密码，
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(mContext, InternationalizationHelper.getString("JX_InputPassWord"), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // 加密之后的密码
        final String digestPwd = Md5Util.toMD5(password);
        // 加密后的手机号码
        String digestPhoneNumber = Md5Util.toMD5(phoneNumber);

        DialogHelper.showDefaulteMessageProgressDialog(this);

        Map<String, String> params = new HashMap<>();
        params.put("telephone", digestPhoneNumber);          // 账号登陆的时候需要MD5加密，服务器需求
        params.put("areaCode", String.valueOf(mobilePrefix));// 账号登陆的时候需要MD5加密，服务器需求
        params.put("password", digestPwd);
        params.put("xmppVersion", "1");
        // 附加信息+
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
        // 地址信息
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        if (MyApplication.IS_OPEN_CLUSTER) {// 服务端集群需要
            String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
            if (!TextUtils.isEmpty(area)) {
                params.put("area", area);
            }
        }

        String url;
        if (TextUtils.isEmpty(thirdToken)) {
            url = coreManager.getConfig().USER_LOGIN;
        } else {
            params.put("type", "2");
            params.put("loginInfo", WXHelper.parseOpenId(thirdToken));
            if (third) {
                // 第三方自动登录，
                // 先尝试直接用这个微信登录，
                // 如果返回1040305表示这个微信没有绑定IM账号，
                // 留在这个登录页面等待用户输入账号密码，
                url = coreManager.getConfig().USER_THIRD_LOGIN;
            } else {
                // 用户输入IM账号密码后将该IM账号与微信绑定，
                // 如果返回1040306表示这个IM账号不存在，跳到注册页面让用户走注册IM账号并绑定微信，
                url = coreManager.getConfig().USER_THIRD_BIND;
                // 账号绑定的时候不需要MD5加密，并拼接区号，
                params.put("telephone", mobilePrefix + phoneNumber);
            }
        }
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {

                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<LoginRegisterResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (!Result.checkSuccess(getApplicationContext(), result)) {
                            if (Result.checkError(result, Result.CODE_THIRD_NO_EXISTS)) {
                                // 如果返回1040306表示这个IM账号不存在，跳到注册页面让用户走注册IM账号并绑定微信，
                                register();
                            }
                            return;
                        }
                        boolean success = LoginHelper.setLoginUser(mContext, coreManager, phoneNumber, digestPwd, result);
                        if (success) {
                            LoginRegisterResult.Settings settings = result.getData().getSettings();
                            MyApplication.getInstance().initPayPassword(result.getData().getUserId(), result.getData().getPayPassword());
                            MyApplication.getInstance().initPrivateSettingStatus(result.getData().getUserId(), settings.getChatSyncTimeLen(),
                                    settings.getIsEncrypt(), settings.getIsVibration(), settings.getIsTyping(),
                                    settings.getIsUseGoogleMap(), settings.getMultipleDevices());

                            startActivity(new Intent(mContext, DataDownloadActivity.class));
                            finish();
                        } else { //  登录出错 || 用户资料不全
                            String message = TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.tip_incomplete_information) : result.getResultMsg();
                            ToastUtil.showToast(mContext, message);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS)
            return;
        mobilePrefix = data.getIntExtra(Constants.MOBILE_PREFIX, 86);
        tv_prefix.setText("+" + mobilePrefix);
    }
}

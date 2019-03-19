package com.sk.weichat.ui.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.MessageLogin;
import com.sk.weichat.bean.LoginRegisterResult;
import com.sk.weichat.bean.LoginRegisterResult.Login;
import com.sk.weichat.bean.User;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.sp.UserSp;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.ActivityStack;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.EventBusHelper;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;

import java.util.HashMap;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 历史登陆界面
 */

public class LoginHistoryActivity extends BaseActivity implements View.OnClickListener {
    private ImageView mAvatarImgView;
    private TextView mNickNameTv;
    private EditText mPasswordEdit;
    private int mobilePrefix = 86;
    private User mLastLoginUser;
    private int mOldLoginStatus;

    public LoginHistoryActivity() {
        noLoginRequired();
    }

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, LoginHistoryActivity.class);
        // 清空activity栈，
        // 重建期间白屏，暂且放弃，
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_history);
        PreferenceUtils.putBoolean(this, Constants.LOGIN_CONFLICT, false);// 重置登录冲突记录
        String userId = UserSp.getInstance(this).getUserId("");
        mLastLoginUser = UserDao.getInstance().getUserByUserId(userId);
        mOldLoginStatus = MyApplication.getInstance().mUserStatus;
        if (!LoginHelper.isUserValidation(mLastLoginUser)) {
            if(!UiUtils.isNormalClick()){
                return;
            }
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        TextView tv1 = (TextView) findViewById(R.id.tv_title_left);
        TextView tv2 = (TextView) findViewById(R.id.tv_title_right);
        tv1.setText(R.string.app_name);
        tv2.setText(R.string.switch_account);
        tv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!UiUtils.isNormalClick()){
                    return;
                }
                Intent intent = new Intent(LoginHistoryActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        initView();
        EventBusHelper.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    @Override
    public void onClick(View v) {
        if(!UiUtils.isNormalClick()){
            return;
        }
        switch (v.getId()) {
            case R.id.login_btn:
                login();
                break;
            case R.id.register_account_btn:
                startActivity(new Intent(LoginHistoryActivity.this, RegisterActivity.class));
                break;
            case R.id.forget_password_btn:
                startActivity(new Intent(this, FindPwdActivity.class));
                break;
        }
    }

    private void initView() {
        mAvatarImgView = (ImageView) findViewById(R.id.avatar_img);
        mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);
        mobilePrefix = PreferenceUtils.getInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        Button loginBtn, registerBtn, forgetPasswordBtn;
        loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        loginBtn.setOnClickListener(this);
        registerBtn = (Button) findViewById(R.id.register_account_btn);
        registerBtn.setOnClickListener(this);
        if (coreManager.getConfig().isOpenRegister) {
            registerBtn.setVisibility(View.VISIBLE);
        } else {
            registerBtn.setVisibility(View.GONE);
        }
        forgetPasswordBtn = (Button) findViewById(R.id.forget_password_btn);
        if (coreManager.getConfig().registerUsername) {
            forgetPasswordBtn.setVisibility(View.GONE);
        } else {
            forgetPasswordBtn.setOnClickListener(this);
        }
/*
        registerBtn.setTextColor(SkinUtils.getSkin(this).getAccentColor());
        forgetPasswordBtn.setTextColor(SkinUtils.getSkin(this).getAccentColor());
*/

        // mPasswordEdit.setHint(InternationalizationHelper.getString("JX_InputPassWord"));
        loginBtn.setText(InternationalizationHelper.getString("JX_Login"));
        registerBtn.setText(InternationalizationHelper.getString("JX_Register"));
        forgetPasswordBtn.setText(InternationalizationHelper.getString("JX_ForgetPassWord"));

        AvatarHelper.getInstance().displayAvatar(mLastLoginUser.getUserId(), mAvatarImgView, true);
        mNickNameTv.setText(mLastLoginUser.getNickName());
    }

    private void login() {
        PreferenceUtils.putInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        String password = mPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            return;
        }
        final String digestPwd = new String(Md5Util.toMD5(password));

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<>();
        // 得到的为8618720966659,服务器却需要18720966659
        String phoneNumber = mLastLoginUser.getTelephone();
        // 去掉区号,
        String sPrefix = String.valueOf(mobilePrefix);
        String phoneNumberRel;
        if (phoneNumber.startsWith(sPrefix)) {
            phoneNumberRel = phoneNumber.substring(sPrefix.length());
        } else {
            phoneNumberRel = phoneNumber;
        }
        params.put("telephone", Md5Util.toMD5(phoneNumberRel));// 账号登陆的时候需要MD5以下，服务器需求
        params.put("password", digestPwd);
        params.put("xmppVersion", "1");
        params.put("areaCode", String.valueOf(mobilePrefix));
        // 附加信息
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

        HttpUtils.get().url(coreManager.getConfig().USER_LOGIN)
                .params(params)
                .build()
                .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {

                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<LoginRegisterResult> result) {
                        if (result == null) {
                            DialogHelper.dismissProgressDialog();
                            ToastUtil.showErrorData(mContext);
                            return;
                        }
                        boolean success = false;
                        if (result.getResultCode() == Result.CODE_SUCCESS) {
                            success = LoginHelper.setLoginUser(mContext, coreManager, mLastLoginUser.getTelephone(), digestPwd, result);// 设置登陆用户信息
                        }

                        if (success) {
                            LoginRegisterResult.Settings settings = result.getData().getSettings();
                            MyApplication.getInstance().initPayPassword(result.getData().getUserId(), result.getData().getPayPassword());
                            MyApplication.getInstance().initPrivateSettingStatus(result.getData().getUserId(), settings.getChatSyncTimeLen(),
                                    settings.getIsEncrypt(), settings.getIsVibration(), settings.getIsTyping(),
                                    settings.getIsUseGoogleMap(), settings.getMultipleDevices());

                            // 登陆成功
                            Login login = result.getData().getLogin();
                            if (login != null && login.getSerial() != null && login.getSerial().equals(DeviceInfoUtil.getDeviceId(mContext))
                                    && mOldLoginStatus != LoginHelper.STATUS_USER_NO_UPDATE && mOldLoginStatus != LoginHelper.STATUS_NO_USER) {
                                // 如果Token没变，上次更新也是完整更新，那么直接进入Main程序
                                // 其他的登陆地方都需进入DataDownloadActivity，在DataDownloadActivity里发送此广播
                                LoginHelper.broadcastLogin(LoginHistoryActivity.this);
                                // Intent intent = new Intent(mContext, MainActivity.class);
                                Intent intent = new Intent(LoginHistoryActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } else {
                                // 否则，进入数据下载界面
                                startActivity(new Intent(LoginHistoryActivity.this, DataDownloadActivity.class));
                            }
                            finish();
                        } else {
                            // 登录失败
                            String message = TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.login_failed) : result.getResultMsg();
                            ToastUtil.showToast(mContext, message);
                        }
                        DialogHelper.dismissProgressDialog();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        ActivityStack.getInstance().exit();
    }
}

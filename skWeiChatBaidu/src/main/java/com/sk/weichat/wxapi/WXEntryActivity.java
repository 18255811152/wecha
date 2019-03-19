package com.sk.weichat.wxapi;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.sk.weichat.AppConfig;
import com.sk.weichat.R;
import com.sk.weichat.bean.WXUploadResult;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.account.LoginActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.me.redpacket.PayPasswordVerifyDialog;
import com.sk.weichat.ui.me.redpacket.QuXianActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;

public class WXEntryActivity extends BaseActivity implements IWXAPIEventHandler {

    public static void wxLogin(Context ctx) {
        IWXAPI api = WXAPIFactory.createWXAPI(ctx, Constants.VX_APP_ID, false);
        api.registerApp(Constants.VX_APP_ID);
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "login";
        api.sendReq(req);
    }

    private IWXAPI api;

    public WXEntryActivity() {
        // 微信登录的回调是到这里，所以可能没有登录，
        noLoginRequired();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vx_result);

        api = WXAPIFactory.createWXAPI(WXEntryActivity.this, Constants.VX_APP_ID, false);
        api.handleIntent(getIntent(), this);
    }

    // 微信发送请求到第三方应用时，会回调到该方法
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {

    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    @Override
    public void onResp(BaseResp resp) {
        SendAuth.Resp mSendAuthResp = ((SendAuth.Resp) resp);
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                if (Objects.equals(mSendAuthResp.state, "login")) {
                    getOpenId(mSendAuthResp.code);
                } else {
                    updateCodeToService(mSendAuthResp.code);
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                finish();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                finish();
                break;
            default:
                finish();
                break;
        }
    }

    private void getOpenId(String code) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("code", code);

        HttpUtils.get().url(coreManager.getConfig().VX_GET_OPEN_ID)
                .params(params)
                .build()
                .execute(new BaseCallback<WXUploadResult>(WXUploadResult.class) {

                    @Override
                    public void onResponse(ObjectResult<WXUploadResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(getApplicationContext(), result)) {
                            String openId = result.getData().getOpenid();
                            if (TextUtils.isEmpty(openId)) {
                                ToastUtil.showToast(getApplicationContext(), R.string.tip_server_error);
                            } else {
                                LoginActivity.bindThird(getApplicationContext(), result.getData());
                            }
                        }
                        finish();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        finish();
                    }
                });
    }

    private void updateCodeToService(String code) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("code", code);

        HttpUtils.get().url(coreManager.getConfig().VX_UPLOAD_CODE)
                .params(params)
                .build()
                .execute(new BaseCallback<WXUploadResult>(WXUploadResult.class) {

                    @Override
                    public void onResponse(ObjectResult<WXUploadResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            WXUploadResult wxUploadResult = result.getData();
                            transfer(wxUploadResult.getOpenid());
                        } else {
                            Toast.makeText(WXEntryActivity.this, "绑定服务器失败", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        finish();
                    }
                });
    }

    private void transfer(final String vx_openid) {
        PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
        dialog.setAction(getString(R.string.withdraw));
        // amount单位是分，而且是整元乘100得到的分，一定可以被100整除，
        dialog.setMoney(String.valueOf(Integer.valueOf(QuXianActivity.amount) / 100));
        dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
            @Override
            public void onInputFinish(String password) {
                DialogHelper.showDefaulteMessageProgressDialog(WXEntryActivity.this);
                String mAccessToken = coreManager.getSelfStatus().accessToken;
                String mLoginUserId = coreManager.getSelf().getUserId();
                String time = String.valueOf(TimeUtils.sk_time_current_time());

                String str1 = AppConfig.apiKey + vx_openid + mLoginUserId;
                String str2 = Md5Util.toMD5(mAccessToken + QuXianActivity.amount + time);
                String str3 = Md5Util.toMD5(password);
                String secret = Md5Util.toMD5(str1 + str2 + str3);
                Log.d(HttpUtils.TAG, String.format(Locale.CHINA, "addSecret: md5(%s+%s+%s+md5(%s+%s+%s)+md5(%s)) = %s", AppConfig.apiKey, vx_openid, mLoginUserId, mAccessToken, QuXianActivity.amount, time, password, secret));

                final Map<String, String> params = new HashMap<>();
                params.put("access_token", mAccessToken);
                params.put("amount", QuXianActivity.amount);
                params.put("time", time);
                params.put("secret", secret);

                HttpUtils.post().url(coreManager.getConfig().VX_TRANSFER_PAY)
                        .params(params)
                        .build()
                        .execute(new BaseCallback<WXUploadResult>(WXUploadResult.class) {

                            @Override
                            public void onResponse(ObjectResult<WXUploadResult> result) {
                                DialogHelper.dismissProgressDialog();
                                if (result.getResultCode() == 1 && result.getData() != null) {
                                    ToastUtil.showToast(WXEntryActivity.this, R.string.tip_withdraw_success);
                                } else {
                                    ToastUtil.showToast(WXEntryActivity.this, result.getResultMsg());
                                }
                                finish();
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                DialogHelper.dismissProgressDialog();
                                finish();
                                ToastUtil.showErrorData(WXEntryActivity.this);
                            }
                        });
            }
        });
        dialog.setOnDismissListener(dialog1 -> {
            finish();
        });
        dialog.show();
    }
}
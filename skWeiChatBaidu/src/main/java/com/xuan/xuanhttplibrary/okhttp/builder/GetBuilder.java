package com.xuan.xuanhttplibrary.okhttp.builder;

import android.text.TextUtils;
import android.util.Log;

import com.sk.weichat.AppConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.UserStatus;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.TimeUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.Request;

/**
 * @author Administrator
 * @time 2017/3/30 0:11
 * @des ${TODO}
 */

public class GetBuilder extends BaseBuilder {

    @Override
    public GetBuilder url(String url) {
        if (!TextUtils.isEmpty(url)) {
            this.url = url;
        }
        addSecret();
        return this;
    }

    /**
     * 给所有接口调添加secret,
     */
    private GetBuilder addSecret() {
        if (url.contains("config")
                || url.equals("https://ipinfo.io/geo")) {// 获取配置的接口，不验证
            return this;
        }
        // 上面两个接口调用时可能还没有获取到config,
        AppConfig mConfig = CoreManager.requireConfig(MyApplication.getInstance());
        if (url.equals(mConfig.SDK_OPEN_AUTH_INTERFACE)) {
            return this;
        }

        // 所有接口都需要time与secret参数
        String time = String.valueOf(TimeUtils.sk_time_current_time());
        String secret;
        UserStatus status = CoreManager.getSelfStatus(MyApplication.getContext());
        if (url.equals(mConfig.VX_RECHARGE)
                || url.equals(mConfig.REDPACKET_OPEN)
                || url.equals(mConfig.SKTRANSFER_RECEIVE_TRANSFER)) {
            // 微信支付 领红包 领取转账 调用的接口
            if (!checkAccessToken(status)) {
                return this;
            }
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time);
            secret = Md5Util.toMD5(step1 + mLoginUserId + status.accessToken);
        } else if (url.equals(mConfig.USER_LOGIN)
                || url.equals(mConfig.USER_REGISTER)
                || url.equals(mConfig.USER_PASSWORD_RESET)
                || url.equals(mConfig.VERIFY_TELEPHONE)
                || url.equals(mConfig.USER_GETCODE_IMAGE)
                // 未登录之前 && 微信登录相关 调用的接口
                || url.equals(mConfig.VX_GET_OPEN_ID) || url.equals(mConfig.USER_THIRD_LOGIN) || url.equals(mConfig.USER_THIRD_BIND) || url.equals(mConfig.USER_THIRD_REGISTER) || url.equals(mConfig.SEND_AUTH_CODE)) {
            secret = Md5Util.toMD5(AppConfig.apiKey + time);
        } else {
            // 其他接口
            if (!checkAccessToken(status)) {
                return this;
            }
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            secret = Md5Util.toMD5(AppConfig.apiKey + time + mLoginUserId + status.accessToken);
        }

        params("time", time);
        params("secret", secret);

        return this;
    }

    /**
     * @return 返回true表示accessToken正常，
     */
    private boolean checkAccessToken(UserStatus status) {
        String mAccessToken;
        // 如果没有accessToken就不添加time和secret,
        if (status == null) {
            return false;
        } else {
            mAccessToken = status.accessToken;
            if (TextUtils.isEmpty(mAccessToken)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 给需要支付密码的接口调添加secret,
     *
     * @param payPassword 支付密码
     */
    public GetBuilder addSecret(String payPassword, String money) {
        AppConfig mConfig = CoreManager.requireConfig(MyApplication.getInstance());

        // 所有接口都需要time与secret参数
        String time = String.valueOf(TimeUtils.sk_time_current_time());
        String secret;
        String mAccessToken = CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken;
        if (url.equals(mConfig.REDPACKET_SEND)
                || url.equals(mConfig.SKTRANSFER_SEND_TRANSFER)) {
            // 发红包 || 转账 调用的接口
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time + money);
            String step2 = Md5Util.toMD5(payPassword);
            secret = Md5Util.toMD5(step1 + mLoginUserId + mAccessToken + step2);
            Log.d(HttpUtils.TAG, String.format(Locale.CHINA, "addSecret: md5(md5(%s+%s+%s)+%s+%s+md5(%s)) = %s", AppConfig.apiKey, time, money, mLoginUserId, mAccessToken, payPassword, secret));
        } else if (url.equals(mConfig.PAY_CODE_PAYMENT)) {
            // 扫付款码 调用的接口 此时参数payPassword应该传paymentCode
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            String step1 = Md5Util.toMD5(AppConfig.apiKey + time + money + payPassword);
            secret = Md5Util.toMD5(step1 + mLoginUserId + mAccessToken);
        } else {
            // 不走这里，
            Reporter.unreachable();
            String mLoginUserId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            secret = Md5Util.toMD5(AppConfig.apiKey + time + mLoginUserId + mAccessToken);
        }
        /*
        提现接口的secret计算在外面，
        com.sk.weichat.wxapi.WXEntryActivity.transfer
         */
        params("time", time);
        params("secret", secret);
        return this;
    }

    @Override
    public GetBuilder tag(Object tag) {
        return this;
    }

    public GetCall build() {
        url = appenParams();
        build = new Request.Builder().url(url).build();
        Log.i(HttpUtils.TAG, "网络请求参数：" + url);
        return new GetCall();
    }

    private String appenParams() {
        StringBuffer sb = new StringBuffer();
        sb.append(url);

        if (params != null && !params.isEmpty()) {
            sb.append("?");
            for (String key : params.keySet()) {
                sb.append(key).append("=").append(params.get(key)).append("&");
            }
            sb = sb.deleteCharAt(sb.length() - 1); // 去掉后面的&
        }

        return sb.toString();
    }

    @Override
    public GetBuilder params(String k, String v) {
        try {
            // url安全，部分字符不能直接放进url, 要改成百分号开头%的，
            v = URLEncoder.encode(v, "UTF-8");
        } catch (Exception e) {
            // 不可到达，UTF-8不可能不支持，
            e.printStackTrace();
        }
        // this.url = this.url+k+"="+v;
        if (params == null) {
            params = new LinkedHashMap<>();
        }
        params.put(k, v);
        return this;
    }

    public GetBuilder params(Map<String, String> params) {
        for (String key : params.keySet()) {
            params(key, params.get(key));
        }

        return this;
    }

    public class GetCall extends BaseCall {
        @Override
        public void execute(Callback callback) {
            super.execute(callback);
        }
    }
}

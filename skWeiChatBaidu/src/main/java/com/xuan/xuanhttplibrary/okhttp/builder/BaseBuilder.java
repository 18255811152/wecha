package com.xuan.xuanhttplibrary.okhttp.builder;


import android.text.TextUtils;

import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author Administrator
 * @time 2017/3/30 0:14
 * @des ${TODO}
 */

public abstract class BaseBuilder {

    protected String url;
    protected Object tag;
    protected Map<String, String> headers;
    protected Map<String, String> params;
    protected Request build;

    public abstract BaseBuilder url(String url);

    public abstract BaseBuilder tag(Object tag);

    public abstract BaseCall build();

    public abstract BaseBuilder params(String k, String v);

    public class BaseCall {
        public void execute(Callback callback) {
            OkHttpClient mOkHttpClient;
            if (!TextUtils.isEmpty(url)
                    && url.contains("https://ipinfo.io/geo")) {// 为获取Area的接口设置超时时长
                mOkHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(1, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.SECONDS)
                        .build();
            } else {
                mOkHttpClient = HttpUtils.getInstance().getOkHttpClient();
            }
            mOkHttpClient.newCall(build).enqueue(callback);
        }
    }
}

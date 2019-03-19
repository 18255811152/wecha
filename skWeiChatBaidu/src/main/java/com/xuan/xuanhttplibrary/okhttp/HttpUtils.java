package com.xuan.xuanhttplibrary.okhttp;

import com.xuan.xuanhttplibrary.okhttp.builder.GetBuilder;
import com.xuan.xuanhttplibrary.okhttp.builder.PostBuilder;

import okhttp3.OkHttpClient;

/**
 * @author liuxan
 * @time 2017/3/29 23:36
 * @des
 */

public class HttpUtils {

    public static String TAG = "HTTP";

    private static HttpUtils instance = new HttpUtils();

    private HttpUtils() {
    }

    public static HttpUtils getInstance() {
        return instance;
    }

    private OkHttpClient mOkHttpClient;

    public OkHttpClient getOkHttpClient() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }
        return mOkHttpClient;
    }

    public static PostBuilder post() {
        return new PostBuilder();
    }

    public static GetBuilder get() {
        return new GetBuilder();
    }
}

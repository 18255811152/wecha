package com.sk.weichat.xmpp.helloDemon;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.UserStatus;
import com.sk.weichat.ui.base.CoreManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FirebaseMessageService extends FirebaseInstanceIdService {
    private static final String TAG = "FCMService";

    public static void sendRegistrationToServer(Context ctx) {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "sendRegistrationToServer: " + refreshedToken);
        if (TextUtils.isEmpty(refreshedToken)) {
            Log.d(TAG, "还没拿到token，不上传token");
            return;
        }

        UserStatus status = CoreManager.getSelfStatus(ctx);
        if (status == null || TextUtils.isEmpty(status.accessToken)) {
            // 登录后会在MainActivity调用上传，
            Log.d(TAG, "还没登录，不上传token");
            return;
        }

        HttpUtils.post()
                .url(CoreManager.requireConfig(ctx).configFcm)
                .params("token", refreshedToken)
                .params("access_token", status.accessToken)
                .build()
                .execute(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Reporter.post("上传FCM token失败，", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "上传FCM token，onResponse: status = " + response.code());
                    }
                });

    }

    @Override
    public void onTokenRefresh() {
        Log.d(TAG, "Refreshed token: ");
        sendRegistrationToServer(this);
    }
}

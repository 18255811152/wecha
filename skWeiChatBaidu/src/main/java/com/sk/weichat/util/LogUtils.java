package com.sk.weichat.util;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装一些日志打印，统一方式打印一些麻烦的对象，
 * 方便崩溃上报后排查问题，
 */
public final class LogUtils {
    private static final String TAG = "LogUtils";

    private LogUtils() {
    }

    public static void log(Object obj) {
        if (obj instanceof Intent) {
            log((Intent) obj);
        } else if (obj instanceof Bundle) {
            log((Bundle) obj);
        } else {
            log(obj.toString());
        }
    }

    private static void log(Intent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("intent = ");
        if (intent == null) {
            sb.append("null").append('\n');
        } else {
            sb.append(intent.toString()).append('\n');
            sb.append("action = ").append(intent.getAction()).append('\n');
            sb.append("data = ").append(intent.getDataString()).append('\n');
            sb.append("extra = ");
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                sb.append("null").append('\n');
            } else {
                Map<String, Object> map = new HashMap<>();
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    map.put(key, value);
                }
                sb.append(map.toString());
            }
        }
        realLog(sb.toString());
    }

    private static void log(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("bundle = ");
        Map<String, Object> map = new HashMap<>();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            map.put(key, value);
        }
        sb.append(map.toString());
        realLog(sb.toString());
    }

    private static void log(String str) {
        realLog(str);
    }

    private static void realLog(String str) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            Log.w(TAG, str);
        }
    }
}

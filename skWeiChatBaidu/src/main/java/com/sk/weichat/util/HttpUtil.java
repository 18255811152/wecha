package com.sk.weichat.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zq on 2017/5/2 0002.
 */

public class HttpUtil {

    private static final String REGEX_URL = "(http|https)://[\\S\\.]+[:\\d]?[/\\S]+\\??[\\S=\\S&?]+[^\u4e00-\u9fa5]";

    public static boolean isURL(String text) {
        return getURLList(text).size() > 0;
    }

    public static List<String> getURLList(String str) {
        List<String> URLListStr = new ArrayList<>();
        // Pattern pattern = Patterns.WEB_URL;// 系统检测URL的正则
        Pattern pattern = Pattern.compile(REGEX_URL);
        Matcher matcher = pattern.matcher(str);
        StringBuilder stringBuffer = new StringBuilder();
        String s;
        while (matcher.find()) {
            s = matcher.group();
            stringBuffer.append(s);
        }
        if (TextUtils.isEmpty(stringBuffer.toString()))
            return URLListStr;

        String[] split = stringBuffer.toString().split("(http|https)://");
        for (String aSplit : split) {
            Log.e("html", aSplit);
            String mFilterChineseStr = filterChinese(aSplit);
            if (!TextUtils.isEmpty(mFilterChineseStr)) {
                URLListStr.add("http://" + mFilterChineseStr);
            }
        }
        return URLListStr;
    }

    // 过滤掉中文
    private static String filterChinese(String str) {
        String REGEX_CHINESE = "[\u4e00-\u9fa5]";
        Pattern pattern = Pattern.compile(REGEX_CHINESE);
        Matcher matcher = pattern.matcher(str);
        return matcher.replaceAll("");
    }

    /************************************************************/

    // 是否连接了网络
    public static boolean isGprsOrWifiConnected(Context context) {
        ConnectivityManager mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo gprs = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isConnectedGprs = gprs != null && gprs.isConnected();
        boolean isConnectedWifi = wifi != null && wifi.isConnected();
        return isConnectedGprs || isConnectedWifi;
    }

    // 判断是否有外网连接（普通方法不能判断外网的网络是否连接，比如连接上局域网）

    /**
     * ip == www.google.com 可以用来检测手机是否翻墙
     *
     * @param ip
     * @return
     */
    public static boolean ping(String ip) {
        String result = null;
        try {
            /**
             * -c 次数
             * -w 超时时长
             */
            Process p = Runtime.getRuntime().exec("ping -c 5 -w 2 " + ip);
            int status = p.waitFor();
            if (status == 0) {
                result = "success";
                return true;
            } else {
                result = "failed";
            }
        } catch (IOException e) {
            result = "IOException";
        } catch (InterruptedException e) {
            result = "InterruptedException";
        } finally {
            Log.d("----result---", "result = " + result);
        }
        return false;
    }
}

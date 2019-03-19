package com.sk.weichat.util;

import android.view.View;
import android.widget.TextView;

/**
 * Created by Administrator on 2017/10/26.
 */

public class UiUtils {
    private static final int MIN_CLICK_DELAY_TIME = 1000;
    private static long lastClickTime;

    public static void updateNum(TextView numTv, int unReadNum) {
        if (numTv == null) {
            return;
        }
        if (unReadNum < 1) {
            numTv.setText("");
            numTv.setVisibility(View.INVISIBLE);
        } else if (unReadNum > 99) {
            numTv.setText("99+");
            numTv.setVisibility(View.VISIBLE);
        } else {
            numTv.setText(String.valueOf(unReadNum));
            numTv.setVisibility(View.VISIBLE);
        }
    }

    public static boolean isNormalClick() {
        boolean isNormal = false;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            isNormal = true;
        }
        lastClickTime = currentTime;
        return isNormal;
    }
}

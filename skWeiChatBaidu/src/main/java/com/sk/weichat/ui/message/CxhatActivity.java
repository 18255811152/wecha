package com.sk.weichat.ui.message;

import android.os.Bundle;
import android.util.Log;

import com.sk.weichat.R;
import com.sk.weichat.ui.base.BaseActivity;


/**
 * 单聊界面
 */
public class CxhatActivity extends BaseActivity {

    long lastTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lastTime = System.currentTimeMillis();
        setContentView(R.layout.chat);
        Log.e("xuan", "timexxx  oncreate: " + (System.currentTimeMillis() - lastTime));
    }
}

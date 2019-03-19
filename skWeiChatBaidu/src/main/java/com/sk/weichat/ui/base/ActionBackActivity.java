package com.sk.weichat.ui.base;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import com.sk.weichat.AppConfig;

public abstract class ActionBackActivity extends StackActivity {
    protected Context mContext;
    protected String TAG;// 获取Tag，用于日志输出等标志
    private boolean isDestroyed = false;

    public ActionBackActivity() {
        TAG = this.getClass().getSimpleName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;
        if (AppConfig.DEBUG) {
            Log.e(AppConfig.TAG, TAG + " onCreate");
        }
    }

    @Override
    public boolean isDestroyed() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            return super.isDestroyed();
        }
        return isDestroyed;
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        mContext = null;
        if (AppConfig.DEBUG) {
            Log.e(AppConfig.TAG, TAG + " onDestroy");
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            return onHomeAsUp();
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean onHomeAsUp() {
        finish();
        return true;
    }
}

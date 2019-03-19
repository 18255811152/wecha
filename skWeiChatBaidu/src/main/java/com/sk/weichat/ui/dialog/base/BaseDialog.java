package com.sk.weichat.ui.dialog.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by Administrator on 2016/5/3.
 */
public abstract class BaseDialog {
    protected View mView;
    protected Activity mActivity;
    protected int RID;
    protected AlertDialog mDialog;
    protected boolean mCancleable = true;

    protected void initView() {
        if (RID != 0)
            mView = mActivity.getLayoutInflater().inflate(RID, null);
    }

    public BaseDialog show() {
        mDialog = new AlertDialog.Builder(mActivity).setView(mView).create();
        mDialog.setCancelable(mCancleable);
        mDialog.show();
        return this;
    }

    public <T> T $(int rid) {
        return (T) mView.findViewById(rid);
    }

    public String getString(int rstring) {
        return mActivity.getString(rstring);
    }
}

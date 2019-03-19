package com.sk.weichat.ui.dialog;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.dialog.base.BaseDialog;
import com.sk.weichat.util.SkinUtils;

/**
 * Created by Administrator on 2016/4/21.
 */
public class SingleTextDialogView extends BaseDialog {

    private TextView mTvTitle;
    private TextView mTvContent;
    private Button mBtnSure;
    private View.OnClickListener mOnClickListener;

    {
        RID = R.layout.dialog_sigle_text;
    }

    public SingleTextDialogView(Activity activity) {
        this(activity, "", "", null);
    }

    public SingleTextDialogView(Activity activity, String title, String content, View.OnClickListener onClickListener) {
        this.mActivity = activity;
        this.mOnClickListener = onClickListener;
        initView();
        mTvTitle.setText(title);
        mTvContent.setText(content);
    }

    public SingleTextDialogView(Activity activity, String title, String content, View.OnClickListener onClickListener, boolean cancleable) {
        this(activity, title, content, onClickListener);
        mCancleable = cancleable;
    }

    @Override
    protected void initView() {
        super.initView();
        mTvTitle = (TextView) mView.findViewById(R.id.title);
        mTvContent = (TextView) mView.findViewById(R.id.content);
        mBtnSure = (Button) mView.findViewById(R.id.sure_btn);
        mBtnSure.setBackgroundColor(SkinUtils.getSkin(mActivity).getAccentColor());
        mBtnSure.setText(InternationalizationHelper.getString("JX_Confirm"));

        mBtnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                if (mOnClickListener != null)
                    mOnClickListener.onClick(v);
            }
        });
    }

    public void setBottonClick(View.OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void setTitle(int rstring) {
        setTitle(getString(rstring));
    }

    public void setTitle(String title) {
        mTvTitle.setText(title);
    }

    public void setContent(int rstring) {
        setContent(getString(rstring));
    }


    public void setContent(String content) {
        mTvContent.setText(content);
    }


    public void setButtonText(int rstring) {
        setButtonText(getString(rstring));
    }

    public void setButtonText(String text) {
        mBtnSure.setText(text);
    }
}

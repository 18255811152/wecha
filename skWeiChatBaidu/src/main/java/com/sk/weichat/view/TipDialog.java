package com.sk.weichat.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.util.ScreenUtil;


/**
 * Created by zq on 2017/8/26 0026.
 * <p>
 * 仿ios提示dialog
 */
public class TipDialog extends Dialog {
    private TextView mTipTv;
    private TextView mConfirm;
    private ConfirmOnClickListener mConfirmOnClickListener;
    private String tip;

    public TipDialog(Context context) {
        super(context, R.style.MyDialog);
    }

    public void setTip(String tip) {// 单独设置提示语，不对点击事件做处理
        this.tip = tip;
    }

    /**
     * 点确定或者返回键取消对话框都调用这个回调，
     */
    public void setmConfirmOnClickListener(String tip, ConfirmOnClickListener mConfirmOnClickListener) {
        this.tip = tip;
        this.mConfirmOnClickListener = mConfirmOnClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tip_dialog);
        initView();
    }

    private void initView() {
        mTipTv = (TextView) findViewById(R.id.tip_tv);
        if (tip != null) {
            mTipTv.setText(tip);
        }
        mConfirm = (TextView) findViewById(R.id.confirm);
        mConfirm.setText(InternationalizationHelper.getString("JX_Confirm"));
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.7);
        initEvent();
    }

    private void initEvent() {
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mConfirmOnClickListener != null) {
                    mConfirmOnClickListener.confirm();
                }
            }
        });
    }

    public interface ConfirmOnClickListener {
        void confirm();
    }
}

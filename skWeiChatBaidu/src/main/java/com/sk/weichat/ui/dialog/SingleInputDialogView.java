package com.sk.weichat.ui.dialog;

import android.app.Activity;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.dialog.base.BaseDialog;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.view.ClearEditText;

/**
 * Created by Administrator on 2016/4/21.
 */
public class SingleInputDialogView extends BaseDialog {

    private TextView mTitleTv;
    private ClearEditText mContentET;
    private Button mCommitBtn;
    private View.OnClickListener mOnClickListener;

    {
        RID = R.layout.dialog_single_input;
    }

    public SingleInputDialogView(Activity activity) {
        mActivity = activity;
        initView();
    }

    public SingleInputDialogView(Activity activity, View.OnClickListener onClickListener) {
        mActivity = activity;
        initView();
        mOnClickListener = onClickListener;
    }

    // User In RoomInfoActivity Modify Group Name.Desc
    public SingleInputDialogView(Activity activity, String title, String hint, int maxLines, int lines, View.OnClickListener onClickListener) {
        mActivity = activity;
        initView();
        setView(title, hint, maxLines, lines);
        this.mOnClickListener = onClickListener;
    }

    public SingleInputDialogView(Activity activity, String title, String hint, int maxLines, int lines, InputFilter[] i, View.OnClickListener onClickListener) {
        mActivity = activity;
        initView();
        setView(title, hint, maxLines, lines, i);
        this.mOnClickListener = onClickListener;
    }

    protected void initView() {
        super.initView();
        mTitleTv = (TextView) mView.findViewById(R.id.title);
        mContentET = (ClearEditText) mView.findViewById(R.id.content);
        mCommitBtn = (Button) mView.findViewById(R.id.sure_btn);
        mCommitBtn.setBackgroundColor(SkinUtils.getSkin(mActivity).getAccentColor());
        mTitleTv.setText(InternationalizationHelper.getString("JXNewRoomVC_CreatRoom"));
        mContentET.setHint(InternationalizationHelper.getString("JX_InputRoomName"));
        mCommitBtn.setText(InternationalizationHelper.getString("JX_Confirm"));
    }

    private void setView(String title, String hint, int maxLines, int lines) {
        mTitleTv.setText(title);
        mContentET.setHint(hint);

        mContentET.setFilters(new InputFilter[]{DialogHelper.mExpressionFilter, DialogHelper.mChineseEnglishNumberFilter});

        mCommitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                if (mOnClickListener != null)
                    mOnClickListener.onClick(mContentET);
            }
        });
    }

    private void setView(String title, String hint, int maxLines, int lines, InputFilter[] i) {
        mTitleTv.setText(title);
        mContentET.setHint(hint);

        mCommitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                if (mOnClickListener != null)
                    mOnClickListener.onClick(mContentET);
            }
        });
    }

    public View getmView() {
        return mView;
    }

    public void setSureClick(View.OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void setTitle(String title) {
        mTitleTv.setText(title);
    }

    public void setHint(String hint) {
        mContentET.setHint(hint);
    }

    public void setLines(int lines) {
        mContentET.setLines(lines);
    }

    public void setMaxLines(int maxLines) {
        mContentET.setMaxLines(maxLines);
    }

    public void setFilters(InputFilter[] i) {
        mContentET.setFilters(i);
    }

    public String getContent() {
        return mContentET.getText().toString();
    }
}

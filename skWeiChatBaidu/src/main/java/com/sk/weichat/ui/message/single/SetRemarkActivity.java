package com.sk.weichat.ui.message.single;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

public class SetRemarkActivity extends BaseActivity implements View.OnClickListener {
    private TextView mRemarkNameTv;
    private EditText mRemarkNameEdit;

    private String mLoginUserId;
    private String mFriendId;
    private Friend mFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motify_cpyname);
        mLoginUserId = coreManager.getSelf().getUserId();
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.change_remark);
    }

    private void initView() {
        mRemarkNameTv = (TextView) findViewById(R.id.department_name);
        mRemarkNameTv.setText(R.string.remark);
        mRemarkNameEdit = (EditText) findViewById(R.id.department_edit);
        mRemarkNameEdit.setHint(R.string.tip_input_remark);
        if (!TextUtils.isEmpty(mFriend.getRemarkName())) {
            mRemarkNameEdit.setText(mFriend.getRemarkName());
        }

        findViewById(R.id.create_department_btn).setOnClickListener(this);
        findViewById(R.id.create_department_btn).setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.create_department_btn:
                String s = mRemarkNameEdit.getText().toString();
                if (TextUtils.isEmpty(s)) {
                    Toast.makeText(this, getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
                    return;
                }
                remarkFriend(s);
                break;
        }
    }

    private void remarkFriend(final String remarkName) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("remarkName", remarkName);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_REMARK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            FriendDao.getInstance().updateRemarkName(mLoginUserId, mFriendId, remarkName);
                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                            sendBroadcast(new Intent("NAME_CHANGE"));
                            finish();
                        } else {
                            ToastUtil.showErrorData(mContext);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_change_remark_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

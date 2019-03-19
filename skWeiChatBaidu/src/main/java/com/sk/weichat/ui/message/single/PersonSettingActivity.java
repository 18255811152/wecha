package com.sk.weichat.ui.message.single;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.Label;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.LabelDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.groupchat.SelectContactsActivity;
import com.sk.weichat.ui.message.SearchChatHistoryActivity;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.view.MsgSaveDaysDialog;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.view.TipDialog;
import com.suke.widget.SwitchButton;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * Created by Administrator on 2018/4/18 0018.
 */

public class PersonSettingActivity extends BaseActivity implements View.OnClickListener {

    private ImageView mFriendAvatarIv;
    private TextView mFriendNameTv;
    private TextView mRemarkNameTv;
    private TextView mLabelNameTv;
    private SwitchButton mIsReadFireSb;
    private SwitchButton mTopSb;
    private SwitchButton mIsDisturbSb;
    private TextView mMsgSaveDays;

    private String mLoginUserId;
    private String mFriendId;

    MsgSaveDaysDialog.OnMsgSaveDaysDialogClickListener onMsgSaveDaysDialogClickListener = new MsgSaveDaysDialog.OnMsgSaveDaysDialogClickListener() {
        @Override
        public void tv1Click() {
            updateChatRecordTimeOut(-1);
        }

        @Override
        public void tv2Click() {
            updateChatRecordTimeOut(0.04); // 1小时过期
            //            updateChatRecordTimeOut(0.00347); // 五分钟过期
        }

        @Override
        public void tv3Click() {
            updateChatRecordTimeOut(1);
        }

        @Override
        public void tv4Click() {
            updateChatRecordTimeOut(7);
        }

        @Override
        public void tv5Click() {
            updateChatRecordTimeOut(30);
        }

        @Override
        public void tv6Click() {
            updateChatRecordTimeOut(90);
        }

        @Override
        public void tv7Click() {
            updateChatRecordTimeOut(365);
        }
    };
    private Friend mFriend;
    private String mFriendName;
    private RefreshBroadcastReceiver receiver = new RefreshBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_setting);

        mLoginUserId = coreManager.getSelf().getUserId();
        mFriendId = getIntent().getStringExtra("ChatObjectId");
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);

        initActionBar();
        initView();
        registerReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);// Friend也更新下
        if (mFriend == null) {
            Toast.makeText(this, R.string.tip_friend_removed, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            mFriendName = TextUtils.isEmpty(mFriend.getRemarkName()) ? mFriend.getNickName() : mFriend.getRemarkName();
            mFriendNameTv.setText(mFriendName);
            if (!TextUtils.isEmpty(mFriend.getRemarkName())) {
                mRemarkNameTv.setText(mFriend.getRemarkName());
            }
            List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mFriendId);
            String labelNames = "";
            if (friendLabelList != null && friendLabelList.size() > 0) {
                for (int i = 0; i < friendLabelList.size(); i++) {
                    if (i == friendLabelList.size() - 1) {
                        labelNames += friendLabelList.get(i).getGroupName();
                    } else {
                        labelNames += friendLabelList.get(i).getGroupName() + "，";
                    }
                }
            }
            mLabelNameTv.setText(labelNames);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.chat_settings));
    }

    private void initView() {
        mFriendAvatarIv = (ImageView) findViewById(R.id.avatar);
        AvatarHelper.getInstance().displayAvatar(mFriendId, mFriendAvatarIv, true);
        mFriendNameTv = (TextView) findViewById(R.id.name);
        mRemarkNameTv = (TextView) findViewById(R.id.remark_name);
        mLabelNameTv = (TextView) findViewById(R.id.label_name);
        TextView mNoDisturbTv = (TextView) findViewById(R.id.no_disturb_tv);
        mNoDisturbTv.setText(InternationalizationHelper.getString("JX_MessageFree"));
        // 阅后即焚 && 置顶 && 消息免打扰
        mIsReadFireSb = (SwitchButton) findViewById(R.id.sb_read_fire);
        int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + mFriendId + mLoginUserId, 0);
        mIsReadFireSb.setChecked(isReadDel == 1);
        mIsReadFireSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                PreferenceUtils.putInt(mContext, Constants.MESSAGE_READ_FIRE + mFriendId + mLoginUserId, isChecked ? 1 : 0);
                if (isChecked) {
                    TipDialog tipDialog = new TipDialog(PersonSettingActivity.this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_status_burn), new TipDialog.ConfirmOnClickListener() {
                        @Override
                        public void confirm() {
                        }
                    });
                    tipDialog.show();
                }
            }
        });

        mTopSb = (SwitchButton) findViewById(R.id.sb_top_chat);
        mTopSb.setChecked(mFriend.getTopTime() != 0);// TopTime不为0，当前状态为置顶
        mTopSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    FriendDao.getInstance().updateTopFriend(mFriendId, mFriend.getTimeSend());
                } else {
                    FriendDao.getInstance().resetTopFriend(mFriendId);
                }
            }
        });

        mIsDisturbSb = (SwitchButton) findViewById(R.id.sb_no_disturb);
        mIsDisturbSb.setChecked(mFriend.getOfflineNoPushMsg() == 1);
        mIsDisturbSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                updateDisturbStatus(isChecked);
            }
        });

        mMsgSaveDays = (TextView) findViewById(R.id.msg_save_days_tv);
        mMsgSaveDays.setText(conversion(mFriend.getChatRecordTimeOut()));

        findViewById(R.id.avatar).setOnClickListener(this);
        if (coreManager.getLimit().cannotCreateGroup()) {
            findViewById(R.id.add_contacts).setVisibility(View.GONE);
        } else {
            findViewById(R.id.add_contacts).setOnClickListener(this);
        }
        findViewById(R.id.chat_history_search).setOnClickListener(this);
        findViewById(R.id.remark_rl).setOnClickListener(this);
        findViewById(R.id.label_rl).setOnClickListener(this);
        findViewById(R.id.msg_save_days_rl).setOnClickListener(this);
        findViewById(R.id.set_background_rl).setOnClickListener(this);
        findViewById(R.id.chat_history_empty).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(!UiUtils.isNormalClick()){
            return;
        }
        switch (v.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.avatar:
                Intent intentBasic = new Intent(this, BasicInfoActivity.class);
                intentBasic.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentBasic);
                break;
            case R.id.add_contacts:
                Intent intentAdd = new Intent(this, SelectContactsActivity.class);
                intentAdd.putExtra("QuicklyCreateGroup", true);
                intentAdd.putExtra("ChatObjectId", mFriendId);
                intentAdd.putExtra("ChatObjectName", mFriendName);
                startActivity(intentAdd);
                break;
            case R.id.chat_history_search:
                Intent intentChat = new Intent(this, SearchChatHistoryActivity.class);
                intentChat.putExtra("isSearchSingle", true);
                intentChat.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentChat);
                break;
            case R.id.remark_rl:
                Intent intentRemark = new Intent(this, SetRemarkActivity.class);
                intentRemark.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentRemark);
                break;
            case R.id.label_rl:
                Intent intentLabel = new Intent(this, SetLabelActivity.class);
                intentLabel.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentLabel);
                break;
            case R.id.msg_save_days_rl:
                MsgSaveDaysDialog msgSaveDaysDialog = new MsgSaveDaysDialog(this, onMsgSaveDaysDialogClickListener);
                msgSaveDaysDialog.show();
                break;
            case R.id.set_background_rl:
                Intent intentBackground = new Intent(this, SetChatBackActivity.class);
                intentBackground.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentBackground);
                break;
            case R.id.chat_history_empty:
                emptyServerMessage();

                // 清空聊天记录
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(null, getString(R.string.tip_confirm_clean_history), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        FriendDao.getInstance().resetFriendMessage(mLoginUserId, mFriendId);
                        ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, mFriendId);
                        sendBroadcast(new Intent(Constants.CHAT_HISTORY_EMPTY));// 清空聊天界面
                        MsgBroadcast.broadcastMsgUiUpdate(mContext);
                        Toast.makeText(PersonSettingActivity.this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                    }
                });
                selectionFrame.show();
                break;
        }
    }

    // 更新消息免打扰状态
    private void updateDisturbStatus(final boolean isChecked) {
        final String offlineNoPushMsg = isChecked ? "1" : "0";
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        params.put("toUserId", mFriendId);
        params.put("offlineNoPushMsg", offlineNoPushMsg);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_NOPULL_MSG)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            FriendDao.getInstance().updateOfflineNoPushMsgStatus(mFriendId, isChecked ? 1 : 0);
                        } else {
                            Toast.makeText(PersonSettingActivity.this, R.string.tip_edit_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(PersonSettingActivity.this);
                    }
                });
    }

    // 更新消息保存天数
    private void updateChatRecordTimeOut(final double outTime) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("chatRecordTimeOut", String.valueOf(outTime));

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(PersonSettingActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            mMsgSaveDays.setText(conversion(outTime));
                            FriendDao.getInstance().updateChatRecordTimeOut(mFriendId, outTime);
                            sendBroadcast(new Intent("NAME_CHANGE"));// 刷新聊天界面
                        } else {
                            Toast.makeText(PersonSettingActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 服务器上与该人的聊天记录也需要删除
    private void emptyServerMessage() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", String.valueOf(0));// 0 清空单人 1 清空所有
        params.put("toUserId", mFriendId);

        HttpUtils.get().url(coreManager.getConfig().EMPTY_SERVER_MESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private String conversion(double outTime) {
        String outTimeStr;
        if (outTime == -1 || outTime == 0) {
            outTimeStr = getString(R.string.permanent);
        } else if (outTime == -2) {
            outTimeStr = getString(R.string.no_sync);
        } else if (outTime == 0.04) {
            outTimeStr = getString(R.string.one_hour);
        } else if (outTime == 1) {
            outTimeStr = getString(R.string.one_day);
        } else if (outTime == 7) {
            outTimeStr = getString(R.string.one_week);
        } else if (outTime == 30) {
            outTimeStr = getString(R.string.one_month);
        } else if (outTime == 90) {
            outTimeStr = getString(R.string.one_season);
        } else {
            outTimeStr = getString(R.string.one_year);
        }
        return outTimeStr;
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("QC_FINISH");
        registerReceiver(receiver, intentFilter);
    }

    public class RefreshBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("QC_FINISH")) {
                // 快速创建群组 || 更换聊天背景 成功，接收到该广播结束当前界面
                finish();
            }
        }
    }
}

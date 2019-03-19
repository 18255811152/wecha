package com.sk.weichat.ui.other;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.AddAttentionResult;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.AttentionUser;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.Report;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.NewFriendDao;
import com.sk.weichat.db.dao.UserAvatarDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.FriendHelper;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.BusinessCircleActivity;
import com.sk.weichat.ui.map.MapActivity;
import com.sk.weichat.ui.message.ChatActivity;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.BasicInfoWindow;
import com.sk.weichat.view.NoDoubleClickListener;
import com.sk.weichat.view.ReportDialog;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
import com.sk.weichat.xmpp.listener.NewFriendListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

/**
 * 基本信息
 */
public class BasicInfoActivity extends BaseActivity implements NewFriendListener {
    private ImageView ivRight;
    private BasicInfoWindow menuWindow;

    private ImageView mAvatarImg;
    private TextView mNameTv;
    private TextView mRenameTv;
    private TextView mSexTv;
    private TextView mBirthdayTv;
    private TextView mCityTv;
    private Button mNextStepBtn;

    private String mUserId;
    private String mLoginUserId;
    private boolean isMyInfo = false;
    private User mUser;
    private Friend mFriend;
    /**
     * Todo All NewFriendMessage packetId
     */
    private String addhaoyouid = null;
    private String addblackid = null;
    private String removeblack = null;
    private String deletehaoyou = null;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            menuWindow.dismiss();
            if (mFriend == null) {
                mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            }
            switch (v.getId()) {
                case R.id.set_remark_nameS:
                    // 设置备注名
                    showRemarkDialog(mFriend);
                    break;
                case R.id.add_blacklist:
                    // 加入黑名单
                    showBlacklistDialog(mFriend);
                    break;
                case R.id.remove_blacklist:
                    // 移除黑名单
                    showBlacklistDialog(mFriend);
                    break;
                case R.id.delete_tv:
                    // 彻底删除
                    showDeleteAllDialog(mFriend);
                    break;
                case R.id.report_tv:
                    ReportDialog mReportDialog = new ReportDialog(BasicInfoActivity.this, false, new ReportDialog.OnReportListItemClickListener() {
                        @Override
                        public void onReportItemClick(Report report) {
                            report(mFriend, report);
                        }
                    });
                    mReportDialog.show();
                    break;
            }
        }
    };
    private int isyanzheng = 0;// 该好友是否需要验证

    public static void start(Context ctx, String userId) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info);
        if (getIntent() != null) {
            mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        }
        mLoginUserId = coreManager.getSelf().getUserId();
        if (TextUtils.isEmpty(mUserId)) {
            mUserId = mLoginUserId;
        }
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        initActionBar();
        initView();

        if (mUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {// 客服公众号
            findViewById(R.id.part_1).setVisibility(View.VISIBLE);
            findViewById(R.id.part_2).setVisibility(View.GONE);
            findViewById(R.id.go_publish_tv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFriend != null) {
                        Intent intent = new Intent(BasicInfoActivity.this, ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND, mFriend);
                        startActivity(intent);
                    } else {
                        Toast.makeText(BasicInfoActivity.this, R.string.tip_not_like_public_number, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return;
        }

        if (mLoginUserId.equals(mUserId)) { // 显示自己资料
            isMyInfo = true;
            loadMyInfoFromDb();
        } else { // 显示其他用户的资料
            isMyInfo = false;
            loadOthersInfoFromNet();
        }
        initEvent();

        ListenerManager.getInstance().addNewFriendListener(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_BaseInfo"));

        ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.title_moress);
        ivRight.setVisibility(View.GONE);
    }

    private void initView() {
        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);

        mNameTv = (TextView) findViewById(R.id.name_tv);
        mRenameTv = (TextView) findViewById(R.id.rename_tv);
        mSexTv = (TextView) findViewById(R.id.sex_tv);
        mBirthdayTv = (TextView) findViewById(R.id.birthday_tv);
        mCityTv = (TextView) findViewById(R.id.city_tv);
        TextView nickNameTv = (TextView) findViewById(R.id.name_text);
        nickNameTv.setText(InternationalizationHelper.getString("JX_NickName"));
        TextView sexTv = (TextView) findViewById(R.id.sex_text);
        sexTv.setText(InternationalizationHelper.getString("JX_Sex"));
        TextView birthdayTv = (TextView) findViewById(R.id.birthday_text);
        birthdayTv.setText(InternationalizationHelper.getString("JX_BirthDay"));
        TextView cityTv = (TextView) findViewById(R.id.city_text);
        cityTv.setText(InternationalizationHelper.getString("JX_Address"));

        TextView tv = (TextView) findViewById(R.id.look_location_tv);
        tv.setText(InternationalizationHelper.getString("JXUserInfoVC_Loation"));
        TextView qrCode = (TextView) findViewById(R.id.look_location_tv_02);
        qrCode.setText(InternationalizationHelper.getString("JXQR_QRImage"));
        TextView mCircleTv = (TextView) findViewById(R.id.look_bussic_cicle_tv);

        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
        mNextStepBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        // mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));

        if (mFriend != null) {// 该人为我的好友，先赋值，接口成功后在更新ui
            mNameTv.setText(mFriend.getNickName());
            mRenameTv.setText(TextUtils.isEmpty(mFriend.getRemarkName()) ? "" : mFriend.getRemarkName());
        }

        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.city_rl).setVisibility(View.GONE);
        }
    }

    private void initEvent() {
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuWindow = new BasicInfoWindow(BasicInfoActivity.this, itemsOnClick, mFriend);
                // 显示窗口
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // +x右,-x左,+y下,-y上
                // pop向左偏移显示
                menuWindow.showAsDropDown(view,
                        -(menuWindow.getContentView().getMeasuredWidth() - view.getWidth() / 2 - 40),
                        0);
            }
        });

        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                intent.putExtra(AppConstant.EXTRA_IMAGE_URI, mUserId);
                startActivity(intent);
            }
        });

        findViewById(R.id.erweima).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUser != null) {
                    Intent intent = new Intent(BasicInfoActivity.this, QRcodeActivity.class);
                    intent.putExtra("isgroup", false);
                    intent.putExtra("userid", mUser.getUserId());
                    startActivity(intent);
                }
            }
        });

        findViewById(R.id.look_bussic_cicle_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUser != null) {
                    Intent intent = new Intent(BasicInfoActivity.this, BusinessCircleActivity.class);
                    intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                    intent.putExtra(AppConstant.EXTRA_NICK_NAME, mUser.getNickName());
                    startActivity(intent);
                }
            }
        });

        findViewById(R.id.look_location_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double latitude = 0;
                double longitude = 0;
                if (mUser != null && mUser.getLoginLog() != null) {
                    latitude = mUser.getLoginLog().getLatitude();
                    longitude = mUser.getLoginLog().getLongitude();
                }
                if (latitude == 0 || longitude == 0) {
                    ToastUtil.showToast(mContext, getString(R.string.this_friend_not_open_position));
                    return;
                }
                Intent intent = new Intent(mContext, MapActivity.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("address", mUser.getNickName());
                startActivity(intent);
            }
        });
    }

    // 加载自己的信息
    private void loadMyInfoFromDb() {
        mUser = coreManager.getSelf();
        updateUI();
        // 隐藏 备注 一项
        findViewById(R.id.rn_rl).setVisibility(View.GONE);
    }

    // 加载好友的信息
    private void loadOthersInfoFromNet() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mUserId);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {

                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            mUser = result.getData();
                            if (mUser.getUserType() != 2) {// 公众号不做该操作 否则会将公众号的status变为好友
                                // 服务器的状态 与本地状态对比
                                AttentionUser attentionUser = mUser.getFriends();
                                if (FriendHelper.updateFriendRelationship(mLoginUserId, mUser.getUserId(), attentionUser)) {
                                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                                }
                            }
                            updateUI();
                        } else {
                            ToastUtil.showErrorData(mContext);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void updateFriendName(User user) {
        if (user != null) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            if (friend != null) {
                FriendDao.getInstance().updateNickName(mLoginUserId, mUserId, user.getNickName());
            }
        }
    }

    private void updateUI() {
        if (mUser == null) {
            return;
        }

        // 更新用户的头像
        AvatarHelper.updateAvatar(mUser.getUserId());
        displayAvatar(mUser.getUserId());

        mNameTv.setText(mUser.getNickName());
        updateFriendName(mUser);
        if (mUser.getFriends() != null && mFriend != null) {
            FriendDao.getInstance().updateChatRecordTimeOut(mFriend.getUserId(), mUser.getFriends().getChatRecordTimeOut());// 更新消息保存天数
            if (!TextUtils.isEmpty(mUser.getFriends().getRemarkName())) {
                mRenameTv.setText(mUser.getFriends().getRemarkName());

                FriendDao.getInstance().updateChatRecordTimeOut(mFriend.getUserId(), mUser.getFriends().getChatRecordTimeOut());
                if (TextUtils.isEmpty(mFriend.getRemarkName())
                        || !mFriend.getRemarkName().equals(mUser.getFriends().getRemarkName())) {// 本地备注为空 || 本地备注与服务端不一致 更新数据库
                    FriendDao.getInstance().updateRemarkName(coreManager.getSelf().getUserId(),
                            mUserId, mUser.getFriends().getRemarkName());// 更新备注名
                    // 刷新消息、通讯录、聊天页面
                    MsgBroadcast.broadcastMsgUiUpdate(mContext);
                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                    sendBroadcast(new Intent("NAME_CHANGE"));
                }
            }
        }
        mSexTv.setText(mUser.getSex() == 0 ? R.string.sex_woman : R.string.sex_man);
        mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str(mUser.getBirthday()));
        mCityTv.setText(Area.getProvinceCityString(mUser.getProvinceId(), mUser.getCityId()));

        if (isMyInfo) {
            mNextStepBtn.setVisibility(View.GONE);
        } else {
            mNextStepBtn.setVisibility(View.VISIBLE);
            if (mUser.getFriends() == null) {// 陌生人
                ivRight.setVisibility(View.GONE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JX_AddFriend"));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            } else if (mUser.getFriends().getBlacklist() == 1) {  //  需显示移除黑名单
                ivRight.setVisibility(View.GONE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("REMOVE"));
                mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());
            } else if (mUser.getFriends().getIsBeenBlack() == 1) {//  需显示加入黑名单
                ivRight.setVisibility(View.GONE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("TO_BLACKLIST"));
            } else if (mUser.getFriends().getStatus() == 2 || mUser.getFriends().getStatus() == 4) {// 好友
                ivRight.setVisibility(View.VISIBLE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
                mNextStepBtn.setOnClickListener(new SendMsgListener());
            } else {
                ivRight.setVisibility(View.GONE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JX_AddFriend"));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            }
        }
    }

    public void displayAvatar(final String userId) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(userId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(userId);

            Glide.with(MyApplication.getContext())
                    .load(mOriginalUrl)
                    .placeholder(R.drawable.avatar_normal)
                    .signature(new StringSignature(time))
                    .dontAnimate()
                    .error(R.drawable.avatar_normal)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            DialogHelper.dismissProgressDialog();
                            mAvatarImg.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            DialogHelper.dismissProgressDialog();
                            Log.e("zq", "加载原图失败：" + mOriginalUrl);// 该用户未设置头像，网页访问该URL也是404
                        }
                    });
        } else {
            DialogHelper.dismissProgressDialog();
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
        if (messageState == ChatMessageListener.MESSAGE_SEND_SUCCESS) {
            msgSendSuccess(message, message.getPacketId());
        } else if (messageState == ChatMessageListener.MESSAGE_SEND_FAILED) {
            msgSendFailed(message.getPacketId());
        }
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        if (!TextUtils.equals(mUserId, mLoginUserId)
                && TextUtils.equals(message.getUserId(), mUserId)) {// 当前对象正在对我进行操作
            loadOthersInfoFromNet();
            return false;
        }
        if (message.getType() == XmppMessage.TYPE_PASS) {// 对方同意好友请求 更新当前界面
            loadOthersInfoFromNet();
        }
        return false;
    }

    // xmpp消息发送成功最终回调到这，
    // 在这里调整ui,
    // 还有存本地数据库，
    public void msgSendSuccess(NewFriendMessage message, String packet) {
        if (addhaoyouid != null && addhaoyouid.equals(packet)) {
            if (isyanzheng == 0) {// 需要验证
                Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JXAlert_SayHiOK"), Toast.LENGTH_SHORT).show();
                ivRight.setVisibility(View.GONE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);

                ChatMessage sayChatMessage = new ChatMessage();
                sayChatMessage.setContent(InternationalizationHelper.getString("JXFriendObject_WaitPass"));
                sayChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                // 各种Dao都是本地数据库的操作，
                // 这个是朋友表里更新最后一条消息，
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, sayChatMessage);

                // 这个是新的朋友界面用的，等验证什么的都在新的朋友页面，
                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_10);// 朋友状态
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);
            } else if (isyanzheng == 1) {
                Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JX_AddSuccess"), Toast.LENGTH_SHORT).show();
                ivRight.setVisibility(View.VISIBLE);
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
                mNextStepBtn.setOnClickListener(new SendMsgListener());

                NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mLoginUserId, mUser.getUserId());// 加好友

                ChatMessage addChatMessage = new ChatMessage();
                addChatMessage.setContent(InternationalizationHelper.getString("JXNearVC_AddFriends") + ":" + mUser.getNickName());
                addChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addChatMessage);

                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_22);//添加了xxx
                FriendDao.getInstance().updateFriendContent(mLoginUserId, mUser.getUserId(), InternationalizationHelper.getString("JXMessageObject_BeFriendAndChat"), XmppMessage.TYPE_TEXT, TimeUtils.sk_time_current_time());
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

                loadOthersInfoFromNet();
                CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
            }
            // 已经是好友了，mFriend不能为空，
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        } else if (addblackid != null && addblackid.equals(packet)) {
            Toast.makeText(getApplicationContext(), getString(R.string.add_blacklist_succ), Toast.LENGTH_SHORT).show();
            ivRight.setVisibility(View.GONE);
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
            mNextStepBtn.setText(InternationalizationHelper.getString("REMOVE"));
            mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());

            // 更新当前持有的Friend对象，
            mFriend.setStatus(Friend.STATUS_BLACKLIST);
            FriendDao.getInstance().updateFriendStatus(message.getOwnerId(), message.getUserId(), mFriend.getStatus());
            FriendHelper.addBlacklistExtraOperation(message.getOwnerId(), message.getUserId());

            ChatMessage addBlackChatMessage = new ChatMessage();
            addBlackChatMessage.setContent(InternationalizationHelper.getString("JXFriendObject_AddedBlackList") + " " + mUser.getNickName());
            addBlackChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addBlackChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_18);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (removeblack != null && removeblack.equals(packet)) {
            Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("REMOVE_BLACKLIST"), Toast.LENGTH_SHORT).show();
            ivRight.setVisibility(View.VISIBLE);
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
            mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
            mNextStepBtn.setOnClickListener(new SendMsgListener());

            // 更新当前持有的Friend对象，
            mFriend.setStatus(Friend.STATUS_FRIEND);
            NewFriendDao.getInstance().ascensionNewFriend(message, mFriend.getStatus());
            FriendHelper.beAddFriendExtraOperation(message.getOwnerId(), message.getUserId());

            ChatMessage removeChatMessage = new ChatMessage();
            removeChatMessage.setContent(coreManager.getSelf().getNickName() + InternationalizationHelper.getString("REMOVE"));
            removeChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);
           /* FriendDao.getInstance().updateFriendContent(mLoginUserId, message.getUserId(),
                    InternationalizationHelper.getString("JXMessageObject_BeFriendAndChat"), XmppMessage.TYPE_TEXT, TimeUtils.sk_time_current_time());*/

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(message.getUserId(), Friend.STATUS_24);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            loadOthersInfoFromNet();
        } else if (deletehaoyou != null && deletehaoyou.equals(packet)) {
            Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JXAlert_DeleteOK"), Toast.LENGTH_SHORT).show();

            FriendHelper.removeAttentionOrFriend(mLoginUserId, message.getUserId());

            ChatMessage deleteChatMessage = new ChatMessage();
            deleteChatMessage.setContent(InternationalizationHelper.getString("JXAlert_DeleteFirend") + " " + mUser.getNickName());
            deleteChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, deleteChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_16);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void msgSendFailed(String packet) {
        DialogHelper.dismissProgressDialog();
        if (packet.equals(addhaoyouid)) {
            Toast.makeText(this, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(addblackid)) {
            Toast.makeText(this, R.string.tip_put_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(removeblack)) {
            Toast.makeText(this, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(deletehaoyou)) {
            Toast.makeText(this, R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Todo NextStep && ivRight Operating
     */

    // 点击加好友就调用这个方法，
    private void doAddAttention() {
        if (mUser == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        // 首先是调接口，
        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {

                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            if (result.getData().getType() == 1 || result.getData().getType() == 3) {
                                isyanzheng = 0;// 需要验证
                                // 需要验证就发送打招呼的消息，
                                doSayHello(InternationalizationHelper.getString("JXUserInfoVC_Hello"));
                            } else if (result.getData().getType() == 2 || result.getData().getType() == 4) {// 已经是好友了
                                isyanzheng = 1;// 不需要验证
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_FRIEND, null, mUser);
                                NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                                // 不需要验证的话直接加上，就发个xmpp消息，
                                // 这里最终调用smack的方法发送xmpp消息，
                                coreManager.sendNewFriendMessage(mUser.getUserId(), message);

                                addhaoyouid = message.getPacketId();
                            } else if (result.getData().getType() == 5) {
                                ToastUtil.showToast(mContext, R.string.add_attention_failed);
                            }
                        } else {
                            Toast.makeText(BasicInfoActivity.this, result.getResultMsg() + "", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 打招呼
    private void doSayHello(String text) {
        if (TextUtils.isEmpty(text)) {
            text = InternationalizationHelper.getString("HEY-HELLO");
        }
        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                XmppMessage.TYPE_SAYHELLO, text, mUser);
        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
        // 这里最终调用smack的发送消息，
        coreManager.sendNewFriendMessage(mUser.getUserId(), message);

        addhaoyouid = message.getPacketId();

        // 发送打招呼的消息
        ChatMessage sayMessage = new ChatMessage();
        sayMessage.setFromUserId(coreManager.getSelf().getUserId());
        sayMessage.setFromUserName(coreManager.getSelf().getNickName());
        sayMessage.setContent(InternationalizationHelper.getString("HEY-HELLO"));
        sayMessage.setType(XmppMessage.TYPE_TEXT); //文本类型
        sayMessage.setMySend(true);
        sayMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        sayMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sayMessage.setTimeSend(TimeUtils.sk_time_current_time());
        ChatMessageDao.getInstance().saveNewSingleChatMessage(message.getOwnerId(), message.getUserId(), sayMessage);
    }

    private void showRemarkDialog(final Friend friend) {
        DialogHelper.showLimitSingleInputDialog(this, InternationalizationHelper.getString("JXUserInfoVC_SetName"), friend.getShowName(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = ((EditText) v).getText().toString().trim();
                if (input.equals(friend.getShowName())) {// 备注名没变
                    return;
                }

                if (TextUtils.isEmpty(input)) {
                    Toast.makeText(mContext, getString(R.string.name_cannot_ull), Toast.LENGTH_SHORT).show();
                    return;
                }
                remarkFriend(friend, input);
            }
        });
    }

    private void remarkFriend(final Friend friend, final String remarkName) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
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
                            mRenameTv.setText(remarkName);
                            if (mFriend != null) {
                                mFriend.setRemarkName(remarkName);
                            }
                            FriendDao.getInstance().updateRemarkName(coreManager.getSelf().getUserId(),
                                    friend.getUserId(), remarkName);// 更新数据库

                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                            sendBroadcast(new Intent("NAME_CHANGE"));
                        } else {
                            ToastUtil.showErrorData(mContext);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.change_remark_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 加入、移除黑名单
    private void showBlacklistDialog(final Friend friend) {
/*
        if (friend.getStatus() == Friend.STATUS_BLACKLIST) {
            removeBlacklist(friend);
        } else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
            addBlacklist(friend);
        }
*/
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.add_black_list), getString(R.string.sure_add_friend_blacklist), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                addBlacklist(friend);
            }
        });
        mSF.show();
    }

    private void addBlacklist(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (friend.getStatus() == Friend.STATUS_FRIEND) {
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_BLACK, null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);// 加入黑名单

                                // 记录加入黑名单消息packet，如收到消息回执，在做后续操作
                                addblackid = message.getPacketId();
                            }
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, getString(R.string.add_blacklist_fail), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeBlacklist(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_REFUSED, null, friend);
                            coreManager.sendNewFriendMessage(friend.getUserId(), message);// 移除黑名单

                            // 记录移除黑名单消息packet，如收到消息回执，在做后续操作
                            removeblack = message.getPacketId();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 删除好友
    private void showDeleteAllDialog(final Friend friend) {
        if (friend.getStatus() == Friend.STATUS_UNKNOW) {// 陌生人
            return;
        }
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.delete_friend), getString(R.string.sure_delete_friend), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                deleteFriend(friend, 1);
            }
        });
        mSF.show();
    }

    private void deleteFriend(final Friend friend, final int type) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_DELALL, null, friend);
                            coreManager.sendNewFriendMessage(mUser.getUserId(), message); // 删除好友

                            // 记录删除好友消息packet，如收到消息回执，在做后续操作
                            deletehaoyou = message.getPacketId();
                        } else {
                            ToastUtil.showErrorData(mContext);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void report(Friend friend, Report report) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        params.put("reason", String.valueOf(report.getReportId()));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            ToastUtil.showToast(BasicInfoActivity.this, R.string.report_success);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
    }

    // 加关注
    private class AddAttentionListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            doAddAttention();
        }
    }

    // 移除黑名单  直接变为好友  不需要验证
    private class RemoveBlacklistListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());// 更新好友的状态
            removeBlacklist(friend);
        }
    }

    // 发消息
    private class SendMsgListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());
            MsgBroadcast.broadcastMsgUiUpdate(BasicInfoActivity.this);
            MsgBroadcast.broadcastMsgNumReset(BasicInfoActivity.this);

            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra(ChatActivity.FRIEND, friend);
            startActivity(intent);
        }
    }
}
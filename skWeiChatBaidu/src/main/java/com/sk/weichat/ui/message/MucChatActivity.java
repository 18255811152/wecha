package com.sk.weichat.ui.message;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.adapter.MessageLocalVideoFile;
import com.sk.weichat.adapter.MessageUploadChatRecord;
import com.sk.weichat.adapter.MessageVideoFile;
import com.sk.weichat.audio_x.VoicePlayer;
import com.sk.weichat.bean.EventNewNotice;
import com.sk.weichat.bean.EventUploadFileRate;
import com.sk.weichat.bean.EventXMPPJoinGroupFailed;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.MsgRoamTask;
import com.sk.weichat.bean.RoomMember;
import com.sk.weichat.bean.VideoFile;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.bean.company.StructBeanNetInfo;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.ChatRecord;
import com.sk.weichat.bean.message.MucRoom;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.bean.redpacket.OpenRedpacket;
import com.sk.weichat.bean.redpacket.RedPacket;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.MsgRoamTaskDao;
import com.sk.weichat.db.dao.RoomMemberDao;
import com.sk.weichat.db.dao.VideoFileDao;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.FileDataHelper;
import com.sk.weichat.helper.UploadEngine;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.dialog.CreateCourseDialog;
import com.sk.weichat.ui.map.MapPickerActivity;
import com.sk.weichat.ui.me.MyCollection;
import com.sk.weichat.ui.me.redpacket.MucSendRedPacketActivity;
import com.sk.weichat.ui.me.redpacket.RedDetailsActivity;
import com.sk.weichat.ui.message.multi.InviteVerifyActivity;
import com.sk.weichat.ui.message.multi.RoomInfoActivity;
import com.sk.weichat.ui.mucfile.XfileUtils;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.HtmlUtils;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.util.log.FileUtils;
import com.sk.weichat.video.MediaRecordActivity;
import com.sk.weichat.video.MessageEventGpu;
import com.sk.weichat.view.ChatBottomView;
import com.sk.weichat.view.ChatBottomView.ChatBottomListener;
import com.sk.weichat.view.ChatContentView;
import com.sk.weichat.view.ChatContentView.MessageEventListener;
import com.sk.weichat.view.NoDoubleClickListener;
import com.sk.weichat.view.PullDownListView;
import com.sk.weichat.view.SelectCardPopupWindow;
import com.sk.weichat.view.SelectFileDialog;
import com.sk.weichat.view.SelectRoomMemberPopupWindow;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.view.TipDialog;
import com.sk.weichat.view.photopicker.PhotoPickerActivity;
import com.sk.weichat.view.photopicker.SelectModel;
import com.sk.weichat.view.photopicker.intent.PhotoPickerIntent;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
import com.sk.weichat.xmpp.listener.MucListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import org.jivesoftware.smackx.muclight.element.MUCLightGetAffiliationsIQ;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardforchat;
import okhttp3.Call;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * 聊天主界面
 */
public class MucChatActivity extends BaseActivity implements
        MessageEventListener, ChatBottomListener, ChatMessageListener, MucListener,
        SelectRoomMemberPopupWindow.SendMember, SelectCardPopupWindow.SendCardS {

    private static final int REQUEST_CODE_INVITE = 895;
    /***********************
     * 拍照和选择照片
     **********************/
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_SELECT_VIDEO = 3;
    private static final int REQUEST_CODE_SEND_COLLECTION = 4;// 我的收藏 返回
    private static final int REQUEST_CODE_SELECT_Locate = 5;
    private static final int REQUEST_CODE_QUICK_SEND = 6;
    private static final int REQUEST_CODE_SELECT_FILE = 7;
    /**
     * 得到被@的群成员,设置聊天消息,高亮显示
     */
    List<String> atUserId = new ArrayList<>();
    // 该群组不存在本地聊天记录，根据漫游时长去服务端同步
    List<ChatMessage> chatMessages;
    @SuppressWarnings("unused")
    private ChatContentView mChatContentView;
    // 存储聊天消息
    private List<ChatMessage> mChatMessages;
    private ChatBottomView mChatBottomView;
    private AudioManager mAudioManager;
    // 当前聊天对象
    private Friend mFriend;
    private String mLoginUserId;
    private String mLoginNickName;
    private String instantMessage;
    // 是否为通知栏进入
    private boolean isNotificationComing;
    // 当前聊天对象的UserId（就是房间jid）
    private String mUseId;
    // 当前聊天对象的昵称（就是房间名称）
    private String mNickName;
    // 是否是群聊
    private boolean isGroupChat;
    private String[] noticeFriendList;
    private String roomId;
    private boolean isSearch;
    private long mSearchTime = 0;
    private LinearLayout mNewMsgLl;
    private TextView mNewMsgTv;
    private int mNewMsgNum;
    private TextView mTvTitleLeft;
    private TextView mTvTitle;
    private boolean isFriendNull = false;
    // 置顶公告，
    private View llNotice;
    private TextView tvNotice;
    // @群成员的popWindow
    private SelectRoomMemberPopupWindow mSelectRoomMemberPopupWindow;
    // 发送名片的popWindow
    private SelectCardPopupWindow mSelectCardPopupWindow;
    private RoomMember mRoomMember;
    private double mMinId = 0;
    private int mPageSize = 20;
    private boolean mHasMoreData = true;
    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {

        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            send(message);
        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {
            for (int i = 0; i < mChatMessages.size(); i++) {
                ChatMessage msg = mChatMessages.get(i);
                if (message.get_id() == msg.get_id()) {
                    msg.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mUseId, message.get_id(),
                            ChatMessageListener.MESSAGE_SEND_FAILED);
                    mChatContentView.notifyDataSetInvalidated(false);
                    break;
                }
            }
        }
    };
    private Uri mNewPhotoUri;
    private boolean allowUploadFile;
    private TipDialog tipDialog;
    /*******************************************
     * 接收到广播后的后续操作
     ******************************************/
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MsgBroadcast.ACTION_MSG_STATE_UPDATE)) {
                // 改变某条消息的状态  显示已读人数
                String packetId = intent.getStringExtra("packetId");
                for (int i = 0; i < mChatMessages.size(); i++) {
                    ChatMessage chatMessage = mChatMessages.get(i);
                    if (packetId.equals(chatMessage.getPacketId())) {
                        /* if (chatMessage.getFromUserId().equals(mLoginUserId)) { return; } // 有时候可能会收到自己的已读回执，不记录  */
                        chatMessage.setReadPersons(chatMessage.getReadPersons() + 1);
                        // mChatContentView.changeReadPersons(i, count + 1);a
                        mChatContentView.notifyDataSetChanged();
                        break;
                    }
                }
            } else if (action.equals("REFRESH_MANAGER") || action.equals(MsgBroadcast.ACTION_MSG_ROLE_CHANGED)) {
                // 被设置/取消了管理员时重新初始化roomMember以重置禁言状态以及撤回权限，
                // 隐身人监控人也一样，
                initRoomMember();
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE)) {
                // 群组已被锁定
                mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriend.getUserId());// 重新获取friend对象
                if (mFriend.getGroupStatus() == 3) {
                    groupTip(getString(R.string.tip_group_disable_by_service));
                }
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UPDATE_ROOM)) {
                // 其他人被邀请入群 全体禁言 需要刷新ui
                mChatContentView.notifyDataSetChanged();

                if (tipDialog != null && tipDialog.isShowing()) {
                    tipDialog.dismiss();
                }
                getMyInfoInThisRoom();

                updateBannedStatus();
            } else if (action.equals("MSG_BACK")) {
                // 撤回消息
                String packetId = intent.getStringExtra("packetId");
                if (TextUtils.isEmpty(packetId)) {
                    return;
                }
                for (ChatMessage chatMessage : mChatMessages) {
                    if (packetId.equals(chatMessage.getPacketId())) {
                        if (chatMessage.getType() == XmppMessage.TYPE_VOICE
                                && !TextUtils.isEmpty(VoicePlayer.instance().getVoiceMsgId())
                                && packetId.equals(VoicePlayer.instance().getVoiceMsgId())) {// 语音 && 正在播放的msgId不为空 撤回的msgId==正在播放的msgId
                            // 停止播放语音
                            VoicePlayer.instance().stop();
                        }
                        ChatMessage chat = ChatMessageDao.getInstance().findMsgById(mLoginUserId, mUseId, packetId);
                        chatMessage.setContent(chat.getContent());
                        chatMessage.setType(chat.getType());
                        break;
                    }
                }
                mChatContentView.notifyDataSetInvalidated(false);
            } else if (action.equals(Constants.CHAT_MESSAGE_DELETE_ACTION)) {
                // 删除消息
                if (mChatContentView != null) {
                    int position = intent.getIntExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, 10000);
                    if (position == 10000) {
                        return;
                    }
                    ChatMessage message = mChatMessages.get(position);

                    deleteMessage(message.getPacketId());// 服务端也需要删除

                    if (ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message)) {
                        if (mChatMessages.size() > 0 && mChatMessages.size() - 1 == position) {// 删除的为最后一条消息，更新LastContent
                            message.setType(XmppMessage.TYPE_TEXT);
                            message.setContent("");
                            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mUseId, message);
                        }
                        mChatMessages.remove(position);
                        mChatContentView.notifyDataSetInvalidated(false);
                    } else {
                        Toast.makeText(mContext, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (action.equals(Constants.SHOW_MORE_SELECT_MENU)) {// 显示多选菜单
                int position = intent.getIntExtra(Constants.CHAT_SHOW_MESSAGE_POSITION, 0);
                moreSelected(true, position);
            } else if (action.equals(Constants.CHAT_TIME_OUT_ACTION)) {
                String friendid = intent.getStringExtra("friend_id");
                double timeOut = intent.getDoubleExtra("time_out", -1);
                mFriend.setChatRecordTimeOut(timeOut);

            } else if (action.equals(Constants.CHAT_HISTORY_EMPTY)) {
                // 清空聊天记录
                mChatMessages.clear();
                mChatContentView.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onCoreReady() {
        super.onCoreReady();
        if (isGroupChat) {
            // 之前friend.getTimeSend崩溃了，原因可能为回调到coreReady之后mUserId还未赋值，导致friend空了
            if (TextUtils.isEmpty(mUseId)) {
                if (getIntent() != null) {
                    mUseId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
                }
            }
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
            if (friend != null) {
                coreManager.joinMucChat(mUseId, friend.getTimeSend());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(AppConstant.EXTRA_USER_ID, mUseId);
        outState.putString(AppConstant.EXTRA_NICK_NAME, mNickName);
        outState.putBoolean(AppConstant.EXTRA_IS_GROUP_CHAT, isGroupChat);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        /*AndroidBug5497Workaround.assistActivity(this);*/
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        if (getIntent() != null) {
            mUseId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            mNickName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
            isGroupChat = getIntent().getBooleanExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
            noticeFriendList = getIntent().getStringArrayExtra(Constants.GROUP_JOIN_NOTICE);// 获得加入群新朋友的列表.
            isSearch = getIntent().getBooleanExtra("isserch", false);
            if (isSearch) {
                mSearchTime = getIntent().getLongExtra("jilu_id", 0);
            }
            instantMessage = getIntent().getStringExtra("messageId");
            isNotificationComing = getIntent().getBooleanExtra(Constants.IS_NOTIFICATION_BAR_COMING, false);
        }
        mNewMsgNum = getIntent().getIntExtra(Constants.NEW_MSG_NUMBER, 0);

        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
        if (mFriend == null) {
            ToastUtil.showToast(mContext, getString(R.string.tip_program_error));
            isFriendNull = true;
            finish();
            return;
        }
        roomId = mFriend.getRoomId();
        mAudioManager = (AudioManager) getSystemService(android.app.Service.AUDIO_SERVICE);
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + mLoginUserId
                + File.separator + Environment.DIRECTORY_MUSIC);

        initView();

        // 绑定服务，添加监听，注册EventBus,注册广播
        ListenerManager.getInstance().addChatMessageListener(this);
        ListenerManager.getInstance().addMucListener(this);
        EventBus.getDefault().register(this);
        IntentFilter filter = new IntentFilter();
        // 消息状态的改变
        filter.addAction(MsgBroadcast.ACTION_MSG_STATE_UPDATE);
        filter.addAction(MsgBroadcast.ACTION_MSG_UPDATE_ROOM);
        filter.addAction(MsgBroadcast.ACTION_MSG_ROLE_CHANGED);
        filter.addAction(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE);
        // 消息撤回
        filter.addAction("MSG_BACK");
        filter.addAction("REFRESH_MANAGER");
        filter.addAction(Constants.CHAT_MESSAGE_DELETE_ACTION);
        filter.addAction(Constants.SHOW_MORE_SELECT_MENU);
        filter.addAction(Constants.CHAT_HISTORY_EMPTY);
        filter.addAction(Constants.CHAT_TIME_OUT_ACTION);
        registerReceiver(broadcastReceiver, filter);
    }

    private void setLastNotice(MucRoom.Notice notice) {
        // 公告置顶7天，
        if (notice != null && TimeUnit.SECONDS.toMillis(notice.getTime()) + TimeUnit.DAYS.toMillis(7) > System.currentTimeMillis()) {
            setLastNotice(notice.getText());
        } else {
            // 暂时没有在响应公告展示后隐藏的情况，
            // llNotice.setVisibility(View.GONE);
            tvNotice.setText(getString(R.string.no_notice));
        }
    }

    // 展示置顶公告，
    private void setLastNotice(String notice) {
        llNotice.setVisibility(View.VISIBLE);
        tvNotice.setText(notice);
        tvNotice.setSelected(true);
    }

    private void initView() {
        mChatMessages = new ArrayList<>();
        mChatBottomView = (ChatBottomView) findViewById(R.id.chat_bottom_view);
        initActionBar();
        mChatBottomView.setChatBottomListener(this);
        mChatBottomView.getmShotsLl().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChatBottomView.getmShotsLl().setVisibility(View.GONE);
                String shots = PreferenceUtils.getString(mContext, Constants.SCREEN_SHOTS, "No_Shots");
                QuickSendPreviewActivity.startForResult(MucChatActivity.this, shots, REQUEST_CODE_QUICK_SEND);
            }
        });
        mChatBottomView.setGroup(true);

        mChatContentView = (ChatContentView) findViewById(R.id.chat_content_view);
        mChatContentView.setToUserId(mUseId);
        mChatContentView.setRoomId(mFriend.getRoomId());
        mChatContentView.setCurGroup(true, mFriend.getRoomMyNickName());
        mChatContentView.setData(mChatMessages);
        mChatContentView.setChatBottomView(mChatBottomView);// 需要获取多选菜单的点击事件
        mChatContentView.setMessageEventListener(this);
        mChatContentView.setRefreshListener(new PullDownListView.RefreshingListener() {
            @Override
            public void onHeaderRefreshing() {
                loadDatas(false);
            }
        });

        // 表示已读
        if (isNotificationComing) {
            Intent intent = new Intent();
            intent.putExtra(AppConstant.EXTRA_FRIEND, mFriend);
            intent.setAction(Constants.NOTIFY_MSG_SUBSCRIPT);
            sendBroadcast(intent);
        } else {
            FriendDao.getInstance().markUserMessageRead(mLoginUserId, mUseId);
        }
        if (mFriend.getIsAtMe() != 0) {// 更新@状态
            FriendDao.getInstance().updateAtMeStatus(mFriend.getUserId(), 0);
        }

        mNewMsgLl = (LinearLayout) findViewById(R.id.msg_up_ll);
        mNewMsgTv = (TextView) findViewById(R.id.msg_up_tv);
        mNewMsgLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewMsgLl.setVisibility(View.GONE);
                mChatContentView.smoothScrollToPosition(0);
            }
        });
        llNotice = findViewById(R.id.llNotice);
        tvNotice = findViewById(R.id.tvNotice);
        llNotice.setVisibility(View.VISIBLE);
        llNotice.setOnClickListener(v -> {
            llNotice.setVisibility(View.GONE);
        });

        loadDatas(true);

        initRoomMember();

        getMyInfoInThisRoom();
    }

    private void loadDatas(boolean scrollToBottom) {
        boolean isFirstEnter;
        if (mChatMessages.size() <= 0) {
            isFirstEnter = true;
            ChatMessage mLastChatMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, mFriend.getUserId());
            if (mLastChatMessage == null) {
                synchronizeChatHistory();
                return;
            } else {
                if (mLastChatMessage.getTimeSend() != 0) {
                    mMinId = mLastChatMessage.getDoubleTimeSend() + 1;  // sq < mMinId
                } else {
                    mMinId = TimeUtils.sk_time_current_time();// 理论上不存在
                }
            }
        } else {
            isFirstEnter = false;
            mMinId = mChatMessages.get(0).getDoubleTimeSend();
        }

        List<ChatMessage> chatLists;
        if (isFirstEnter && mNewMsgNum > 20) {// 第一次进入当前页面且新消息数量>20,查出所有新消息
            chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mLoginUserId,
                    mFriend.getUserId(), mMinId, 100);// 最多获取100条 因为群组离线消息只保存了最后一百条，当超过100条时，就要靠分页漫游了

            mNewMsgTv.setText(getString(R.string.new_message_count_place_holder, chatLists.size()));
            mNewMsgLl.setVisibility(View.VISIBLE);
        } else {
            chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mLoginUserId,
                    mFriend.getUserId(), mMinId, mPageSize);
        }
        if (chatLists == null || chatLists.size() <= 0) {
            /** 加载漫游 */
            if (!scrollToBottom) {
                getNetSingle();
            }
        } else {

            long currTime = TimeUtils.sk_time_current_time();
            for (int i = 0; i < chatLists.size(); i++) {
                ChatMessage message = chatLists.get(i);
                // 防止过期的消息出现在列表中
                if (message.getDeleteTime() > 0 && message.getDeleteTime() < currTime) {
                    //                    ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                    continue;
                }

                mChatMessages.add(0, message);
            }

            if (isSearch) {// 查找聊天记录 进入
                isSearch = false;
                int position = 0;
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).getTimeSend() == mSearchTime) {
                        position = i;
                    }
                }
                mChatContentView.notifyDataSetInvalidated(position);// 定位到该条消息
            } else {
                if (scrollToBottom) {
                    mChatContentView.notifyDataSetInvalidated(scrollToBottom);
                } else {
                    mChatContentView.notifyDataSetInvalidated(chatLists.size());
                }
            }
            mChatContentView.headerRefreshingCompleted();
            if (!mHasMoreData) {
                mChatContentView.setNeedRefresh(false);
            }
        }
    }

    // 草稿
    protected void onSaveContent() {
        String str = mChatBottomView.getmChatEdit().getText().toString().trim();
        // 清除 回车与空格
        str = str.replaceAll("\\s", "");
        str = str.replaceAll("\\n", "");
        if (TextUtils.isEmpty(str)) {
            if (XfileUtils.isNotEmpty(mChatMessages)) {
                ChatMessage chatMessage = mChatMessages.get(mChatMessages.size() - 1);
                String fromUserName;
                if (chatMessage.getType() == XmppMessage.TYPE_TIP) {// 群组控制消息不添加FromUserId
                    fromUserName = "";
                } else {
                    fromUserName = TextUtils.isEmpty(chatMessage.getFromUserName()) ? "" : chatMessage.getFromUserName() + " : ";
                }
                FriendDao.getInstance().updateFriendContent(mLoginUserId, mFriend.getUserId(),
                        fromUserName + chatMessage.getContent(),
                        chatMessage.getType(),
                        chatMessage.getTimeSend());
            }
        } else {
            // 更新朋友表最后一次事件
            FriendDao.getInstance().updateFriendContent(mLoginUserId,
                    mFriend.getUserId(),
                    "&8824" + str,  //InternationalizationHelper.getString("JX_Draft")
                    XmppMessage.TYPE_TEXT, TimeUtils.sk_time_current_time());
        }
        PreferenceUtils.putString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, str);
    }

    private void doBack() {
        if (!TextUtils.isEmpty(instantMessage)) {
            SelectionFrame selectionFrame = new SelectionFrame(this);
            selectionFrame.setSomething(null, getString(R.string.tip_forwarding_quit), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    onSaveContent();
                    MsgBroadcast.broadcastMsgUiUpdate(mContext);
                    finish();
                }
            });
            selectionFrame.show();
        } else {
            onSaveContent();
            MsgBroadcast.broadcastMsgUiUpdate(mContext);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (!JVCideoPlayerStandardforchat.handlerBack()) {
            doBack();
        }
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 给新入群的小伙伴们发通知
     */
  /*  private void sendNoticeJoinNewFriend() {
        if (noticeFriendList != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendNotice(InternationalizationHelper.getString("NEW_FRIEND_CHAT"));
                    // 防止重复发送提示消息
                    noticeFriendList = null;
                }
            }, 1000);
        }
    }*/
    private void sendNotice(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TIP);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        // 获取[草稿]
        String draft = PreferenceUtils.getString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, "");
        if (!TextUtils.isEmpty(draft)) {
            String s = StringUtils.replaceSpecialChar(draft);
            CharSequence content = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
            if (draft.contains("@")) {
                // 防止SelectRoomMemberPopupWindow还未初始化的时候被调用
                mChatBottomView.getmChatEdit().setText(content + ",");
            } else {
                mChatBottomView.getmChatEdit().setText(content);
            }
            softKeyboardControl(true, 200);
        }
        // 记录当前聊天对象的id
        MyApplication.IsRingId = mFriend.getUserId();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (TextUtils.isEmpty(mChatBottomView.getmChatEdit().getText().toString())) {// 清空草稿，以防消息发送出去后，通过onPause--onResume的方式给输入框赋值
            PreferenceUtils.putString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, "");
        }
        // 将当前聊天对象id重置
        MyApplication.IsRingId = "Empty";
        VoicePlayer.instance().stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFriendNull) {
            return;
        }
        JCVideoPlayer.releaseAllVideos();
        if (mChatBottomView != null) {
            mChatBottomView.recordCancel();
        }
        ListenerManager.getInstance().removeChatMessageListener(this);
        ListenerManager.getInstance().removeMucListener(this);
        EventBus.getDefault().unregister(this);
        unregisterReceiver(broadcastReceiver);
    }

    /***************************************
     * ChatContentView的回调
     ***************************************/
    @Override
    public void onMyAvatarClick() {
        mChatBottomView.reset();
        mChatBottomView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!UiUtils.isNormalClick()){
                    return;
                }
                Intent intent = new Intent(mContext, BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
                startActivity(intent);
            }
        }, 100);
    }

    @Override
    public void onFriendAvatarClick(final String friendUserId) {
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        if (isAllowSecretlyChat || isOk()) {
            mChatBottomView.reset();
            mChatBottomView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!UiUtils.isNormalClick()){
                        return;
                    }
                    Intent intent = new Intent(mContext, BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, friendUserId);
                    startActivity(intent);
                }
            }, 100);
        } else {
            tip(getString(R.string.tip_member_disable_privately_chat));
        }
    }

    // 长按头像@群成员
    @Override
    public void LongAvatarClick(ChatMessage chatMessage) {
        if (chatMessage.getFromUserId().equals(mLoginUserId)) {// @自己不处理
            return;
        }
        String editContent = mChatBottomView.getmChatEdit().getText().toString();
        editContent += "@" + chatMessage.getFromUserName() + " ";
        atUserId.add(chatMessage.getFromUserId());
        SpannableString spannableString = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"),
                editContent, editContent);
        mChatBottomView.getmChatEdit().setText(spannableString);
    }

    @Override
    public void onNickNameClick(String friendUserId) {

    }

    @Override
    public void onMessageClick(ChatMessage chatMessage) {

    }

    @Override
    public void onMessageLongClick(ChatMessage chatMessage) {

    }

    @Override
    public void onEmptyTouch() {
        mChatBottomView.reset();
    }

    @Override
    public void onTipMessageClick(ChatMessage message) {
        if(!UiUtils.isNormalClick()){
            return;
        }
        if (!TextUtils.isEmpty(message.getObjectId())
                && message.getObjectId().contains("userIds")
                && message.getObjectId().contains("userNames")
                && message.getObjectId().contains("isInvite")) {
            // 符合以上的条件，我们可以判断该条消息为群组邀请确认消息，跳转至邀请详情界面
            Intent intent = new Intent(MucChatActivity.this, InviteVerifyActivity.class);
            intent.putExtra("VERIFY_MESSAGE_FRIEND_ID", mUseId);
            intent.putExtra("VERIFY_MESSAGE_PACKET", message.getPacketId());
            intent.putExtra("VERIFY_MESSAGE_ROOM_ID", mFriend.getRoomId());
            startActivityForResult(intent, REQUEST_CODE_INVITE);
        }
    }

    @Override
    public void onSendAgain(ChatMessage message) {
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                // 将需要上传的消息状态置为发送中，防止在上传的时候退出当前界面，回来后[还未上传成功]读取数据库又变为了感叹号
                ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mFriend.getUserId(),
                        message.get_id(), ChatMessageListener.MESSAGE_SEND_ING);
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mUseId, message, mUploadResponse);
            } else {
                send(message);
            }
        } else {
            send(message);
        }
    }

    public void deleteMessage(String msgIdListStr) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", msgIdListStr);
        params.put("delete", "1");  // 1单方删除 2-双方删除
        params.put("type", "2");    // 1单聊记录 2-群聊记录

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_CHATMESSAGE)
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

    /**
     * 消息撤回
     */
    @Override
    public void onMessageBack(final ChatMessage chatMessage, final int position) {
        DialogHelper.showMessageProgressDialog(MucChatActivity.this, InternationalizationHelper.getString("MESSAGE_REVOCATION"));
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", chatMessage.getPacketId());
        params.put("roomJid", mUseId);
        params.put("type", "2");
        params.put("delete", "2");

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_CHATMESSAGE)
                .params(params)
                .build()
                .execute(new ListCallback<StructBeanNetInfo>(StructBeanNetInfo.class) {
                    @Override
                    public void onResponse(ArrayResult<StructBeanNetInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (chatMessage.getType() == XmppMessage.TYPE_VOICE) {// 撤回的为语音消息，停止播放
                            if (VoicePlayer.instance().getVoiceMsgId().equals(chatMessage.getPacketId())) {
                                VoicePlayer.instance().stop();
                            }
                        } else if (chatMessage.getType() == XmppMessage.TYPE_VIDEO) {
                            JCVideoPlayer.releaseAllVideos();
                        }
                        // 发送撤回消息
                        ChatMessage message = new ChatMessage();
                        message.setType(XmppMessage.TYPE_BACK);
                        message.setFromUserId(mLoginUserId);
                        message.setFromUserName(mLoginNickName);
                        if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
                            message.setFromUserName(mFriend.getRoomMyNickName());
                        }
                        message.setToUserId(mUseId);
                        message.setContent(chatMessage.getPacketId());
                        message.setTimeSend(TimeUtils.sk_time_current_time());
                        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                        coreManager.sendMucChatMessage(mUseId, message);
                        ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, mFriend.getUserId(), chatMessage.getPacketId(), getString(R.string.you));
                        mChatMessages.get(position).setType(XmppMessage.TYPE_TIP);
                        mChatMessages.get(position).setContent(InternationalizationHelper.getString("JX_AlreadyWithdraw"));
                        mChatContentView.notifyDataSetInvalidated(false);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MucChatActivity.this);
                    }
                });
    }

    @Override
    public void onCallListener(int type) {

    }

    /***************************************
     * ChatBottomView的回调
     ***************************************/

    private void softKeyboardControl(boolean isShow, long delayMillis) {
        // 软键盘消失
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (isShow) {
            mChatBottomView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mChatBottomView.getmChatEdit().requestFocus();
                    mChatBottomView.getmChatEdit().setSelection(mChatBottomView.getmChatEdit().getText().toString().length());
                    imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
                }
            }, delayMillis);
        } else {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void send(ChatMessage message) {
        coreManager.sendMucChatMessage(mUseId, message);
    }

    private void sendMessage(ChatMessage message) {
        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), mLoginUserId);
        if (member != null && member.getRole() == 3) {// 普通成员需要判断是否被禁言
            if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("HAS_BEEN_BANNED"));
                mChatMessages.remove(message);
                mChatContentView.notifyDataSetInvalidated(true);
                return;
            }
        } else if (member == null) {// 也需要判断是否被禁言
            if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("HAS_BEEN_BANNED"));
                mChatMessages.remove(message);
                mChatContentView.notifyDataSetInvalidated(true);
                return;
            }
        }

        message.setToUserId(mUseId);
        if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
            message.setFromUserName(mFriend.getRoomMyNickName());
        }

        if (mFriend.getChatRecordTimeOut() == -1 || mFriend.getChatRecordTimeOut() == 0) {// 永久
            message.setDeleteTime(-1);
        } else {
            long deleteTime = TimeUtils.sk_time_current_time() + (long) (mFriend.getChatRecordTimeOut() * 24 * 60 * 60);
            message.setDeleteTime(deleteTime);
        }

        // 加密
        boolean isEncrypt = PreferenceUtils.getBoolean(this, Constants.IS_ENCRYPT + mLoginUserId, false);
        if (isEncrypt) {
            message.setIsEncrypt(1);
        } else {
            message.setIsEncrypt(0);
        }

        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));

        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mUseId, message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mUseId, message, mUploadResponse);
            } else {
                send(message);
            }
        } else {
            send(message);
        }
    }

    @Override
    public void stopVoicePlay() {
        VoicePlayer.instance().stop();
    }

    // 展示@界面
    @Override
    public void sendAt() {
        List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);
        if (mRoomMember != null && roomMember.size() > 0) {
            // 移除掉自己
            for (int i = 0; i < roomMember.size(); i++) {
                if (roomMember.get(i).getUserId().equals(mLoginUserId)) {
                    roomMember.remove(roomMember.get(i));
                }
            }
            mSelectRoomMemberPopupWindow = new SelectRoomMemberPopupWindow(MucChatActivity.this, this, roomMember, mRoomMember.getRole());
            mSelectRoomMemberPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
            mSelectRoomMemberPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    softKeyboardControl(true, 200);
                }
            });
        } else {
            loadMembers(roomId, true);
        }
    }

    // 选择了@群成员的回调
    @Override
    public void sendAtContent(RoomMember member) {
        String atS = mChatBottomView.getmChatEdit().getText().toString();
        atS += member.getUserName() + " ";
        atUserId.add(member.getUserId());
        if (atS.contains("@全体成员")) {
            atUserId.clear();
            atS = "@" + member.getUserName() + " ";
            atUserId.add(member.getUserId());
        }
        SpannableString atString = StringUtils.matcherSearchTitle(Color.parseColor("#63B8FF"),
                atS, atS);
        mChatBottomView.getmChatEdit().setText(atString);
    }

    // 选择了@全体成员的回调
    @Override
    public void sendEveryOne(String everyOne) {
        SpannableString atString = StringUtils.matcherSearchTitle(Color.parseColor("#63B8FF"),
                everyOne, everyOne);
        mChatBottomView.getmChatEdit().setText(atString);
    }

    // 发送@消息
    @Override
    public void sendAtMessage(String text) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        // 空格隔开
        //[10008295 10009232...]
        String at = "";
        if (text.contains("@全体成员")) {
            // roomJid
            at = mUseId;
        } else {
            for (int i = 0; i < atUserId.size(); i++) {
                if (i == atUserId.size() - 1) {
                    at += atUserId.get(i);
                } else {
                    at += atUserId.get(i) + " ";
                }
            }
        }
        message.setObjectId(at);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
        atUserId.clear();
    }

    @Override
    public void sendText(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(text);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
        // 遍历消息集合，查询红包类型消息
        for (ChatMessage msg : mChatMessages) {
            if (msg.getType() == XmppMessage.TYPE_RED// 红包
                    && StringUtils.strEquals(msg.getFilePath(), "3")// 口令红包
                    && text.equalsIgnoreCase(msg.getContent())// 发送的文本与口令一致
                    && msg.getFileSize() == 1) // 可以领取的状态
            {
                //打开红包
                openRedPacket(msg);
                // 表示已经领取过了一次,不可再领取
                msg.setFileSize(0);
                // 更新本地数据库
                ChatMessageDao.getInstance().updateMessageFilesize(mLoginUserId, mFriend.getUserId()
                        , msg.getPacketId(), 0);
            }
        }
    }

    /**
     * 打开红包
     */
    public void openRedPacket(final ChatMessage message) {
        HashMap<String, String> params = new HashMap<String, String>();
        String redId = message.getObjectId();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("id", redId);

        HttpUtils.get().url(coreManager.getConfig().REDPACKET_OPEN)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (result.getData() != null) {
                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 1);
                            bundle.putInt("timeOut", 0);

                            bundle.putBoolean("isGroup", true);
                            bundle.putString("mToUserId", mFriend.getUserId());
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                            // 更新余额
                            coreManager.updateMyBalance();
                        } else {
                            Toast.makeText(MucChatActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    @Override
    public void sendGif(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendCollection(String collection) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent(collection);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setUpload(true);// 自定义表情，不需要上传
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendVoice(String filePath, int timeLen) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VOICE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendImage(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        //TODO message添加图片的高跟宽  设置在经纬度中
        int[] imageParam = FileDataHelper.getImageParamByIntsFile(filePath);
        message.setLocation_x(String.valueOf(imageParam[0]));
        message.setLocation_y(String.valueOf(imageParam[1]));
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendVideo(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendFile(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_FILE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendLocate(double latitude, double longitude, String address, String snapshot) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setContent("");
        message.setFilePath(snapshot);
        message.setObjectId(address);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    /**
     * 得到选中的名片
     */
    @Override
    public void sendCardS(List<Friend> friends) {
        for (int i = 0; i < friends.size(); i++) {
            sendCard(friends.get(i));
        }
    }

    public void sendCard(Friend friend) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setObjectId(friend.getUserId());
        message.setContent(friend.getNickName());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

//        @Override
//        public void clickPwdRed(String str) {
//            mChatBottomView.getmChatEdit().setText(str);
//        }

    /**
     * 发送红包方法
     *
     * @param type  类型(口令、普通、拼手气)
     * @param money 金额
     * @param count 数量
     * @param words 祝福语(或者口令)
     */
    public void sendRed(String type, String money, String count, String words, String payPassword) {
        /**
         * 步骤
         * 1.调发红包的接口，发送一个红包
         * 2.吧消息发送出去
         */
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("roomJid", mUseId);
        Log.e("TAG", "params"+params);
        HttpUtils.get().url(coreManager.getConfig().REDPACKET_SEND)
                .params(params)
                .addSecret(payPassword, money)
                .build()
                .execute(new BaseCallback<RedPacket>(RedPacket.class) {
                    @Override
                    public void onResponse(ObjectResult<RedPacket> result) {
                        RedPacket redPacket = result.getData();
                        if (result.getResultCode() != 1) {
                            // 发送红包失败，
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            String objectId = redPacket.getId();
                            ChatMessage message = new ChatMessage();
                            message.setType(XmppMessage.TYPE_RED);
                            message.setFromUserName(mLoginNickName);
                            message.setFromUserId(mLoginUserId);
                            message.setTimeSend(TimeUtils.sk_time_current_time());
                            message.setContent(redPacket.getGreetings()); // 祝福语
                            message.setObjectId(objectId); // 红包id
                            message.setFilePath(redPacket.getType() + "");// 用FilePath来储存红包类型
                            // 群组发送普通红包
                            message.setFileSize(redPacket.getStatus());   // 用filesize来储存红包状态
                            mChatMessages.add(message);
                            mChatContentView.notifyDataSetInvalidated(true);
                            sendMessage(message);
                            // 更新余额
                            CoreManager.updateMyBalance();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    @Override
    public void clickPhoto() {
        if(!UiUtils.isNormalClick()){
            return;
        }
        // 将其置为true
        /*MyApplication.GalleyNotBackGround = true;
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);*/
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(MucChatActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        // 已选中的照片地址， 用于回显选中状态
        intent.setSelectedPaths(imagePaths);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickCamera() {
        if(!UiUtils.isNormalClick()){
            return;
        }
       /* mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);*/
       /* Intent intent = new Intent(this, EasyCameraActivity.class);
        startActivity(intent);*/
        mChatBottomView.reset();
        Intent intent = new Intent(this, MediaRecordActivity.class);
        startActivity(intent);
    }

    @Override
    public void clickStartRecord() {
        // 现拍照录像ui和二为一，统一在clickCamera内处理
       /* Intent intent = new Intent(this, VideoRecorderActivity.class);
        startActivity(intent);*/
    }

    @Override
    public void clickLocalVideo() {
        // 现拍照录像ui和二为一，统一在clickCamera内处理
       /* Intent intent = new Intent(this, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);*/
    }

    @Override
    public void clickAudio() {

    }

    @Override
    public void clickVideoChat() {

    }

    @Override
    public void clickFile() {
        if (allowUploadFile) {
            SelectFileDialog dialog = new SelectFileDialog(this, new SelectFileDialog.OptionFileListener() {
                @Override
                public void option(List<File> files) {
                    if (files != null && files.size() > 0) {
                        for (int i = 0; i < files.size(); i++) {
                            sendFile(files.get(i));
                        }
                    }
                }

                @Override
                public void intent() {
                    if (!UiUtils.isNormalClick()){
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
                }
            });
            dialog.show();
        } else {
            tip(getString(R.string.tip_cannot_upload));
        }
    }

    @Override
    public void clickLocation() {
        if(!UiUtils.isNormalClick()){
            return;
        }
        Intent intent = new Intent(this, MapPickerActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_Locate);
    }

    @Override
    public void clickCard() {
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        if (isAllowSecretlyChat || isOk()) {
            mSelectCardPopupWindow = new SelectCardPopupWindow(MucChatActivity.this, this);
            mSelectCardPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
        } else {
            tip(getString(R.string.tip_card_disable_privately_chat));
        }
    }

    @Override
    public void clickRedpacket() {
        if(!UiUtils.isNormalClick()){
            return;
        }
        Intent intent = new Intent(this, MucSendRedPacketActivity.class);
        startActivityForResult(intent, ChatActivity.REQUEST_CODE_SEND_RED);
    }

    @Override
    public void clickTransferMoney() {
        // 群组暂不支持转账
    }

    @Override
    public void clickCollection() {
        if(!UiUtils.isNormalClick()){
            return;
        }
        Intent intent = new Intent(this, MyCollection.class);
        intent.putExtra("IS_SEND_COLLECTION", true);
        startActivityForResult(intent, REQUEST_CODE_SEND_COLLECTION);
    }

    private void clickCollectionSend(
            int type,
            String content,
            int timeLen,
            String filePath,
            long fileSize
    ) {
        if (isAuthenticated()) {
            return;
        }

        if (TextUtils.isEmpty(content)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(type);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(content);
        message.setTimeLen(timeLen);
        message.setFileSize((int) fileSize);
        message.setUpload(true);
        if (!TextUtils.isEmpty(filePath)) {
            message.setFilePath(filePath);
        }
        message.setIsReadDel(0);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    private void clickCollectionSend(CollectionEvery collection) {
        // 不管什么收藏消息类型，都可能有文字，单独发一条文字消息，
        if (!TextUtils.isEmpty(collection.getCollectContent())) {
            sendText(collection.getCollectContent());
        }
        int type = collection.getXmppType();
        if (type == XmppMessage.TYPE_TEXT) {
            // 文字消息发出了文字就可以结束了，
            return;
        } else if (type == XmppMessage.TYPE_IMAGE) {
            // 图片可能有多张，分开发送，
            String allUrl = collection.getUrl();
            for (String url : allUrl.split(",")) {
                clickCollectionSend(type, url, collection.getFileLength(), collection.getFileName(), collection.getFileSize());
            }
            return;
        }
        clickCollectionSend(type, collection.getUrl(), collection.getFileLength(), collection.getFileName(), collection.getFileSize());
    }

    @Override
    public void clickShake() {

    }

    @Override
    public void onInputState() {

    }

    /**
     * 复制自com.sk.weichat.ui.me.LocalVideoActivity#helloEventBus(com.sk.weichat.adapter.MessageVideoFile)
     * 主要是CameraDemoActivity录制结束不走activity result, 而是发EventBus,
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUploadFileRate message) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            if (mChatMessages.get(i).getPacketId().equals(message.getPacketId())) {
                mChatMessages.get(i).setUploadSchedule(message.getRate());
                mChatMessages.get(i).setUpload(message.getRate() == 100);
                mChatContentView.notifyDataSetChanged();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageVideoFile message) {
        VideoFile videoFile = new VideoFile();
        videoFile.setCreateTime(TimeUtils.f_long_2_str(System.currentTimeMillis()));
        videoFile.setFileLength(message.timelen);
        videoFile.setFileSize(message.length);
        videoFile.setFilePath(message.path);
        videoFile.setOwnerId(coreManager.getSelf().getUserId());
        VideoFileDao.getInstance().addVideoFile(videoFile);
        String filePath = message.path;
        if (TextUtils.isEmpty(filePath)) {
            ToastUtil.showToast(this, R.string.record_failed);
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            ToastUtil.showToast(this, R.string.record_failed);
            return;
        }
        sendVideo(file);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageLocalVideoFile message) {
        sendVideo(message.file);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_FILE: // 系统管理器返回文件
                    String file_path = FileUtils.getPath(MucChatActivity.this, data.getData());
                    Log.e("xuan", "conversionFile: " + file_path);
                    if (file_path == null) {
                        ToastUtil.showToast(mContext, R.string.tip_file_not_supported);
                    } else {
                        sendFile(new File(file_path));
                    }
                    break;
                case REQUEST_CODE_CAPTURE_PHOTO:
                    // 拍照返回
                    if (mNewPhotoUri != null) {
                        // 先将图片进行压缩，在上传
                        photograph(new File(mNewPhotoUri.getPath()));
                    }
                    break;
                case REQUEST_CODE_PICK_PHOTO:
                    if (data != null) {
                        boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                        album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                    } else {
                        ToastUtil.showToast(this, R.string.c_photo_album_failed);
                    }
                    break;
                case REQUEST_CODE_SELECT_VIDEO: {
                    // 选择视频的返回
                    if (data == null) {
                        return;
                    }
                    String json = data.getStringExtra(AppConstant.EXTRA_VIDEO_LIST);
                    List<VideoFile> fileList = JSON.parseArray(json, VideoFile.class);
                    if (fileList == null || fileList.size() == 0) {
                        // 不可到达，列表里有做判断，
                        Reporter.unreachable();
                    } else {
                        for (VideoFile videoFile : fileList) {
                            String filePath = videoFile.getFilePath();
                            if (TextUtils.isEmpty(filePath)) {
                                // 不可到达，列表里有做过滤，
                                Reporter.unreachable();
                            } else {
                                File file = new File(filePath);
                                if (!file.exists()) {
                                    // 不可到达，列表里有做过滤，
                                    Reporter.unreachable();
                                } else {
                                    sendVideo(file);
                                }
                            }
                        }
                    }
                    break;
                }
                case REQUEST_CODE_SELECT_Locate:
                    double latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
                    double longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
                    String address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
                    String snapshot = data.getStringExtra(AppConstant.EXTRA_SNAPSHOT);

                    if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)
                            && !TextUtils.isEmpty(snapshot)) {
                        sendLocate(latitude, longitude, address, snapshot);
                    } else {
                        ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXServer_CannotLocation"));
                    }
                    break;
                case REQUEST_CODE_SEND_COLLECTION: {
                    String json = data.getStringExtra("data");
                    CollectionEvery collection = JSON.parseObject(json, CollectionEvery.class);
                    clickCollectionSend(collection);
                    break;
                }
                case REQUEST_CODE_QUICK_SEND:
                    String image = QuickSendPreviewActivity.parseResult(data);
                    sendImage(new File(image));
                    break;
                case REQUEST_CODE_INVITE:
                    if (data != null && data.getExtras() != null) {
                        // 直接刷新消息列表，
                        mChatMessages.clear();
                        loadDatas(false);
                    }
                    break;
            }
        } else {
            switch (requestCode) {
                case ChatActivity.REQUEST_CODE_SEND_RED:
                    if (data != null && data.getExtras() != null) {
                        Bundle bundle = data.getExtras();
                        sendRed(bundle.getString("type")
                                , bundle.getString("money")
                                , bundle.getString("count")
                                , bundle.getString("words")
                                , bundle.getString("payPassword")
                        );
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    // 单张图片压缩 拍照
    private void photograph(final File file) {
        Log.e("zq", "压缩前图片路径:" + file.getPath() + "压缩前图片大小:" + file.length() / 1024 + "KB");
        // 拍照出来的图片Luban一定支持，
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // 原图小于100kb 不压缩
                // .putGear(2)     // 设定压缩档次，默认三挡
                // .setTargetDir() // 指定压缩后的图片路径
                .setCompressListener(new OnCompressListener() { // 设置回调
                    @Override
                    public void onStart() {
                        Log.e("zq", "开始压缩");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.e("zq", "压缩成功，压缩后图片位置:" + file.getPath() + "压缩后图片大小:" + file.length() / 1024 + "KB");
                        sendImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("zq", "压缩失败,原图上传");
                        sendImage(file);
                    }
                }).launch();// 启动压缩
    }

    // 多张图片压缩 相册
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// 原图发送，不压缩
            Log.e("zq", "原图发送，不压缩，开始发送");
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                sendImage(new File(stringArrayListExtra.get(i)));
            }
            Log.e("zq", "原图发送，不压缩，发送结束");
            return;
        }

        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            // gif动图不压缩，
            if (stringArrayListExtra.get(i).endsWith("gif")) {
                fileList.add(new File(stringArrayListExtra.get(i)));
                stringArrayListExtra.remove(i);
            } else {
                // Luban只处理特定后缀的图片，不满足的不处理也不走回调，
                // 只能挑出来不压缩，
                List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
                boolean support = false;
                for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                    if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                        support = true;
                        break;
                    }
                }
                if (!support) {
                    fileList.add(new File(stringArrayListExtra.get(i)));
                    stringArrayListExtra.remove(i);
                }
            }
        }

        if (fileList.size() > 0) {
            for (File file : fileList) {// 不压缩的部分，直接发送
                sendImage(file);
            }
        }

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// 原图小于100kb 不压缩
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        sendImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// 启动压缩
    }

    /**********************
     * MUC Message Listener
     ********************/
    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {
        if (TextUtils.isEmpty(msgId)) {
            return;
        }
        for (int i = 0; i < mChatMessages.size(); i++) {
            ChatMessage msg = mChatMessages.get(i);
            if (msgId.equals(msg.getPacketId())) {
                msg.setMessageState(messageState);
                mChatContentView.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) { // 新消息到来
        /**
         *  因为重发机制，当对方处于弱网时，不能及时接收我方的消息回执而给我方发送了两条甚至多条一样的消息
         *  而我方则会收到两条甚至多条一样的消息存入数据库(数据库已去重)，如果我正好处于消息发送方的聊天界面
         *  则会回调多次onNewMessage方法，而该方法内又没做去重，所以会出现显示两条一模一样的消息，退出当前界面在进入
         *  该界面又只有一条的问题
         *
         */
        if (mChatMessages.size() > 0) {
           /* if (mChatMessages.get(mChatMessages.size() - 1).getPacketId().equals(message.getPacketId())) {// 最后一条消息的msgId==新消息的msgId
                Log.e("zq", "收到一条重复消息");
                return false;
            }*/
            for (int i = 0; i < mChatMessages.size(); i++) {// 群组控制消息可能一下子来几条
                if (mChatMessages.get(i).getPacketId().equals(message.getPacketId())) {
                    return false;
                }
            }
        }

        if (isGroupMsg != isGroupChat) {
            return false;
        }

        if (mUseId.compareToIgnoreCase(fromUserId) == 0) {// 是该人的聊天消息
            mChatMessages.add(message);
            if (mChatContentView.shouldScrollToBottom()) {
                mChatContentView.notifyDataSetInvalidated(true);
            } else {
                mChatContentView.notifyDataSetChanged();
            }

            return true;
        }
        return false;
    }

    /**********************
     * MUC Operation Listener
     ********************/
    @Override
    public void onMyBeDelete(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {// 当前群组
            Toast.makeText(this, R.string.tip_been_kick_self, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onDeleteMucRoom(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {// 当前群组
            Toast.makeText(this, R.string.tip_group_been_disbanded, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onNickNameChange(String toUserId, String changedUserId, String changedName) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            // 群名已改变
            if (changedUserId.equals("ROOMNAMECHANGE")) {
                mFriend.setNickName(changedName);
                // 群组人数
                List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);
                updateMemberCount(roomMember.size());
                return;
            }
            // 群内成员名改变
            if (changedUserId.equals(mLoginUserId)) {// 自己改变需要做些操作
                mFriend.setRoomMyNickName(changedName);
                mChatContentView.setCurGroup(true, changedName);
            }
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (TextUtils.equals(mChatMessages.get(i).getFromUserId(), changedUserId)) {
                    mChatMessages.get(i).setFromUserName(changedName);
                }
            }
            mChatContentView.notifyDataSetChanged();
        }
    }

    /*******************************************
     * 接收到EventBus后的后续操作
     ******************************************/

    @Override
    public void onMyVoiceBanned(String toUserId, int time) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            mFriend.setRoomTalkTime(time);
        }
    }

    /**
     * 收到新公告，
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNewNotice message) {
        setLastNotice(message.getText());
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventXMPPJoinGroupFailed message) {
        if (message.roomJId.equals(mFriend.getUserId())) {
            DialogHelper.tip(MucChatActivity.this, "加入群组失败，暂时无法收发此群组的消息，可尝试退出当前界面重进或关闭app重进");
        }
    }

    // 发送多选消息
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventMoreSelected message) {
        List<ChatMessage> mSelectedMessageList = new ArrayList<>();
        if (message.getToUserId().equals("MoreSelectedCollection") || message.getToUserId().equals("MoreSelectedEmail")) {// 多选 收藏 || 保存
            moreSelected(false, 0);
            return;
        }
        if (message.getToUserId().equals("MoreSelectedDelete")) {// 多选 删除
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (mChatMessages.get(i).isMoreSelected) {
                    if (ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mUseId, mChatMessages.get(i))) {
                        Log.e("more_selected", "删除成功");
                    } else {
                        Log.e("more_selected", "删除失败");
                    }
                    mSelectedMessageList.add(mChatMessages.get(i));
                }
            }

            String mMsgIdListStr = "";
            for (int i = 0; i < mSelectedMessageList.size(); i++) {
                if (i == mSelectedMessageList.size() - 1) {
                    mMsgIdListStr += mSelectedMessageList.get(i).getPacketId();
                } else {
                    mMsgIdListStr += mSelectedMessageList.get(i).getPacketId() + ",";
                }
            }
            deleteMessage(mMsgIdListStr);// 服务端也需要删除

            mChatMessages.removeAll(mSelectedMessageList);
        } else {// 多选 转发
            if (message.isSingleOrMerge()) {// 合并转发
                List<String> mStringHistory = new ArrayList<>();
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).isMoreSelected) {
                        String body = mChatMessages.get(i).toJsonString();
                        mStringHistory.add(body);
                    }
                }
                String detail = JSON.toJSONString(mStringHistory);
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType(XmppMessage.TYPE_CHAT_HISTORY);
                chatMessage.setFromUserId(mLoginUserId);
                chatMessage.setFromUserName(mLoginNickName);
                chatMessage.setToUserId(message.getToUserId());
                chatMessage.setContent(detail);
                chatMessage.setMySend(true);
                chatMessage.setReSendCount(0);
                chatMessage.setSendRead(false);
                chatMessage.setIsEncrypt(0);
                chatMessage.setIsReadDel(0);
                chatMessage.setObjectId(getString(R.string.group_chat_history));
                chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.getToUserId(), chatMessage);
                if (message.isGroupMsg()) {
                    coreManager.sendMucChatMessage(message.getToUserId(), chatMessage);
                } else {
                    coreManager.sendChatMessage(message.getToUserId(), chatMessage);
                }
                if (message.getToUserId().equals(mFriend.getUserId())) {// 转发给当前对象
                    mChatMessages.add(chatMessage);
                }
            } else {// 逐条转发
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).isMoreSelected) {
                        ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, mFriend.getUserId(), mChatMessages.get(i).getPacketId());
                        if (chatMessage.getType() == XmppMessage.TYPE_RED) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_red_packet));
                        } else if (chatMessage.getType() >= XmppMessage.TYPE_IS_CONNECT_VOICE
                                && chatMessage.getType() <= XmppMessage.TYPE_EXIT_VOICE) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_video_voice));
                        } else if (chatMessage.getType() == XmppMessage.TYPE_SHAKE) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_shake));
                        }
                        chatMessage.setFromUserId(mLoginUserId);
                        chatMessage.setFromUserName(mLoginNickName);
                        chatMessage.setToUserId(message.getToUserId());
                        chatMessage.setUpload(true);
                        chatMessage.setMySend(true);
                        chatMessage.setSendRead(false);
                        chatMessage.setIsEncrypt(0);
                        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                        mSelectedMessageList.add(chatMessage);
                    }
                }

                for (int i = 0; i < mSelectedMessageList.size(); i++) {
                    ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.getToUserId(), mSelectedMessageList.get(i));
                    if (message.isGroupMsg()) {
                        coreManager.sendMucChatMessage(message.getToUserId(), mSelectedMessageList.get(i));
                    } else {
                        coreManager.sendChatMessage(message.getToUserId(), mSelectedMessageList.get(i));
                    }

                    if (message.getToUserId().equals(mFriend.getUserId())) {// 转发给当前对象
                        mChatMessages.add(mSelectedMessageList.get(i));
                    }
                }
            }
        }
        moreSelected(false, 0);
    }

    public void moreSelected(boolean isShow, int position) {
        mChatBottomView.showMoreSelectMenu(isShow);
        if (isShow) {
            findViewById(R.id.iv_title_left).setVisibility(View.GONE);
            mTvTitleLeft.setVisibility(View.VISIBLE);
            mChatMessages.get(position).setMoreSelected(true);
        } else {
            findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
            mTvTitleLeft.setVisibility(View.GONE);
            for (int i = 0; i < mChatMessages.size(); i++) {
                mChatMessages.get(i).setMoreSelected(false);
            }
        }
        mChatContentView.setIsShowMoreSelect(isShow);
        mChatContentView.notifyDataSetChanged();
    }

    /**
     * 发送我的课程
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageUploadChatRecord message) {
        final CreateCourseDialog dialog = new CreateCourseDialog(MucChatActivity.this, new CreateCourseDialog.CoureseDialogConfirmListener() {
            @Override
            public void onClick(String content) {
                upLoadChatList(message.chatIds, content);
            }

        });
        dialog.show();
    }

    private void upLoadChatList(String chatIds, String name) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageIds", chatIds);
        params.put("userId", mLoginUserId);
        params.put("courseName", name);
        params.put("createTime", TimeUtils.sk_time_current_time() + "");
        params.put("roomJid", mUseId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_ADD_COURSE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(MucChatActivity.this, "课件创建成功");
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MucChatActivity.this);
                    }
                });
    }

    private void initRoomMember() {
        if (mFriend.getGroupStatus() == 0) {// 正常状态
            List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);
            if (roomMember.size() > 0) {
                updateMemberCount(roomMember.size());
                mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(roomId, mLoginUserId);
                if (mRoomMember == null) {// 更新群成员表
                    loadMembers(mFriend.getRoomId(), false);
                } else {
                    // 成员列表传进去为了显示管理员的头像，
                    mChatContentView.setRoomMemberList(roomMember);
                    updateBannedStatus();
                    mChatContentView.setRole(mRoomMember.getRole());
                }
            } else {
                loadMembers(roomId, false);
            }
        }
    }

    /*******************************************
     * 初始化ActionBar与其点击事件
     ******************************************/

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBack();
            }
        });

        mTvTitleLeft = (TextView) findViewById(R.id.tv_title_left);
        mTvTitleLeft.setVisibility(View.GONE);
        mTvTitleLeft.setText(getString(R.string.cancel));
        mTvTitleLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreSelected(false, 0);
            }
        });
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        if (!TextUtils.isEmpty(mNickName)) {
            mTvTitle.setText(mNickName);
        }
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.chat_more);
        ivRight.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                if (mFriend.getGroupStatus() == 0) {
                    mChatBottomView.reset();
                    mChatBottomView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(!UiUtils.isNormalClick()){
                                return;
                            }
                            Log.e("TAG", "RoomInfoActivity   ............");
                            // 进入房间信息的Activity
                            Intent intent = new Intent(MucChatActivity.this, RoomInfoActivity.class);
                            intent.putExtra(AppConstant.EXTRA_USER_ID, mUseId);
                            intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
                            startActivity(intent);
                        }
                    }, 1);
                }
            }
        });
    }

    public void synchronizeChatHistory() {
        long startTime;
        String chatSyncTimeLen = PreferenceUtils.getString(MucChatActivity.this, Constants.CHAT_SYNC_TIME_LEN + mLoginUserId, "1");
        if (Double.parseDouble(chatSyncTimeLen) == -2) {// 不同步
            return;
        }
        if (Double.parseDouble(chatSyncTimeLen) == -1 || Double.parseDouble(chatSyncTimeLen) == 0) {// 同步 永久
            startTime = 0;
        } else {
            long syncTimeLen = (long) (Double.parseDouble(chatSyncTimeLen) * 24 * 60 * 60);// 得到消息同步时长
            startTime = TimeUtils.sk_time_current_time() - syncTimeLen;
        }

        Map<String, String> params = new HashMap();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mFriend.getUserId());
        params.put("startTime", String.valueOf(startTime * 1000));
        params.put("endTime", String.valueOf(System.currentTimeMillis()));
        params.put("pageSize", String.valueOf(100));

        HttpUtils.get().url(coreManager.getConfig().GET_CHAT_MSG_MUC)
                .params(params)
                .build()
                .execute(new ListCallback<ChatRecord>(ChatRecord.class) {
                    @Override
                    public void onResponse(ArrayResult<ChatRecord> result) {
                        final List<ChatRecord> chatRecordList = result.getData();
                        if (chatRecordList != null && chatRecordList.size() > 0) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    chatMessages = new ArrayList<>();

                                    for (int i = 0; i < chatRecordList.size(); i++) {
                                        ChatRecord data = chatRecordList.get(i);
                                        String messageBody = data.getBody();
                                        messageBody = messageBody.replaceAll("&quot;", "\"");
                                        ChatMessage chatMessage = new ChatMessage(messageBody);

                                        if (!TextUtils.isEmpty(chatMessage.getFromUserId()) &&
                                                chatMessage.getFromUserId().equals(mLoginUserId)) {
                                            chatMessage.setMySend(true);
                                        }

                                        chatMessage.setSendRead(true); // 漫游的默认已读
                                        // 漫游的默认已上传
                                        chatMessage.setUpload(true);
                                        chatMessage.setUploadSchedule(100);
                                        chatMessage.setMessageState(MESSAGE_SEND_SUCCESS);

                                        if (TextUtils.isEmpty(chatMessage.getPacketId())) {
                                            if (!TextUtils.isEmpty(data.getMessageId())) {
                                                chatMessage.setPacketId(data.getMessageId());
                                            } else {
                                                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                            }
                                        }
                                        int type = chatMessage.getType();
                                        if (type < 100) {
                                            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage)) {
                                                chatMessages.add(chatMessage);
                                            }
                                        }
                                    }

                                    mTvTitle.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                                                mChatMessages.add(chatMessages.get(i));
                                            }
                                            mChatContentView.headerRefreshingCompleted();
                                            mChatContentView.notifyDataSetInvalidated(chatMessages.size());
                                        }
                                    });
                                }
                            }).start();
                        } else {// 无历史聊天记录
                            mChatContentView.headerRefreshingCompleted();
                            mChatContentView.setNeedRefresh(false);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mChatContentView.headerRefreshingCompleted();
                        ToastUtil.showErrorData(MucChatActivity.this);
                    }
                });
    }

    // 本地已查询不出数据，服务端获取漫游聊天记录
    public void getNetSingle() {
        Map<String, String> params = new HashMap<>();

        String startTime = "1262275200000";
        String endTime;
        if (mChatMessages != null && mChatMessages.size() > 0) {
            endTime = String.valueOf(mChatMessages.get(0).getTimeSend() * 1000);
        } else {
            endTime = String.valueOf(System.currentTimeMillis());
        }

        final MsgRoamTask mLastMsgRoamTask = MsgRoamTaskDao.getInstance().getFriendLastMsgRoamTask(mLoginUserId, mFriend.getUserId());
        if (mLastMsgRoamTask != null) {// 该群组存在任务，为startTime与endTime重新赋值
            startTime = String.valueOf(mLastMsgRoamTask.getStartTime() * 1000);
            endTime = String.valueOf(mLastMsgRoamTask.getEndTime() * 1000);
        }

        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mUseId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("pageSize", String.valueOf(100));

        HttpUtils.get().url(coreManager.getConfig().GET_CHAT_MSG_MUC)
                .params(params)
                .build()
                .execute(new ListCallback<ChatRecord>(ChatRecord.class) {
                    @Override
                    public void onResponse(ArrayResult<ChatRecord> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<ChatRecord> chatRecordList = result.getData();
                            long mLastTaskNewEndTime = 0;
                            long currTime = TimeUtils.sk_time_current_time();

                            if (chatRecordList != null && chatRecordList.size() > 0) {
                                for (int i = 0; i < chatRecordList.size(); i++) {
                                    ChatRecord data = chatRecordList.get(i);
                                    String messageBody = data.getBody();
                                    messageBody = messageBody.replaceAll("&quot;", "\"");
                                    ChatMessage chatMessage = new ChatMessage(messageBody);

                                    // 有一种情况，因为服务器1个小时删除一次，所以可能会拉到已过期的时间
                                    if (chatMessage.getDeleteTime() > 1 && chatMessage.getDeleteTime() < currTime) {
                                        // 已过期的消息,扔掉
                                        continue;
                                    }


                                    mLastTaskNewEndTime = chatMessage.getTimeSend();

                                    if (!TextUtils.isEmpty(chatMessage.getFromUserId())
                                            && chatMessage.getFromUserId().equals(mLoginUserId)) {
                                        chatMessage.setMySend(true);
                                    }
                                    chatMessage.setSendRead(true);// 漫游的群聊消息，默认为已读
                                    // 漫游的默认已上传
                                    chatMessage.setUpload(true);
                                    chatMessage.setUploadSchedule(100);
                                    chatMessage.setMessageState(MESSAGE_SEND_SUCCESS);

                                    if (TextUtils.isEmpty(chatMessage.getPacketId())) {
                                        if (!TextUtils.isEmpty(data.getMessageId())) {
                                            chatMessage.setPacketId(data.getMessageId());
                                        } else {
                                            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                        }
                                    }
                                    int type = chatMessage.getType();
                                    if (type < 100) {
                                        ChatMessageDao.getInstance().saveRoamingChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage);
                                    }
                                }

                                mHasMoreData = chatRecordList.size() == 100;
                                // notifyChatAdapter();
                            } else {
                                mHasMoreData = false;
                                mChatContentView.headerRefreshingCompleted();
                                mChatContentView.setNeedRefresh(false);
                            }

                            if (mLastMsgRoamTask != null) {
                                mHasMoreData = true;// 任务不为空，必须支持继续下拉

                                if (chatRecordList != null && chatRecordList.size() == 100) {// 正常返回100条消息，该任务还未完成，更新最后一条任务的EndTime
                                    MsgRoamTaskDao.getInstance().updateMsgRoamTaskEndTime(mLoginUserId, mLastMsgRoamTask.getUserId(),
                                            mLastMsgRoamTask.getTaskId(), mLastTaskNewEndTime);
                                } else {// 该段任务已结束，可删除
                                    MsgRoamTaskDao.getInstance().deleteMsgRoamTask(mLoginUserId, mLastMsgRoamTask.getUserId(), mLastMsgRoamTask.getTaskId());
                                }
                            }

                            notifyChatAdapter();// 必须要放到updateMsgRoamTaskEndTime方法后面

                        } else {
                            ToastUtil.showErrorData(MucChatActivity.this);
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    private void notifyChatAdapter() {
        // 代码运行到这里说明之前一定没有查出消息，同步了漫游之后我们再次使用 mMinId 去查询一下数据
        if (mChatMessages.size() > 0) {
            mMinId = mChatMessages.get(0).getTimeSend();
        } else {
            mMinId = TimeUtils.sk_time_current_time();
        }
        List<ChatMessage> chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mLoginUserId,
                mFriend.getUserId(), mMinId, mPageSize);

        for (int i = 0; i < chatLists.size(); i++) {
            ChatMessage message = chatLists.get(i);
            mChatMessages.add(0, message);
        }
/*
        if (chatLists.size() == 0) {
            mHasMoreData = false;
            mChatContentView.headerRefreshingCompleted();
            mChatContentView.setNeedRefresh(false);
        }
*/
        mChatContentView.notifyDataSetInvalidated(chatLists.size());
        mChatContentView.headerRefreshingCompleted();
        if (!mHasMoreData) {
            mChatContentView.setNeedRefresh(false);
        }
    }

    /*******************************************
     * 获取音视频会议id && @群成员(当数据库内无该张群组表时，再去访问服务器)
     * 转发 && 消息加密 && 高亮显示@消息
     ******************************************/
    private void loadMembers(final String roomId, final boolean isAtAction) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();
                                     // 置顶展示最新公告，
                                     setLastNotice(mucRoom.getLastNotice());
                                     // 更新群成员人数，
                                     updateMemberCount(mucRoom.getUserSize());
                                     // 更新消息保存时长
                                     mFriend.setChatRecordTimeOut(mucRoom.getChatRecordTimeOut());
                                     FriendDao.getInstance().updateChatRecordTimeOut(mFriend.getUserId(), mucRoom.getChatRecordTimeOut());
                                     // 更新部分群属性
                                     MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(), mucRoom.getAllowSendCard(),
                                             mucRoom.getAllowConference(), mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime());

                                     List<RoomMember> roomMemberList = new ArrayList<>();
                                     for (int i = 0; i < mucRoom.getMembers().size(); i++) {
                                         RoomMember roomMember = new RoomMember();
                                         roomMember.setRoomId(mucRoom.getId());
                                         roomMember.setUserId(mucRoom.getMembers().get(i).getUserId());
                                         roomMember.setUserName(mucRoom.getMembers().get(i).getNickName());
                                         if (TextUtils.isEmpty(mucRoom.getMembers().get(i).getRemarkName())) {
                                             roomMember.setCardName(mucRoom.getMembers().get(i).getNickName());
                                         } else {
                                             roomMember.setCardName(mucRoom.getMembers().get(i).getRemarkName());
                                         }
                                         roomMember.setRole(mucRoom.getMembers().get(i).getRole());
                                         roomMember.setCreateTime(mucRoom.getMembers().get(i).getCreateTime());
                                         if (Objects.equals(roomMember.getUserId(), mLoginUserId)) {
                                             mRoomMember = roomMember;
                                             onRoleChanged(roomMember.getRole());
                                         }
                                         roomMemberList.add(roomMember);
                                     }

                                     AsyncUtils.doAsync(this, (AsyncUtils.Function<AsyncUtils.AsyncContext<BaseCallback<MucRoom>>>) baseCallbackAsyncContext -> {
                                         // 分表存储，一个群组一张表
                                         RoomMemberDao.getInstance().deleteRoomMemberTable(mucRoom.getId());
                                         for (int i = 0; i < roomMemberList.size(); i++) {// 在异步任务内存储
                                             RoomMemberDao.getInstance().saveSingleRoomMember(mucRoom.getId(), roomMemberList.get(i));
                                         }
                                     });

                                     // 成员列表传进去为了显示管理员的头像，
                                     mChatContentView.setRoomMemberList(roomMemberList);
                                     updateMemberCount(roomMemberList.size());
                                     if (isAtAction) {// 为@操作 存表之后在查询 跳转至@界面
                                         // 移除掉自己
                                         for (int i = 0; i < roomMemberList.size(); i++) {
                                             if (roomMemberList.get(i).getUserId().equals(mLoginUserId)) {
                                                 roomMemberList.remove(roomMemberList.get(i));
                                             }
                                         }
                                         mSelectRoomMemberPopupWindow = new SelectRoomMemberPopupWindow(MucChatActivity.this, MucChatActivity.this, roomMemberList, mRoomMember.getRole());
                                         mSelectRoomMemberPopupWindow.showAtLocation(findViewById(R.id.root_view),
                                                 Gravity.CENTER, 0, 0);
                                     }
                                 } else {
                                     ToastUtil.showErrorData(mContext);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                             }
                         }
                );
    }

    @SuppressLint("SetTextI18n")
    private void updateMemberCount(int userSize) {
        mTvTitle.setText(mFriend.getNickName() + "（" + userSize + "" + getString(R.string.people) + "）");
    }

    private void instantChatMessage() {
        if (!TextUtils.isEmpty(instantMessage)) {
            String toUserId = getIntent().getStringExtra("fromUserId");
            ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, instantMessage);
            instantMessage = null;
            if (!allowUploadFile && chatMessage.getType() == ChatMessage.TYPE_FILE) {
                tip(getString(R.string.tip_cannot_upload));
                return;
            }
            chatMessage.setFromUserId(mLoginUserId);
            chatMessage.setFromUserName(mLoginNickName);
            chatMessage.setToUserId(mFriend.getUserId());
            chatMessage.setUpload(true);
            chatMessage.setMySend(true);
            // 因为该消息的原主人可能开启了消息传输加密，我们对于content字段解密后存入了数据库，但是isEncrypt字段并未改变
            // 如果我们将此消息转发给另一人，对方可能会对我方已解密的消息再次进行解密
            chatMessage.setIsEncrypt(0);
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            mChatMessages.add(chatMessage);
            mChatContentView.notifyDataSetInvalidated(true);
            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage);
            send(chatMessage);
        }
    }

    /**
     * 获取自己在该群组的信息(职位、昵称、禁言时间等)以及群属性
     * <p>
     * 获取置顶公告，
     */
    private void getMyInfoInThisRoom() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// 数据结果与room/get接口一样，只是服务端没有返回群成员列表的数据
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();

                                     if (mucRoom.getS() == -1) {// 该群组已被后台禁用
                                         groupTip(getString(R.string.tip_group_disable_by_service));
                                         return;
                                     }

                                     if (mucRoom.getMember() == null) {// 被踢出该群组
                                         coreManager.exitMucChat(mucRoom.getJid());// XMPP退群
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 1);// 更新本地群组状态

                                         groupTip(getString(R.string.tip_been_kick_self));
                                     } else {// 正常状态
                                         // 置顶展示最新公告，
                                         setLastNotice(mucRoom.getLastNotice());

                                         // 更新群成员人数，
                                         updateMemberCount(mucRoom.getUserSize());

                                         mFriend.setGroupStatus(0);
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 0);// 更新本地群组状态
                                         // 更新禁言状态
                                         FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, mucRoom.getJid(), mucRoom.getMember().getTalkTime());
                                         onMyVoiceBanned(mucRoom.getJid(), mucRoom.getMember().getTalkTime());

                                         // 更新消息保存时长
                                         mFriend.setChatRecordTimeOut(mucRoom.getChatRecordTimeOut());
                                         FriendDao.getInstance().updateChatRecordTimeOut(mFriend.getUserId(), mucRoom.getChatRecordTimeOut());

                                         // 更新部分群属性
                                         MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(),
                                                 mucRoom.getAllowSendCard(), mucRoom.getAllowConference(),
                                                 mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime());

                                         // 更新个人职位
                                         // 此时可能正在初始化群成员信息，本地可能没有自己的信息，
                                         int role = mucRoom.getMember().getRole();
                                         RoomMemberDao.getInstance().updateRoomMemberRole(mucRoom.getId(), mLoginUserId, role);
                                         onRoleChanged(role);
                                         allowUploadFile = mucRoom.getAllowUploadFile() == 1 || role != RoomMember.ROLE_MEMBER;

                                         // 如果有转发进来的消息就处理一下，
                                         instantChatMessage();
                                     }
                                 } else {
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mFriend.getUserId(), 2);// 更新本地群组状态

                                     groupTip(TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.tip_group_been_disbanded) : result.getResultMsg());
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                             }
                         }
                );
    }

    private void groupTip(String tip) {
        if (tipDialog == null) {
            tipDialog = new TipDialog(MucChatActivity.this);
        }
        if (tipDialog.isShowing()) {
            tipDialog.dismiss();
        }
        tipDialog.setmConfirmOnClickListener(tip, new TipDialog.ConfirmOnClickListener() {
            @Override
            public void confirm() {
                finish();
            }
        });
        tipDialog.show();
    }

    /*******************************************
     * 是否被踢出群组|| 群组已经解散 && 是否离线&&重连
     ******************************************/
    public boolean getGroupStatus() {
        if (mFriend.getGroupStatus() == 1) {
            tip(getString(R.string.tip_been_kick));
            return true;
        } else if (mFriend.getGroupStatus() == 2) {
            tip(getString(R.string.tip_disbanded));
            return true;
        } else {
            return false;
        }
    }

    public void tip(String tip) {
        TipDialog tipDialog = new TipDialog(mContext);
        tipDialog.setTip(tip);
        tipDialog.show();
    }

    public boolean isOk() {// 群主与管理员不受限制
        boolean isOk = true;
        if (mRoomMember != null) {
            if (mRoomMember.getRole() == 1 || mRoomMember.getRole() == 2) {
                isOk = true;
            } else {
                isOk = false;
            }
        }
        return isOk;
    }

    public boolean isAuthenticated() {
        boolean isLogin = coreManager.isLogin();
        if (!isLogin) {
            coreManager.askReconnect();
        }
        return !isLogin;
    }

    // 更新禁言状态，影响全体禁言以及隐身人禁言，
    private void updateBannedStatus() {
        // 禁言状态
        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mFriend.getUserId(), false);
        if (mRoomMember != null) {
            if (mRoomMember.isInvisible()) {
                mChatBottomView.isBanned(true, R.string.hint_invisible);
            } else {
                mChatBottomView.isAllBanned(isAllShutUp && mRoomMember.isAllBannedEffective());
            }
        } else {
            mChatBottomView.isAllBanned(isAllShutUp);
        }
    }

    public void onRoleChanged(int role) {
        if (mRoomMember != null) {
            mRoomMember.setRole(role);
        }
        updateBannedStatus();
        mChatContentView.setRole(role);
    }
}

package com.sk.weichat.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.sk.weichat.AppConfig;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.MessageEventClickable;
import com.sk.weichat.audio_x.VoiceAnimView;
import com.sk.weichat.audio_x.VoicePlayer;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.RoomMember;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.bean.redpacket.Balance;
import com.sk.weichat.bean.redpacket.OpenRedpacket;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.RoomMemberDao;
import com.sk.weichat.downloader.DownloadListener;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.map.MapHelper;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.map.MapActivity;
import com.sk.weichat.ui.me.redpacket.RedDetailsActivity;
import com.sk.weichat.ui.message.ChatHistoryActivity;
import com.sk.weichat.ui.message.EventMoreSelected;
import com.sk.weichat.ui.message.InstantMessageActivity;
import com.sk.weichat.ui.message.multi.RoomReadListActivity;
import com.sk.weichat.ui.mucfile.DownManager;
import com.sk.weichat.ui.mucfile.MucFileDetails;
import com.sk.weichat.ui.mucfile.XfileUtils;
import com.sk.weichat.ui.mucfile.bean.MucFileBean;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;
import com.sk.weichat.ui.tool.WebViewActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.DES;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.util.HtmlUtils;
import com.sk.weichat.util.HttpUtil;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.SmileyParser;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.util.downloadTask;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardSecond;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardforchat;
import okhttp3.Call;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import static com.sk.weichat.bean.message.XmppMessage.TYPE_CHAT_HISTORY;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_END_CONNECT_VIDEO;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_END_CONNECT_VOICE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_FILE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IMAGE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IMAGE_TEXT;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IMAGE_TEXT_MANY;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IS_MU_CONNECT_VOICE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IS_MU_CONNECT_Video;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_NO_CONNECT_VIDEO;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_NO_CONNECT_VOICE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_TEXT;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_VIDEO;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_VOICE;
import static com.sk.weichat.ui.tool.WebViewActivity.EXTRA_URL;

@SuppressWarnings("unused")
public class ChatContent1 extends PullDownListView implements ChatBottomView.MoreSelectMenuListener {
    public static final int CLICK_RED_TYPE_TO = 0;
    public static final int CLICK_RED_TYPE_FROM = 1;
    private static final String TAG = ChatContentView.class.getSimpleName();
    public static int mCurrtPos = -1;// 当前正在播放的声音消息的id
    public int mRole = 3;// 我在当前群组的职位，用于控制群组消息能否撤回(default==3普通成员)
    public ExcessFunctionListener mExcessListener;
    public String mRoomId;
    // 匹配图标时用到的文件类型
    String[] fileTypes = new String[]{"apk", "avi", "bat", "bin", "bmp", "chm", "css", "dat", "dll", "doc", "docx",
            "dos", "dvd", "gif", "html", "ifo", "inf", "iso", "java", "jpeg", "jpg", "log", "m4a", "mid", "mov",
            "movie", "mp2", "mp2v", "mp3", "mp4", "mpe", "mpeg", "mpg", "pdf", "php", "png", "ppt", "pptx", "psd",
            "rar", "tif", "ttf", "txt", "wav", "wma", "wmv", "xls", "xlsx", "xml", "xsl", "zip"};
    List<ChatMessage> collectionMessages = new ArrayList<>();
    private Context mContext;
    /* 根据self.userId和mToUserId 唯一确定一张表 */
    private User self;
    private String mRoomNickName;
    private String mToUserId;
    private boolean is_group = false;   // 用于标记聊天界面是否为群聊界面
    private boolean isLiveChat = false; // 用于标记聊天界面是否为直播界面
    private boolean isShowMoreSelect = false; // 用于标记是否显示多选框
    private boolean course;
    private Handler mHandler = new Handler();
    private LayoutInflater mInflater;
    private int mDelayTime = 0;
    // 是否正在底部，如果是，来新消息时需要跳到底部，
    private boolean shouldScrollToBottom;
    private ChatContentAdapter mChatContentAdapter;
    private List<ChatMessage> mChatMessages;
    // 待删除的消息的ID, 用于实现删除消息时的淡出效果，主要是广播收到删除消息的通知时拿不到view对象，只能记下，
    // adapter.getView时判断要删除就加上动画效果，
    private Set<String> mDeletedChatMessageId = new HashSet<>();
    private ChatBottomView mChatBottomView;
    private MessageEventListener mMessageEventListener;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            scrollToBottom();
        }
    };
    private Map<String, CountDownTimer> mTextBurningMaps = new HashMap<>();
    private Map<Integer, VoiceViewHolder> viewHolderMap = new HashMap<>();
    private ChatTextClickPpWindow mChatPpWindow;
    private Map<String, String> mRemarksMap = new HashMap<>();
    private Map<String, String> mImageMaps = new HashMap<>();
    private Map<String, RoomMember> memberMap = new HashMap<>();
    private OnScrollListener customScrollListener;

    public ChatContent1(Context context) {
        super(context);
        init(context);
    }

    public ChatContent1(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw > h) {
            mHandler.removeCallbacks(runnable);
            mHandler.postDelayed(runnable, mDelayTime);
        }
    }

    private void init(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mDelayTime = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

        setCacheColorHint(0x00000000);
        self = CoreManager.requireSelf(context);

        super.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (customScrollListener != null) {
                    customScrollListener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                shouldScrollToBottom = firstVisibleItem + visibleItemCount >= totalItemCount;
                if (customScrollListener != null) {
                    customScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }
        });

        List<Friend> mFriendList = FriendDao.getInstance().getAllFriends(CoreManager.requireSelf(context).getUserId());
        for (int i = 0; i < mFriendList.size(); i++) {
            if (!TextUtils.isEmpty(mFriendList.get(i).getRemarkName())) {// 针对该好友进行了备注
                mRemarksMap.put(mFriendList.get(i).getUserId(), mFriendList.get(i).getRemarkName());
            }
        }

        //        VoicePlayer.instance().addVoicePlayListener(new VoiceManager.VoicePlayListener() {
        //
        //            @Override
        //            public void onFinishPlay() {
        //                if (viewHolderMap.containsKey(mCurClickPos)) {
        //                    viewHolderMap.remove(mCurClickPos);
        //                }
        //
        //                // 取出下一个要播放的holder
        //                int pos = queryMinVoicePosition();
        //                if (pos == 99999 || pos >= mChatMessages.size()) {
        //                    return;
        //                }
        //                // 下一个要播放holder
        //                mCurClickPos = pos;
        //                VoiceViewHolder holder = viewHolderMap.get(pos);
        //                ChatMessage message = mChatMessages.get(pos);
        //                VoicePlayer.instance().playVoice(holder.chat_voice);
        //                if (message.getIsReadDel()) {
        //                    ChatMessageDao.getInstance().deleteSingleChatMessage(self.getUserId(), message.getFromUserId(), message.getPacketId());
        //                    notifyDataSetChanged();
        //                }
        //
        //                viewHolderMap.remove(pos);
        //                // 发送自动播放的已读消息
        //                sendReadMessage(message); // 语音的已读消息
        //                if (holder.unread_img_view != null) {
        //                    holder.unread_img_view.setVisibility(View.GONE);
        //                }
        //            }
        //
        //            @Override
        //            public void onErrorPlay() {
        //
        //            }
        //        });

    }

    public boolean shouldScrollToBottom() {
        return shouldScrollToBottom;
    }

    @Override
    public void setOnScrollListener(OnScrollListener customScrollListener) {
        this.customScrollListener = customScrollListener;
    }

    private int queryMinVoicePosition() {
        int temp = 999999;
        for (int i : viewHolderMap.keySet()) {
            if (i < temp) {
                temp = i;
            }
        }

        if (temp < mCurrtPos) {
            viewHolderMap.remove(temp);
            return queryMinVoicePosition();
        } else {
            return temp;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mMessageEventListener != null) {
                mMessageEventListener.onEmptyTouch();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * 从外面传入一些值
     */
    public void setToUserId(String toUserId) {
        mToUserId = toUserId;
    }

    public void setIsLiveChat(boolean isLiveChat) {
        this.isLiveChat = isLiveChat;
    }

    public void setCourse(boolean course) {
        this.course = course;
    }

    public void setIsShowMoreSelect(boolean isShowMoreSelect) {
        this.isShowMoreSelect = isShowMoreSelect;
    }

    public void set_is_group(boolean is_group) {
        this.is_group = is_group;
    }

    public void setRoomNickName(String roomNickName) {
        mRoomNickName = roomNickName;
    }

    public void setRoomId(String roomId) {
        this.mRoomId = roomId;
    }

    public void setRole(int role) {
        this.mRole = role;
    }

    public void setChatBottomView(ChatBottomView chatBottomView) {
        this.mChatBottomView = chatBottomView;
        if (mChatBottomView != null) {
            mChatBottomView.setMoreSelectMenuListener(this);
        }
    }

    public void setData(List<ChatMessage> chatMessages) {
        mChatMessages = chatMessages;
        mChatContentAdapter = new ChatContentAdapter();
        setmChatContentAdapter(mChatContentAdapter);
        setAdapter(mChatContentAdapter);
        mChatContentAdapter.notifyDataSetInvalidated();
    }

    public void setRoomMemberList(List<RoomMember> memberList) {
        memberMap.clear();
        for (RoomMember member : memberList) {
            memberMap.put(member.getUserId(), member);
        }
        notifyDataSetChanged();
    }

    public void remove(final ChatMessage message) {
        mDeletedChatMessageId.add(message.getPacketId());
        if (mChatContentAdapter == null) {
            return;
        }
        mChatContentAdapter.notifyDataSetChanged();
    }

    public ChatContentAdapter getmChatContentAdapter() {
        return mChatContentAdapter;
    }

    /*
     * private String getLengthDesc(int seconds) { if (seconds < 60) { seconds =
     * 1000 * seconds; } int s = seconds / 1000; int m = (seconds % 1000) / 100;
     * return (s + "." + m + "''"); }
     */

    public void setmChatContentAdapter(ChatContentAdapter mChatContentAdapter) {
        this.mChatContentAdapter = mChatContentAdapter;
    }

    // 刷新单条
    public void notifySingleDate(int position, ChatMessage chat) {
        ChatMessage chatMessage = mChatMessages.get(position);
        chatMessage.setReadPersons(chat.getReadPersons());
        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        mChatContentAdapter.notifyDataSetChanged();
    }

    public void notifyDataSetInvalidated(final int position) {
        if (mChatContentAdapter == null) {
            return;
        }
        mChatContentAdapter.notifyDataSetInvalidated();

        this.post(new Runnable() {
            @Override
            public void run() {
                setSelection(position);
            }
        });
    }

    public void notifyDataSetInvalidated(boolean scrollToBottom) {
        if (mChatContentAdapter == null) {
            return;
        }
        mChatContentAdapter.notifyDataSetInvalidated();
        if (scrollToBottom)
            scrollToBottom();
    }

    public void notifyDataSetInvalidated() {
        if (mChatContentAdapter == null) {
            return;
        }
        mChatContentAdapter.notifyDataSetInvalidated();
    }

    public void notifyDataSetChanged() {
        if (mChatContentAdapter == null) {
            return;
        }
        mChatContentAdapter.notifyDataSetChanged();
    }

    public void scrollToBottom() {
        if (mChatMessages == null) {
            return;
        }
        setSelection(mChatMessages.size());
    }

    public void setMessageEventListener(MessageEventListener listener) {
        mMessageEventListener = listener;
    }

    public void setExcessFunctionListener(ExcessFunctionListener listener) {
        mExcessListener = listener;
    }

    /**
     * 发送已读消息的方法
     */
    public void sendReadMessage(ChatMessage message) {
        if (message.isMySend()) {// 自己发送的消息不发已读
            return;
        }

        if (course) {// 课程不发送已读
            return;
        }

        // 该群是否开启群已读，如开启，需要发送已读
        boolean isSendReadInGroup = PreferenceUtils.getBoolean(mContext, Constants.IS_SHOW_READ + mToUserId, false);

/*
        if (is_group
                && message.getType() == XmppMessage.TYPE_TEXT
                && !TextUtils.isEmpty(message.getObjectId())) {
            // 为@消息，无论是否开启都需发送已读
            isSendReadInGroup = true;
        }
*/

        if (is_group && !isSendReadInGroup) { // 该群组关闭群已读，本地默认置为发送了已读
            message.setSendRead(true);
            ChatMessageDao.getInstance().updateMessageRead(self.getUserId(), mToUserId, message.getPacketId(), true);
            return;
        }

        Intent intent = new Intent();
        intent.setAction("Read");
        Bundle bundle = new Bundle();
        bundle.putString("packetId", message.getPacketId());
        bundle.putBoolean("isGroup", is_group);
        if (message.getFromUserId().equals(message.getToUserId())) {// 我的设备
            bundle.putString("friendId", self.getUserId());
        } else {
            bundle.putString("friendId", mToUserId);
        }
        bundle.putString("fromUserName", TextUtils.isEmpty(mRoomNickName) ? self.getNickName() : mRoomNickName);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
        message.setSendRead(true); // 自动发送的已读消息，先给一个已读标志，等有消息回执确认发送成功后在去修改数据库
    }

    /**
     * 填充消息的状态显示，包括已读，未读，发送失败，发送中
     */
    private void fillMessageState(final ChatMessage message, ContentViewHolder contentViewHolder) {
        if (message.isMySend()) {
            int state = message.getMessageState();
            if (contentViewHolder.progress != null) {
                // 有的布局可能没有progress,
                contentViewHolder.progress.setVisibility(View.GONE);
            }
            contentViewHolder.failed_img_view.setVisibility(View.GONE);
            contentViewHolder.flStateclick.setVisibility(View.GONE);
            if (state == ChatMessageListener.MESSAGE_SEND_ING) {
                if (contentViewHolder.progress != null) {
                    contentViewHolder.progress.setVisibility(View.VISIBLE);
                }
            } else if (state == ChatMessageListener.MESSAGE_SEND_FAILED) {
                contentViewHolder.failed_img_view.setVisibility(View.VISIBLE);
            } else if (state == ChatMessageListener.MESSAGE_SEND_SUCCESS) {
                contentViewHolder.flStateclick.setVisibility(View.VISIBLE);
                if (!is_group) {
                    if (message.isSendRead()) {
                        contentViewHolder.imageview_read.setText(R.string.status_read);
                        contentViewHolder.imageview_read.setBackgroundResource(R.drawable.bg_send_read);
                    } else {
                        contentViewHolder.imageview_read.setText(R.string.status_send);
                        contentViewHolder.imageview_read.setBackgroundResource(R.drawable.bg_send_to);
                    }
                }
            }
        } else if (!is_group) {
            if (contentViewHolder.flStateclick != null) {
                contentViewHolder.flStateclick.setVisibility(View.GONE);
            }
        }

        if (is_group) {
            boolean showMucRead = PreferenceUtils.getBoolean(mContext, Constants.IS_SHOW_READ + mToUserId, false);
            String objectId = message.getObjectId();
/*
            if (message.getType() == XmppMessage.TYPE_TEXT
                    && !TextUtils.isEmpty(objectId)
                    && !TextUtils.isEmpty(message.getContent())
                    && message.getContent().contains("@")) {// 多选 转发红包消息 type改为TYPE_TEXT 且objectId不为空，需要兼容
                // 说明是@的消息，应该强制显示，并且发送已读
                showMucRead = true;
            }
*/
            if (showMucRead) {
                int count = message.getReadPersons();
                contentViewHolder.imageview_read.setText(count + getContext().getString(R.string.people));
                contentViewHolder.imageview_read.setBackgroundResource(R.drawable.bg_send_read);
                contentViewHolder.flStateclick.setVisibility(View.VISIBLE);
            } else {
                contentViewHolder.flStateclick.setVisibility(View.GONE);
            }

            contentViewHolder.flStateclick.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (is_group) {
                        if(!UiUtils.isNormalClick()){
                            return;
                        }
                        Intent intent = new Intent(mContext, RoomReadListActivity.class);
                        intent.putExtra("packetId", message.getPacketId());
                        intent.putExtra("roomId", mToUserId);
                        mContext.startActivity(intent);
                    }
                }
            });
        }
    }

    private void fillFileInco(String type, ImageView v, ChatMessage chat) {
        if (type.equals("mp3")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_y);
            chat.setTimeLen(2);
        } else if (type.equals("mp4") || type.equals("avi")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_v);
            chat.setTimeLen(3);
        } else if (type.equals("xls")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_x);
            chat.setTimeLen(5);
        } else if (type.equals("doc")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_w);
            chat.setTimeLen(6);
        } else if (type.equals("ppt")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_p);
            chat.setTimeLen(4);
        } else if (type.equals("pdf")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_f);
            chat.setTimeLen(10);
        } else if (type.equals("apk")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_a);
            chat.setTimeLen(11);
        } else if (type.equals("txt")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_t);
            chat.setTimeLen(8);
        } else if (type.equals("rar") || type.equals("zip")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_z);
            chat.setTimeLen(7);
        } else {
            v.setImageResource(R.drawable.ic_muc_flie_type_what);
            chat.setTimeLen(9);
        }
    }

    private void fillLinkData(LocationViewHolder holder, String oDa, final String packetId) {
        try {
            JSONObject json = new JSONObject(oDa);
            String tile = json.getString("title");
            final String url = json.getString("url");
            String img = json.getString("img");
            holder.chat_address.setText("[" + InternationalizationHelper.getString("JXLink") + "]" + " " + tile);
            AvatarHelper.getInstance().displayUrl(img, holder.chat_img_view);

            holder.chat_location.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), WebViewActivity.class);
                    intent.putExtra(EXTRA_URL, url);
                    intent.putExtra("isChat", true);
                    intent.putExtra("fromUserId", mToUserId);
                    intent.putExtra("messageId", packetId);
                    getContext().startActivity(intent);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillShareLinkData(LinkViewHolder holder, String sourceData, final String packetId) {
        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSONObject.parseObject(sourceData);
            String appName = json.getString("appName");
            String appIcon = json.getString("appIcon");
            String title = json.getString("title");
            String subTitle = json.getString("subTitle");
            final String url = json.getString("url");
            String imageUrl = json.getString("imageUrl");

            holder.link_app_name_tv.setText(appName);
            AvatarHelper.getInstance().displayImage(appIcon, holder.link_app_icon_iv);
            holder.link_title_tv.setText(title);
            holder.link_text_tv.setText(subTitle);
            if (TextUtils.isEmpty(appIcon) && TextUtils.isEmpty(imageUrl)) {
                holder.link_iv.setImageResource(R.drawable.browser);
            } else if (TextUtils.isEmpty(imageUrl)) {
                AvatarHelper.getInstance().displayImage(appIcon, holder.link_iv);
            } else {
                AvatarHelper.getInstance().displayImage(imageUrl, holder.link_iv);
            }

            holder.link_text_tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), WebViewActivity.class);
                    intent.putExtra(EXTRA_URL, url);
                    intent.putExtra("isChat", true);
                    intent.putExtra("fromUserId", mToUserId);
                    intent.putExtra("messageId", packetId);
                    mContext.startActivity(intent);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 填充单条图文
     */
    private void fillTextImgDatas(TextImgViewHolder holder, String oDa) {
        try {
            JSONObject json = new JSONObject(oDa);
            String tile = json.getString("title");
            String sub = json.getString("sub");
            String img = json.getString("img");
            final String url = json.getString("url");
            holder.tvTitle.setText(tile);
            holder.tvText.setText(sub);
            // ImageLoader.getInstance().displayImage(img, holder.ivImg);
            AvatarHelper.getInstance().displayUrl(img, holder.ivImg);

            holder.llRootView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), WebViewActivity.class);
                    intent.putExtra(EXTRA_URL, url);
                    getContext().startActivity(intent);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 填充多条图文数据
     */
    private void fillTextImgDatas(TextImgManyHolder holder, String oDa) {
        try {
            JSONArray jsonArray = new JSONArray(oDa);
            if (jsonArray != null && jsonArray.length() > 0) {
                List<TextImgBean> datas = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    TextImgBean info = new TextImgBean();
                    info.title = json.getString("title");
                    info.img = json.getString("img");
                    final String url = json.getString("url");
                    info.url = url;
                    if (i > 0) {
                        datas.add(info);
                    } else {
                        holder.tvTitle.setText(info.title);
                        AvatarHelper.getInstance().displayUrl(info.img, holder.ivImg);
                        holder.llRootView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(getContext(), WebViewActivity.class);
                                intent.putExtra(EXTRA_URL, url);
                                getContext().startActivity(intent);
                            }
                        });
                    }
                }
                //                holder.listView.setAdapter(new TextImgManyAdapter(getContext(), datas));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载文件到sd卡
     */
    public void DownloadFileToSD(String url, String name) {
        // 获取SD卡目录
        String dowloadDir = Environment.getExternalStorageDirectory()
                + "/sk/";
        File file = new File(dowloadDir);
        //创建下载目录
        if (!file.exists()) {
            file.mkdirs();
        }

        //读取下载线程数，如果为空，则单线程下载
        int downloadTN = 2;
        //启动文件下载线程
        new downloadTask(url, Integer.valueOf(downloadTN), dowloadDir + name).start();
    }

    /**
     * Todo  多选菜单点击事件
     */

    private boolean isNull() {
        for (int i = 0; i < mChatMessages.size(); i++) {
            if (mChatMessages.get(i).isMoreSelected) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clickForwardMenu() {
        final Dialog mForwardDialog = new Dialog(mContext, R.style.BottomDialog);
        View contentView = mInflater.inflate(R.layout.forward_dialog, null);
        mForwardDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        mForwardDialog.setCanceledOnTouchOutside(true);
        mForwardDialog.getWindow().setGravity(Gravity.BOTTOM);
        mForwardDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mForwardDialog.show();
        mForwardDialog.findViewById(R.id.single_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {// 逐条转发
                mForwardDialog.dismiss();
                if (isNull()) {
                    Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转至转发页面
                Intent intent = new Intent(mContext, InstantMessageActivity.class);
                intent.putExtra(Constants.IS_MORE_SELECTED_INSTANT, true);
                mContext.startActivity(intent);
            }
        });
        mForwardDialog.findViewById(R.id.sum_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {// 合并转发
                mForwardDialog.dismiss();
                if (isNull()) {
                    Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转至转发页面
                Intent intent = new Intent(mContext, InstantMessageActivity.class);
                intent.putExtra(Constants.IS_MORE_SELECTED_INSTANT, true);
                intent.putExtra(Constants.IS_SINGLE_OR_MERGE, true);
                mContext.startActivity(intent);
            }
        });
        mForwardDialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mForwardDialog.dismiss();
            }
        });
    }

    @Override
    public void clickCollectionMenu() {
        if (isNull()) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(null, "选中消息中，文字 / 图片 / 语音 / 视频 / 文件 消息才能被收藏", "取消", "收藏",
                new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        for (int i = 0; i < mChatMessages.size(); i++) {
                            if (mChatMessages.get(i).isMoreSelected
                                    && (mChatMessages.get(i).getType() == XmppMessage.TYPE_TEXT
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_IMAGE
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_VOICE
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_VIDEO
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_FILE)) {
                                collectionMessages.add(mChatMessages.get(i));
                            }
                        }
                        moreSelectedCollection(collectionMessages);
                        // 清空收藏list
                        collectionMessages.clear();
                        // 发送EventBus，通知聊天页面解除多选状态
                        EventBus.getDefault().post(new EventMoreSelected("MoreSelectedCollection", false, is_group));
                    }
                });
        selectionFrame.show();
    }

    @Override
    public void clickDeleteMenu() {
        if (isNull()) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog mDeleteDialog = new Dialog(mContext, R.style.BottomDialog);
        View contentView = mInflater.inflate(R.layout.delete_dialog, null);
        mDeleteDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        mDeleteDialog.setCanceledOnTouchOutside(true);
        mDeleteDialog.getWindow().setGravity(Gravity.BOTTOM);
        mDeleteDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mDeleteDialog.show();
        mDeleteDialog.findViewById(R.id.delete_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeleteDialog.dismiss();
                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedDelete", false, is_group));
            }
        });

        mDeleteDialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeleteDialog.dismiss();
            }
        });
    }

    @Override
    public void clickEmailMenu() {
        if (isNull()) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog mEmailDialog = new Dialog(mContext, R.style.BottomDialog);
        View contentView = mInflater.inflate(R.layout.email_dialog, null);
        mEmailDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        mEmailDialog.setCanceledOnTouchOutside(true);
        mEmailDialog.getWindow().setGravity(Gravity.BOTTOM);
        mEmailDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mEmailDialog.show();
        mEmailDialog.findViewById(R.id.save_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailDialog.dismiss();
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(null, getContext().getString(R.string.save_only_image), getContext().getString(R.string.cancel), getContext().getString(R.string.save),
                        new SelectionFrame.OnSelectionFrameClickListener() {
                            @Override
                            public void cancelClick() {

                            }

                            @Override
                            public void confirmClick() {
                                for (int i = 0; i < mChatMessages.size(); i++) {
                                    if (mChatMessages.get(i).isMoreSelected && mChatMessages.get(i).getType() == XmppMessage.TYPE_IMAGE) {
                                        FileUtil.downImageToGallery(mContext, mChatMessages.get(i).getContent());
                                    }
                                }
                                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedEmail", false, is_group));
                            }
                        });
                selectionFrame.show();
            }
        });

        mEmailDialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailDialog.dismiss();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
    }

    private void resendMessage(ChatMessage message) {
        message.setMessageState(ChatMessageListener.MESSAGE_SEND_ING);
        mMessageEventListener.onSendAgain(message);
    }

    /**
     * 打开红包方法
     */
    public void openRedPacket(final ChatMessage message) {
        HashMap<String, String> params = new HashMap<String, String>();
        String redId = message.getObjectId();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);
        params.put("id", redId);

        HttpUtils.get().url(CoreManager.requireConfig(getContext()).REDPACKET_OPEN)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        OpenRedpacket openRedpacket = result.getData();
                        Bundle bundle = new Bundle();
                        Intent intent = new Intent(mContext, RedDetailsActivity.class);
                        bundle.putSerializable("openRedpacket", openRedpacket);
                        bundle.putInt("redAction", 1);
                        bundle.putInt("timeOut", 0);

                        bundle.putBoolean("isGroup", is_group);
                        bundle.putString("mToUserId", mToUserId);
                        intent.putExtras(bundle);
                        mContext.startActivity(intent);
                        // 更新余额
                        updateMyBalance();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    public void clickRedpacket(final ChatMessage message, final int clickType) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);
        params.put("id", message.getObjectId());

        HttpUtils.get().url(CoreManager.requireConfig(getContext()).RENDPACKET_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        OpenRedpacket openRedpacket = result.getData();
                        int resultCode = result.getResultCode();
                        Bundle bundle = new Bundle();
                        Intent intent = new Intent(mContext, RedDetailsActivity.class);
                        bundle.putSerializable("openRedpacket", openRedpacket);
                        if (result.getResultMsg() != null) //result message为null表示没有过期
                        {
                            bundle.putInt("timeOut", 1);
                        } else {
                            bundle.putInt("timeOut", 0);
                        }
                        bundle.putInt("redAction", 0);

                        bundle.putBoolean("isGroup", is_group);
                        bundle.putString("mToUserId", mToUserId);
                        intent.putExtras(bundle);
                        if (is_group) {
                            if (resultCode == 1) // 红包是否可以领取
                            {
                                // 自己是否领取过该红包
                                if (message.getFileSize() == 1) {
                                    if (message.getFilePath().equals("3")) { // 是否为口令红包
                                        if (mExcessListener != null) {
                                            mExcessListener.clickPwdRed(message.getContent());
                                        }
                                    } else {
                                        // 非口令红包直接打开
                                        openRedPacket(message);
                                    }
                                } else {
                                    mContext.startActivity(intent);
                                }
                            } else {
                                mContext.startActivity(intent);
                            }
                        } else {
                            if (clickType == CLICK_RED_TYPE_TO) {
                                // 自己发的红包
                                mContext.startActivity(intent);
                            } else if (clickType == CLICK_RED_TYPE_FROM) {
                                // 收到的红包
                                if (resultCode == 1) {
                                    if (message.getFilePath().equals("3")) {
                                        // ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_WantOpenGift"));
                                        if (mExcessListener != null) {
                                            mExcessListener.clickPwdRed(message.getContent());
                                        }
                                    } else {
                                        openRedPacket(message);
                                    }
                                } else {
                                    mContext.startActivity(intent);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    /**
     * 更新用户的余额，每次打开红包或者是发送红包后都会有变化
     */
    public void updateMyBalance() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        Balance b = result.getData();
                        if (b != null) {
                            self.setBalance(b.getBalance());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    /**
     * 收藏和存表情共用一个接口，参数也基本相同，
     *
     * @param flag 是收藏就为true, 是存表情就为false,
     */
    private String collectionParam(List<ChatMessage> messageList, boolean flag) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        for (ChatMessage message : messageList) {
            int type = 6;
            if (flag) {// 收藏
                if (message.getType() == TYPE_IMAGE) {
                    type = CollectionEvery.TYPE_IMAGE;
                } else if (message.getType() == TYPE_VIDEO) {
                    type = CollectionEvery.TYPE_VIDEO;
                } else if (message.getType() == TYPE_FILE) {
                    type = CollectionEvery.TYPE_FILE;
                } else if (message.getType() == TYPE_VOICE) {
                    type = CollectionEvery.TYPE_VOICE;
                } else if (message.getType() == TYPE_TEXT) {
                    type = CollectionEvery.TYPE_TEXT;
                }
            }
            com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
            json.put("type", String.valueOf(type));
            json.put("msg", message.getContent());
            if (flag) {
                // 收藏消息id
                json.put("msgId", message.getPacketId());
                if (is_group) {
                    // 群组收藏需要添加jid
                    json.put("roomJid", mToUserId);
                }
            } else {
                // 表情url
                json.put("url", message.getContent());
            }
            array.add(json);
        }
        return JSON.toJSONString(array);
    }

    /**
     * 添加为表情 && 收藏
     * 添加为表情Type 6.表情
     * 收藏Type    1.图片 2.视频 3.文件 4.语音 5.文本
     */
    public void collectionEmotion(ChatMessage message, final boolean flag) {
        if (TextUtils.isEmpty(message.getContent())) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);
        params.put("emoji", collectionParam(Collections.singletonList(message), flag));

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, InternationalizationHelper.getString("JX_CollectionSuccess"), Toast.LENGTH_SHORT).show();
                            if (!flag) { // 添加为表情
                                // 收藏成功后将对应的url存入内存中，以防下次再次收藏该链接
                                // PreferenceUtils.putInt(mContext, self.getUserId() + message.getContent(), 1);
                                // 发送广播更新收藏列表
                                MyApplication.getInstance().sendBroadcast(new Intent("CollectionRefresh"));
                            }
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    /**
     * 多选 收藏
     */
    public void moreSelectedCollection(List<ChatMessage> chatMessageList) {
        if (chatMessageList == null || chatMessageList.size() <= 0) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);
        params.put("emoji", collectionParam(chatMessageList, true));

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, InternationalizationHelper.getString("JX_CollectionSuccess"), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    // 多选，根据消息类型返回文字
    private String getMsgContent(ChatMessage chatMessage) {
        int type = chatMessage.getType();
        String text = "";
        if (type == XmppMessage.TYPE_TEXT) {
            text = chatMessage.getContent();
        } else if (type == XmppMessage.TYPE_VOICE) {
            text = "[" + InternationalizationHelper.getString("JX_Voice") + "]";
        } else if (type == XmppMessage.TYPE_GIF) {
            text = "[" + InternationalizationHelper.getString("emojiVC_Anma") + "]";
        } else if (type == XmppMessage.TYPE_IMAGE) {
            text = "[" + InternationalizationHelper.getString("JX_Image") + "]";
        } else if (type == XmppMessage.TYPE_VIDEO) {
            text = "[" + InternationalizationHelper.getString("JX_Video") + "]";
        } else if (type >= XmppMessage.TYPE_IS_CONNECT_VOICE
                && type <= XmppMessage.TYPE_EXIT_VOICE) {
            text = MyApplication.getContext().getString(R.string.msg_video_voice);
        } else if (type == XmppMessage.TYPE_FILE) {
            text = "[" + InternationalizationHelper.getString("JX_File") + "]";
        } else if (type == XmppMessage.TYPE_LOCATION) {
            text = "[" + InternationalizationHelper.getString("JX_Location") + "]";
        } else if (type == XmppMessage.TYPE_CARD) {
            text = "[" + InternationalizationHelper.getString("JX_Card") + "]";
        } else if (type == XmppMessage.TYPE_RED) {
            text = "[" + InternationalizationHelper.getString("JX_RED") + "]";
        } else if (type == XmppMessage.TYPE_SHAKE) {
            text = MyApplication.getContext().getString(R.string.msg_shake);
        } else if (type == XmppMessage.TYPE_LINK || type == XmppMessage.TYPE_SHARE_LINK) {
            text = "[" + InternationalizationHelper.getString("JXLink") + "]";
        } else if (type == TYPE_IMAGE_TEXT || type == TYPE_IMAGE_TEXT_MANY) {
            text = "[" + InternationalizationHelper.getString("JXGraphic") + InternationalizationHelper.getString("JXMainViewController_Message") + "]";
        } else if (type == TYPE_CHAT_HISTORY) {
            text = MyApplication.getContext().getString(R.string.msg_chat_history);
        }
        return chatMessage.getFromUserName() + "：" + text;
    }

    private void intentWebView(String url, String packetId) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra("isChat", true);
        intent.putExtra("fromUserId", mToUserId);
        intent.putExtra("messageId", packetId);
        mContext.startActivity(intent);
    }

    public interface MessageEventListener {
        // 点击空白处，让输入框归位
        void onEmptyTouch();

        void onTipMessageClick(ChatMessage message);

        void onMyAvatarClick();

        void onFriendAvatarClick(String friendUserId);

        void LongAvatarClick(ChatMessage chatMessage);

        void onNickNameClick(String friendUserId);

        void onMessageClick(ChatMessage chatMessage);

        void onMessageLongClick(ChatMessage chatMessage);

        void onSendAgain(ChatMessage chatMessage);

        void onMessageBack(ChatMessage chatMessage, int position);

        void onCallListener(int type);
    }

    public interface ExcessFunctionListener {
        void clickPwdRed(String str); // 口令红包的点击
    }

    public class ChatContentAdapter extends BaseAdapter {
        private static final int VIEW_SYSTEM = 0;
        private static final int VIEW_FROM_ME_TEXT = 1;
        private static final int VIEW_FROM_ME_IMAGE = 2;
        private static final int VIEW_FROM_ME_VOICE = 3;
        private static final int VIEW_FROM_ME_LOCATION = 4;
        private static final int VIEW_FROM_ME_GIF = 5;
        private static final int VIEW_FROM_ME_VIDEO = 6;
        private static final int VIEW_FROM_ME_FILE = 13;
        private static final int VIEW_FROM_ME_CARD = 15;
        private static final int VIEW_FROM_ME_RED = 18; // 红包类型
        private static final int VIEW_FROM_ME_RED_KEY = 20; // 口令红包类型
        private static final int VIEW_FROM_ME_LINK = 21; // 链接
        private static final int VIEW_FROM_ME_IMAGE_TEXT = 23; // 单条图文消息
        private static final int VIEW_FROM_ME_IMAGE_TEXT_MANY = 25; // 多条条图文消息
        private static final int VIEW_FROM_ME_MEDIA_CALL = 27; // 音视频通话
        private static final int VIEW_FROM_ME_SHAKE = 29;
        private static final int VIEW_FROM_ME_CHAT_HISTORY = 31;
        private static final int VIEW_FROM_ME_SHARE_LINK = 33;// 分享进来的链接

        private static final int VIEW_TO_ME_TEXT = 7;
        private static final int VIEW_TO_ME_IMAGE = 8;
        private static final int VIEW_TO_ME_VOICE = 9;
        private static final int VIEW_TO_ME_LOCATION = 10;
        private static final int VIEW_TO_ME_GIF = 11;
        private static final int VIEW_TO_ME_VIDEO = 12;
        private static final int VIEW_TO_ME_FILE = 14;
        private static final int VIEW_TO_ME_CARD = 16;
        private static final int VIEW_TO_ME_RED = 17; // 红包类型
        private static final int VIEW_TO_ME_RED_KEY = 19; // 口令红包类型
        private static final int VIEW_TO_ME_LINK = 22; // 链接
        private static final int VIEW_TO_ME_IMAGE_TEXT = 24; // 单条图文消息
        private static final int VIEW_TO_ME_IMAGE_TEXT_MANY = 26; // 多条条图文消息
        private static final int VIEW_TO_ME_MEDIA_CALL = 28; // 音视频通话
        private static final int VIEW_TO_ME_SHAKE = 30;
        private static final int VIEW_TO_ME_CHAT_HISTORY = 32;
        private static final int VIEW_TO_ME_SHARE_LINK = 34; // 分享进来的链接
        int X, Y;

        public int getCount() {
            return mChatMessages.size();
        }

        public ChatMessage getItem(int position) {
            return mChatMessages.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 35;
        }

        @Override
        public int getItemViewType(int position) {
            int messageType = mChatMessages.get(position).getType();
            String fromUserId = mChatMessages.get(position).getFromUserId();
            if (messageType == XmppMessage.TYPE_TIP) {
                return VIEW_SYSTEM;// 消息提示
            }

            if (mChatMessages.get(position).isMySend()) {
                if (messageType == TYPE_END_CONNECT_VIDEO || messageType == TYPE_END_CONNECT_VOICE
                        || messageType == TYPE_NO_CONNECT_VOICE || messageType == TYPE_NO_CONNECT_VIDEO
                        || messageType == TYPE_IS_MU_CONNECT_Video || messageType == TYPE_IS_MU_CONNECT_VOICE) {

                    return VIEW_FROM_ME_MEDIA_CALL;
                }
            } else {
                if (messageType == TYPE_END_CONNECT_VIDEO || messageType == TYPE_END_CONNECT_VOICE
                        || messageType == TYPE_NO_CONNECT_VOICE || messageType == TYPE_NO_CONNECT_VIDEO
                        || messageType == TYPE_IS_MU_CONNECT_Video || messageType == TYPE_IS_MU_CONNECT_VOICE) {

                    return VIEW_TO_ME_MEDIA_CALL;
                }
            }

            if (mChatMessages.get(position).getFromUserId().equals(mChatMessages.get(position).getToUserId())) {
                if (!TextUtils.isEmpty(mChatMessages.get(position).getFromId())) {
                    if (mChatMessages.get(position).getFromId().contains("android")) {
                        return getTypeForMe(messageType, position);
                    } else {
                        return getTypeForElse(messageType, position);
                    }
                }
            }

            if (mChatMessages.get(position).isMySend() || mChatMessages.get(position).getFromUserId().equals(self.getUserId())) {// 我的消息
                return getTypeForMe(messageType, position);
            } else {
                return getTypeForElse(messageType, position);
            }
        }

        private int getTypeForMe(int messageType, int position) {
            switch (messageType) {
                case XmppMessage.TYPE_TEXT:
                    return VIEW_FROM_ME_TEXT;
                case XmppMessage.TYPE_IMAGE:
                    return VIEW_FROM_ME_IMAGE;
                case XmppMessage.TYPE_VOICE:
                    return VIEW_FROM_ME_VOICE;
                case XmppMessage.TYPE_LOCATION:
                    return VIEW_FROM_ME_LOCATION;
                case XmppMessage.TYPE_GIF:
                    return VIEW_FROM_ME_GIF;
                case XmppMessage.TYPE_VIDEO:
                    return VIEW_FROM_ME_VIDEO;
                case XmppMessage.TYPE_FILE:
                    return VIEW_FROM_ME_FILE;
                case XmppMessage.TYPE_CARD:
                    return VIEW_FROM_ME_CARD;
                case XmppMessage.TYPE_RED: {
                    if (!TextUtils.isEmpty(mChatMessages.get(position).getFilePath())) {
                        int redType = Integer.parseInt(mChatMessages.get(position).getFilePath());
                        return redType == 3 ? VIEW_FROM_ME_RED_KEY : VIEW_FROM_ME_RED;
                    } else {
                        return VIEW_FROM_ME_RED;
                    }
                }
                case XmppMessage.TYPE_LINK: // 链接
                    return VIEW_FROM_ME_LINK;
                case XmppMessage.TYPE_SHARE_LINK: // 分享进来的链接
                    return VIEW_FROM_ME_SHARE_LINK;
                case XmppMessage.TYPE_IMAGE_TEXT:
                    return VIEW_FROM_ME_IMAGE_TEXT;
                case XmppMessage.TYPE_IMAGE_TEXT_MANY:
                    return VIEW_FROM_ME_IMAGE_TEXT_MANY;
                case XmppMessage.TYPE_END_CONNECT_VIDEO:
                case XmppMessage.TYPE_END_CONNECT_VOICE:
                case XmppMessage.TYPE_NO_CONNECT_VOICE:
                case XmppMessage.TYPE_NO_CONNECT_VIDEO:
                case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                case XmppMessage.TYPE_IS_MU_CONNECT_Video:
                    return VIEW_FROM_ME_MEDIA_CALL;
                case XmppMessage.TYPE_SHAKE: // 戳一戳
                    return VIEW_FROM_ME_SHAKE;
                case XmppMessage.TYPE_CHAT_HISTORY: // 聊天记录
                    return VIEW_FROM_ME_CHAT_HISTORY;
            }
            return VIEW_SYSTEM;
        }

        private int getTypeForElse(int messageType, int position) {
            switch (messageType) {
                case XmppMessage.TYPE_TEXT:
                    return VIEW_TO_ME_TEXT;
                case XmppMessage.TYPE_IMAGE:
                    return VIEW_TO_ME_IMAGE;
                case XmppMessage.TYPE_VOICE:
                    return VIEW_TO_ME_VOICE;
                case XmppMessage.TYPE_LOCATION:
                    return VIEW_TO_ME_LOCATION;
                case XmppMessage.TYPE_GIF:
                    return VIEW_TO_ME_GIF;
                case XmppMessage.TYPE_VIDEO:
                    return VIEW_TO_ME_VIDEO;
                case XmppMessage.TYPE_FILE:
                    return VIEW_TO_ME_FILE;
                case XmppMessage.TYPE_CARD:
                    return VIEW_TO_ME_CARD;
                case XmppMessage.TYPE_RED: {
                    if (!TextUtils.isEmpty(mChatMessages.get(position).getFilePath())) {
                        int redType = Integer.parseInt(mChatMessages.get(position).getFilePath());
                        return redType == 3 ? VIEW_TO_ME_RED_KEY : VIEW_TO_ME_RED;
                    } else {
                        return VIEW_TO_ME_RED;
                    }
                }
                case XmppMessage.TYPE_LINK: // 链接
                    return VIEW_TO_ME_LINK;
                case XmppMessage.TYPE_SHARE_LINK: // 分享进来的链接
                    return VIEW_TO_ME_SHARE_LINK;
                case XmppMessage.TYPE_IMAGE_TEXT:
                    return VIEW_TO_ME_IMAGE_TEXT;
                case XmppMessage.TYPE_IMAGE_TEXT_MANY:
                    return VIEW_TO_ME_IMAGE_TEXT_MANY;
                case XmppMessage.TYPE_END_CONNECT_VIDEO:
                case XmppMessage.TYPE_END_CONNECT_VOICE:
                case XmppMessage.TYPE_NO_CONNECT_VOICE:
                case XmppMessage.TYPE_NO_CONNECT_VIDEO:
                case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                case XmppMessage.TYPE_IS_MU_CONNECT_Video:
                    return VIEW_TO_ME_MEDIA_CALL;
                case XmppMessage.TYPE_SHAKE: // 戳一戳
                    return VIEW_TO_ME_SHAKE;
                case XmppMessage.TYPE_CHAT_HISTORY: // 聊天记录
                    return VIEW_TO_ME_CHAT_HISTORY;
            }
            return VIEW_SYSTEM;
        }

        @SuppressLint("NewApi")
        public View getView(final int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            SystemViewHolder systemViewHolder = null;
            ContentViewHolder contentViewHolder = null;
            if (convertView == null || ((Integer) convertView.getTag(R.id.tag_key_list_item_type)) != viewType) {
                convertView.setTag(R.id.tag_key_list_item_type, viewType);
                if (systemViewHolder != null) {
                    convertView.setTag(R.id.tag_key_list_item_view, systemViewHolder);
                } else if (contentViewHolder != null) {
                    contentViewHolder.time_tv = (TextView) convertView.findViewById(R.id.time_tv);
                    contentViewHolder.chat_head_iv = convertView.findViewById(R.id.chat_head_iv);
                    contentViewHolder.nick_name = (TextView) convertView.findViewById(R.id.nick_name);
                    contentViewHolder.chat_more_select = (CheckBox) convertView.findViewById(R.id.chat_msc);
                    contentViewHolder.mRootView = convertView.findViewById(R.id.chat_content_layout);
                    contentViewHolder.mRootViewFather = (LinearLayout) convertView.findViewById(R.id.item_ll);
                    if (is_group) {
                        contentViewHolder.nick_name.setVisibility(VISIBLE);
                    } else {
                        contentViewHolder.nick_name.setVisibility(GONE);
                    }
                    convertView.setTag(R.id.tag_key_list_item_view, contentViewHolder);
                }
            } else {
                if (viewType == VIEW_SYSTEM) {
                    systemViewHolder = (SystemViewHolder) convertView.getTag(R.id.tag_key_list_item_view);
                } else {
                    contentViewHolder = (ContentViewHolder) convertView.getTag(R.id.tag_key_list_item_view);
                }
            }

            /* 设置数据 */
            final ChatMessage message = mChatMessages.get(position);

            // 阅后即焚消息加上火焰小图标，
            // viewHolder居然也可能为空，
            if (contentViewHolder != null) {
                // 可能有的布局没有相应控件，先判断下，
                if (contentViewHolder.ivFire != null) {
                    if (message.getIsReadDel()) {
                        contentViewHolder.ivFire.setVisibility(VISIBLE);
                    } else {
                        contentViewHolder.ivFire.setVisibility(GONE);
                    }
                }
            }


            /**
             * 在此处对消息进行解密
             */
            if (message.getIsEncrypt() == 1) {
                try {
                    String decryptKey = Md5Util.toMD5(AppConfig.apiKey + message.getTimeSend() + message.getPacketId());
                    String decryptContent = DES.decryptDES(message.getContent(), decryptKey);
                    // 为chatMessage重新设值
                    message.setContent(decryptContent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * 看到的消息就直接发送已读回执
             * 在listview中把未读状态的消息发送已读回执给发送方
             * 标记此消息已经发送了回执
             */
            if (!message.isMySend() && !message.isSendRead()) {
                int type = message.getType();
                if (type == XmppMessage.TYPE_TEXT
                        || type == XmppMessage.TYPE_IMAGE
                        || type == XmppMessage.TYPE_LOCATION
                        || type == XmppMessage.TYPE_GIF
                        || type == XmppMessage.TYPE_RED
                        || viewType == VIEW_TO_ME_MEDIA_CALL
                        || type == XmppMessage.TYPE_IMAGE_TEXT
                        || type == XmppMessage.TYPE_IMAGE_TEXT_MANY
                        || type == XmppMessage.TYPE_LINK
                        || type == XmppMessage.TYPE_SHARE_LINK
                        || type == XmppMessage.TYPE_SHAKE
                        || type == XmppMessage.TYPE_CHAT_HISTORY) { // 文字，图片，位置，动画，红包，音视频通话，戳一戳，聊天记录自动发送已读回执
                    if (!is_group && message.getIsReadDel()) {
                        if (type == XmppMessage.TYPE_TEXT || type == XmppMessage.TYPE_IMAGE) {
                            // 文本与图片的阅后即焚消息，需要点击查看后在发送已读
                        } else { // 其他阅后即焚类型自动发送已读消息
                            sendReadMessage(message);
                        }
                    } else {// 自动发送已读消息
                        sendReadMessage(message);
                    }
                }
            }

            /* 是否显示日期 */
            boolean showTime = true;
            if (position >= 1) {
                ChatMessage prevMessage = mChatMessages.get(position - 1);
                long prevTime = prevMessage.getTimeSend();
                long nowTime = message.getTimeSend();
                if (nowTime - prevTime < 15 * 60) {// 小于15分钟，不显示
                    showTime = false;
                }
            }

            if (viewType == VIEW_SYSTEM) {
                String time = XfileUtils.fromatTime(message.getTimeSend() * 1000, "MM-dd HH:mm");
                if (isLiveChat) {
                    systemViewHolder.chat_content_tv.setText(message.getContent());
                } else {
                    SpannableString tip;
                    String str;
                    if (message.isDownload()) {
                        str = MyApplication.getContext().getString(R.string.has_confirm);
                    } else {
                        str = MyApplication.getContext().getString(R.string.to_confirm);
                    }
                    if (showTime) {
                        tip = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"), message.getContent() + "(" + time + ")", str);
                    } else {
                        tip = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"), message.getContent(), str);
                    }
                    systemViewHolder.chat_content_tv.setText(tip);

                    systemViewHolder.chat_content_tv.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mMessageEventListener.onTipMessageClick(message);
                        }
                    });
                }
                return convertView;
            }

            if (showTime) {
                contentViewHolder.time_tv.setVisibility(View.VISIBLE);
                if (TextUtils.isEmpty(String.valueOf(message.getTimeSend()))) {
                    contentViewHolder.time_tv.setVisibility(View.GONE);
                } else {
                    contentViewHolder.time_tv.setText(TimeUtils.sk_time_long_to_chat_time_str(message.getTimeSend()));
                }
            } else {
                contentViewHolder.time_tv.setVisibility(View.GONE);
            }

            // Todo 新添加消息类型，如需在聊天界面显示且需要显示 送达、已读等，需要修改此处
            // 填充转圈、感叹号、送达、已读、图标
            if (viewType < VIEW_TO_ME_CHAT_HISTORY) {
                fillMessageState(message, contentViewHolder);
            }

            /**
             * 加载头像
             */
            final String fromUserId = message.getFromUserId();
            RoomMember roomMember = memberMap.get(fromUserId);
            //            contentViewHolder.chat_head_iv.setGroupManager(roomMember != null && roomMember.isGroupOwnerOrManager());
            AvatarHelper.getInstance().displayAvatar(fromUserId, contentViewHolder.chat_head_iv.getHeadImage(), true);

            contentViewHolder.chat_head_iv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (message.isMySend()) {
                        mMessageEventListener.onFriendAvatarClick(self.getUserId());
                    } else {
                        mMessageEventListener.onFriendAvatarClick(fromUserId);
                    }
                }
            });

            if (!message.isMySend()) {// 长按@
                contentViewHolder.chat_head_iv.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mMessageEventListener.LongAvatarClick(mChatMessages.get(position));
                        return true;
                    }
                });
            }

            if (contentViewHolder.failed_img_view != null) {
                contentViewHolder.failed_img_view.setOnClickListener(new OnClickListener() {// 发送失败，点击重发
                    @Override
                    public void onClick(View v) {
                        if (message.getMessageState() == ChatMessageListener.MESSAGE_SEND_FAILED) {
                            resendMessage(message);
                        }
                    }
                });
            }

/*
            if (is_group) {
                boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mToUserId, true);
                if (!isAllowSecretlyChat) {// 该群组禁止私聊，显示默认头像
                    contentViewHolder.chat_head_iv.setImageResource(R.drawable.avatar_normal);
                }
            }
*/

            // ToDo something you can do here
            /**
             * 显示昵称
             */
            if (is_group) {
                if (message.isMySend() || message.getFromUserId().equals(self.getUserId())) {
                    contentViewHolder.nick_name.setVisibility(GONE);// 自己 不显示
                } else {
                    contentViewHolder.nick_name.setVisibility(VISIBLE);
                }
                if (isLiveChat) {// 直播
                    contentViewHolder.nick_name.setVisibility(VISIBLE);
                    contentViewHolder.nick_name.setText(message.getFromUserName() + ":");
                    contentViewHolder.nick_name.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mMessageEventListener.onNickNameClick(message.getFromUserId());
                        }
                    });
                } else {
                    if (mRole == 1) {// 群主显示自己对该群员备注的名字
                        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mRoomId, message.getFromUserId());
                        if (member != null) {
                            contentViewHolder.nick_name.setText(member.getCardName());
                        } else {
                            // 群主不显示好友备注，只显示群内备注，不要搞乱了
                            contentViewHolder.nick_name.setText(message.getFromUserName());
                        }
                    } else {
                        if (mRemarksMap.containsKey(message.getFromUserId())) {// 群组内 我的好友 显示 我对他备注的名字
                            contentViewHolder.nick_name.setText(mRemarksMap.get(message.getFromUserId()));
                        } else {
                            contentViewHolder.nick_name.setText(message.getFromUserName());
                        }
                    }
                }
            }

            if (isShowMoreSelect) {
                contentViewHolder.chat_more_select.setVisibility(VISIBLE);
                contentViewHolder.mRootViewFather.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (message.getIsReadDel()) {
                            TipDialog tipDialog = new TipDialog(mContext);
                            tipDialog.setTip(getContext().getString(R.string.tip_cannot_multi_select_burn));
                            tipDialog.show();
                            return;
                        }

                        if (message.isMoreSelected) {
                            mChatMessages.get(position).setMoreSelected(false);
                        } else {
                            mChatMessages.get(position).setMoreSelected(true);
                        }
                    }
                });
                contentViewHolder.chat_more_select.setChecked(message.isMoreSelected); // 是否选中
            } else {
                contentViewHolder.chat_more_select.setVisibility(GONE);
            }
            View longView = null;

            /**
             *  处理具体显示的消息内容
             */
            switch (message.getType()) {
                case TYPE_NO_CONNECT_VIDEO: {
                    // ((CallViewHolder) contentViewHolder).chat_text.setText("已取消");
                    String content = null;
                    if (message.getTimeLen() == 0) {
                        content = InternationalizationHelper.getString("JXSip_Canceled") + " " + InternationalizationHelper.getString("JX_VideoChat");
                    } else {
                        content = InternationalizationHelper.getString("JXSip_noanswer");
                    }
                    ((CallViewHolder) contentViewHolder).chat_text.setText(content);
                    ((CallViewHolder) contentViewHolder).chat_img.setImageResource(R.drawable.ic_chat_end_conn);
                }
                longView = ((CallViewHolder) contentViewHolder).chat_text;
                break;
                case TYPE_END_CONNECT_VIDEO: {
                    // 结束
                    int timelen = message.getTimeLen();
                    ((CallViewHolder) contentViewHolder).chat_text.setText(InternationalizationHelper.getString("JXSip_finished") + " " + InternationalizationHelper.getString("JX_VideoChat") + "," +
                            InternationalizationHelper.getString("JXSip_timeLenth") + ":" + timelen + InternationalizationHelper.getString("JX_second"));
                    ((CallViewHolder) contentViewHolder).chat_img.setImageResource(R.drawable.ic_chat_end_conn);
                }
                longView = ((CallViewHolder) contentViewHolder).chat_text;
                break;
                case TYPE_NO_CONNECT_VOICE: {
                    String content = null;
                    if (message.getTimeLen() == 0) {
                        content = InternationalizationHelper.getString("JXSip_Canceled") + " " + InternationalizationHelper.getString("JX_VoiceChat");
                    } else {
                        content = InternationalizationHelper.getString("JXSip_noanswer");
                    }
                    ((CallViewHolder) contentViewHolder).chat_text.setText(content);
                    ((CallViewHolder) contentViewHolder).chat_img.setImageResource(R.drawable.ic_chat_no_conn);
                }
                longView = ((CallViewHolder) contentViewHolder).chat_text;
                break;
                case TYPE_END_CONNECT_VOICE: {
                    // 结束
                    int timelen = message.getTimeLen();
                    ((CallViewHolder) contentViewHolder).chat_text.setText(InternationalizationHelper.getString("JXSip_finished") + " " + InternationalizationHelper.getString("JX_VoiceChat") + "," +
                            InternationalizationHelper.getString("JXSip_timeLenth") + ":" + timelen + InternationalizationHelper.getString("JX_second"));
                    ((CallViewHolder) contentViewHolder).chat_img.setImageResource(R.drawable.ic_chat_no_conn);
                }
                longView = ((CallViewHolder) contentViewHolder).chat_text;
                break;
                case TYPE_IS_MU_CONNECT_VOICE: {
                    ((CallViewHolder) contentViewHolder).chat_text.setText(R.string.tip_invite_voice_meeting);
                    ((CallViewHolder) contentViewHolder).chat_img.setImageResource(R.drawable.ic_chat_no_conn);
                }
                longView = ((CallViewHolder) contentViewHolder).chat_text;
                break;
                case TYPE_IS_MU_CONNECT_Video: {
                    ((CallViewHolder) contentViewHolder).chat_text.setText(R.string.tip_invite_video_meeting);
                    ((CallViewHolder) contentViewHolder).chat_img.setImageResource(R.drawable.ic_chat_end_conn);
                }
                longView = ((CallViewHolder) contentViewHolder).chat_text;
                break;
                case XmppMessage.TYPE_TEXT: {
                    int size = PreferenceUtils.getInt(mContext, Constants.FONT_SIZE, 1);
                    ((TextViewHolder) contentViewHolder).chat_text.setTextSize(13 + size);

                    String s = StringUtils.replaceSpecialChar(message.getContent());
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);

                    if (message.getIsReadDel()) {// 阅后即焚
                        if (!message.isGroup() && !message.isMySend() && !message.isSendRead()) {
                            ((TextViewHolder) contentViewHolder).chat_text.setText(R.string.tip_click_to_read);
                        } else {
                            // 已经查看了，当适配器再次刷新的时候，不需要重新赋值
                            ((TextViewHolder) contentViewHolder).chat_text.setText(charSequence);
                        }
                    } else {
                        ((TextViewHolder) contentViewHolder).chat_text.setText(charSequence);
                    }
                }
                longView = ((TextViewHolder) contentViewHolder).chat_text;
                break;
                case XmppMessage.TYPE_VOICE: {
                    VoiceViewHolder voiceHolder = (VoiceViewHolder) contentViewHolder;
                    voiceHolder.chat_voice.fillData(message);
                    if (!message.isMySend()) {
                        if (voiceHolder.voice_progress != null) {
                            voiceHolder.voice_progress.setVisibility(View.GONE);
                        }

                        if (voiceHolder.unread_img_view != null) {
                            if (!message.isSendRead()) {
                                viewHolderMap.put(position, voiceHolder);
                                voiceHolder.unread_img_view.setVisibility(View.VISIBLE);
                            } else {
                                voiceHolder.unread_img_view.setVisibility(View.GONE);
                            }
                        }
                    }

                    // 是否要去下载
                    boolean voicefromDisk = false;
                    File voicefile = null;
                    String filePath = message.getFilePath();
                    if (!TextUtils.isEmpty(filePath)) {
                        voicefile = new File(filePath);
                        if (voicefile.exists()) {
                            voicefromDisk = true;
                        }
                    }
                    if (!voicefromDisk) {
                        Downloader.getInstance().addDownload(message.getContent(), voiceHolder.progress,
                                new VoiceDownloadListener(message));
                    }
                    longView = voiceHolder.chat_voice;
                }
                break;
                case XmppMessage.TYPE_GIF: {
                    GifViewHolder gifViewHolder = (GifViewHolder) contentViewHolder;
                    String gifName = message.getContent();
                    int resId = SmileyParser.Gifs.textMapId(gifName);
                    if (resId != -1) {
                        int margin = DisplayUtil.dip2px(mContext, 20);
                        RelativeLayout.LayoutParams paramsL = (RelativeLayout.LayoutParams) gifViewHolder.chat_gif_view
                                .getLayoutParams();
                        paramsL.setMargins(margin, 0, margin, 0);
                        gifViewHolder.chat_gif_view.setImageResource(resId);
                    } else {
                        gifViewHolder.chat_gif_view.setImageBitmap(null);
                    }
                    longView = gifViewHolder.chat_gif_view; //12
                }
                break;
                case XmppMessage.TYPE_IMAGE: {
                    boolean imageFromDisk = false;
                    File file = null;
                    int res;
                    if (message.isMySend()
                            || message.getFromUserId().equals(self.getUserId())) {
                        res = R.drawable.chat_bg_card_white;
                        String filePath = message.getFilePath();
                        if (!TextUtils.isEmpty(filePath)) {
                            file = new File(filePath);
                            if (file.exists()) {
                                imageFromDisk = true;
                            }
                        }
                    } else {
                        res = R.drawable.chat_bg_white_new;
                    }

                    final ImageViewHolder holder = (ImageViewHolder) contentViewHolder;

                    ViewGroup.LayoutParams mLayoutParams = holder.chat_image.getLayoutParams();
                    if (!TextUtils.isEmpty(message.getLocation_x())
                            || mImageMaps.containsKey(message.getPacketId())) {// 消息内带有图片宽高的 || 计算出宽高的
                        float x;
                        float y;
                        if (mImageMaps.containsKey(message.getPacketId())) {
                            String xy = mImageMaps.get(message.getPacketId());
                            String[] split = xy.split(",");
                            x = Float.parseFloat(split[0]);
                            y = Float.parseFloat(split[1]);
                        } else {
                            x = Float.parseFloat(message.getLocation_x());
                            y = Float.parseFloat(message.getLocation_y());
                        }
                        if (x - y >= 200) {// 16:9
                            x = 160;
                            y = 90;
                        } else if (y - x >= 200) {// 9:16
                            x = 90;
                            y = 160;
                        } else {// 1:1
                            x = 100;
                            y = 100;
                        }
                        mLayoutParams.width = DisplayUtil.dip2px(mContext,
                                x);
                        mLayoutParams.height = DisplayUtil.dip2px(mContext,
                                y);
                    } else {// 消息内未带图片宽高的，宽高统一为100、100
                        mLayoutParams.width = DisplayUtil.dip2px(mContext,
                                100);
                        mLayoutParams.height = DisplayUtil.dip2px(mContext,
                                100);
                    }
                    holder.chat_image.setLayoutParams(mLayoutParams);

                    try {
                        if (imageFromDisk) {// 加载本地动图与图片
                            if (message.getContent().endsWith(".gif")) {
                                GifDrawable gifFromFile = new GifDrawable(file);
                                holder.chat_image.setImageDrawable(gifFromFile);
                            } else {
                                Glide.with(mContext)
                                        .load(file)
                                        .transform(new CustomShapeTransformation(mContext, res))
                                        .error(R.drawable.fez)
                                        .into(new ImageLoadingFromUrlListener(holder.img_progress, holder.chat_image, message));
                            }
                        } else {// 加载网络动图与图片
                            if ((!TextUtils.isEmpty(message.getContent()))) {
                                if (message.getContent().endsWith(".gif")) {
                                    Downloader.getInstance().addDownload(message.getContent(), holder.img_progress,
                                            new FileDownloadListener(message, holder.chat_image));
                                } else {
                                    Glide.with(mContext)
                                            .load(message.getContent())
                                            .transform(new CustomShapeTransformation(mContext, res))
                                            .error(R.drawable.fez)
                                            .into(new ImageLoadingFromUrlListener(holder.img_progress, holder.chat_image, message));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 判断是否为阅后即焚类型的图片，如果是 模糊显示该图片
                    if (!is_group) {
                        if (message.getIsReadDel()) {
                            holder.chat_image.setAlpha(0.1f);
                        } else {
                            holder.chat_image.setAlpha(1.0f);
                        }
                    }

                    longView = holder.chat_warp_view;
                }
                break;
                case XmppMessage.TYPE_VIDEO: {
                    final VideoViewHolder videoViewHolder = (VideoViewHolder) contentViewHolder;
                    if (!message.isMySend() && videoViewHolder.video_progress != null) {
                        videoViewHolder.video_progress.setVisibility(View.GONE);
                        if (videoViewHolder.unread_img_view != null) {
                            if (!message.isSendRead()) {
                                videoViewHolder.unread_img_view.setVisibility(View.VISIBLE);
                            } else {
                                videoViewHolder.unread_img_view.setVisibility(View.GONE);
                            }
                        }
                    }
                    // 本地
                    boolean isExist = false;
                    File file;
                    String filePath = message.getFilePath();
                    if (!TextUtils.isEmpty(filePath)) {
                        file = new File(filePath);
                        if (file.exists()) {
                            isExist = true;
                        }
                    }

                    String mVideoFilePath = message.getFilePath();
                    String mVideoUrl = message.getContent();
                    if (isExist) {
                        videoViewHolder.chat_thumb.setUp(mVideoFilePath, JVCideoPlayerStandardSecond.SCREEN_LAYOUT_NORMAL, "");
                    } else {
                        videoViewHolder.chat_thumb.setUp(mVideoUrl, JVCideoPlayerStandardSecond.SCREEN_LAYOUT_NORMAL, "");
                    }

                    if (isExist) {
                        AvatarHelper.getInstance().displayVideoThumb(filePath, videoViewHolder.chat_thumb.thumbImageView);
                    } else {// 下载视频
                        videoViewHolder.chat_thumb.setTag(message.get_id());// 设置Tag，防止在下载完成设置图片的时候，Item被其他视图回收使用了，覆盖其他的视图了
                        Downloader.getInstance().addDownload(mVideoUrl, videoViewHolder.video_progress,
                                new VideoDownloadListener(message, videoViewHolder.chat_thumb, videoViewHolder.chat_thumb.thumbImageView));
                    }

                    if (!is_group
                            && !message.isMySend()
                            && message.getIsReadDel()) {
                        Intent broadcast = new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
                        broadcast.putExtra("isVideo", true);
                        broadcast.putExtra("mVideoFilePath", mVideoFilePath);
                        broadcast.putExtra("mVideoUrl", mVideoUrl);
                        mContext.sendBroadcast(broadcast);
                    }

                    longView = videoViewHolder.chat_warp_view;
                }
                break;
                case XmppMessage.TYPE_FILE:
                    FileViewHolder fileViewHolder = (FileViewHolder) contentViewHolder;
                    if (!message.isMySend()) {
                        if (fileViewHolder.file_progress != null) {
                            fileViewHolder.file_progress.setVisibility(View.GONE);
                        }
                        if (fileViewHolder.unread_img_view != null) {
                            if (!message.isSendRead()) {
                                fileViewHolder.unread_img_view.setVisibility(View.VISIBLE);
                            } else {
                                fileViewHolder.unread_img_view.setVisibility(View.GONE);
                            }
                        }
                    }
                    String filePath = message.getFilePath();
                    if (TextUtils.isEmpty(filePath)) {// 基本不存在
                        filePath = message.getContent();
                        // 文件名
                        int start = filePath.lastIndexOf("/");
                        String name = filePath.substring(start + 1).toLowerCase();
                        fileViewHolder.chat_file_name.setText(name);
                    } else {
                        // 文件名
                        int start = filePath.lastIndexOf("/");
                        String name = filePath.substring(start + 1).toLowerCase();
                        fileViewHolder.chat_file_name.setText(name);

                        File file = new File(filePath);
                        if (!file.exists()) {// 文件不存在，即可判断该文件不是我发送的
                            filePath = message.getContent();
                        }
                    }
                    // 取出后缀
                    int pointIndex = filePath.lastIndexOf(".");
                    if (pointIndex != -1) {
                        String type = filePath.substring(pointIndex + 1).toLowerCase();
                        if (type.equals("png") || type.equals("jpg") || type.equals("gif")) {
                            Glide.with(getContext()).load(filePath).override(100, 100).into(fileViewHolder.chat_warp_file);
                            message.setTimeLen(1);
                        } else {
                            fillFileInco(type, fileViewHolder.chat_warp_file, message);
                        }
                    }

                    longView = fileViewHolder.relativeLayout;
                    break;
                case XmppMessage.TYPE_LOCATION: {
                    final LocationViewHolder locationViewHolder = (LocationViewHolder) contentViewHolder;
                    if (!TextUtils.isEmpty(message.getContent())) {
                        locationViewHolder.chat_location.setVisibility(View.VISIBLE);
                        // 加载地图缩略图与位置
                        locationViewHolder.chat_address.setText(message.getObjectId());
                        float[] radius = {16.0f, 16.0f, 16.0f, 16.0f, 0.0f, 0.0f, 0.0f, 0.0f};
                        locationViewHolder.chat_img_view.setRadius(radius);
                        final MapHelper.LatLng latLng = new MapHelper.LatLng(Double.valueOf(message.getLocation_x()), Double.valueOf(message.getLocation_y()));
                        // 直接展示消息里带的地址，也就是发送方地图截图上传的url,
                        String url = message.getContent();
                        AvatarHelper.getInstance().displayUrl(url, locationViewHolder.chat_img_view);
                        locationViewHolder.chat_location.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                if(!UiUtils.isNormalClick()){
                                    return;
                                }
                                Intent intent = new Intent(mContext, MapActivity.class);
                                intent.putExtra("latitude", latLng.getLatitude());
                                intent.putExtra("longitude", latLng.getLongitude());
                                intent.putExtra("address", message.getObjectId());
                                mContext.startActivity(intent);
                            }
                        });
                    } else {
                        locationViewHolder.chat_location.setVisibility(View.GONE);
                    }
                    longView = locationViewHolder.chat_location;
                }
                break;
                case XmppMessage.TYPE_CARD:
                    CardViewHolder cardViewHolder = (CardViewHolder) contentViewHolder;
                    if (!TextUtils.isEmpty(message.getContent())) {
                        cardViewHolder.chat_head_iv.setVisibility(View.VISIBLE);
                        AvatarHelper.getInstance().displayAvatar(message.getObjectId(), cardViewHolder.chat_warp_head, true);
                        cardViewHolder.chat_person_name.setText("" + message.getContent());
                        if (!message.isMySend()) {
                            if (cardViewHolder.card_progress != null) {
                                cardViewHolder.card_progress.setVisibility(View.GONE);
                            }
                            if (cardViewHolder.unread_img_view != null) {
                                if (!message.isSendRead()) {
                                    cardViewHolder.unread_img_view.setVisibility(View.VISIBLE);
                                } else {
                                    cardViewHolder.unread_img_view.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                    longView = cardViewHolder.relativeLayout;
                    break;
                case XmppMessage.TYPE_RED: {
                    String s = StringUtils.replaceSpecialChar(message.getContent());
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
                    ((RedViewHolder) contentViewHolder).chat_text.setText(charSequence);
                    longView = ((RedViewHolder) contentViewHolder).relativeLayout;
                }
                break;
                case XmppMessage.TYPE_LINK: {
                    LocationViewHolder holder = (LocationViewHolder) contentViewHolder;
                    String mSourceData = message.getContent();
                    fillLinkData(holder, mSourceData, message.getPacketId());
                    longView = holder.chat_location;
                }
                break;
                case XmppMessage.TYPE_SHARE_LINK: {
                    LinkViewHolder holder = (LinkViewHolder) contentViewHolder;
                    String mSourceData = message.getObjectId();// 获取数据JSON
                    fillShareLinkData(holder, mSourceData, message.getPacketId());
                    longView = holder.link_text_tv;
                }
                break;
                case XmppMessage.TYPE_IMAGE_TEXT: // 单条图文 注：单条图文、多条图文、戳一戳暂不作长按处理
                    String data = message.getContent();
                    TextImgViewHolder textImgHolder = (TextImgViewHolder) contentViewHolder;
                    fillTextImgDatas(textImgHolder, data);
                    break;
                case XmppMessage.TYPE_IMAGE_TEXT_MANY: // 多条图文
                    String data2 = message.getContent();
                    TextImgManyHolder textImgManyHolder = (TextImgManyHolder) contentViewHolder;
                    fillTextImgDatas(textImgManyHolder, data2);
                    break;
                case XmppMessage.TYPE_SHAKE:
                    if (message.isMySend()) {
                        ((ShakeHolder) contentViewHolder).shake_image.setBackgroundResource(R.drawable.shake_frame);
                    } else {
                        ((ShakeHolder) contentViewHolder).shake_image.setBackgroundResource(R.drawable.shake_frame_f);
                    }
                    if (!message.isDownload()) {
                        AnimationDrawable animationDrawable = (AnimationDrawable) ((ShakeHolder) contentViewHolder).shake_image.getBackground();
                        if (animationDrawable.isRunning()) {
                            animationDrawable.stop();
                        }
                        animationDrawable.start();
                    }
                    message.setDownload(true);
                    mChatMessages.remove(position);
                    mChatMessages.add(position, message);
                    if (message.isMySend()) {
                        ChatMessageDao.getInstance().updateMessageShakeState(self.getUserId(), message.getToUserId(), message.getPacketId());
                    } else {
                        ChatMessageDao.getInstance().updateMessageShakeState(self.getUserId(), message.getFromUserId(), message.getPacketId());
                    }
                    break;
                case XmppMessage.TYPE_CHAT_HISTORY:
                    ChatHistoryHolder chatHistoryHolder = (ChatHistoryHolder) contentViewHolder;
                    String detail = message.getContent();
                    List<String> mStringHistory = JSON.parseArray(detail, String.class);
                    List<ChatMessage> mChatMessageHistory = new ArrayList<>();
                    for (int i = 0; i < mStringHistory.size(); i++) {
                        ChatMessage chatMessage = new ChatMessage(mStringHistory.get(i));// 解析json,还原ChatMessage
                        mChatMessageHistory.add(chatMessage);
                    }
                    chatHistoryHolder.chat_title.setText(message.getObjectId());
                    // 支持显示本地表情
                    String s = StringUtils.replaceSpecialChar(getMsgContent(mChatMessageHistory.get(0)));
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
                    chatHistoryHolder.chat_tv1.setText(charSequence);
                    if (mChatMessageHistory.size() < 2) {// 只有一条聊天记录
                        chatHistoryHolder.chat_tv2.setVisibility(GONE);
                        chatHistoryHolder.chat_tv3.setVisibility(GONE);
                    } else if (mChatMessageHistory.size() < 3) {
                        String s1 = StringUtils.replaceSpecialChar(getMsgContent(mChatMessageHistory.get(1)));
                        CharSequence charSequence1 = HtmlUtils.transform200SpanString(s1.replaceAll("\n", "\r\n"), true);
                        chatHistoryHolder.chat_tv2.setText(charSequence1);
                        chatHistoryHolder.chat_tv2.setVisibility(VISIBLE);
                        chatHistoryHolder.chat_tv3.setVisibility(GONE);
                    } else if (mChatMessageHistory.size() >= 3) {
                        String s1 = StringUtils.replaceSpecialChar(getMsgContent(mChatMessageHistory.get(1)));
                        CharSequence charSequence1 = HtmlUtils.transform200SpanString(s1.replaceAll("\n", "\r\n"), true);
                        String s2 = StringUtils.replaceSpecialChar(getMsgContent(mChatMessageHistory.get(2)));
                        CharSequence charSequence2 = HtmlUtils.transform200SpanString(s2.replaceAll("\n", "\r\n"), true);
                        chatHistoryHolder.chat_tv2.setText(charSequence1);
                        chatHistoryHolder.chat_tv3.setText(charSequence2);
                        chatHistoryHolder.chat_tv2.setVisibility(VISIBLE);
                        chatHistoryHolder.chat_tv3.setVisibility(VISIBLE);
                    }
                    longView = ((ChatHistoryHolder) contentViewHolder).chat_warp_view;
                    break;
            }
            // 设置长按弹出复制,转发窗口
            if (longView != null) {
                setLongClickInterface(message, longView, position);
            }

            /**
             * ToDo something you can do here
             * 处理点击事件的监听
             */
            if (viewType == VIEW_FROM_ME_TEXT || viewType == VIEW_TO_ME_TEXT) {
                final TextViewHolder textViewHolder = (TextViewHolder) contentViewHolder;
                if (!isLiveChat) {
                    if (HttpUtil.isURL(message.getContent())) {
                        textViewHolder.chat_text.setTextColor(mContext.getResources().getColor(R.color.link_color));
                    } else {
                        textViewHolder.chat_text.setTextColor(mContext.getResources().getColor(R.color.black));
                    }

                    if (message.getIsReadDel() && !message.isGroup() && !message.isMySend() && !message.isSendRead()) {
                        textViewHolder.chat_text.setTextColor(mContext.getResources().getColor(R.color.redpacket_bg));
                    }

                    if (textViewHolder.read_time != null) {
                        textViewHolder.read_time.setVisibility(GONE);
                    }


                    textViewHolder.chat_text.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if ((!message.isGroup()
                                    && !message.isMySend()
                                    && message.getIsReadDel()
                                    && !message.isSendRead())) {// 非群组、对方发送、阅后即焚类型、未发送已读 才可以点击
                                textViewHolder.chat_text.setTextColor(mContext.getResources().getColor(R.color.black));
                                String s = StringUtils.replaceSpecialChar(message.getContent());
                                CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
                                textViewHolder.chat_text.setText(charSequence);
                                textViewHolder.read_time.setVisibility(View.VISIBLE);

                                textViewHolder.chat_text.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendReadMessage(message);// 发送已读消息回执，对方收到后会删除该条阅后即焚消息
                                        long time = textViewHolder.chat_text.getLineCount() * 10000;// 计算时间，一行10s

                                        CountDownTimer mCountDownTimer = mTextBurningMaps.get(message.getPacketId());
                                        if (mCountDownTimer != null) { // 理论上不可能
                                            mCountDownTimer.cancel();// 取消上一个事件
                                            mTextBurningMaps.remove(message.getPacketId());
                                        }

                                        CountDownTimer mNewCountDownTimer = new CountDownTimer(time, 1000) {
                                            @Override
                                            public void onTick(long millisUntilFinished) {
                                                textViewHolder.read_time.setText(String.valueOf(millisUntilFinished / 1000));
                                                message.setReadTime(millisUntilFinished);
                                                ChatMessageDao.getInstance().updateMessageReadTime(self.getUserId(), message.getFromUserId(),
                                                        message.getPacketId(), millisUntilFinished);
                                            }

                                            @Override
                                            public void onFinish() {
                                                mTextBurningMaps.remove(message.getPacketId());

                                                Intent broadcast = new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
                                                broadcast.putExtra("TEXT_READ_FIRE_TYPE", true);
                                                broadcast.putExtra("FRIEND_ID", mToUserId);
                                                broadcast.putExtra("TEXT_READ_FIRE_PACKET", message.getPacketId());
                                                mContext.sendBroadcast(broadcast);
                                            }
                                        }.start();
                                        mTextBurningMaps.put(message.getPacketId(), mNewCountDownTimer);
                                    }
                                });
                            }

                            String content = message.getContent();
                            if (HttpUtil.isURL(content)) {
                                List<String> mURLList = HttpUtil.getURLList(content);
                                if (mURLList.size() > 1) {// 有多个URL，弹窗选择跳转
                                    SelectURLDialog mSelectURLDialog = new SelectURLDialog(mContext, mURLList, new SelectURLDialog.OnURLListItemClickListener() {
                                        @Override
                                        public void onURLItemClick(String URL) {
                                            intentWebView(URL, message.getPacketId());
                                        }
                                    });
                                    mSelectURLDialog.show();
                                } else {// 单个URL,直接跳转
                                    intentWebView(mURLList.get(0), message.getPacketId());
                                }
                            }
                        }
                    });
                }
            } else if (viewType == VIEW_FROM_ME_MEDIA_CALL || viewType == VIEW_TO_ME_MEDIA_CALL) {
                CallViewHolder callViewHolder = (CallViewHolder) contentViewHolder;
                callViewHolder.chat_text.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMessageEventListener != null) {
                            mMessageEventListener.onCallListener(message.getType());
                        }
                    }
                });
            } else if (viewType == VIEW_FROM_ME_VOICE || viewType == VIEW_TO_ME_VOICE) {
                final VoiceViewHolder voiceHolder = (VoiceViewHolder) contentViewHolder;
                voiceHolder.chat_warp_view.setOnClickListener(null);
                if (!message.isMySend() && message.isSendRead() && message.getIsReadDel()) {
                    voiceHolder.chat_voice.setOnClickListener(null);
                } else {
                    voiceHolder.chat_voice.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mCurrtPos = position;
                            VoicePlayer.instance().playVoice(voiceHolder.chat_voice);

                            // 一条对方的没有点击过的消息，第一次点击之后去掉未读的标记(小红点),标记为已读
                            if (!message.isMySend() && !message.isSendRead()) {
                                sendReadMessage(message); // 语音的已读消息
                                if (voiceHolder.unread_img_view != null) {
                                    voiceHolder.unread_img_view.setVisibility(View.GONE);
                                }
                                if (message.getIsReadDel()) {// 阅后即焚消息需要删除
                                    ChatMessageDao.getInstance().deleteSingleChatMessage(self.getUserId(), message.getFromUserId(), message.getPacketId());
                                }
                            }
                        }
                    });
                }
            } else if (viewType == VIEW_FROM_ME_IMAGE || viewType == VIEW_TO_ME_IMAGE) {
                ImageViewHolder imageViewHolder = (ImageViewHolder) contentViewHolder;
                imageViewHolder.chat_warp_view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (message.getIsReadDel() && !message.isSendRead()) {
                            sendReadMessage(message);// 发送已读
                        }
                        Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                        intent.putExtra(AppConstant.EXTRA_IMAGE_URI, message.getContent());
                        intent.putExtra("image_path", message.getFilePath());
                        if (!is_group) {
                            intent.putExtra("isReadDel", message.getIsReadDel());
                            intent.putExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, position);
                        }
                        mContext.startActivity(intent);
                    }
                });
            } else if (viewType == VIEW_FROM_ME_VIDEO || viewType == VIEW_TO_ME_VIDEO) {
                final VideoViewHolder videoViewHolder = (VideoViewHolder) contentViewHolder;
                videoViewHolder.chat_thumb.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {// 记录当前点击的坐标
                        X = (int) event.getX();
                        Y = (int) event.getY();
                        return false;
                    }
                });

                videoViewHolder.chat_thumb.addChatClicklistener(new JVCideoPlayerStandardforchat.JVOnClicklistener() {

                    @Override
                    public void onLongClick() {
                        if (isShowMoreSelect) {// 多选状态长按不做操作
                            return;
                        }

                        mChatPpWindow = new ChatTextClickPpWindow(mContext, new ClickListener(message, position),
                                message, mToUserId, course, is_group, false, mRole);

                        int xoffSet = X - mChatPpWindow.getWidth() / 2;
                        // mChatBottomView的高度是可变的，选择Edit来计算
                        // int yoffSet = 0 - (v.getHeight() - Y) - mChatBottomView.getHeight();
                        int yoffSet = 0 - (videoViewHolder.chat_thumb.getHeight() - Y) - mChatBottomView.getmChatEdit().getHeight()
                                - DisplayUtil.dip2px(mContext, 12);// 再向上偏移12dp
                        mChatPpWindow.showAsDropDown(videoViewHolder.chat_thumb, xoffSet, yoffSet);
                    }

                    @Override
                    public void onClick() {
                        if (!message.isMySend() && !message.isSendRead()) {
                            sendReadMessage(message); // 做已读回执
                            if (videoViewHolder.unread_img_view != null) {
                                videoViewHolder.unread_img_view.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            } else if (viewType == VIEW_FROM_ME_FILE || viewType == VIEW_TO_ME_FILE) {
                final FileViewHolder fileViewHolder = (FileViewHolder) contentViewHolder;
                fileViewHolder.relativeLayout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!message.isMySend() && !message.isSendRead()) {
                            sendReadMessage(message); // 文件的已读消息
                            if (fileViewHolder.unread_img_view != null) {
                                fileViewHolder.unread_img_view.setVisibility(View.GONE);
                            }
                        }

                        MucFileBean data = new MucFileBean();
                        String url = message.getContent();
                        String filePath = message.getFilePath();
                        if (TextUtils.isEmpty(filePath)) {
                            filePath = url;
                        }
                        int size = message.getFileSize();

                        // 取出文件名称
                        int start = filePath.lastIndexOf("/");
                        String name = filePath.substring(start + 1).toLowerCase();

                        data.setNickname(name);
                        data.setUrl(url);
                        data.setName(name);
                        data.setSize(size);
                        data.setState(DownManager.STATE_UNDOWNLOAD);
                        data.setType(message.getTimeLen());
                        Intent intent = new Intent(mContext, MucFileDetails.class);
                        intent.putExtra("data", data);
                        mContext.startActivity(intent);
                    }
                });
            } else if (viewType == VIEW_TO_ME_CARD || viewType == VIEW_FROM_ME_CARD) {
                final CardViewHolder cardViewHolder = (CardViewHolder) contentViewHolder;
                cardViewHolder.relativeLayout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!message.isMySend() && !message.isSendRead()) {
                            sendReadMessage(message); // 个人名片的已读回执
                            if (cardViewHolder.unread_img_view != null) {
                                cardViewHolder.unread_img_view.setVisibility(View.GONE);
                            }
                        }
                        Intent intent = new Intent(mContext, BasicInfoActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, message.getObjectId());
                        mContext.startActivity(intent);
                    }
                });
            } else if (viewType == VIEW_FROM_ME_RED || viewType == VIEW_FROM_ME_RED_KEY) {
                RedViewHolder redViewHolder = (RedViewHolder) contentViewHolder;
                redViewHolder.relativeLayout.setOnClickListener(new NoDoubleClickListener() {
                    @Override
                    public void onNoDoubleClick(View view) {
                        clickRedpacket(message, CLICK_RED_TYPE_TO);
                    }
                });
            } else if (viewType == VIEW_TO_ME_RED || viewType == VIEW_TO_ME_RED_KEY) {
                RedViewHolder redViewHolder = (RedViewHolder) contentViewHolder;
                redViewHolder.relativeLayout.setOnClickListener(new NoDoubleClickListener() {
                    @Override
                    public void onNoDoubleClick(View view) {
                        clickRedpacket(message, CLICK_RED_TYPE_FROM);
                    }
                });
            } else if (viewType == VIEW_FROM_ME_SHAKE || viewType == VIEW_TO_ME_SHAKE) {
                ShakeHolder shakeHolder = (ShakeHolder) contentViewHolder;
                shakeHolder.shake_image.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventBus.getDefault().post(new MessageEventClickable(message));
                    }
                });
            } else if (viewType == VIEW_FROM_ME_CHAT_HISTORY || viewType == VIEW_TO_ME_CHAT_HISTORY) {
                ChatHistoryHolder chatHistoryHolder = (ChatHistoryHolder) contentViewHolder;
                chatHistoryHolder.chat_warp_view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, ChatHistoryActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, mToUserId);
                        intent.putExtra(AppConstant.EXTRA_MSG_ID, message.getPacketId());
                        mContext.startActivity(intent);
                    }
                });
            }

            // 删除视图的淡出效果，
            final View view = convertView;
            view.setAlpha(1f);
            if (mDeletedChatMessageId.contains(message.getPacketId())) {
                Animation anim = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        view.setAlpha(1f - interpolatedTime);
                    }
                };
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mChatMessages.remove(message);
                        notifyDataSetChanged();
                        view.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                anim.setDuration(1000);
                view.startAnimation(anim);
            }

            return convertView;
        }

        private void setLongClickInterface(final ChatMessage message, View view,
                                           final int position) {
            view.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {// 记录当前点击的坐标
                    X = (int) event.getX();
                    Y = (int) event.getY();
                    return false;
                }
            });

            view.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    if (isLiveChat || isShowMoreSelect) {// 当前为直播 || 多选状态，长按不处理
                        return false;
                    }

                    /*Vibrator vib = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
                    vib.vibrate(40);// 只震动一秒，一次*/

                    mChatPpWindow = new ChatTextClickPpWindow(mContext, new ClickListener(message, position),
                            message, mToUserId, course, is_group, false, mRole);

                    int xoffSet = X - mChatPpWindow.getWidth() / 2;
                    // mChatBottomView的高度是可变的，选择Edit来计算
                    int yoffSet;
                    if (mChatBottomView == null) {// 我的讲课无mChatBottomView
                        yoffSet = 0 - (v.getHeight() - Y) - DisplayUtil.dip2px(mContext, 12);// 再向上偏移12dp
                    } else {
                        yoffSet = 0 - (v.getHeight() - Y) - mChatBottomView.getmChatEdit().getHeight()
                                - DisplayUtil.dip2px(mContext, 12);// 再向上偏移12dp
                    }

                    mChatPpWindow.showAsDropDown(v, xoffSet, yoffSet);
                    return false;
                }
            });
        }
    }

    public class TextImgBean {
        public String title;
        public String img;
        public String url;
    }

    public class ClickListener implements OnClickListener {
        private ChatMessage message;
        private int position;

        public ClickListener(ChatMessage message, int position) {
            this.message = message;
            this.position = position;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onClick(View v) {
            mChatPpWindow.dismiss();
            if(!UiUtils.isNormalClick()){
                return;
            }
            switch (v.getId()) {
                case R.id.item_chat_copy_tv:
                    // 复制
                    if (message.getIsReadDel()) {
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setTip(getContext().getString(R.string.tip_cannot_copy_burn));
                        tipDialog.show();
                        return;
                    }
                    String s = StringUtils.replaceSpecialChar(message.getContent());
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
                    // 获得剪切板管理者,复制文本内容
                    ClipboardManager cmb = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cmb.setText(charSequence);
                    break;
                case R.id.item_chat_relay_tv:
                    // 转发消息
                    if (message.getIsReadDel()) {
                        // 为阅后即焚类型的消息，不可转发
                        // Toast.makeText(mContext, "阅后即焚类型的消息不可以转发哦", Toast.LENGTH_SHORT).show();
                        Toast.makeText(mContext, InternationalizationHelper.getString("CANNOT_FORWARDED"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(mContext, InstantMessageActivity.class);
                    intent.putExtra("fromUserId", mToUserId);
                    intent.putExtra("messageId", message.getPacketId());
                    mContext.startActivity(intent);
                    ((Activity) mContext).finish();
                    break;
                case R.id.item_chat_collection_tv:
                    // 添加为表情
                    if (message.getIsReadDel()) {
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setTip(getContext().getString(R.string.tip_cannot_save_burn_image));
                        tipDialog.show();
                        return;
                    }
                    collectionEmotion(message, false);
                    break;
                case R.id.collection_other:
                    // 收藏
                    if (message.getIsReadDel()) {
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setTip(getContext().getString(R.string.tip_cannot_collect_burn));
                        tipDialog.show();
                        return;
                    }
                    collectionEmotion(message, true);
                    break;
                case R.id.item_chat_back_tv:
                    // 撤回消息
                    mMessageEventListener.onMessageBack(message, position);
                    break;
                case R.id.item_chat_del_tv:
                    // 删除
                    if (course) {
                        if (mMessageEventListener != null) {
                            mMessageEventListener.onMessageClick(message);
                        }
                    } else {
                        // 发送广播去界面更新
                        Intent broadcast = new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
                        broadcast.putExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, position);
                        mContext.sendBroadcast(broadcast);
                    }
                    break;
                case R.id.item_chat_more_select:
                    // 多选
                    Intent showIntent = new Intent(Constants.SHOW_MORE_SELECT_MENU);
                    showIntent.putExtra(Constants.CHAT_SHOW_MESSAGE_POSITION, position);
                    mContext.sendBroadcast(showIntent);
                    break;
                default:
                    break;
            }
        }
    }

    class VoiceDownloadListener implements DownloadListener {
        private ChatMessage message;

        public VoiceDownloadListener(ChatMessage message) {
            this.message = message;
        }

        @Override
        public void onStarted(String uri, View view) {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailed(String uri, com.sk.weichat.downloader.FailReason failReason, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        @Override
        public void onComplete(String uri, String filePath, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
            message.setFilePath(filePath);
            ChatMessageDao.getInstance().updateMessageDownloadState(self.getUserId(), mToUserId, message.get_id(), true,
                    filePath);
        }

        @Override
        public void onCancelled(String uri, View view) {

        }
    }

    class ImageLoadingFromUrlListener extends GlideDrawableImageViewTarget {
        private ProgressBar mProgressBar;
        private ImageView mImageView;
        private ChatMessage mChatMessage;

        public ImageLoadingFromUrlListener(ProgressBar progressBar, ImageView imageview, ChatMessage chatMessage) {
            super(imageview);
            this.mProgressBar = progressBar;
            this.mImageView = imageview;
            this.mChatMessage = chatMessage;
        }

        @Override
        public void onLoadStarted(Drawable placeholder) {
        }

        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            mImageView.setImageResource(R.drawable.fez);
        }

        @Override
        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
            super.onResourceReady(resource, glideAnimation);

            if (TextUtils.isEmpty(mChatMessage.getLocation_x())
                    && !mImageMaps.containsKey(mChatMessage.getPacketId())) {
                Log.e("zq", String.valueOf(resource.getIntrinsicWidth()) + "，" + String.valueOf(resource.getIntrinsicHeight()));

                mImageMaps.put(mChatMessage.getPacketId(),
                        String.valueOf(resource.getIntrinsicWidth()) + "," + String.valueOf(resource.getIntrinsicHeight()));
                // 更新数据库
                if (mChatMessage.isMySend()) {
                    ChatMessageDao.getInstance().updateMessageLocationXY(CoreManager.requireSelf(getContext()).getUserId(), mChatMessage.getToUserId(), mChatMessage.getPacketId()
                            , String.valueOf(resource.getIntrinsicWidth()), String.valueOf(resource.getIntrinsicHeight()));
                } else {
                    ChatMessageDao.getInstance().updateMessageLocationXY(CoreManager.requireSelf(getContext()).getUserId(), mChatMessage.getFromUserId(), mChatMessage.getPacketId()
                            , String.valueOf(resource.getIntrinsicWidth()), String.valueOf(resource.getIntrinsicHeight()));
                }
            }

            // setSelection(mChatMessages.size());
        }
    }

    class VideoDownloadListener implements DownloadListener {
        private ChatMessage message;
        private JVCideoPlayerStandardforchat jvCideoPlayerStandardforchat;
        private ImageView imageView;

        public VideoDownloadListener(ChatMessage message, JVCideoPlayerStandardforchat jvCideoPlayerStandardforchat, ImageView imageView) {
            this.message = message;
            this.jvCideoPlayerStandardforchat = jvCideoPlayerStandardforchat;
            this.imageView = imageView;
        }

        @Override
        public void onStarted(String uri, View view) {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailed(String uri, com.sk.weichat.downloader.FailReason failReason, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        @SuppressLint("NewApi")
        @Override
        public void onComplete(String uri, String filePath, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
            message.setFilePath(filePath);
            ChatMessageDao.getInstance().updateMessageDownloadState(self.getUserId(), mToUserId, message.get_id(), true, filePath);
            if (jvCideoPlayerStandardforchat != null) {
                jvCideoPlayerStandardforchat.setUp(filePath, JVCideoPlayerStandardSecond.SCREEN_LAYOUT_NORMAL, "");
            }
            if (imageView != null) {
                AvatarHelper.getInstance().displayVideoThumb(filePath, imageView);
            }
        }

        @Override
        public void onCancelled(String uri, View view) {

        }
    }

    class FileDownloadListener implements DownloadListener {
        private ChatMessage message;
        private ImageView imageView;

        public FileDownloadListener(ChatMessage message, ImageView imageView) {
            this.message = message;
            this.imageView = imageView;
        }

        @Override
        public void onStarted(String uri, View view) {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailed(String uri, com.sk.weichat.downloader.FailReason failReason, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        @Override
        public void onComplete(String uri, String filePath, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
            message.setFilePath(filePath);
            ChatMessageDao.getInstance().updateMessageDownloadState(self.getUserId(), mToUserId, message.get_id(), true,
                    filePath);
            if (imageView != null) { // 加载Gif
                File file = new File(filePath);
                try {
                    GifDrawable gif = new GifDrawable(file);
                    imageView.setImageDrawable(gif);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onCancelled(String uri, View view) {

        }
    }

    /***************
     * ViewHolder
     *****************************/
    class SystemViewHolder {
        TextView chat_time_tv;
        TextView chat_content_tv;
        TextView chat_nick_name;
    }

    abstract class ContentViewHolder {
        TextView time_tv; // 时间控件
        HeadView chat_head_iv;
        TextView nick_name;
        CheckBox chat_more_select; // 多选时显示
        ProgressBar progress; // 只有From的item有
        ImageView failed_img_view; // 只有From的item有
        TextView imageview_read; // 显示是否已读状态的图标
        FrameLayout flStateclick; // 聊天状态的点击
        View mRootView; // 内容显示布局
        LinearLayout mRootViewFather; // item总布局
        ImageView ivFire; // 阅后即焚小图标，
    }

    class TextViewHolder extends ContentViewHolder {
        TextView chat_text;
        // 阅后即焚文本消息专属;
        TextView read_time;
    }

    class VoiceViewHolder extends ContentViewHolder {
        LinearLayout chat_warp_view;
        VoiceAnimView chat_voice;
        ProgressBar voice_progress;// 只有To_me才有
        ImageView unread_img_view; // 只有To_me才有
    }

    class GifViewHolder extends ContentViewHolder {
        GifImageView chat_gif_view;
    }

    class ImageViewHolder extends ContentViewHolder {
        FrameLayout chat_warp_view;
        ImageView chat_image;
        ProgressBar img_progress;
    }

    class VideoViewHolder extends ContentViewHolder {
        FrameLayout chat_warp_view;
        JVCideoPlayerStandardforchat chat_thumb;
        ProgressBar video_progress;// 只有To_me才有
        ImageView unread_img_view; // 只有To_me才有
    }

    class CallViewHolder extends ContentViewHolder {
        TextView chat_text;
        ImageView chat_img;
        LinearLayout chat_warp_view;
    }

    class FileViewHolder extends ContentViewHolder {
        RelativeLayout relativeLayout;
        ImageView chat_warp_file;
        TextView chat_file_name;
        ProgressBar file_progress;// 只有To_me才有
        ImageView unread_img_view;// 只有To_me才有
    }

    class LocationViewHolder extends ContentViewHolder {
        LinearLayout chat_location;
        RoundView chat_img_view;
        TextView chat_address;
    }

    class CardViewHolder extends ContentViewHolder {
        RelativeLayout relativeLayout;
        ImageView chat_warp_head;
        TextView chat_person_name;
        TextView chat_person_sex;
        ProgressBar card_progress;// 只有To_me才有
        ImageView unread_img_view;// 只有To_me才有
    }

    class RedViewHolder extends ContentViewHolder {
        FrameLayout relativeLayout;
        ImageView chat_warp_head;
        TextView chat_text;
    }

    class LinkViewHolder extends ContentViewHolder {
        TextView link_app_name_tv;
        ImageView link_app_icon_iv;
        TextView link_title_tv;
        TextView link_text_tv;
        ImageView link_iv;
    }

    class ShakeHolder extends ContentViewHolder {
        FrameLayout chat_warp_view;
        ImageView shake_image;
    }

    class ChatHistoryHolder extends ContentViewHolder {
        RelativeLayout chat_warp_view;
        TextView chat_title;
        TextView chat_tv1;
        TextView chat_tv2;
        TextView chat_tv3;
    }

    /**
     * 单条图文
     */
    class TextImgViewHolder extends ContentViewHolder {
        TextView tvTitle;  // 主标题
        TextView tvText;   // 副标题
        ImageView ivImg; // 图像
        LinearLayout llRootView; // 根视图
    }

    /**
     * 多条图文
     */
    class TextImgManyHolder extends ContentViewHolder {
        TextView tvTitle;
        ImageView ivImg;
        MyListView listView;
        RelativeLayout llRootView; // 第一个标题的根视图
    }
}

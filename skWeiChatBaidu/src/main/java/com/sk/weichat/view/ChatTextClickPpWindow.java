package com.sk.weichat.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.course.ChatRecordHelper;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.util.TimeUtils;

import static com.sk.weichat.bean.message.XmppMessage.TYPE_EXIT_VOICE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_FILE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IMAGE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_IS_CONNECT_VOICE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_RED;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_TEXT;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_TRANSFER;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_VIDEO;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_VOICE;


/**
 * 聊天消息长按事件
 */
public class ChatTextClickPpWindow extends PopupWindow {
    private View mMenuView;
    private TextView tvCopy;
    private TextView tvRelay;
    private TextView tvCollection;// 存表情
    private TextView tvCollectionOther; // 收藏其他类型的消息
    private TextView tvBack;
    private TextView tvDel;
    private TextView tvMoreSelected;
    // 开始 & 停止录制
//    private TextView tvRecord;

    private int mWidth, mHeight;

    private boolean isGroup;
    private boolean isDevice;
    private int mRole;

    public ChatTextClickPpWindow(Context context, View.OnClickListener listener,
                                 final ChatMessage type, final String toUserId, boolean course,
                                 boolean group, boolean device, int role) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        mMenuView = inflater.inflate(R.layout.item_chat_long_click, null);

        this.isGroup = group;
        this.isDevice = device;
        this.mRole = role;

        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mMenuView.measure(w, h);
        // 获取PopWindow宽和高
        mHeight = mMenuView.getMeasuredHeight();
        mWidth = mMenuView.getMeasuredWidth();

        tvCopy = (TextView) mMenuView.findViewById(R.id.item_chat_copy_tv);
        tvRelay = (TextView) mMenuView.findViewById(R.id.item_chat_relay_tv);
        tvCollection = (TextView) mMenuView.findViewById(R.id.item_chat_collection_tv);
        tvCollectionOther = (TextView) mMenuView.findViewById(R.id.collection_other);
        tvBack = (TextView) mMenuView.findViewById(R.id.item_chat_back_tv);
        tvDel = (TextView) mMenuView.findViewById(R.id.item_chat_del_tv);
        tvMoreSelected = (TextView) mMenuView.findViewById(R.id.item_chat_more_select);
//        tvRecord = (TextView) mMenuView.findViewById(R.id.item_chat_record);
        tvCopy.setText(InternationalizationHelper.getString("JX_Copy"));
        tvRelay.setText(InternationalizationHelper.getString("JX_Relay"));
        tvCollectionOther.setText(InternationalizationHelper.getString("JX_Collection"));
        tvBack.setText(InternationalizationHelper.getString("JX_Withdraw"));
        tvDel.setText(InternationalizationHelper.getString("JX_Delete"));
//        tvRecord.setText(InternationalizationHelper.getString("JX_StartRecording"));

//        if (type.getIsReadDel()) {
//            tvRecord.setVisibility(View.GONE);
//        }

        //设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LayoutParams.WRAP_CONTENT);
        //	 this.setWidth(ViewPiexlUtil.dp2px(context,200));
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0000000000);
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        /*mMenuView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    } else if (y > bottom) {
                        dismiss();
                    }
                }
                return true;
            }
        });*/

        hideButton(type, course);
        // 设置按钮监听
        tvCopy.setOnClickListener(listener);
        tvRelay.setOnClickListener(listener);
        tvCollection.setOnClickListener(listener);
        tvCollectionOther.setOnClickListener(listener);
        tvBack.setOnClickListener(listener);
        tvDel.setOnClickListener(listener);
        tvMoreSelected.setOnClickListener(listener);
//        tvRecord.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (ChatRecordHelper.instance().getState() == ChatRecordHelper.STATE_UN_RECORD) {
//                    // 未录制 --> 开始录制
//                    ChatRecordHelper.instance().start(type);
//                } else {
//                    // 停止录制
//                    ChatRecordHelper.instance().stop(type, toUserId);
//                }
//                dismiss();
//            }
//        });
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /*
    根据消息类型隐藏部分操作
     */
    private void hideButton(ChatMessage message, boolean course) {
        int type = message.getType();
        // 文本类型可复制
        if (type != XmppMessage.TYPE_TEXT) {
            tvCopy.setVisibility(View.GONE);
        } else {
            tvCopy.setVisibility(View.VISIBLE);
        }

        // 图片类型可存表情
        if (type == TYPE_IMAGE) {
            tvCollection.setVisibility(View.VISIBLE);
        } else {
            tvCollection.setVisibility(View.GONE);
        }

        // 文本、图片、语音、视频、文件类型可收藏
        if (type == TYPE_TEXT || type == TYPE_IMAGE || type == TYPE_VOICE || type == TYPE_VIDEO || type == TYPE_FILE) {
            tvCollectionOther.setVisibility(View.VISIBLE);
        } else {
            tvCollectionOther.setVisibility(View.GONE);
        }

        // 撤回
        if (isGroup) {
            if ((message.isMySend() || mRole == 1 || mRole == 2) && type != TYPE_RED) {
                tvBack.setVisibility(View.VISIBLE);
            } else {
                tvBack.setVisibility(View.GONE);
            }
        } else {
            if (!message.isMySend()
                    || type == TYPE_RED
                    || type == TYPE_TRANSFER
                    || ((type >= TYPE_IS_CONNECT_VOICE && type <= TYPE_EXIT_VOICE))) {
                // 该条消息 NotSendByMe || 红包 || 音视频通话 类型不可撤回
                tvBack.setVisibility(View.GONE);
            } else {
                tvBack.setVisibility(View.VISIBLE);
                /*if (judgeTime(message.getTimeSend())) {
                    // 超时不可撤回
                    tvBack.setVisibility(View.GONE);
                } else {
                    tvBack.setVisibility(View.VISIBLE);
                }*/
            }
        }

        // 红包 || 音视频通话 类型不可转发
        if (type == TYPE_RED
                || type == TYPE_TRANSFER
                || (type >= TYPE_IS_CONNECT_VOICE && type <= TYPE_EXIT_VOICE)) {
            tvRelay.setVisibility(View.GONE);
        } else {
            tvRelay.setVisibility(View.VISIBLE);
        }

        // 当前正在 我的讲课-讲课详情 页面，只保留 复制 与 删除
        if (course) {
            tvRelay.setVisibility(View.GONE);
            tvCollection.setVisibility(View.GONE);
            tvCollectionOther.setVisibility(View.GONE);
            tvBack.setVisibility(View.GONE);
            tvMoreSelected.setVisibility(View.GONE);
//            tvRecord.setVisibility(View.GONE);
        }

//        if (message.getFromUserId().equals(CoreManager.requireSelf(MyApplication.getInstance()).getUserId())) {// 只录制自己的
//            ChatRecordHelper.instance().iniText(tvRecord, message);
//        } else {
//            tvRecord.setVisibility(View.GONE);
//        }

//        if (isDevice) {// 正在‘我的设备’聊天界面 隐藏讲课
//            tvRecord.setVisibility(View.GONE);
//        }
        mMenuView.findViewById(R.id.item_chat_text_ll).setBackgroundResource(R.drawable.bg_chat_text_long);
    }

    /*
    判断当前消息已发送的时间是否超过五分钟
     */
    private boolean judgeTime(long timeSend) {
        return timeSend + 300 < TimeUtils.sk_time_current_time();
    }
}

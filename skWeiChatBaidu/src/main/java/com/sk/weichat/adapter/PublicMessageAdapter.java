package com.sk.weichat.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.audio.AudioPalyer;
import com.sk.weichat.audio_x.VoiceAnimView;
import com.sk.weichat.audio_x.VoicePlayer;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.Report;
import com.sk.weichat.bean.circle.Comment;
import com.sk.weichat.bean.circle.Praise;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.bean.circle.PublicMessage.Body;
import com.sk.weichat.bean.circle.PublicMessage.Resource;
import com.sk.weichat.bean.collection.Collectiion;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.CircleMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.circle.BusinessCircleActivity;
import com.sk.weichat.ui.circle.BusinessCircleActivity.ListenerAudio;
import com.sk.weichat.ui.circle.MessageEventComment;
import com.sk.weichat.ui.circle.MessageEventReply;
import com.sk.weichat.ui.circle.range.PraiseListActivity;
import com.sk.weichat.ui.map.MapActivity;
import com.sk.weichat.ui.mucfile.DownManager;
import com.sk.weichat.ui.mucfile.MucFileDetails;
import com.sk.weichat.ui.mucfile.XfileUtils;
import com.sk.weichat.ui.mucfile.bean.MucFileBean;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.ui.tool.MultiImagePreviewActivity;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;
import com.sk.weichat.ui.tool.WebViewActivity;
import com.sk.weichat.util.HtmlUtils;
import com.sk.weichat.util.HttpUtil;
import com.sk.weichat.util.LinkMovementClickMethod;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.SystemUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.util.UploadCacheUtils;
import com.sk.weichat.view.CheckableImageView;
import com.sk.weichat.view.MyGridView;
import com.sk.weichat.view.ReportDialog;
import com.sk.weichat.view.SelectURLDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardSecond;
import okhttp3.Call;

/**
 * 1.我的空间
 * 2.我的收藏 adapter
 */
public class PublicMessageAdapter extends BaseAdapter implements ListenerAudio {
    private static final int VIEW_TYPE_NORMAL_TEXT = 0;
    private static final int VIEW_TYPE_FW_TEXT = 1;
    private static final int VIEW_TYPE_NORMAL_SINGLE_IMAGE = 2;
    private static final int VIEW_TYPE_FW_SINGLE_IMAGE = 3;
    private static final int VIEW_TYPE_NORMAL_MULTI_IMAGE = 4;
    private static final int VIEW_TYPE_FW_MULTI_IMAGE = 5;
    private static final int VIEW_TYPE_NORMAL_VOICE = 6;
    private static final int VIEW_TYPE_FW_VOICE = 7;
    private static final int VIEW_TYPE_NORMAL_VIDEO = 8;
    private static final int VIEW_TYPE_FW_VIDEO = 9;
    // 我的收藏专属-文件类型
    private static final int VIEW_TYPE_NORMAL_FILE = 10;
    // 分享的链接
    private static final int VIEW_TYPE_NORMAL_LINK = 11;
    private Context mContext;
    private CoreManager coreManager;
    private List<PublicMessage> mMessages;
    private LayoutInflater mInflater;
    private String mLoginUserId;
    private String mLoginNickName;
    private ViewHolder mVoicePlayViewHolder;
    private AudioPalyer mAudioPalyer;
    private String mVoicePlayId = null;
    private Map<String, Boolean> mClickOpenMaps = new HashMap<>();
    private int collectionType;

    public PublicMessageAdapter(Context context, CoreManager coreManager, List<PublicMessage> messages) {
        mContext = context;
        this.coreManager = coreManager;
        mMessages = messages;
        mInflater = LayoutInflater.from(mContext);
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        mAudioPalyer = new AudioPalyer();
        mAudioPalyer.setAudioPlayListener(new AudioPalyer.AudioPlayListener() {
            @Override
            public void onSeekComplete() {
            }

            @Override
            public void onPrepared() {
            }

            @Override
            public void onError() {
                mVoicePlayId = null;
                if (mVoicePlayViewHolder != null) {
                    updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
                }
                mVoicePlayViewHolder = null;
            }

            @Override
            public void onCompletion() {
                mVoicePlayId = null;
                if (mVoicePlayViewHolder != null) {
                    updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
                }
                mVoicePlayViewHolder = null;
            }

            @Override
            public void onBufferingUpdate(int percent) {
            }

            @Override
            public void onPreparing() {
            }
        });
    }

    /**
     * @see PublicMessage#getType() <br/>
     * 1=文字消息；2=图文消息；3=语音消息； 4=视频消息；5、转载<br/>
     * 分的视图类型有： <br/>
     * {@link #VIEW_TYPE_NORMAL_TEXT}0、普通文字消息视图<br/>
     * {@link #VIEW_TYPE_FW_TEXT} 1、转载文字消息视图 <br/>
     * {@link #VIEW_TYPE_NORMAL_SINGLE_IMAGE} 2、普通单张图片的视图<br/>
     * {@link #VIEW_TYPE_FW_SINGLE_IMAGE} 3、转载单张图片的视图<br/>
     * {@link #VIEW_TYPE_NORMAL_MULTI_IMAGE}4、普通多张图片的视图<br/>
     * {@link #VIEW_TYPE_FW_MULTI_IMAGE} 5、转载多张图片的视图<br/>
     * {@link #VIEW_TYPE_NORMAL_VOICE} 6、普通音频视图<br/>
     * {@link #VIEW_TYPE_FW_VOICE} 7、转载音频视图<br/>
     * {@link #VIEW_TYPE_NORMAL_VIDEO}8、普通视频视图<br/>
     * {@link #VIEW_TYPE_FW_VIDEO} 9、转载视频视图<br/>
     * {@link #VIEW_TYPE_FW_VIDEO} 10、普通文件视图<br/>
     */

    public void reset() {
        stopVoice();
    }

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 12;
    }

    @Override
    public int getItemViewType(int position) {
        PublicMessage message = mMessages.get(position);
        boolean fromSelf = message.getSource() == PublicMessage.SOURCE_SELF;
        Body body = message.getBody();
        if (body == null) {
            // 如果为空，那么可能是数据错误，直接返回一个普通的文本视图
            return VIEW_TYPE_NORMAL_TEXT;
        }
        if (body.getType() == PublicMessage.TYPE_TEXT) {
            // 文本视图
            if (fromSelf)
                return VIEW_TYPE_NORMAL_TEXT;
            else
                return VIEW_TYPE_FW_TEXT;
        } else if (body.getType() == PublicMessage.TYPE_IMG) {
            if (body.getImages() == null || body.getImages().size() <= 1) {
                // 普通的单张图片的视图
                if (fromSelf)
                    return VIEW_TYPE_NORMAL_SINGLE_IMAGE;
                else
                    return VIEW_TYPE_FW_SINGLE_IMAGE;
            } else {// 普通的多张图片视图
                if (fromSelf)
                    return VIEW_TYPE_NORMAL_MULTI_IMAGE;
                else
                    return VIEW_TYPE_FW_MULTI_IMAGE;
            }
        } else if (body.getType() == PublicMessage.TYPE_VOICE) {// 普通音频
            if (fromSelf)
                return VIEW_TYPE_NORMAL_VOICE;
            else
                return VIEW_TYPE_FW_VOICE;
        } else if (body.getType() == PublicMessage.TYPE_VIDEO) {// 普通视频
            if (fromSelf)
                return VIEW_TYPE_NORMAL_VIDEO;
            else
                return VIEW_TYPE_FW_VIDEO;
        } else if (body.getType() == PublicMessage.TYPE_FILE) {
            // 文件
            return VIEW_TYPE_NORMAL_FILE;
        } else if (body.getType() == PublicMessage.TYPE_LINK) {
            // 链接
            return VIEW_TYPE_NORMAL_LINK;
        } else {
            // 其他，数据错误
            return VIEW_TYPE_NORMAL_TEXT;
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        ViewHolder viewHolder = null;
        if (convertView == null || ((Integer) convertView.getTag(R.id.tag_key_list_item_type)) != viewType) {
            convertView = mInflater.inflate(R.layout.p_msg_item_main_body, null);
            View innerView = null;
            if (viewType == VIEW_TYPE_NORMAL_TEXT) {
                viewHolder = new NormalTextHolder();
            } else if (viewType == VIEW_TYPE_FW_TEXT) {
                FwTextHolder holder = new FwTextHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_fw_text, null);
                holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_NORMAL_SINGLE_IMAGE) {
                NormalSingleImageHolder holder = new NormalSingleImageHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_normal_single_img, null);
                holder.image_view = (ImageView) innerView.findViewById(R.id.image_view);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_FW_SINGLE_IMAGE) {
                FwSingleImageHolder holder = new FwSingleImageHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_fw_single_img, null);
                holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
                holder.image_view = (ImageView) innerView.findViewById(R.id.image_view);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_NORMAL_MULTI_IMAGE) {
                NormalMultiImageHolder holder = new NormalMultiImageHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_normal_multi_img, null);
                holder.grid_view = (MyGridView) innerView.findViewById(R.id.grid_view);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_FW_MULTI_IMAGE) {
                FwMultiImageHolder holder = new FwMultiImageHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_fw_multi_img, null);
                holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
                holder.grid_view = (MyGridView) innerView.findViewById(R.id.grid_view);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_NORMAL_VOICE) {
                NormalVoiceHolder holder = new NormalVoiceHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_normal_voice, null);
                holder.img_view = (ImageView) innerView.findViewById(R.id.img_view);
                holder.voice_action_img = (ImageView) innerView.findViewById(R.id.voice_action_img);
                holder.voice_desc_tv = (TextView) innerView.findViewById(R.id.voice_desc_tv);
                holder.chat_to_voice = (VoiceAnimView) innerView.findViewById(R.id.chat_to_voice);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_FW_VOICE) {
                FwVoiceHolder holder = new FwVoiceHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_fw_voice, null);
                holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
                holder.img_view = (ImageView) innerView.findViewById(R.id.img_view);
                holder.voice_action_img = (ImageView) innerView.findViewById(R.id.voice_action_img);
                holder.voice_desc_tv = (TextView) innerView.findViewById(R.id.voice_desc_tv);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_NORMAL_VIDEO) {
                NormalVideoHolder holder = new NormalVideoHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_normal_video, null);
                holder.gridViewVideoPlayer = (JVCideoPlayerStandardSecond) innerView.findViewById(R.id.preview_video);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_FW_VIDEO) {
                FwVideoHolder holder = new FwVideoHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_fw_video, null);
                holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
                holder.video_thumb_img = (ImageView) innerView.findViewById(R.id.video_thumb_img);
                holder.video_desc_tv = (TextView) innerView.findViewById(R.id.video_desc_tv);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_NORMAL_FILE) {
                NormalFileHolder holder = new NormalFileHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_normal_file, null);
                holder.file_click = (RelativeLayout) innerView.findViewById(R.id.collection_file);
                holder.file_image = (ImageView) innerView.findViewById(R.id.file_img);
                holder.text_tv = (TextView) innerView.findViewById(R.id.file_name);
                viewHolder = holder;
            } else if (viewType == VIEW_TYPE_NORMAL_LINK) {
                NormalLinkHolder holder = new NormalLinkHolder();
                innerView = mInflater.inflate(R.layout.p_msg_item_normal_link, null);
                holder.link_click = (LinearLayout) innerView.findViewById(R.id.link_ll);
                holder.link_image = (ImageView) innerView.findViewById(R.id.link_iv);
                holder.link_tv = (TextView) innerView.findViewById(R.id.link_text_tv);
                viewHolder = holder;
            }
            viewHolder.avatar_img = (ImageView) convertView.findViewById(R.id.avatar_img);
            viewHolder.nick_name_tv = (TextView) convertView.findViewById(R.id.nick_name_tv);
            viewHolder.time_tv = (TextView) convertView.findViewById(R.id.time_tv);
            viewHolder.body_tv = (TextView) convertView.findViewById(R.id.body_tv);
            viewHolder.body_tvS = (TextView) convertView.findViewById(R.id.body_tvS);
            viewHolder.open_tv = (TextView) convertView.findViewById(R.id.open_tv);
            viewHolder.content_fl = (FrameLayout) convertView.findViewById(R.id.content_fl);
            viewHolder.delete_tv = (TextView) convertView.findViewById(R.id.delete_tv);

            viewHolder.llOperator = convertView.findViewById(R.id.llOperator);
            viewHolder.llThumb = convertView.findViewById(R.id.llThumb);
            viewHolder.ivThumb = convertView.findViewById(R.id.ivThumb);
            viewHolder.tvThumb = convertView.findViewById(R.id.tvThumb);
            viewHolder.llComment = convertView.findViewById(R.id.llComment);
            viewHolder.ivComment = convertView.findViewById(R.id.ivComment);
            viewHolder.tvComment = convertView.findViewById(R.id.tvComment);
            viewHolder.llCollection = convertView.findViewById(R.id.llCollection);
            viewHolder.ivCollection = convertView.findViewById(R.id.ivCollection);
            viewHolder.llReport = convertView.findViewById(R.id.llReport);

            if (collectionType == 1 || collectionType == 2) {
                // 当前适配器用于我的收藏列表，隐藏评论 && 赞功能
                viewHolder.llOperator.setVisibility(View.GONE);
            } else {
                viewHolder.llOperator.setVisibility(View.VISIBLE);
            }
            viewHolder.multi_praise_tv = (TextView) convertView.findViewById(R.id.multi_praise_tv);
            viewHolder.tvLoadMore = (TextView) convertView.findViewById(R.id.tvLoadMore);
            viewHolder.line_v = convertView.findViewById(R.id.line_v);
            viewHolder.command_listView = (ListView) convertView.findViewById(R.id.command_listView);
            viewHolder.location_tv = (TextView) convertView.findViewById(R.id.location_tv);
            if (innerView != null) {
                viewHolder.content_fl.addView(innerView);
            }
            convertView.setTag(R.id.tag_key_list_item_type, viewType);
            convertView.setTag(R.id.tag_key_list_item_view, viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag(R.id.tag_key_list_item_view);
        }
        // 和ViewHolder一样的，只不过用作匿名内部类里面调用需要final
        final ViewHolder finalHolder = viewHolder;
        // set data
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return convertView;
        }
        /* 设置头像 */
       /* Glide.with(mContext).
                load(AvatarHelper.getAvatarUrl(message.getUserId(), true))
                .placeholder(R.drawable.avatar_normal)
                .into(viewHolder.avatar_img);*/
        AvatarHelper.getInstance().displayAvatar(message.getUserId(), viewHolder.avatar_img);
        /* 设置昵称 */
        SpannableStringBuilder nickNamebuilder = new SpannableStringBuilder();
        final String userId = message.getUserId();
        String showName = getShowName(userId, message.getNickName());
        UserClickableSpan.setClickableSpan(mContext, nickNamebuilder, showName, message.getUserId());
        viewHolder.nick_name_tv.setText(nickNamebuilder);
        viewHolder.nick_name_tv.setLinksClickable(true);
        viewHolder.nick_name_tv.setMovementMethod(LinkMovementClickMethod.getInstance());

        // 设置头像的点击事件
        viewHolder.avatar_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mContext instanceof BusinessCircleActivity) {
                    //防止重复进入
                    return;
                }

                if (!UiUtils.isNormalClick()){
                    return;
                }
                Intent intent = new Intent(mContext, BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, message.getUserId());
                mContext.startActivity(intent);

            }
        });

        // 获取消息本身的内容
        Body body = message.getBody();
        if (body == null) {
            return convertView;
        }

        // 是否是转载的
        boolean isForwarding = message.getSource() == PublicMessage.SOURCE_FORWARDING;

        // 设置body_tv
        if (TextUtils.isEmpty(body.getText())) {
            viewHolder.body_tv.setVisibility(View.GONE);
            viewHolder.body_tvS.setVisibility(View.GONE);
            viewHolder.open_tv.setVisibility(View.GONE);
        } else {
            // 支持emoji显示
            String s = StringUtils.replaceSpecialChar(body.getText());
            CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);

            viewHolder.body_tv.setText(charSequence);
            viewHolder.body_tvS.setText(charSequence);

            // 判断文本长度是否等于限定的长度
            if (viewHolder.body_tv.getText().length() >= 200) {
                viewHolder.body_tv.setVisibility(View.VISIBLE);
                viewHolder.body_tvS.setVisibility(View.GONE);
                viewHolder.open_tv.setText(InternationalizationHelper.getString("WeiboCell_AllText"));
                viewHolder.open_tv.setVisibility(View.VISIBLE);
            } else {
                viewHolder.body_tv.setVisibility(View.VISIBLE);
                viewHolder.body_tvS.setVisibility(View.GONE);
                viewHolder.open_tv.setVisibility(View.GONE);
            }

            if (mClickOpenMaps.containsKey(message.getMessageId())) {// 全文/收起
                if (mClickOpenMaps.get(message.getMessageId())) {// 为展开状态
                    viewHolder.body_tv.setVisibility(View.GONE);
                    viewHolder.body_tvS.setVisibility(View.VISIBLE);
                    finalHolder.open_tv.setVisibility(View.VISIBLE);
                    finalHolder.open_tv.setText(InternationalizationHelper.getString("WeiboCell_Stop"));
                }
            }

           /*
            URL跳转
             */
            final String detail = body.getText();
            if (HttpUtil.isURL(detail)) {
                viewHolder.body_tv.setTextColor(mContext.getResources().getColor(R.color.link_color));
            } else {
                viewHolder.body_tv.setTextColor(mContext.getResources().getColor(R.color.black));
            }

            viewHolder.body_tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (HttpUtil.isURL(detail)) {
                        List<String> mURLList = HttpUtil.getURLList(detail);
                        if (mURLList.size() > 1) {// 有多个URL，弹窗选择跳转
                            SelectURLDialog mSelectURLDialog = new SelectURLDialog(mContext, mURLList, new SelectURLDialog.OnURLListItemClickListener() {
                                @Override
                                public void onURLItemClick(String URL) {
                                    intentWebView(URL);
                                }
                            });
                            mSelectURLDialog.show();
                        } else {// 单个URL,直接跳转
                            intentWebView(mURLList.get(0));
                        }
                    }
                }
            });
        }

        viewHolder.body_tv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBodyTextLongClickDialog(finalHolder.body_tv.getText().toString());
                return true;
            }
        });
        viewHolder.body_tvS.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBodyTextLongClickDialog(finalHolder.body_tvS.getText().toString());
                return true;
            }
        });

        viewHolder.open_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean clickStatus;
                if (mClickOpenMaps.containsKey(message.getMessageId())) {// 可判断之前必定展开过该条说说
                    clickStatus = !mClickOpenMaps.get(message.getMessageId());// 改变其原状态
                } else {// 之前未展开过，展开
                    clickStatus = true;
                }
                mClickOpenMaps.put(message.getMessageId(), clickStatus);

                if (clickStatus) {// 展开
                    finalHolder.body_tv.setVisibility(View.GONE);
                    finalHolder.body_tvS.setVisibility(View.VISIBLE);
                    finalHolder.open_tv.setText(InternationalizationHelper.getString("WeiboCell_Stop"));
                } else {// 收起
                    finalHolder.body_tv.setVisibility(View.VISIBLE);
                    finalHolder.body_tvS.setVisibility(View.GONE);
                    finalHolder.open_tv.setText(InternationalizationHelper.getString("WeiboCell_AllText"));
                }
            }
        });

        // 设置发布时间
        viewHolder.time_tv.setText(TimeUtils.getFriendlyTimeDesc(mContext, (int) message.getTime()));
        // 设置删除按钮
        viewHolder.delete_tv.setText(InternationalizationHelper.getString("JX_Delete"));
        if (collectionType == 1) {
            viewHolder.delete_tv.setVisibility(View.VISIBLE);
            viewHolder.delete_tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteMsgDialog(position);
                }
            });
        } else if (collectionType == 2) {
            viewHolder.delete_tv.setVisibility(View.GONE);
        } else {
            if (userId.equals(mLoginUserId)) {
                // 是我发的消息
                viewHolder.delete_tv.setVisibility(View.VISIBLE);
                viewHolder.delete_tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDeleteMsgDialog(position);
                    }
                });
            } else {
                viewHolder.delete_tv.setVisibility(View.GONE);
                viewHolder.delete_tv.setOnClickListener(null);
            }
        }

        final ViewHolder vh = viewHolder;
        vh.ivThumb.setChecked(1 == message.getIsPraise());
        vh.tvThumb.setText(String.valueOf(message.getPraise()));
        vh.llThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 是否是点过赞，
                final boolean isPraise = vh.ivThumb.isChecked();
                // 调接口，旧代码保留，传的是相反的状态，
                onPraise(position, !isPraise);
                // 更新赞数，调接口完成还会刷新，
                int praiseCount = message.getPraise();
                if (isPraise) {
                    praiseCount--;
                } else {
                    praiseCount++;
                }
                vh.tvThumb.setText(String.valueOf(praiseCount));
                vh.ivThumb.toggle();
            }
        });
        // 是否点评论过，
        // TODO: 不准了，评论分页加载，
        boolean isComment = false;
        if (message.getComments() != null) {
            for (Comment comment : message.getComments()) {
                if (mLoginUserId.equals(comment.getUserId())) {
                    isComment = true;
                }
            }
        }
        vh.ivComment.setChecked(isComment);
        vh.tvComment.setText(String.valueOf(message.getCommnet()));
        vh.llComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 调接口，旧代码保留，
                onComment(position, vh.command_listView);
                // 评论数在调接口完成后还会刷新，
            }
        });
        vh.ivCollection.setChecked(1 == message.getIsCollect());
        vh.llCollection.setOnClickListener(v -> {
            onCollection(position);
        });
        vh.llReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReport(position);
            }
        });

        /* 显示多少人赞过 */
        List<Praise> praises = message.getPraises();
        if (praises != null && praises.size() > 0) {
            viewHolder.multi_praise_tv.setVisibility(View.VISIBLE);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            for (int i = 0; i < praises.size(); i++) {
                String praiseName = getShowName(praises.get(i).getUserId(), praises.get(i).getNickName());
                UserClickableSpan.setClickableSpan(mContext, builder, praiseName, praises.get(i).getUserId());
                if (i < praises.size() - 1)
                    builder.append(",");
            }
            if (message.getPraise() > praises.size()) {
                builder.append(mContext.getString(R.string.praise_ending_place_holder, message.getPraise()));
            }
            viewHolder.multi_praise_tv.setText(builder);
        } else {
            viewHolder.multi_praise_tv.setVisibility(View.GONE);
            viewHolder.multi_praise_tv.setText("");
        }
        viewHolder.multi_praise_tv.setLinksClickable(true);
        viewHolder.multi_praise_tv.setMovementMethod(LinkMovementClickMethod.getInstance());
        viewHolder.multi_praise_tv.setOnClickListener(v -> {
            PraiseListActivity.start(mContext, message.getMessageId());
        });

        /* 设置回复 */
        final List<Comment> comments = message.getComments();
        viewHolder.command_listView.setVisibility(View.VISIBLE);
        CommentAdapter adapter = new CommentAdapter(position, comments);
        viewHolder.command_listView.setAdapter(adapter);
        viewHolder.line_v.setVisibility(View.VISIBLE);
        viewHolder.tvLoadMore.setVisibility(View.GONE);
        if (comments != null && comments.size() > 0) {
            if (message.getCommnet() > comments.size()) {
                // 需要分页加载，
                viewHolder.tvLoadMore.setVisibility(View.VISIBLE);
                viewHolder.tvLoadMore.setOnClickListener(v -> {
                    loadCommentsNextPage(vh.tvLoadMore, message.getMessageId(), adapter);
                });
            }
        }

/*
        mAdapter = (CommentAdapter) viewHolder.command_listView.getAdapter();
        if (mAdapter == null) {
            mAdapter = new CommentAdapter();
            viewHolder.command_listView.setAdapter(mAdapter);
        }

        if (comments != null && comments.size() > 0) {
            viewHolder.line_v.setVisibility(View.VISIBLE);
            viewHolder.command_listView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.line_v.setVisibility(View.GONE);
            viewHolder.command_listView.setVisibility(View.GONE);
        }
        mAdapter.setData(position, comments);
*/

        if (!TextUtils.isEmpty(message.getLocation())) {
            viewHolder.location_tv.setText(message.getLocation());
            viewHolder.location_tv.setVisibility(View.VISIBLE);
        } else {
            viewHolder.location_tv.setVisibility(View.GONE);
        }

        viewHolder.location_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MapActivity.class);
                intent.putExtra("latitude", message.getLatitude());
                intent.putExtra("longitude", message.getLongitude());
                intent.putExtra("userName", message.getLocation());
                mContext.startActivity(intent);
            }
        });

        // //////////////////上面是公用的部分，下面是每个Type不同的部分/////////////////////////////////////////
        // 转载的消息会有一个转载人和text
        SpannableStringBuilder forwardingBuilder = null;
        if (isForwarding) {// 转载的那个人和说的话
            forwardingBuilder = new SpannableStringBuilder();
            String forwardName = getShowName(message.getFowardUserId(), message.getFowardNickname());
            UserClickableSpan.setClickableSpan(mContext, forwardingBuilder, forwardName, message.getFowardUserId());
            if (!TextUtils.isEmpty(message.getFowardText())) {
                forwardingBuilder.append(" : ");
                forwardingBuilder.append(message.getFowardText());
            }
        }
        if (viewType == VIEW_TYPE_NORMAL_TEXT) {
            viewHolder.content_fl.setVisibility(View.GONE);
        } else if (viewType == VIEW_TYPE_FW_TEXT) {
            TextView text_tv = ((FwTextHolder) viewHolder).text_tv;
            text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");
        } else if (viewType == VIEW_TYPE_NORMAL_SINGLE_IMAGE) {
            ImageView image_view = ((NormalSingleImageHolder) viewHolder).image_view;
            String url = message.getFirstImageOriginal();
            if (!TextUtils.isEmpty(url)) {
                Glide.with(mContext)
                        .load(url)
                        .centerCrop()
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(image_view);
                image_view.setOnClickListener(new SingleImageClickListener(url));
            } else {
                image_view.setImageBitmap(null);
            }
        } else if (viewType == VIEW_TYPE_FW_SINGLE_IMAGE) {
            TextView text_tv = ((FwSingleImageHolder) viewHolder).text_tv;
            ImageView image_view = ((FwSingleImageHolder) viewHolder).image_view;
            text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");

            String url = message.getFirstImageOriginal();
            if (!TextUtils.isEmpty(url)) {
                Glide.with(mContext)
                        .load(url)
                        .centerCrop()
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(image_view);
                image_view.setOnClickListener(new SingleImageClickListener(url));
            } else {
                image_view.setImageBitmap(null);
            }
        } else if (viewType == VIEW_TYPE_NORMAL_MULTI_IMAGE) {
            MyGridView grid_view = ((NormalMultiImageHolder) viewHolder).grid_view;
            if (body.getImages() != null) {
                grid_view.setAdapter(new ImagesInnerGridViewAdapter(mContext, body.getImages()));
                grid_view.setOnItemClickListener(new MultipleImagesClickListener(body.getImages()));
            } else {
                grid_view.setAdapter(null);
            }
        } else if (viewType == VIEW_TYPE_FW_MULTI_IMAGE) {
            TextView text_tv = ((FwMultiImageHolder) viewHolder).text_tv;
            MyGridView grid_view = ((FwMultiImageHolder) viewHolder).grid_view;
            text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");
            if (body.getImages() != null) {
                grid_view.setAdapter(new ImagesInnerGridViewAdapter(mContext, body.getImages()));
                grid_view.setOnItemClickListener(new MultipleImagesClickListener(body.getImages()));
            } else {
                grid_view.setAdapter(null);
            }
        } else if (viewType == VIEW_TYPE_NORMAL_VOICE) {
            // 相关代码相当混乱且有大量废弃代码，
            // 修改语音消息外观时尽少修改代码，
            final NormalVoiceHolder holder = (NormalVoiceHolder) viewHolder;
            holder.chat_to_voice.fillData(message);
            holder.chat_to_voice.setOnClickListener(v -> {
                VoicePlayer.instance().playVoice(holder.chat_to_voice);
            });

        } else if (viewType == VIEW_TYPE_FW_VOICE) {
            final FwVoiceHolder holder = (FwVoiceHolder) viewHolder;
            holder.text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");
            if (mVoicePlayId == null || !mVoicePlayId.equals(message.getMessageId())) {
                // 处于非播放状态
                holder.voice_action_img.setImageResource(R.drawable.feed_main_player_play);
            } else {
                holder.voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
            }
            holder.voice_action_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    play(holder, message);
                }
            });

            String imageUrl = message.getFirstImageOriginal();
            if (TextUtils.isEmpty(imageUrl)) {
                // AvatarHelper.getInstance().displayAvatar(message.getUserId(), holder.img_view, false);
                Glide.with(mContext)
                        .load(AvatarHelper.getAvatarUrl(message.getUserId(), false))
                        .signature(new StringSignature(UUID.randomUUID().toString()))
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(holder.img_view);
                holder.img_view.setOnClickListener(
                        new SingleImageClickListener(AvatarHelper.getAvatarUrl(message.getUserId(), false)));
            } else {
                Glide.with(mContext)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(holder.img_view);
                holder.img_view.setOnClickListener(new SingleImageClickListener(imageUrl));
            }

        } else if (viewType == VIEW_TYPE_NORMAL_VIDEO) {
            NormalVideoHolder holder = (NormalVideoHolder) viewHolder;
            String imageUrl = message.getFirstImageOriginal();
            // 判断是自己处理就直接使用本地文件，
            String videoUrl = UploadCacheUtils.get(mContext, message.getFirstVideo());
            if (!TextUtils.isEmpty(videoUrl)) {
                if (videoUrl.equals(message.getFirstVideo())) {
                    // 如果不是自己上传的，就使用视频缓存库统一缓存，
                    videoUrl = MyApplication.getProxy(mContext).getProxyUrl(message.getFirstVideo());
                }
                holder.gridViewVideoPlayer.setUp(videoUrl,
                        JVCideoPlayerStandardSecond.SCREEN_LAYOUT_NORMAL, "");
            }
            if (TextUtils.isEmpty(imageUrl)) {
                AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(videoUrl, holder.gridViewVideoPlayer.thumbImageView);
            } else {
                Glide.with(mContext)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(holder.gridViewVideoPlayer.thumbImageView);
            }
        } else if (viewType == VIEW_TYPE_FW_VIDEO) {
            FwVideoHolder holder = (FwVideoHolder) viewHolder;
            holder.text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");
            String imageUrl = message.getFirstImageOriginal();
            if (TextUtils.isEmpty(imageUrl)) {
                // AvatarHelper.getInstance().displayAvatar(message.getUserId(), holder.video_thumb_img, false);
                Glide.with(mContext)
                        .load(AvatarHelper.getAvatarUrl(message.getUserId(), false))
                        .signature(new StringSignature(UUID.randomUUID().toString()))
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(holder.video_thumb_img);
            } else {
                // ImageLoader.getInstance().displayImage(imageUrl, holder.video_thumb_img);
                Glide.with(mContext)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_gray)
                        .error(R.drawable.default_gray)
                        .into(holder.video_thumb_img);
            }
        } else if (viewType == VIEW_TYPE_NORMAL_FILE) {
            // 文件
            NormalFileHolder holder = (NormalFileHolder) viewHolder;
            final String mFileUrl = message.getFirstFile();

            if (TextUtils.isEmpty(mFileUrl)) {
                return convertView;
            }
            // 朋友圈的接口数据没有fileName,
            if (!TextUtils.isEmpty(message.getFileName())) {
                holder.text_tv.setText("[" + InternationalizationHelper.getString("JX_File") + "]" + message.getFileName());
            } else {
                try {
                    message.setFileName(mFileUrl.substring(mFileUrl.lastIndexOf('/') + 1));
                    holder.text_tv.setText("[" + InternationalizationHelper.getString("JX_File") + "]" + message.getFileName());
                } catch (Exception ignored) {
                    // 万一url有问题，没有斜杠/直接抛异常下来显示url,
                    holder.text_tv.setText("[" + InternationalizationHelper.getString("JX_File") + "]" + mFileUrl);
                }
            }

            String suffix = "";
            int index = mFileUrl.lastIndexOf(".");
            if (index != -1) {
                suffix = mFileUrl.substring(index + 1).toLowerCase();
                if (suffix.equals("png") || suffix.equals("jpg")) {
                    Glide.with(mContext).load(mFileUrl).override(100, 100).into(holder.file_image);
                } else {
                    AvatarHelper.getInstance().fillFileView(suffix, holder.file_image);
                }
            }

            final long size = message.getBody().getFiles().get(0).getSize();
            Log.e("xuan", "setOnClickListener: " + size);

            holder.file_click.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    intentPreviewFile(mFileUrl, message.getFileName(), message.getNickName(), size);

                    //                    Intent intent = new Intent(mContext, WebViewActivity.class);
                    //                    intent.putExtra(WebViewActivity.EXTRA_URL, mFileUrl);
                    //                    mContext.startActivity(intent);
                }
            });
        } else if (viewType == VIEW_TYPE_NORMAL_LINK) {
            NormalLinkHolder holder = (NormalLinkHolder) viewHolder;
            if (TextUtils.isEmpty(message.getBody().getSdkIcon())) {
                holder.link_image.setImageResource(R.drawable.browser);
            } else {
                AvatarHelper.getInstance().displayImage(message.getBody().getSdkIcon(), holder.link_image);
            }
            holder.link_tv.setText(message.getBody().getSdkTitle());

            holder.link_click.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, WebViewActivity.class);
                    intent.putExtra(WebViewActivity.EXTRA_URL, message.getBody().getSdkUrl());
                    mContext.startActivity(intent);
                }
            });
        }
        return convertView;
    }

    /**
     * 跳转到文件预览
     *
     * @param filePath
     */
    private void intentPreviewFile(String filePath, String fileName, String fromName, long size) {
        MucFileBean data = new MucFileBean();

        // 取出文件后缀
        int start = filePath.lastIndexOf(".");
        String suffix = start > -1 ? filePath.substring(start + 1).toLowerCase() : "";

        int fileType = XfileUtils.getFileType(suffix);
        data.setNickname(fromName);
        data.setUrl(filePath);
        data.setName(fileName);
        data.setSize(size);
        data.setState(DownManager.STATE_UNDOWNLOAD);
        data.setType(fileType);
        Intent intent = new Intent(mContext, MucFileDetails.class);
        intent.putExtra("data", data);
        mContext.startActivity(intent);
    }

    /**
     * 第一次调用时就已经是阅读完第一页，加载第二页了，
     */
    private void loadCommentsNextPage(TextView view, String messageId, CommentAdapter adapter) {
        // isLoading同时有noMore效果，只有加载出新数据时才设置isLoading为false,
        if (adapter.isLoading()) {
            return;
        }
        adapter.setLoading(true);
        // 只能是20， 因为朋友圈消息列表接口直接返回了的是第一页20条，
        int pageSize = 20;
        // 有20个就加载第二页也就是index==1, 21个是加载第三页，得到空列表，就能停止了，
        int index = (adapter.getCount() + (pageSize - 1)) / pageSize;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(index));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("messageId", messageId);

        String url = coreManager.getConfig().MSG_COMMENT_LIST;

        view.setTag(messageId);
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new ListCallback<Comment>(Comment.class) {
                    @Override
                    public void onResponse(ArrayResult<Comment> result) {
                        List<Comment> data = result.getData();
                        if (data.size() > 0) {
                            adapter.addAll(data);
                            adapter.setLoading(false);
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_no_more);
                            if (view.getTag() == messageId) {
                                // 隐藏加载按钮，
                                view.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Reporter.post("评论分页加载失败，", e);
                        ToastUtil.showToast(mContext, mContext.getString(R.string.tip_comment_load_error));
                    }
                });

    }

    private String getShowName(String userId, String defaultName) {
        String showName = "";

        if (userId.equals(mLoginUserId)) {
            showName = coreManager.getSelf().getNickName();
        } else {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userId);
            if (friend != null) {
                showName = TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
            }
        }

        if (TextUtils.isEmpty(showName)) {
            showName = defaultName;
        }
        return showName;
    }

    /* 操作事件 */
    private void showDeleteMsgDialog(final int position) {
        DialogHelper.showSingleTextDialog((Activity) mContext, InternationalizationHelper.getString("JX_Tip"), mContext.getString(R.string.delete_prompt), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (collectionType == 1 || collectionType == 2) {
                    // 删除收藏
                    deleteCollection(position);
                } else {
                    // 删除评论
                    deleteMsg(position);
                }
            }
        });
    }

    private void deleteMsg(final int position) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        DialogHelper.showDefaulteMessageProgressDialog((Activity) mContext);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).CIRCLE_MSG_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        CircleMessageDao.getInstance().deleteMessage(message.getMessageId());// 删除数据库的记录（如果存在的话）
                        mMessages.remove(position);
                        notifyDataSetChanged();

                        // 删除成功，停止正在播放的视频、音频
                        JCVideoPlayer.releaseAllVideos();
                        stopVoice();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    public void deleteCollection(final int position) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emojiId", message.getEmojiId());
        DialogHelper.showDefaulteMessageProgressDialog((Activity) mContext);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_REMOVE)
                .params(params)
                .build()
                .execute(new BaseCallback<Collectiion>(Collectiion.class) {

                    @Override
                    public void onResponse(ObjectResult<Collectiion> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            mMessages.remove(position);
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void showBodyTextLongClickDialog(final String text) {
        CharSequence[] items = new CharSequence[]{InternationalizationHelper.getString("JX_Copy")};
        new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // 复制文字
                        SystemUtil.copyText(mContext, text);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    private void showCommentLongClickDialog(final int messagePosition, final int commentPosition,
                                            final CommentAdapter adapter) {
        if (messagePosition < 0 || messagePosition >= mMessages.size()) {
            return;
        }
        final PublicMessage message = mMessages.get(messagePosition);
        if (message == null) {
            return;
        }
        final List<Comment> comments = message.getComments();
        if (comments == null) {
            return;
        }
        if (commentPosition < 0 || commentPosition >= comments.size()) {
            return;
        }
        final Comment comment = comments.get(commentPosition);

        CharSequence[] items;
        if (comment.getUserId().equals(mLoginUserId) || message.getUserId().equals(mLoginUserId)) {
            // 我的评论 || 我的消息，那么我就可以删除
            items = new CharSequence[]{InternationalizationHelper.getString("JX_Copy"), InternationalizationHelper.getString("JX_Delete")};
        } else {
            items = new CharSequence[]{InternationalizationHelper.getString("JX_Copy")};
        }
        new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:// 复制文字
                        if (TextUtils.isEmpty(comment.getBody())) {
                            return;
                        }
                        SystemUtil.copyText(mContext, comment.getBody());
                        break;
                    case 1:
                        deleteComment(message.getMessageId(), comment.getCommentId(), comments, commentPosition, adapter);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    /**
     * 删除一条回复
     */
    private void deleteComment(String messageId, String commentId, final List<Comment> comments,
                               final int commentPosition, final CommentAdapter adapter) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        params.put("commentId", commentId);
        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).MSG_COMMENT_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        comments.remove(commentPosition);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    public void onPraise(int messagePosition, boolean isPraise) {
        praiseOrCancel(messagePosition, isPraise);
    }

    public void onComment(int messagePosition, ListView view) {
        if (mContext instanceof BusinessCircleActivity) {
            ((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition, null, null, null);
        } else {
            PublicMessage message = mMessages.get(messagePosition);
            String path = "";
            if (message.getType() == 3) {
                //语音
                path = message.getFirstAudio();
            } else if (message.getType() == 2) {
                //图片
                path = message.getFirstImageOriginal();
            } else if (message.getType() == 6) {
                //视频
                path = message.getFirstVideo();
            }
            view.setTag(message);
            EventBus.getDefault().post(new MessageEventComment("Comment", message.getMessageId(),
                    message.getType(), path, message, view));
        }
    }

    private <T> T firstOrNull(List<T> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private String collectionParam(PublicMessage message) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = message.getCollectionType();
        json.put("type", String.valueOf(type));
        String msg = "";
        String collectContent = "";
        String fileName = "";
        long fileLength = 0;
        long fileSize = 0;
        String id = message.getMessageId();
        Resource res = null;
        if (message.getBody() != null) {
            collectContent = message.getBody().getText();
            switch (type) {
                case CollectionEvery.TYPE_TEXT:
                    msg = message.getBody().getText();
                    break;
                case CollectionEvery.TYPE_IMAGE:
                    List<Resource> images = message.getBody().getImages();
                    StringBuilder sb = new StringBuilder();
                    boolean firstTime = true;
                    for (Resource token : images) {
                        String url = token.getOriginalUrl();
                        if (TextUtils.isEmpty(url)) {
                            continue;
                        }
                        if (firstTime) {
                            firstTime = false;
                        } else {
                            sb.append(',');
                        }
                        sb.append(url);
                    }
                    msg = sb.toString();
                    break;
                case CollectionEvery.TYPE_FILE:
                    res = firstOrNull(message.getBody().getFiles());
                    break;
                case CollectionEvery.TYPE_VIDEO:
                    res = firstOrNull(message.getBody().getVideos());
                    break;
                case CollectionEvery.TYPE_VOICE:
                    res = firstOrNull(message.getBody().getAudios());
                    break;
                default:
                    throw new IllegalStateException("类型<" + type + ">不存在，");
            }
        }

        if (res != null) {
            if (!TextUtils.isEmpty(res.getOriginalUrl())) {
                msg = res.getOriginalUrl();
            }
            fileLength = res.getLength();
            fileSize = res.getSize();
        }
        if (!TextUtils.isEmpty(message.getFileName())) {
            fileName = message.getFileName();
        }

        json.put("msg", msg);
        json.put("fileName", fileName);
        json.put("fileSize", fileSize);
        json.put("fileLength", fileLength);
        json.put("collectContent", collectContent);
        json.put("collectType", 1);
        json.put("collectMsgId", id);
        array.add(json);
        return JSON.toJSONString(array);
    }

    private void onCollection(final int messagePosition) {
        PublicMessage message = mMessages.get(messagePosition);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        if (1 == message.getIsCollect()) {
            params.put("messageId", message.getMessageId());

            HttpUtils.get().url(coreManager.getConfig().MSG_COLLECT_DELETE)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(mContext, R.string.tip_collection_canceled);
                                message.setIsCollect(0);
                                notifyDataSetChanged();
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
        } else {
            params.put("emoji", collectionParam(message));

            HttpUtils.get().url(coreManager.getConfig().Collection_ADD)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            if (result.getResultCode() == 1) {
                                Toast.makeText(mContext, InternationalizationHelper.getString("JX_CollectionSuccess"), Toast.LENGTH_SHORT).show();
                                message.setIsCollect(1);
                                notifyDataSetChanged();
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
    }

    public void onReport(final int messagePosition) {
        ReportDialog mReportDialog = new ReportDialog(mContext, false, new ReportDialog.OnReportListItemClickListener() {
            @Override
            public void onReportItemClick(Report report) {
                report(messagePosition, report);
            }
        });
        mReportDialog.show();
    }

    /**
     * 赞 || 取消赞
     */
    private void praiseOrCancel(final int position, final boolean isPraise) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        String requestUrl;
        if (isPraise) {
            requestUrl = CoreManager.requireConfig(MyApplication.getInstance()).MSG_PRAISE_ADD;
        } else {
            requestUrl = CoreManager.requireConfig(MyApplication.getInstance()).MSG_PRAISE_DELETE;
        }
        HttpUtils.get().url(requestUrl)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        message.setIsPraise(isPraise ? 1 : 0);
                        List<Praise> praises = message.getPraises();
                        if (praises == null) {
                            praises = new ArrayList<>();
                            message.setPraises(praises);
                        }
                        int praiseCount = message.getPraise();
                        if (isPraise) {
                            // 代表我点赞
                            // 消息实体的改变
                            Praise praise = new Praise();
                            praise.setUserId(mLoginUserId);
                            praise.setNickName(mLoginNickName);
                            // praises.add(0, praise);
                            praises.add(praise);// 不建议将其添加到第一位，否则赞与取消赞的操作会造成点赞名单闪烁的现象
                            praiseCount++;
                            message.setPraise(praiseCount);
                        } else {
                            // 取消我的赞
                            // 消息实体的改变
                            for (int i = 0; i < praises.size(); i++) {
                                if (mLoginUserId.equals(praises.get(i).getUserId())) {
                                    praises.remove(i);
                                    praiseCount--;
                                    message.setPraise(praiseCount);
                                    break;
                                }
                            }
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void report(int position, Report report) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", message.getUserId());
        params.put("reason", String.valueOf(report.getReportId()));

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_REPORT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                        if (result.getResultCode() == 1) {
                            ToastUtil.showToast(mContext, mContext.getString(R.string.report_success));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /**
     * 停止播放声音
     */
    public void stopVoice() {
        if (mAudioPalyer != null) {
            mAudioPalyer.stop();
        }
        VoicePlayer.instance().stop();
    }

    /**
     * @param viewHolder
     */
    private void play(ViewHolder viewHolder, PublicMessage message) {

        JCVideoPlayer.releaseAllVideos();

        String voiceUrl = message.getFirstAudio();
        if (mVoicePlayId == null) {
            // 没有在播放
            try {
                mAudioPalyer.play(voiceUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mVoicePlayId = message.getMessageId();
            updateVoiceViewHolderIconStatus(true, viewHolder);
            mVoicePlayViewHolder = viewHolder;
        } else {
            if (mVoicePlayId == message.getMessageId()) {
                mAudioPalyer.stop();
                mVoicePlayId = null;
                updateVoiceViewHolderIconStatus(false, viewHolder);
                mVoicePlayViewHolder = null;
            } else {
                // 正在播放别的， 在播放这个
                mAudioPalyer.stop();
                mVoicePlayId = null;
                if (mVoicePlayViewHolder != null) {
                    updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
                }
                try {
                    mAudioPalyer.play(voiceUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mVoicePlayId = message.getMessageId();
                updateVoiceViewHolderIconStatus(true, viewHolder);
                mVoicePlayViewHolder = viewHolder;
            }
        }
    }

    private void updateVoiceViewHolderIconStatus(boolean play, ViewHolder viewHolder) {
        if (viewHolder instanceof NormalVoiceHolder) {
            // 普通音频
            if (play) {
                ((NormalVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
            } else {
                ((NormalVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_play);
            }
        } else {
            // 转载音频
            if (play) {
                ((FwVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
            } else {
                ((FwVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_play);
            }
        }
    }

    /**
     * 节口回调的方法
     */
    @Override
    public void ideChange() {
        stopVoice();
    }

    /**
     * 刷新适配器
     **/
    public void setData(List<PublicMessage> mMessages) {
        this.mMessages = mMessages;
        this.notifyDataSetChanged();
    }

    private void intentWebView(String url) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, url);
        mContext.startActivity(intent);
    }

    /**
     * 0:正常 default==0
     * 1:查看我的收藏 隐藏赞与评论功能 删除按钮一直显示
     * 2.发送我的收藏 隐藏赞与评论功能 删除按钮一直隐藏
     */
    public void setCollectionType(int collectionType) {
        this.collectionType = collectionType;
    }

    static class ViewHolder {
        ImageView avatar_img;
        TextView nick_name_tv;
        TextView time_tv;
        TextView body_tv;
        TextView body_tvS;
        TextView open_tv;
        FrameLayout content_fl;
        TextView delete_tv;
        TextView multi_praise_tv;
        View line_v;
        ListView command_listView;
        TextView tvLoadMore;
        TextView location_tv;

        View llOperator;
        View llThumb;
        CheckableImageView ivThumb;
        TextView tvThumb;
        View llComment;
        CheckableImageView ivComment;
        TextView tvComment;
        View llCollection;
        CheckableImageView ivCollection;
        View llReport;
    }

    /* 普通的Text */
    static class NormalTextHolder extends ViewHolder {

    }

    /* 转载的Text */
    static class FwTextHolder extends ViewHolder {
        TextView text_tv;
    }

    /* 普通的单张图片 */
    static class NormalSingleImageHolder extends ViewHolder {
        ImageView image_view;
    }

    /* 转载的单张图片 */
    static class FwSingleImageHolder extends ViewHolder {
        TextView text_tv;
        ImageView image_view;
    }

    /* 普通的多张图片 */
    static class NormalMultiImageHolder extends ViewHolder {
        MyGridView grid_view;
    }

    /* 转载的多张图片 */
    static class FwMultiImageHolder extends ViewHolder {
        TextView text_tv;
        MyGridView grid_view;
    }

    /* 普通的音频 */
    static class NormalVoiceHolder extends ViewHolder {
        ImageView img_view;
        ImageView voice_action_img;
        TextView voice_desc_tv;
        VoiceAnimView chat_to_voice;
    }

    /* 转载的音频 */
    static class FwVoiceHolder extends ViewHolder {
        TextView text_tv;
        ImageView img_view;
        ImageView voice_action_img;
        TextView voice_desc_tv;
    }

    /* 普通的视频 */
    static class NormalVideoHolder extends ViewHolder {
        JVCideoPlayerStandardSecond gridViewVideoPlayer;
    }

    /* 转载的视频 */
    static class FwVideoHolder extends ViewHolder {
        TextView text_tv;
        ImageView video_thumb_img;
        TextView video_desc_tv;
    }

    /************************* 播放声音 ******************************/

    /* 普通的文件 */
    static class NormalFileHolder extends ViewHolder {
        RelativeLayout file_click;
        ImageView file_image;
        TextView text_tv;
    }

    /* 普通的链接 */
    static class NormalLinkHolder extends ViewHolder {
        LinearLayout link_click;
        ImageView link_image;
        TextView link_tv;
    }

    static class CommentViewHolder {
        TextView text_view;
    }

    public class CommentAdapter extends BaseAdapter {
        private int messagePosition;
        private boolean loading;
        private List<Comment> datas;

        CommentAdapter(int messagePosition, List<Comment> data) {
            this.messagePosition = messagePosition;
            if (data == null) {
                datas = new ArrayList<>();
            } else {
                this.datas = data;
            }
        }

/*
        public void setData(int messagePosition, List<Comment> data) {
            this.messagePosition = messagePosition;
            this.datas = data;
            notifyDataSetChanged();
        }
*/

        public void addAll(List<Comment> data) {
            this.datas.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CommentViewHolder holder;
            if (convertView == null) {
                holder = new CommentViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.p_msg_comment_list_item, null);
                holder.text_view = (TextView) convertView.findViewById(R.id.text_view);
                convertView.setTag(holder);
            } else {
                holder = (CommentViewHolder) convertView.getTag();
            }
            final Comment comment = datas.get(position);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            String showName = getShowName(comment.getUserId(), comment.getNickName());
            UserClickableSpan.setClickableSpan(mContext, builder, showName, comment.getUserId());            // 设置评论者的ClickSpanned
            if (!TextUtils.isEmpty(comment.getToUserId()) && !TextUtils.isEmpty(comment.getToNickname())) {
                builder.append(InternationalizationHelper.getString("JX_Reply"));
                String toShowName = getShowName(comment.getToUserId(), comment.getToNickname());
                UserClickableSpan.setClickableSpan(mContext, builder, toShowName, comment.getToUserId());// 设置被评论者的ClickSpanned
            }
            builder.append(":");
            // 设置评论内容
            String commentBody = comment.getBody();
            if (!TextUtils.isEmpty(commentBody)) {
                commentBody = StringUtils.replaceSpecialChar(comment.getBody());
                CharSequence charSequence = HtmlUtils.transform200SpanString(commentBody.replaceAll("\n", "\r\n"),
                        true);
                builder.append(charSequence);
            }
            holder.text_view.setText(builder);
            holder.text_view.setLinksClickable(true);
            holder.text_view.setMovementMethod(LinkMovementClickMethod.getInstance());

            holder.text_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (comment.getUserId().equals(mLoginUserId)) {
                        // 如果消息是我发的，那么就弹出删除和复制的对话框
                        showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
                    } else {
                        // 弹出回复的框
                        String toShowName = getShowName(comment.getUserId(), comment.getNickName());
                        if (mContext instanceof BusinessCircleActivity) {
                            ((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition, comment.getUserId(), comment.getNickName(), toShowName);
                        } else {
                            EventBus.getDefault().post(new MessageEventReply("Reply", comment, messagePosition, toShowName));
                        }
                    }
                }
            });

            holder.text_view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
                    return true;
                }
            });

            return convertView;
        }

        public boolean isLoading() {
            return loading;
        }

        public void setLoading(boolean loading) {
            this.loading = loading;
        }

        public void addComment(Comment comment) {
            this.datas.add(0, comment);
            notifyDataSetChanged();
        }
    }

    private class SingleImageClickListener implements View.OnClickListener {
        private String url;

        SingleImageClickListener(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGE_URI, url);
            mContext.startActivity(intent);
        }
    }

    private class MultipleImagesClickListener implements AdapterView.OnItemClickListener {
        private List<Resource> images;

        MultipleImagesClickListener(List<Resource> images) {
            this.images = images;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (images == null || images.size() <= 0) {
                return;
            }
            ArrayList<String> lists = new ArrayList<String>();
            for (int i = 0; i < images.size(); i++) {
                lists.add(images.get(i).getOriginalUrl());
            }
            Intent intent = new Intent(mContext, MultiImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGES, lists);
            intent.putExtra(AppConstant.EXTRA_POSITION, position);
            intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
            mContext.startActivity(intent);
        }
    }
}

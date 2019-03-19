package com.sk.weichat.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.adapter.MessageEventHongdian;
import com.sk.weichat.adapter.PublicMessageAdapter;
import com.sk.weichat.bean.EventAvatarUploadSuccess;
import com.sk.weichat.bean.MyZan;
import com.sk.weichat.bean.circle.Comment;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.CircleMessageDao;
import com.sk.weichat.db.dao.MyZanDao;
import com.sk.weichat.db.dao.UserAvatarDao;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.circle.MessageEventComment;
import com.sk.weichat.ui.circle.MessageEventNotifyDynamic;
import com.sk.weichat.ui.circle.MessageEventReply;
import com.sk.weichat.ui.circle.SelectPicPopupWindow;
import com.sk.weichat.ui.circle.range.NewZanActivity;
import com.sk.weichat.ui.circle.range.SendAudioActivity;
import com.sk.weichat.ui.circle.range.SendFileActivity;
import com.sk.weichat.ui.circle.range.SendShuoshuoActivity;
import com.sk.weichat.ui.circle.range.SendVideoActivity;
import com.sk.weichat.ui.mucfile.UploadingHelper;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.LogUtils;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.view.TrillCommentInputDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

/**
 * 朋友圈的Fragment
 * Created by Administrator
 */

public class DiscoverFragment extends EasyFragment {
    private static final int REQUEST_CODE_SEND_MSG = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static int PAGER_SIZE = 10;
    private String mUserId;
    private String mUserName;
    private TextView mTvTitle;
    private ImageView mIvTitleRight;
    private SelectPicPopupWindow menuWindow;
    // 头部
    private View mHeadView;
    private ImageView ivHeadBg, ivHead;
    // 通知...
    private LinearLayout mTipLl;
    private ImageView mTipIv;
    private TextView mTipTv;
    // 页面
    private PullToRefreshListView mListView;
    private PublicMessageAdapter mAdapter;
    private List<PublicMessage> mMessages = new ArrayList<>();
    private boolean more;
    private String messageId;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            menuWindow.dismiss();
            if(!UiUtils.isNormalClick()){
                return;
            }

            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.btn_send_picture:
                    // 发表图文，
                    intent.setClass(getActivity(), SendShuoshuoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_voice:
                    // 发表语音
                    intent.setClass(getActivity(), SendAudioActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_video:
                    // 发表视频
                    intent.setClass(getActivity(), SendVideoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_file:
                    // 发表文件
                    intent.setClass(getActivity(), SendFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.new_comment:
                    // 最新评论&赞
                    Intent intent2 = new Intent(getActivity(), NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    mTipLl.setVisibility(View.GONE);
                    EventBus.getDefault().post(new MessageEventHongdian(0));
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_discover;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        initActionBar();
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + coreManager.getSelf().getUserId()
                + File.separator + Environment.DIRECTORY_MOVIES);// 初始化视频下载目录
        initViews();
        initData();
    }

    public void onDestroy() {
        super.onDestroy();
        // 退出页面时关闭视频和语音，
        JCVideoPlayer.releaseAllVideos();
        if (mAdapter != null) {
            mAdapter.stopVoice();
        }
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        if (coreManager.getConfig().newUi) {
            findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requireActivity().finish();
                }
            });
        } else {
            findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        }
        mTvTitle = ((TextView) findViewById(R.id.tv_title_center));
        mTvTitle.setText(getString(R.string.life_circle));
        mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        mIvTitleRight.setImageResource(R.drawable.ic_app_add);
        mIvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!UiUtils.isNormalClick()){
                    return;
                }
                menuWindow = new SelectPicPopupWindow(getActivity(), itemsOnClick);
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                menuWindow.showAsDropDown(v,
                        -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                        0);
            }
        });
    }

    public void initViews() {
        more = true;
        mUserId = coreManager.getSelf().getUserId();
        mUserName = coreManager.getSelf().getNickName();

        // ---------------------------初始化头部-----------------------
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mHeadView = inflater.inflate(R.layout.space_cover_view, null);
        ivHeadBg = (ImageView) mHeadView.findViewById(R.id.cover_img);
        ivHeadBg.setOnClickListener(v -> {
            if (UiUtils.isNormalClick()) {
                changeBackgroundImage();
            }
        });
        ivHead = (ImageView) mHeadView.findViewById(R.id.avatar_img);
        ivHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isNormalClick()){
                    return;
                }
                Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                startActivity(intent);
            }
        });

        displayAvatar();

        mTipLl = (LinearLayout) mHeadView.findViewById(R.id.tip_ll);
        mTipIv = (ImageView) mHeadView.findViewById(R.id.tip_avatar);
        mTipTv = (TextView) mHeadView.findViewById(R.id.tip_content);
        mTipLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipLl.setVisibility(View.GONE);
                EventBus.getDefault().post(new MessageEventHongdian(0));

                Intent intent = new Intent(getActivity(), NewZanActivity.class);
                intent.putExtra("OpenALL", false); // 是否展示全部还是单条
                startActivity(intent);
            }
        });

        // ---------------------------初始化主视图-----------------------
        mListView = (PullToRefreshListView) findViewById(R.id.discover_listview);
        mListView.getRefreshableView().addHeaderView(mHeadView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        mListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                requestData(true);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                requestData(false);
            }
        });

        EventBus.getDefault().register(this);
    }

    private void changeBackgroundImage() {
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);
    }

    private void updateBackgroundImage(String path) {
        File bg = new File(path);
        if (!bg.exists()) {
            LogUtils.log(path);
            Reporter.unreachable();
            ToastUtil.showToast(requireContext(), R.string.image_not_found);
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(requireActivity());
        UploadingHelper.upfile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), new File(path), new UploadingHelper.OnUpFileListener() {
            @Override
            public void onSuccess(String url, String filePath) {
                Map<String, String> params = new HashMap<>();
                params.put("access_token", coreManager.getSelfStatus().accessToken);
                params.put("msgBackGroundUrl", url);

                HttpUtils.get().url(coreManager.getConfig().USER_UPDATE)
                        .params(params)
                        .build()
                        .execute(new BaseCallback<Void>(Void.class) {

                            @Override
                            public void onResponse(ObjectResult<Void> result) {
                                DialogHelper.dismissProgressDialog();
                                coreManager.getSelf().setMsgBackGroundUrl(url);
                                UserDao.getInstance().updateMsgBackGroundUrl(coreManager.getSelf().getUserId(), url);
                                if (getContext() == null) {
                                    return;
                                }
                                displayAvatar();
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                DialogHelper.dismissProgressDialog();
                                if (getContext() == null) {
                                    return;
                                }
                                ToastUtil.showErrorNet(requireContext());
                            }
                        });
            }

            @Override
            public void onFailure(String err, String filePath) {
                DialogHelper.dismissProgressDialog();
                if (getContext() == null) {
                    return;
                }
                ToastUtil.showErrorNet(requireContext());
            }
        });

    }

    public void initData() {
        mAdapter = new PublicMessageAdapter(getActivity(), coreManager, mMessages);
        mListView.getRefreshableView().setAdapter(mAdapter);
        requestData(true);
    }

    private void requestData(boolean isPullDownToRefresh) {
        if (isPullDownToRefresh) {// 上拉刷新
            updateTip();
            messageId = null;
            if (mMessages != null) {
                mMessages.clear();
            }
            more = true;
        }

        if (!more) {
            // ToastUtil.showToast(getContext(), getString(R.string.tip_last_item));
            mListView.setReleaseLabel(getString(R.string.tip_last_item));
            mListView.setRefreshingLabel(getString(R.string.tip_last_item));
            refreshComplete();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageSize", String.valueOf(PAGER_SIZE));
        if (messageId != null) {
            params.put("messageId", messageId);
        }

        HttpUtils.get().url(coreManager.getConfig().MSG_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(ArrayResult<PublicMessage> result) {
                        List<PublicMessage> data = result.getData();
                        if (data != null && data.size() > 0) {
                            mMessages.addAll(data);
                            // 记录最后一条说说的id
                            messageId = data.get(data.size() - 1).getMessageId();
                            if (data.size() == PAGER_SIZE) {
                                more = true;
                            } else {
                                // 服务器返回未满10条，下拉不在去请求
                                more = false;
                            }
                        } else {
                            // 服务器未返回数据，下拉不再去请求
                            more = false;
                        }
                        mAdapter.notifyDataSetChanged();
                        refreshComplete();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        refreshComplete();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_SEND_MSG) {
            // 发布说说成功,刷新Fragment
            String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
            CircleMessageDao.getInstance().addMessage(mUserId, messageId);
            requestData(true);
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            if (data != null && data.getData() != null) {
                String path = CameraUtil.getImagePathFromUri(requireContext(), data.getData());
                updateBackgroundImage(path);
            } else {
                ToastUtil.showToast(requireContext(), R.string.c_photo_album_failed);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventAvatarUploadSuccess message) {
        if (message.event) {// 头像更新了，但该界面没有被销毁，不会去重新加载头像，所以这里更新一下
            displayAvatar();
        }
    }

    public void displayAvatar() {
        // 加载小头像，
        AvatarHelper.getInstance().displayAvatar(mUserId, ivHead, true);
        // 优先加载user信息中的背景图片，失败就加载头像，
        String bg = coreManager.getSelf().getMsgBackGroundUrl();
        if (TextUtils.isEmpty(bg)) {
            realDisplayAvatar();
        }
        Glide.with(requireContext().getApplicationContext())
                .load(bg)
                .placeholder(R.drawable.avatar_normal)
                .dontAnimate()
                .into(new SimpleTarget<GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        ivHeadBg.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        realDisplayAvatar();
                    }
                });

    }

    private void realDisplayAvatar() {
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(mUserId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(mUserId);

            Glide.with(MyApplication.getContext())
                    .load(mOriginalUrl)
                    .placeholder(R.drawable.avatar_normal)
                    .signature(new StringSignature(time))
                    .dontAnimate()
                    .error(R.drawable.avatar_normal)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            ivHeadBg.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            Log.e("zq", "加载原图失败：" + mOriginalUrl);
                        }
                    });
        } else {
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        if (message.message.equals("prepare")) {// 准备播放视频，关闭语音播放
            mAdapter.stopVoice();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventNotifyDynamic message) {
        // 收到赞 || 评论 || 提醒我看 协议 刷新页面
        requestData(true);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventComment message) {
        if (message.event.equals("Comment")) {
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(getActivity(), InternationalizationHelper.getString("ENTER_PINLUNT"),
                    str -> {
                        Comment mComment = new Comment();
                        Comment comment = mComment.clone();
                        if (comment == null)
                            comment = new Comment();
                        comment.setBody(str);
                        comment.setUserId(mUserId);
                        comment.setNickName(mUserName);
                        comment.setTime(TimeUtils.sk_time_current_time());
                        addComment(message, comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventReply message) {
        if (message.event.equals("Reply")) {
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(getActivity(), InternationalizationHelper.getString("JX_Reply") + "：" + message.comment.getNickName(),
                    str -> {
                        Comment mComment = new Comment();
                        Comment comment = mComment.clone();
                        if (comment == null)
                            comment = new Comment();
                        comment.setToUserId(message.comment.getUserId());
                        comment.setToNickname(message.comment.getNickName());
                        comment.setToBody(message.comment.getToBody());
                        comment.setBody(str);
                        comment.setUserId(mUserId);
                        comment.setNickName(mUserId);
                        comment.setTime(TimeUtils.sk_time_current_time());
                        Reply(message.id, comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        }
    }

    /**
     * 停止刷新动画
     */
    private void refreshComplete() {
        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.onRefreshComplete();
            }
        }, 200);
    }

    public void updateTip() {
        int tipCount = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        if (tipCount == 0) {
            mTipLl.setVisibility(View.GONE);
            EventBus.getDefault().post(new MessageEventHongdian(0));
        } else {
            List<MyZan> zanList = MyZanDao.getInstance().queryZan(coreManager.getSelf().getUserId());
            if (zanList == null || zanList.size() == 0) {
                return;
            }
            MyZan zan = zanList.get(zanList.size() - 1);
            AvatarHelper.getInstance().displayAvatar(zan.getFromUserId(), mTipIv, true);
            mTipTv.setText(tipCount + InternationalizationHelper.getString("JX_PieceNewMessage"));
            mTipLl.setVisibility(View.VISIBLE);
            EventBus.getDefault().post(new MessageEventHongdian(tipCount));
        }
    }

    private void addComment(MessageEventComment message, final Comment comment) {
        String messageId = message.id;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        if (comment.isReplaySomeBody()) {
            params.put("toUserId", comment.getToUserId() + "");
            params.put("toNickname", comment.getToNickname());
            params.put("toBody", comment.getToBody());
        }
        params.put("body", comment.getBody());

        HttpUtils.get().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        if (message.view.getTag() == message.pbmessage) {
                            PublicMessageAdapter.CommentAdapter adapter = (PublicMessageAdapter.CommentAdapter) message.view.getAdapter();
                            adapter.addComment(comment);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    /**
     * 回复
     */
    private void Reply(final int position, final Comment comment) {
        final PublicMessage message = mMessages.get(position);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());

        if (!TextUtils.isEmpty(comment.getToUserId())) {
            params.put("toUserId", comment.getToUserId());
        }
        if (!TextUtils.isEmpty(comment.getToNickname())) {
            params.put("toNickname", comment.getToNickname());
        }
        params.put("body", comment.getBody());

        HttpUtils.get().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        List<Comment> comments = message.getComments();
                        if (comments == null) {
                            comments = new ArrayList<>();
                            Log.e("TAG", "comments"+comments);
                            message.setComments(comments);
                        }
                        comment.setCommentId(result.getData());
                        comments.add(0, comment);
                        requestData(true);
                        showToCurrent(result.getData());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    private void setReply(){

    }


    /**
     * 定位到评论位置
     */
    public void showToCurrent(String mCommentId) {
        int pos = -1;
        for (int i = 0; i < mMessages.size(); i++) {
            if (StringUtils.strEquals(mCommentId, mMessages.get(i).getMessageId())) {
                pos = i + 2;
                break;
            }
        }
        // 如果找到就定位到这条说说
        if (pos != -1) {
            mListView.getRefreshableView().setSelection(pos);
        }
    }

}

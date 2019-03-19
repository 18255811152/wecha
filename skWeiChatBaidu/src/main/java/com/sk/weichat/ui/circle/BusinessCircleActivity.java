package com.sk.weichat.ui.circle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sk.weichat.AppConfig;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.PublicMessageAdapter;
import com.sk.weichat.bean.circle.Comment;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.CircleMessageDao;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.FileDataHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.range.NewZanActivity;
import com.sk.weichat.ui.circle.range.SendAudioActivity;
import com.sk.weichat.ui.circle.range.SendFileActivity;
import com.sk.weichat.ui.circle.range.SendShuoshuoActivity;
import com.sk.weichat.ui.circle.range.SendVideoActivity;
import com.sk.weichat.ui.circle.util.RefreshListImp;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.PMsgBottomView;
import com.sk.weichat.view.ResizeLayout;
import com.sk.weichat.volley.ArrayResult;
import com.sk.weichat.volley.StringJsonArrayRequest;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 我的商务圈
 */
public class BusinessCircleActivity extends BaseActivity implements showCEView, RefreshListImp {
    private static final int REQUEST_CODE_SEND_MSG = 1;
    // 自定义的弹出框类
    SelectPicPopupWindow menuWindow;
    /**
     * 接口,调用外部类的方法,让应用不可见时停止播放声音
     */
    ListenerAudio listener;
    CommentReplyCache mCommentReplyCache = null;
    private int mType;
    /* mPageIndex仅用于商务圈情况下 */
    private int mPageIndex = 0;
    private PullToRefreshListView mPullToRefreshListView;
    /* 封面视图 */
    private View mMyCoverView;   // 封面root view
    private ImageView mCoverImg; // 封面图片ImageView
    private ImageView mAvatarImg;// 用户头像
    private ResizeLayout mResizeLayout;
    private PMsgBottomView mPMsgBottomView;
    private List<PublicMessage> mMessages = new ArrayList<>();
    private PublicMessageAdapter mAdapter;
    private String mLoginUserId;       // 当前登陆用户的UserId
    private String mLoginNickName;// 当前登陆用户的昵称
    private boolean isdongtai;
    private String cricleid;
    private String pinglun;
    private String dianzan;
    /* 当前选择的是哪个用户的个人空间,仅用于查看个人空间的情况下 */
    private String mUserId;
    private String mNickName;
    private ImageView mIvTitleLeft;
    private TextView mTvTitle;
    private ImageView mIvTitleRight;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            menuWindow.dismiss();
            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.btn_send_picture:// 发表图文，
                    intent.setClass(getApplicationContext(), SendShuoshuoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_voice:  // 发表语音
                    intent.setClass(getApplicationContext(), SendAudioActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_video:  // 发表视频
                    intent.setClass(getApplicationContext(), SendVideoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_file:   // 发表文件
                    intent.setClass(getApplicationContext(), SendFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.new_comment:     // 最新评论
                    Intent intent2 = new Intent(getApplicationContext(), NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_circle);
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();

        if (getIntent() != null) {
            mType = getIntent().getIntExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_MY_BUSINESS);// 默认的为查看我的商务圈
            mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            mNickName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);

            pinglun = getIntent().getStringExtra("pinglun");
            dianzan = getIntent().getStringExtra("dianzan");
            isdongtai = getIntent().getBooleanExtra("isdongtai", false);
            cricleid = getIntent().getStringExtra("messageid");
        }

        if (!isMyBusiness()) {// 如果查看的是个人空间的话，那么mUserId必须要有意义
            if (TextUtils.isEmpty(mUserId)) {// 没有带userId参数，那么默认看的就是自己的空间
                mUserId = mLoginUserId;
                mNickName = mLoginNickName;
            }
        }

       /* if (mUserId != null && mUserId.equals(mLoginUserId)) {
            String mLastMessage = PreferenceUtils.getString(this, "BUSINESS_CIRCLE_DATA");
            if (!TextUtils.isEmpty(mLastMessage)) {
                mMessages = JSON.parseArray(mLastMessage, PublicMessage.class);
            }
        }*/

        initActionBar();
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + coreManager.getSelf().getUserId()
                + File.separator + Environment.DIRECTORY_MOVIES);// 初始化视频下载目录
        initView();
    }

    private boolean isMyBusiness() {
        return mType == AppConstant.CIRCLE_TYPE_MY_BUSINESS;
    }

    private boolean isMySpace() {
        return mLoginUserId.equals(mUserId);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(mNickName);
        mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        if (mUserId.equals(mLoginUserId)) {// 查看自己的空间才有发布按钮
            mIvTitleRight.setImageResource(R.drawable.ic_app_add);
            mIvTitleRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuWindow = new SelectPicPopupWindow(BusinessCircleActivity.this, itemsOnClick);
                    // 在获取宽高之前需要先测量，否则得不到宽高
                    menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    // +x右,-x左,+y下,-y上
                    // pop向左偏移显示
                    menuWindow.showAsDropDown(v,
                            -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                            0);
                }
            });
        }
    }

    private void initView() {
        initCoverView();
        mResizeLayout = (ResizeLayout) findViewById(R.id.resize_layout);
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        mPMsgBottomView = (PMsgBottomView) findViewById(R.id.bottom_view);
       /* mResizeLayout.setOnResizeListener(new ResizeLayout.OnResizeListener() {
            @Override
            public void OnResize(int w, int h, int oldw, int oldh) {
                if (oldh < h) {// 键盘被隐藏
                    mCommentReplyCache = null;
                    mPMsgBottomView.setHintText("");
                    mPMsgBottomView.reset();
                }
            }
        });*/

        mPMsgBottomView.setPMsgBottomListener(new PMsgBottomView.PMsgBottomListener() {
            @Override
            public void sendText(String text) {
                if (mCommentReplyCache != null) {
                    mCommentReplyCache.text = text;
                    addComment(mCommentReplyCache);
                    mPMsgBottomView.hide();
                }
            }
        });

        if (isdongtai) {
            // 如果是动态，不添加HeadView
        } else {
            mPullToRefreshListView.getRefreshableView().addHeaderView(mMyCoverView, null, false);
        }

        mAdapter = new PublicMessageAdapter(this, coreManager, mMessages);
        setListenerAudio(mAdapter);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);

        if (isdongtai) {
            mPullToRefreshListView.setReflashable(false);
        }
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {

            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                requestData(true);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                requestData(false);
            }
        });

        mPullToRefreshListView.getRefreshableView().setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        if (mPMsgBottomView.getVisibility() != View.GONE) {
                            mPMsgBottomView.hide();
                        }
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    }
                });

        if (isMyBusiness()) {
            readFromLocal();
        } else {
            requestData(true);
        }
    }

    private void initCoverView() {
        mMyCoverView = LayoutInflater.from(this).inflate(R.layout.space_cover_view, null);
        mCoverImg = (ImageView) mMyCoverView.findViewById(R.id.cover_img);
        mAvatarImg = (ImageView) mMyCoverView.findViewById(R.id.avatar_img);
        // 头像
        if (isMyBusiness() || isMySpace()) {
            AvatarHelper.getInstance().displayAvatar(mLoginUserId, mAvatarImg, true);
            AvatarHelper.getInstance().displayAvatar(mLoginUserId, mCoverImg, false);
        } else {
            AvatarHelper.getInstance().displayAvatar(mUserId, mAvatarImg, true);
            AvatarHelper.getInstance().displayAvatar(mUserId, mCoverImg, false);
        }
        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {// 进入个人资料页
                Intent intent = new Intent(getApplicationContext(), BasicInfoActivity.class);
                if (isMyBusiness() || isMySpace()) {
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
                } else {
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                }
                startActivity(intent);
            }
        });
    }

    private void readFromLocal() {
        FileDataHelper.readArrayData(getApplicationContext(), mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE, new StringJsonArrayRequest.Listener<PublicMessage>() {
            @Override
            public void onResponse(ArrayResult<PublicMessage> result) {
                if (result != null && result.getData() != null) {
                    mMessages.clear();
                    mMessages.addAll(result.getData());
                    mAdapter.notifyDataSetInvalidated();
                }
                requestData(true);
            }
        }, PublicMessage.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (mPMsgBottomView != null && mPMsgBottomView.getVisibility() == View.VISIBLE) {
            mPMsgBottomView.hide();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.ideChange();
        }
        listener = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       /* if (mUserId.equals(mLoginUserId)) {
            if (mMessages != null && mMessages.size() > 0) {
                PreferenceUtils.putString(this, "BUSINESS_CIRCLE_DATA", JSON.toJSONString(mMessages));
            }
        }*/
    }

    public void setListenerAudio(ListenerAudio listener) {
        this.listener = listener;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SEND_MSG) {
            if (resultCode == Activity.RESULT_OK) {// 发说说成功
                String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
                CircleMessageDao.getInstance().addMessage(mLoginUserId, messageId);
                requestData(true);
                removeNullTV();
            }
        }
    }

    /********** 公共消息的数据请求部分 *********/

    /**
     * 请求公共消息
     *
     * @param isPullDwonToRefersh 是下拉刷新，还是上拉加载
     */
    private void requestData(boolean isPullDwonToRefersh) {
        if (isMyBusiness()) {
            requestMyBusiness(isPullDwonToRefersh);
        } else {
            if (isdongtai) {
                requestSpacedongtai(isPullDwonToRefersh);
            } else {
                requestSpace(isPullDwonToRefersh);
            }
        }
    }

    private void requestMyBusiness(final boolean isPullDwonToRefersh) {
        if (isPullDwonToRefersh) {
            mPageIndex = 0;
        }
        List<String> msgIds = CircleMessageDao.getInstance().getCircleMessageIds(mLoginUserId, mPageIndex, AppConfig.PAGE_SIZE);

        if (msgIds == null || msgIds.size() <= 0) {
            mPullToRefreshListView.onRefreshComplete(200);
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("ids", JSON.toJSONString(msgIds));

        HttpUtils.get().url(coreManager.getConfig().MSG_GETS)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ArrayResult<PublicMessage> result) {
                        List<PublicMessage> data = result.getData();
                        if (isPullDwonToRefersh) {
                            mMessages.clear();
                        }
                        if (data != null && data.size() > 0) {// 没有更多数据
                            mPageIndex++;
                            if (isPullDwonToRefersh) {
                                FileDataHelper.writeFileData(getApplicationContext(), mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE, result);
                            }
                            mMessages.addAll(data);
                        }
                        mAdapter.notifyDataSetChanged();

                        mPullToRefreshListView.onRefreshComplete();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                        mPullToRefreshListView.onRefreshComplete();
                    }
                });
    }

    private void requestSpace(final boolean isPullDwonToRefersh) {
        String messageId = null;
        if (!isPullDwonToRefersh && mMessages.size() > 0) {
            messageId = mMessages.get(mMessages.size() - 1).getMessageId();
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mUserId);
        params.put("flag", PublicMessage.FLAG_NORMAL + "");

        if (!TextUtils.isEmpty(messageId)) {
            if (isdongtai) {
                params.put("messageId", cricleid);
            } else {
                params.put("messageId", messageId);
            }
        }
        params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));

        HttpUtils.get().url(coreManager.getConfig().MSG_USER_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ArrayResult<PublicMessage> result) {
                        List<PublicMessage> data = result.getData();
                        if (isPullDwonToRefersh) {
                            mMessages.clear();
                        }
                        if (data != null && data.size() > 0) {
                            mMessages.addAll(data);
                        }
                        mAdapter.notifyDataSetChanged();

                        mPullToRefreshListView.onRefreshComplete();
                        if (mAdapter.isEmpty())
                            addNullTV2LV();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                        mPullToRefreshListView.onRefreshComplete();
                    }
                });
    }

    // 最近评论&赞进入
    private void requestSpacedongtai(final boolean isPullDwonToRefersh) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", cricleid);

        HttpUtils.get().url(coreManager.getConfig().MSG_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<PublicMessage> result) {
                        PublicMessage datas = result.getData();
                        List<PublicMessage> datass = new ArrayList<>();
                        datass.add(datas);
                        if (isPullDwonToRefersh) {
                            mMessages.clear();
                        }
                        mMessages.addAll(datass);
                        mAdapter.notifyDataSetChanged();

                        mPullToRefreshListView.onRefreshComplete();
                        if (mAdapter.isEmpty())
                            addNullTV2LV();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                        mPullToRefreshListView.onRefreshComplete();
                    }
                });
    }

    public void showCommentEnterView(int messagePosition, String toUserId, String toNickname, String toShowName) {
        mCommentReplyCache = new CommentReplyCache();
        mCommentReplyCache.messagePosition = messagePosition;
        mCommentReplyCache.toUserId = toUserId;
        mCommentReplyCache.toNickname = toNickname;
        if (TextUtils.isEmpty(toUserId) || TextUtils.isEmpty(toNickname) || TextUtils.isEmpty(toShowName)) {
            mPMsgBottomView.setHintText("");
        } else {
            mPMsgBottomView.setHintText(getString(R.string.replay_text, toShowName));
        }
        mPMsgBottomView.show();
    }

    private void addComment(CommentReplyCache cache) {
        Comment comment = new Comment();
        comment.setUserId(mLoginUserId);
        comment.setNickName(mLoginNickName);
        comment.setToUserId(cache.toUserId);
        comment.setToNickname(cache.toNickname);
        comment.setBody(cache.text);
        addComment(cache.messagePosition, comment);
    }

    private void addComment(final int position, final Comment comment) {
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
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<String> result) {
                        List<Comment> comments = message.getComments();
                        if (comments == null) {
                            comments = new ArrayList<>();
                            message.setComments(comments);
                        }
                        comment.setCommentId(result.getData());
                        comments.add(comment);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                    }
                });
    }

    @Override
    public void showView(int messagePosition, String toUserId, String toNickname, String toShowName) {
        showCommentEnterView(messagePosition, toUserId, toNickname, toShowName);
    }

    @Override
    public void refreshAfterOperation(PublicMessage message) {
        int size = mMessages.size();
        for (int i = 0; i < size; i++) {
            if (StringUtils.strEquals(mMessages.get(i).getMessageId(), message.getMessageId())) {
                mMessages.set(i, message);
                mAdapter.setData(mMessages);
            }
        }
    }

    public void addNullTV2LV() {
        TextView nullTextView = new TextView(this);
        nullTextView.setTag("NullTV");
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int paddingSize = getResources().getDimensionPixelSize(R.dimen.NormalPadding);
        nullTextView.setPadding(0, paddingSize, 0, paddingSize);
        nullTextView.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        nullTextView.setGravity(Gravity.CENTER);

        nullTextView.setLayoutParams(lp);
        nullTextView.setText(InternationalizationHelper.getString("JX_NoData"));
        mPullToRefreshListView.getRefreshableView().addFooterView(nullTextView);
        mPullToRefreshListView.setReflashable(false);
    }

    public void removeNullTV() {
        mPullToRefreshListView.getRefreshableView().removeFooterView(mPullToRefreshListView.findViewWithTag("NullTV"));
        mPullToRefreshListView.setReflashable(true);
    }

    public interface ListenerAudio {
        void ideChange();
    }

    class CommentReplyCache {
        int messagePosition;// 消息的Position
        String toUserId;
        String toNickname;
        String text;
    }
}

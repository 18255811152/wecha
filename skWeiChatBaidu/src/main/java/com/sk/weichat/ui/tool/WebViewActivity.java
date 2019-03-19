package com.sk.weichat.ui.tool;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.R;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.range.SendShuoshuoActivity;
import com.sk.weichat.ui.message.InstantMessageActivity;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.JsonUtils;
import com.sk.weichat.util.PermissionUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.ComplaintDialog;
import com.sk.weichat.view.ExternalOpenDialog;
import com.sk.weichat.view.MatchKeyWordEditDialog;
import com.sk.weichat.view.ModifyFontSizeDialog;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.view.WebMoreDialog;
import com.sk.weichat.view.window.WindowShowService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

/**
 * web
 */
public class WebViewActivity extends BaseActivity {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_DOWNLOAD_URL = "download_url";

    public static String FLOATING_WINDOW_URL;
    public static boolean IS_FLOATING;

    private TextView mTitleTv;
    private ImageView mTitleLeftIv;
    private ImageView mTitleRightIv;

    private ProgressBar mProgressBar;
    private WebView mWebView;
    private boolean isAnimStart = false;
    private int currentProgress;

    private String mUrl; // 网址URL
    private String mDownloadUrl;// ShareSdk 分享链接进来的应用下载地址(跳转，当本地不存在对应应用时使用)

    public static void start(Context ctx, String url) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        if (getIntent() != null) {
            mUrl = getIntent().getStringExtra(EXTRA_URL);
            mDownloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);

            initActionBar();
            initView();
            initClient();
            initEvent();

            stopService(new Intent(WebViewActivity.this, WindowShowService.class));
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //点击悬浮窗进来之后是不会有弹窗的，返回之后才有，这里判断一下进来的场景
        if (IS_FLOATING) {
            getCurrentUrl();
            startService(new Intent(WebViewActivity.this, WindowShowService.class));
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleLeftIv = findViewById(R.id.iv_title_left);
        mTitleLeftIv.setImageResource(R.drawable.icon_close);
        mTitleRightIv = findViewById(R.id.iv_title_right);
        mTitleRightIv.setImageResource(R.drawable.chat_more);
    }

    private void initView() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mWebView = (WebView) findViewById(R.id.mWebView);
        /* 设置支持Js */
        mWebView.getSettings().setJavaScriptEnabled(true);
        /* 设置为true表示支持使用js打开新的窗口 */
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        /* 设置缓存模式 */
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setDomStorageEnabled(true);

        /* 设置为使用webview推荐的窗口 */
        mWebView.getSettings().setUseWideViewPort(true);
        /* 设置为使用屏幕自适配 */
        mWebView.getSettings().setLoadWithOverviewMode(true);
        /* 设置是否允许webview使用缩放的功能,我这里设为false,不允许 */
        mWebView.getSettings().setBuiltInZoomControls(false);
        /* 提高网页渲染的优先级 */
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        /* HTML5的地理位置服务,设置为true,启用地理定位 */
        mWebView.getSettings().setGeolocationEnabled(true);
        /* 设置可以访问文件 */
        mWebView.getSettings().setAllowFileAccess(true);

        int openStatus = openApp(mUrl);
        if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
            finish();
        } else if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应该，加载该应用下载地址
            mWebView.loadUrl(mDownloadUrl);
        } else {
            mWebView.loadUrl(mUrl);
        }
    }

    private void initClient() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setAlpha(1.0f);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                int openStatus = openApp(url);
                if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
                    return true;
                } else if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应该，加载该应用下载地址
                    view.loadUrl(mDownloadUrl);
                } else {
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        // 获取网页加载进度
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                currentProgress = mProgressBar.getProgress();
                if (newProgress >= 100 && !isAnimStart) {
                    // 防止调用多次动画
                    isAnimStart = true;
                    mProgressBar.setProgress(newProgress);
                    // 开启属性动画让进度条平滑消失
                    startDismissAnimation(mProgressBar.getProgress());
                } else {
                    // 开启属性动画让进度条平滑递增
                    startProgressAnimation(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                mTitleTv.setText(title);
            }
        });

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    // 不处理下载，直接抛出去，
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (Exception ignored) {
                    // 无论如何不要崩溃，比如没有浏览器，
                    ToastUtil.showToast(WebViewActivity.this, R.string.download_error);
                }
            }
        });
    }

    private void initEvent() {
        mTitleRightIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WebMoreDialog mWebMoreDialog = new WebMoreDialog(WebViewActivity.this, getCurrentUrl(), new WebMoreDialog.BrowserActionClickListener() {
                    @Override
                    public void floatingWindow() {
                        showFloating();
                    }

                    @Override
                    public void sendToFriend() {
                        forwardToFriend();
                    }

                    @Override
                    public void shareToLifeCircle() {
                        shareMoment();
                    }

                    @Override
                    public void collection() {
                        onCollection(getCurrentUrl());
                    }

                    @Override
                    public void searchContent() {
                        search();
                    }

                    @Override
                    public void copyLink() {
                        ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboardManager.setText(getCurrentUrl());
                        Toast.makeText(WebViewActivity.this, getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void openOutSide() {
                        ExternalOpenDialog externalOpenDialog = new ExternalOpenDialog(mContext, getCurrentUrl());
                        externalOpenDialog.show();
                    }

                    @Override
                    public void modifyFontSize() {
                        setWebFontSiz();
                    }

                    @Override
                    public void refresh() {
                        mWebView.reload();
                    }

                    @Override
                    public void complaint() {
                        report();
                    }
                });
                mWebMoreDialog.show();
            }
        });
    }

    /**
     * 根据url跳转至其他app
     */
    private int openApp(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        try {
            if (!url.startsWith("http") && !url.startsWith("https") && !url.startsWith("ftp")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                // host 和 scheme 都不能为null
                if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(scheme)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (AppUtils.isSupportIntent(this, intent)) {
                        startActivity(intent);
                        return 1;
                    } else {
                        return 2;
                    }
                }
            }
        } catch (Exception e) {
            return 3;
        }
        return 4;
    }

    /****************************************************
     * Start
     ***************************************************/
    private String getCurrentUrl() {
        Log.e("TAG_CURRENT_URL", mWebView.getUrl());
        Log.e("TAG_CURRENT_URL", mWebView.getOriginalUrl());
        String currentUrl = mWebView.getOriginalUrl();
        if (currentUrl.contains("?")) {// ?号后面的都不显示
            currentUrl = currentUrl.substring(0, currentUrl.indexOf("?"));
        }

        FLOATING_WINDOW_URL = currentUrl;

        if (currentUrl.contains("https://view.officeapps.live.com/op/view.aspx?src=")) {
            currentUrl = currentUrl.replace("https://view.officeapps.live.com/op/view.aspx?src=", "");
        }

        return currentUrl;
    }

    /**
     * 浮窗
     */
    private void showFloating() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || AppUtils.checkAlertWindowsPermission(this)) {
            IS_FLOATING = !IS_FLOATING;
            if (IS_FLOATING) {
                startService(new Intent(WebViewActivity.this, WindowShowService.class));
                finish();
            } else {
                stopService(new Intent(WebViewActivity.this, WindowShowService.class));
            }
        } else {
            SelectionFrame dialog = new SelectionFrame(this);
            dialog.setSomething("悬浮窗权限", "您的手机没有授予悬浮窗权限，请开启后再试", "暂不开启", "现在去开启",
                    new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {

                        }

                        @Override
                        public void confirmClick() {
                            PermissionUtil.startApplicationDetailsSettings(WebViewActivity.this, 0x01);
                        }
                    });
            dialog.show();
        }
    }

    /**
     * 发送给朋友
     */
    private void initChat(String url) {
        String mLoginUserId = coreManager.getSelf().getUserId();

        ChatMessage message = new ChatMessage();
        String title = mTitleTv.getText().toString().trim();
        String content = JsonUtils.initJsonContent(title, getCurrentUrl(), url);
        message.setType(XmppMessage.TYPE_LINK);
        message.setContent(content);
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        // Todo 将封装好的消息存入10010 号的msg 表内，在跳转至转发->聊天界面(跳转传值均为10010号与msgId)，之后在聊天界面内通过这两个值查询到对用消息，发送
        String mNewUserId = "10010";
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mNewUserId, message)) {
            Intent intent = new Intent(WebViewActivity.this, InstantMessageActivity.class);
            intent.putExtra("fromUserId", mNewUserId);
            intent.putExtra("messageId", message.getPacketId());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(mContext, "消息封装失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void forwardToFriend() {
        HtmlFactory.instance().queryImage(getCurrentUrl(), new HtmlFactory.DataListener<String>() {// 检索该网页包含的图片

            @Override
            public void onResponse(List<String> data, String title) {
                // 弹窗让用户选择图片 作为链接封面
                SelectImageDialog dialog = new SelectImageDialog(WebViewActivity.this, data, new SelectImageDialog.OptionListener() {
                    @Override
                    public void option(String url) {
                        initChat(url);
                    }
                });
                dialog.show();
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    /**
     * 分享到生活圈
     */
    private void shareMoment() {
        Intent intent = new Intent(WebViewActivity.this, SendShuoshuoActivity.class);
        intent.putExtra(Constants.BROWSER_SHARE_MOMENTS_CONTENT, getCurrentUrl());
        startActivity(intent);
    }

    /**
     * 收藏
     * 链接 当做 文本类型 收藏
     */
    private String collectionParam(String content) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = CollectionEvery.TYPE_TEXT;
        json.put("type", String.valueOf(type));
        String msg = "";
        String collectContent = "";
        msg = content;
        collectContent = content;
        json.put("msg", msg);
        json.put("collectContent", collectContent);
        json.put("collectType", -1);// 与消息无关的收藏
        array.add(json);
        return JSON.toJSONString(array);
    }

    private void onCollection(final String content) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emoji", collectionParam(content));

        HttpUtils.get().url(coreManager.getConfig().Collection_ADD)
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

    /**
     * 搜索页面内容
     */
    private void search() {
        MatchKeyWordEditDialog matchKeyWordEditDialog = new MatchKeyWordEditDialog(this, mWebView);
        Window window = matchKeyWordEditDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
            matchKeyWordEditDialog.show();
        }
    }

    /**
     * 调整字体
     */
    private void setWebFontSiz() {
        ModifyFontSizeDialog modifyFontSizeDialog = new ModifyFontSizeDialog(this, mWebView);
        modifyFontSizeDialog.show();
    }

    /**
     * 投诉
     */
    private void report() {
        ComplaintDialog complaintDialog = new ComplaintDialog(this, report -> {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("webUrl", getCurrentUrl());
            params.put("reason", String.valueOf(report.getReportId()));
            DialogHelper.showDefaulteMessageProgressDialog(WebViewActivity.this);

            HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(WebViewActivity.this, R.string.report_success);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        });
        complaintDialog.show();
    }

    /****************************************************
     * End
     ***************************************************/

    /**
     * progressBar递增动画
     */
    private void startProgressAnimation(int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", currentProgress, newProgress);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /**
     * progressBar消失动画
     */
    private void startDismissAnimation(final int progress) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mProgressBar, "alpha", 1.0f, 0.0f);
        anim.setDuration(1500);  // 动画时长
        anim.setInterpolator(new DecelerateInterpolator());
        // 关键, 添加动画进度监听器
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fraction = valueAnimator.getAnimatedFraction();      // 0.0f ~ 1.0f
                int offset = 100 - progress;
                mProgressBar.setProgress((int) (progress + offset * fraction));
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                // 动画结束
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.GONE);
                isAnimStart = false;
            }
        });
        anim.start();
    }
}

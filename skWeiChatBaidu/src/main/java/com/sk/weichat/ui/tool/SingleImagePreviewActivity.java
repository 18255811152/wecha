package com.sk.weichat.ui.tool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.example.qrcode.utils.DecodeUtils;
import com.google.zxing.Result;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.EventCreateGroupFriend;
import com.sk.weichat.bean.EventSendVerifyMsg;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.MucRoom;
import com.sk.weichat.broadcast.MucgroupUpdateUtil;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.UserAvatarDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.message.MucChatActivity;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.util.HttpUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.SaveWindow;
import com.sk.weichat.view.VerifyDialog;
import com.sk.weichat.view.ZoomImageView;
import com.sk.weichat.view.chatHolder.MessageEventClickFire;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import me.kareluo.imaging.IMGEditActivity;
import okhttp3.Call;
import pl.droidsonroids.gif.GifDrawable;

/**
 * 单张图片预览
 */
public class SingleImagePreviewActivity extends BaseActivity {
    public static final int REQUEST_IMAGE_EDIT = 1;

    private String mImageUri;
    private String mImagePath;
    private String mEditedPath;
    // 是否为阅后即焚类型   当前图片在消息内的位置
    private String delPackedId;

    private ZoomImageView mImageView;
    private Bitmap mBitmap;// 用于 识别图中二维码
    private String mLoginUserId;
    private SaveWindow mSaveWindow;
    private My_BroadcastReceiver my_broadcastReceiver = new My_BroadcastReceiver();

    @SuppressWarnings("unused")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_image_preview);
        if (getIntent() != null) {
            mImageUri = getIntent().getStringExtra(AppConstant.EXTRA_IMAGE_URI);
            mImagePath = getIntent().getStringExtra("image_path");

            // 这个是阅后即焚消息的 packedId
            delPackedId = getIntent().getStringExtra("DEL_PACKEDID");
        }
        mLoginUserId = coreManager.getSelf().getUserId();

        initView();
        regrist();
    }

    public void doBack() {
        if (!TextUtils.isEmpty(delPackedId)) {
            // 发送广播去更新聊天界面，移除该message
            EventBus.getDefault().post(new MessageEventClickFire("delete", delPackedId));
        }
        finish();
        overridePendingTransition(0, 0);// 关闭过场动画
    }

    @Override
    public void onBackPressed() {
        doBack();
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (my_broadcastReceiver != null) {
            unregisterReceiver(my_broadcastReceiver);
        }
    }

    private void initView() {
        getSupportActionBar().hide();
        mImageView = findViewById(R.id.image_view);
        if (TextUtils.isEmpty(mImageUri)) {
            Toast.makeText(mContext, R.string.image_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        /*
        加载图片 || 头像
         */
        if (mImageUri.contains("http")) {// 图片 头像的mImageUri为UserId
            boolean isExists = false;
            if (!TextUtils.isEmpty(mImagePath)) {
                File file = new File(mImagePath);
                if (file.exists()) {
                    isExists = true;
                }
            }
            if (isExists) {// 本地加载
                if (mImageUri.endsWith(".gif")) {
                    try {
                        GifDrawable gifDrawable = new GifDrawable(new File(mImagePath));
                        mImageView.setImageDrawable(gifDrawable);
                    } catch (Exception e) {
                        mImageView.setImageResource(R.drawable.image_download_fail_icon);
                        e.printStackTrace();
                    }
                } else {
                    Glide.with(mContext)
                            .load(mImagePath)
                            .asBitmap()
                            .centerCrop()
                            .dontAnimate()
                            .error(R.drawable.image_download_fail_icon)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    mBitmap = resource;
                                    mImageView.setImageBitmap(resource);
                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    mImageView.setImageResource(R.drawable.image_download_fail_icon);
                                }
                            });

                }
            } else {// 网络加载
                Glide.with(mContext)
                        .load(mImageUri)
                        .asBitmap()
                        .centerCrop()
                        .dontAnimate()
                        .error(R.drawable.image_download_fail_icon)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                mBitmap = resource;
                                mImageView.setImageBitmap(resource);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                mImageView.setImageResource(R.drawable.image_download_fail_icon);
                            }
                        });
            }
        } else {// 头像
            String time = UserAvatarDao.getInstance().getUpdateTime(mImageUri);
            mImageUri = AvatarHelper.getAvatarUrl(mImageUri, false);// 为头像重新赋值，用于保存功能
            if (TextUtils.isEmpty(mImageUri)) {
                mImageView.setImageResource(R.drawable.avatar_normal);
                return;
            }
            Glide.with(MyApplication.getContext())
                    .load(mImageUri)
                    .signature(new StringSignature(time))
                    .error(R.drawable.avatar_normal)
                    .into(mImageView);
            // AvatarHelper.getInstance().displayAvatar(mImageUri, mImageView, false);// 该方式加载头像不知道为什么头像不居中显示
        }
    }

    private void regrist() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("singledown");
        filter.addAction("longpress");
        registerReceiver(my_broadcastReceiver, filter);
    }

    /*
    扫描二维码之后的处理
     */
    private void handleScanResult(String result) {
        if (result.contains("shikuId") && HttpUtil.isURL(result)) {
            // 视酷二维码
            String action = result.substring(result.indexOf("action=") + 7, result.lastIndexOf("&"));
            String userId = result.substring(result.indexOf("shikuId=") + 8);
            Log.e("zq", action + " , " + userId);
            if (action.equals("group")) {
                getRoomInfo(userId);
            } else if (action.equals("user")) {
                Intent intent = new Intent(this, BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
                startActivity(intent);
            }
        } else if (!result.contains("shikuId")
                && HttpUtil.isURL(result)) {
            // 非视酷二维码  访问其网页
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(WebViewActivity.EXTRA_URL, result);
            startActivity(intent);
        } else {
            ToastUtil.showToast(this, getString(R.string.unrecognized));
        }
    }

    /**
     * 获取房间信息
     */
    private void getRoomInfo(String roomId) {
        Friend friend = FriendDao.getInstance().getMucFriendByRoomId(mLoginUserId, roomId);
        if (friend != null) {
            if (friend.getGroupStatus() == 0) {
                interMucChat(friend.getUserId(), friend.getNickName());
                return;
            } else {// 已被踢出该群组 || 群组已被解散
                FriendDao.getInstance().deleteFriend(mLoginUserId, friend.getUserId());
                ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());
            }
        }

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
                            if (mucRoom.getIsNeedVerify() == 1) {
                                VerifyDialog verifyDialog = new VerifyDialog(SingleImagePreviewActivity.this);
                                verifyDialog.setVerifyClickListener(MyApplication.getInstance().getString(R.string.tip_reason_invite_friends), new VerifyDialog.VerifyClickListener() {
                                    @Override
                                    public void cancel() {

                                    }

                                    @Override
                                    public void send(String str) {
                                        EventBus.getDefault().post(new EventSendVerifyMsg(mucRoom.getUserId(), mucRoom.getJid(), str));
                                    }
                                });
                                verifyDialog.show();
                                return;
                            }
                            joinRoom(mucRoom, mLoginUserId);
                        } else {
                            ToastUtil.showErrorData(SingleImagePreviewActivity.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(SingleImagePreviewActivity.this);
                    }
                });
    }

    /**
     * 加入房间
     */
    private void joinRoom(final MucRoom room, final String loginUserId) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", room.getId());
        if (room.getUserId().equals(loginUserId))
            params.put("type", "1");
        else
            params.put("type", "2");

        MyApplication.mRoomKeyLastCreate = room.getJid();

        HttpUtils.get().url(coreManager.getConfig().ROOM_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            EventBus.getDefault().post(new EventCreateGroupFriend(room));
                            mImageView.postDelayed(new Runnable() {
                                @Override
                                public void run() {// 给500ms的时间缓存，防止群组还未创建好就进入群聊天界面
                                    interMucChat(room.getJid(), room.getName());
                                }
                            }, 500);
                        } else {
                            MyApplication.mRoomKeyLastCreate = "compatible";
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(SingleImagePreviewActivity.this);
                        MyApplication.mRoomKeyLastCreate = "compatible";
                    }
                });
    }

    /**
     * 进入房间
     */
    private void interMucChat(String roomJid, String roomName) {
        Intent intent = new Intent(this, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);

        MucgroupUpdateUtil.broadcastUpdateUi(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_EDIT:
                    mImagePath = mEditedPath;
                    mImageUri = new File(mEditedPath).toURI().toString();
                    Glide.with(mContext)
                            .load(mImagePath)
                            .asBitmap()
                            .centerCrop()
                            .dontAnimate()
                            .error(R.drawable.image_download_fail_icon)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    mBitmap = resource;
                                    mImageView.setImageBitmap(resource);
                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    mImageView.setImageResource(R.drawable.image_download_fail_icon);
                                }
                            });
                    // 模拟那个长按，弹出菜单，
                    Intent intent = new Intent("longpress");
                    sendBroadcast(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("singledown")) {
                // 轻触屏幕，退出预览
                doBack();
            } else if (intent.getAction().equals("longpress")) {
                // 长按屏幕，弹出菜单
                mSaveWindow = new SaveWindow(SingleImagePreviewActivity.this, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSaveWindow.dismiss();
                        switch (v.getId()) {
                            case R.id.save_image:
                                if (!TextUtils.isEmpty(delPackedId)) {
                                    Toast.makeText(SingleImagePreviewActivity.this, R.string.tip_burn_image_cannot_save, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (mImageUri.toLowerCase().endsWith("gif")) {// 保存Gif
                                    FileUtil.downImageToGallery(SingleImagePreviewActivity.this, mImagePath);
                                } else {// 保存图片
                                    FileUtil.downImageToGallery(SingleImagePreviewActivity.this, mImageUri);
                                }
                                break;
                            case R.id.edit_image:
                                Glide.with(SingleImagePreviewActivity.this)
                                        .load(mImageUri)
                                        .downloadOnly(new SimpleTarget<File>() {
                                            @Override
                                            public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                                                mEditedPath = FileUtil.createImageFileForEdit().getAbsolutePath();
                                                IMGEditActivity.startForResult(SingleImagePreviewActivity.this, Uri.fromFile(resource), mEditedPath, REQUEST_IMAGE_EDIT);
                                            }
                                        });
                                break;
                            case R.id.identification_qr_code:// 识别图中二维码
                                if (mBitmap != null) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            final Result result = DecodeUtils.decodeFromPicture(mBitmap);
                                            mImageView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (result != null && !TextUtils.isEmpty(result.getText())) {
                                                        handleScanResult(result.getText());
                                                    } else {
                                                        Toast.makeText(SingleImagePreviewActivity.this, R.string.decode_failed, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        }
                                    }).start();
                                } else {
                                    Toast.makeText(SingleImagePreviewActivity.this, R.string.decode_failed, Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    }
                });
                mSaveWindow.show();
            }
        }
    }
}

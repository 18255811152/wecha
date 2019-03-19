package com.sk.weichat.ui.me.sendgroupmessage;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.audio_x.VoicePlayer;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.FileDataHelper;
import com.sk.weichat.helper.UploadEngine;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.map.MapPickerActivity;
import com.sk.weichat.ui.me.LocalVideoActivity;
import com.sk.weichat.ui.me.sendgroupmessage.ChatBottomForSendGroup.ChatBottomListener;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.video.EasyCameraActivity;
import com.sk.weichat.video.MessageEventGpu;
import com.sk.weichat.view.SelectCardPopupWindow;
import com.sk.weichat.view.SelectFileDialog;
import com.sk.weichat.view.photopicker.PhotoPickerActivity;
import com.sk.weichat.view.photopicker.SelectModel;
import com.sk.weichat.view.photopicker.intent.PhotoPickerIntent;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;


/**
 * 群发消息
 */
public class ChatActivityForSendGroup extends BaseActivity implements
        ChatBottomListener, SelectCardPopupWindow.SendCardS {
    // 相册、视频、位置
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_SELECT_VIDE0 = 3;
    private static final int REQUEST_CODE_SELECT_Locate = 5;
    private TextView mCountTv;
    private TextView mNameTv;
    private ChatBottomForSendGroup mChatBottomView;
    private CoreService mService;
    private String mLoginUserId;
    private String mLoginNickName;
    private List<String> userIds;
    private List<String> mCloneUserIds;
    private String userNames;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((CoreServiceBinder) service).getService();
        }
    };
    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {

        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            mService.sendChatMessage(toUserId, message);
        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {

        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_for_sg);

        String ids = getIntent().getStringExtra("USERIDS");
        userIds = JSON.parseArray(ids, String.class);
        userNames = getIntent().getStringExtra("USERNAMES");
        mCloneUserIds = new ArrayList<>();
        mCloneUserIds.addAll(userIds);

        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();

        bindService(CoreService.getIntent(), mConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + mLoginUserId
                + File.separator + Environment.DIRECTORY_MUSIC);

        initActionBar();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unbindService(mConnection);
        EventBus.getDefault().unregister(this);
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
        tvTitle.setText(getString(R.string.mass));
    }

    private void initView() {
        mCountTv = (TextView) findViewById(R.id.send_size_tv);
        mNameTv = (TextView) findViewById(R.id.send_name_tv);
        mCountTv.setText(getString(R.string.you_will_send_a_message_to) + userIds.size() + getString(R.string.bit) + getString(R.string.friend));
        mNameTv.setText(userNames);

        mChatBottomView = (ChatBottomForSendGroup) findViewById(R.id.chat_bottom_view);
        mChatBottomView.setChatBottomListener(this);
    }

    private void setSameParams(final ChatMessage message) {
        DialogHelper.showDefaulteMessageProgressDialogAddCancel(this, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });

        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setIsReadDel(0);

        boolean isEncrypt = PreferenceUtils.getBoolean(this, Constants.IS_ENCRYPT + mLoginUserId, false);
        if (isEncrypt) {
            message.setIsEncrypt(1);
        } else {
            message.setIsEncrypt(0);
        }

        message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));
        message.setTimeSend(TimeUtils.sk_time_current_time());

        for (int i = 0; i < userIds.size(); i++) {
            // 每隔timer2秒在发送给一个人，之前一直存在收不到部分人回执的问题，怀疑为消息发送间隔时间太短引起的
            final int j = i;
            long timer1;
            long timer2;
            if (message.getType() == XmppMessage.TYPE_IMAGE
                    || message.getType() == XmppMessage.TYPE_VOICE
                    || message.getType() == XmppMessage.TYPE_VIDEO
                    || message.getType() == XmppMessage.TYPE_FILE) {// 语音、图片、视频、文件都需要上传，间隔时间为0.5s
                timer1 = (i + 1) * 500;
                timer2 = 500;
            } else {// 其他类型消息间隔时间为0.2s
                timer1 = (i + 1) * 200;
                timer2 = 200;
            }
            CountDownTimer mCountDownTimer = new CountDownTimer(timer1, timer2) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    // 虽然消息内容一致，但是消息的Packet不能一致，否则会对影响到消息回执，同时消息发送状态会紊乱
                    String packet = UUID.randomUUID().toString().replaceAll("-", "");
                    message.setPacketId(packet);
                    sendMessage(message, userIds.get(j));
                }
            }.start();
        }
    }

    private void sendMessage(ChatMessage message, String toUserId) {
        message.setToUserId(toUserId);
        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, toUserId, message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), toUserId, message, mUploadResponse);
            } else {
                mService.sendChatMessage(toUserId, message);
            }
        } else {
            mService.sendChatMessage(toUserId, message);
        }
    }

    @Override
    public void stopVoicePlay() {
        VoicePlayer.instance().stop();
    }

    @Override
    public void sendVoice(String filePath, int timeLen) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VOICE);
        message.setContent("");
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        setSameParams(message);
    }

    @Override
    public void sendText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(text);
        setSameParams(message);
    }

    @Override
    public void sendGif(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setContent(text);
        setSameParams(message);
    }

    @Override
    public void sendCollection(String collection) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent(collection);
        message.setUpload(true);// 已上传服务器
        setSameParams(message);
    }

    public void sendImage(File file) {
        if (!file.exists()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        int[] imageParam = FileDataHelper.getImageParamByIntsFile(filePath);
        message.setLocation_x(String.valueOf(imageParam[0]));
        message.setLocation_y(String.valueOf(imageParam[1]));
        setSameParams(message);
    }

    public void sendVideo(File file) {
        if (!file.exists()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        setSameParams(message);
    }

    public void sendFile(File file) {
        if (!file.exists()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_FILE);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        setSameParams(message);
    }

    public void sendLocate(double latitude, double longitude, String address, String snapshot) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setContent("");
        message.setFilePath(snapshot);
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setObjectId(address);
        setSameParams(message);
    }

    public void sendCard(Friend friend) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setContent(friend.getNickName());
        message.setObjectId(friend.getUserId());
        setSameParams(message);
    }

    @Override
    public void clickPhoto() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(ChatActivityForSendGroup.this);
        intent.setSelectModel(SelectModel.MULTI);
        intent.setSelectedPaths(imagePaths);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickCamera() {
        Intent intent = new Intent(this, EasyCameraActivity.class);
        startActivity(intent);
        mChatBottomView.reset();
    }

    @Override
    public void clickVideo() {
        Intent intent = new Intent(mContext, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDE0);
    }

    @Override
    public void clickFile() {
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

            }
        });
        dialog.show();
    }

    @Override
    public void clickLocation() {
        Intent intent = new Intent(mContext, MapPickerActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_Locate);
    }

    @Override
    public void clickCard() {
        SelectCardPopupWindow mSelectCardPopupWindow = new SelectCardPopupWindow(this, this);
        mSelectCardPopupWindow.showAtLocation(findViewById(R.id.root_view),
                Gravity.CENTER, 0, 0);
    }

    @Override
    public void sendCardS(List<Friend> friends) {
        for (int i = 0; i < friends.size(); i++) {
            sendCard(friends.get(i));
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {// 拍照返回
        photograph(new File(message.event));
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        for (String s : mCloneUserIds) {
            if (message.message.equals(s)) {// 该条消息发送成功
                mCloneUserIds.remove(s);
                if (mCloneUserIds.size() == 0) {// 最后一条消息也发送成功 更新消息页面
                    Log.e("TAG", "over: " + s);
                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
                    DialogHelper.dismissProgressDialog();

                    sendBroadcast(new Intent("SEND_MULTI_NOTIFY"));
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_PHOTO && resultCode == RESULT_OK) {// 相册返回
            if (data != null) {
                boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
            } else {
                ToastUtil.showToast(this, R.string.c_photo_album_failed);
            }
        } else if (requestCode == REQUEST_CODE_SELECT_VIDE0 && resultCode == RESULT_OK) {// 选中视频返回
            if (data == null) {
                return;
            }
            String filePath = data.getStringExtra(AppConstant.EXTRA_FILE_PATH);
            if (TextUtils.isEmpty(filePath)) {
                ToastUtil.showToast(this, R.string.select_failed);
                return;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                ToastUtil.showToast(this, R.string.select_failed);
                return;
            }
            sendVideo(file);
        } else if (requestCode == REQUEST_CODE_SELECT_Locate && resultCode == RESULT_OK) {// 选择位置的返回
            double latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            double longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            String address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
            String snapshot = data.getStringExtra(AppConstant.EXTRA_SNAPSHOT);

            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)
                    && !TextUtils.isEmpty(snapshot)) {
                sendLocate(latitude, longitude, address, snapshot);
            } else {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXLoc_StartLocNotice"));
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
}

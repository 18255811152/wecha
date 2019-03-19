package com.sk.weichat.view.chatHolder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.downloader.DownloadListener;
import com.sk.weichat.downloader.DownloadProgressListener;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.downloader.FailReason;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.video.ChatVideoPreviewActivity;
import com.sk.weichat.view.XuanProgressPar;

public class VideoViewHolder extends AChatHolderInterface implements DownloadListener, DownloadProgressListener {

    // JVCideoPlayerStandardforchat mVideo;
    ImageView mVideo;
    ImageView ivStart;
    XuanProgressPar progressPar;
    TextView tvInvalid;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_video : R.layout.chat_to_item_video;
    }

    @Override
    public void initView(View view) {
        mVideo = view.findViewById(R.id.chat_jcvideo);
        progressPar = view.findViewById(R.id.img_progress);
        ivStart = view.findViewById(R.id.iv_start);
        tvInvalid = view.findViewById(R.id.tv_invalid);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        String filePath = message.getFilePath();
        // 文件不存在 就去下载
        boolean isExist = FileUtil.isExist(filePath);
        tvInvalid.setVisibility(View.GONE);
        if (!isExist) {
            Downloader.getInstance().addDownload(message.getContent(), mSendingBar, this, this);
        } else {
            AvatarHelper.getInstance().displayVideoThumb(filePath, mVideo);
            ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_play_selector);
        }

        if (isMysend) { // 判断是否上传
            // 没有上传或者 进度小于100
            boolean show = !message.isUpload() && message.getUploadSchedule() < 100;
            changeVisible(progressPar, show);
            changeVisible(ivStart, !show);
        }

        mSendingBar.setVisibility(View.GONE);
        progressPar.update(message.getUploadSchedule());
    }

    @Override
    protected void onRootClick(View v) {
        if (tvInvalid.getVisibility() == View.VISIBLE) {
            return;
        }

        String filePath = mdata.getFilePath();
        Intent intent = new Intent(mContext, ChatVideoPreviewActivity.class);
        if (!FileUtil.isExist(filePath)) {
            filePath = mdata.getContent();
        }
        intent.putExtra(AppConstant.EXTRA_VIDEO_FILE_PATH, filePath);
        if (mdata.getIsReadDel()) {
            intent.putExtra("DEL_PACKEDID", mdata.getPacketId());
        }

        ivUnRead.setVisibility(View.GONE);
        mContext.startActivity(intent);
    }

    @Override
    public void onStarted(String uri, View view) {
        changeVisible(progressPar, true);
        changeVisible(ivStart, false);
    }

    @Override
    public void onFailed(String uri, FailReason failReason, View view) {
        changeVisible(progressPar, false);
        ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_error_selector);
        tvInvalid.setVisibility(View.VISIBLE);
        ivStart.setVisibility(View.VISIBLE);
    }

    @Override
    public void onComplete(String uri, String filePath, View view) {
        mdata.setFilePath(filePath);
        changeVisible(progressPar, false);
        changeVisible(ivStart, true);
        ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_play_selector);

        // 更新数据库
        ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserid, mToUserId, mdata.get_id(), true, filePath);
        AvatarHelper.getInstance().displayVideoThumb(filePath, mVideo);
    }

    @Override
    public void onCancelled(String uri, View view) {
        changeVisible(progressPar, false);
        changeVisible(ivStart, true);
    }

    @Override
    public void onProgressUpdate(String imageUri, View view, int current, int total) {
        int pro = (int) (current / (float) total * 100);
        progressPar.update(pro);
    }

    @Override
    public boolean enableUnRead() {
        return true;
    }

    @Override
    public boolean enableFire() {
        return true;
    }

}

package com.sk.weichat.view.chatHolder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.downloader.DownloadListener;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.downloader.FailReason;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.view.ChatImageView;
import com.sk.weichat.view.XuanProgressPar;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class ImageViewHolder extends AChatHolderInterface {

    private static final int IMAGE_MIN_SIZE = 70; // dp
    private static final int IMAGE_MAX_SIZE = 105;
    ChatImageView mImageView;
    XuanProgressPar progressPar;
    private int width, height;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_image : R.layout.chat_to_item_image;
    }

    @Override
    public void initView(View view) {
        mImageView = view.findViewById(R.id.chat_image);
        progressPar = view.findViewById(R.id.img_progress);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        // 修改image布局大小，解决不能滑动到底部的问题
        changeImageLayaoutSize(message, IMAGE_MIN_SIZE, IMAGE_MAX_SIZE);

        String filePath = message.getFilePath();
        if (FileUtil.isExist(filePath)) { // 本地存在
            if (filePath.endsWith(".gif")) { // 加载gif
                fillImageGif(filePath);
            } else {
                if (mHolderListener != null) {
                    Bitmap bitmap = mHolderListener.onLoadBitmap(filePath, width, height);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        mImageView.setImageBitmap(null);
                    }
                }
                // 这种加载 notify 会导致 图片不出来，一直处于加载的状态
                //                AvatarHelper.getInstance().displayChatImage(filePath,mImageView);
            }
        } else {
            if (TextUtils.isEmpty(message.getContent())) {
                Log.e("xuan", "imagevewholder: 收到的图片没有路径");
                mImageView.setImageResource(R.drawable.fez);
            } else {
                Downloader.getInstance().addDownload(message.getContent(), mSendingBar, new FileDownloadListener(message, mImageView));
            }
        }

        // 判断是否为阅后即焚类型的图片，如果是 模糊显示该图片
        if (!isGounp) {
            mImageView.setAlpha(message.getIsReadDel() ? 0.1f : 1f);
        }


        //        if(isMysend){
        //            if (message.isUpload() || message.getUploadSchedule() >= 100){
        //                progressPar.setVisibility(View.GONE);
        //            }else{
        //                progressPar.setVisibility(View.VISIBLE);
        //                mSendingBar.setVisibility(View.GONE);
        //            }
        //        }else{
        //            progressPar.setVisibility(View.GONE);
        //        }

        // 上传进度条 我的消息才有进度条
        if (message.isUpload() || !isMysend || message.getUploadSchedule() >= 100) {
            progressPar.setVisibility(View.GONE);
        } else {
            progressPar.setVisibility(View.VISIBLE);
        }
        progressPar.update(message.getUploadSchedule());
    }

    private void fillImageGif(String filePath) {
        try {
            GifDrawable gifFromFile = new GifDrawable(new File(filePath));
            mImageView.setImageGifDrawable(gifFromFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeImageLayaoutSize(ChatMessage message, int mindp, int maxdp) {
        ViewGroup.LayoutParams mLayoutParams = mImageView.getLayoutParams();

        if (TextUtils.isEmpty(message.getLocation_x()) || TextUtils.isEmpty(message.getLocation_y())) {
            mLayoutParams.width = dp2px(maxdp);
            mLayoutParams.height = dp2px(maxdp);

            width = mLayoutParams.width;
            height = mLayoutParams.height;
            Downloader.getInstance().addDownload(message.getContent(), mSendingBar, new FileDownloadListener(message, mImageView));
        } else {
            float image_width = Float.parseFloat(message.getLocation_x());
            float image_height = Float.parseFloat(message.getLocation_y());

            // 基于宽度进行缩放,三挡:宽图 55/100,窄图100/55
            float width = image_width / image_height < 0.4 ? mindp : maxdp;
            float height = width == maxdp ? Math.max(width / image_width * image_height, mindp) : maxdp;

            mLayoutParams.width = dp2px(width);
            mLayoutParams.height = dp2px(height);

            this.width = mLayoutParams.width;
            this.height = mLayoutParams.height;
        }

        mImageView.setLayoutParams(mLayoutParams);
    }

    // 点击图片
    @Override
    public void onRootClick(View v) {
        // 跳转
        Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
        intent.putExtra(AppConstant.EXTRA_IMAGE_URI, mdata.getContent());
        intent.putExtra("image_path", mdata.getFilePath());
        if (!isGounp && !isMysend && mdata.getIsReadDel()) {
            intent.putExtra("DEL_PACKEDID", mdata.getPacketId());
        }
        mContext.startActivity(intent);
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    // 启用阅后即焚
    @Override
    public boolean enableFire() {
        return true;
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
        public void onFailed(String uri, FailReason failReason, View view) {
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
            ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserid, mToUserId, message.get_id(), true, filePath);
            if (filePath.endsWith(".gif")) { // 加载gif
                fillImageGif(filePath);
            } else { // 保存图片尺寸到数据库
                saveImageSize(filePath);
            }
        }

        private void saveImageSize(String filePath) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options); // 此时返回的bitmap为null

            message.setLocation_x(String.valueOf(options.outWidth));
            message.setLocation_y(String.valueOf(options.outHeight));

            // 重绘图片尺寸
            changeImageLayaoutSize(message, IMAGE_MIN_SIZE, IMAGE_MAX_SIZE);
            AvatarHelper.getInstance().displayUrl(filePath, mImageView, R.drawable.fez);
            // 保存下载到数据库
            ChatMessageDao.getInstance().updateMessageLocationXY(message, mLoginUserid);
        }

        @Override
        public void onCancelled(String uri, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }
}

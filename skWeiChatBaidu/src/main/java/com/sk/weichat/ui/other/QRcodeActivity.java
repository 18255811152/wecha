package com.sk.weichat.ui.other;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qrcode.utils.CommonUtils;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.MessageAvatar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Administrator on 2017/9/14 0014.
 * 二维码类
 */
public class QRcodeActivity extends BaseActivity {
    private ImageView qrcode;
    private ImageView mPAva;
    private MessageAvatar mGAva;
    private boolean isgroup;
    private String userId;
    private String roomJid;
    private String action;
    private String str;

    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_im_code_image);
        if (getIntent() != null) {
            isgroup = getIntent().getBooleanExtra("isgroup", false);
            userId = getIntent().getStringExtra("userid");
            if (isgroup) {
                roomJid = getIntent().getStringExtra("roomJid");
            }
        }
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXQR_QRImage"));
        ImageView mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        mIvTitleRight.setImageResource(R.drawable.save_local);
        mIvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToGallery(getBitmap(QRcodeActivity.this.getWindow().getDecorView()));
            }
        });
    }

    private void initView() {
        qrcode = (ImageView) findViewById(R.id.qrcode);
        mPAva = (ImageView) findViewById(R.id.avatar_img);
        mGAva = (MessageAvatar) findViewById(R.id.avatar_imgS);
        if (isgroup) {
            action = "group";
            mGAva.setVisibility(View.VISIBLE);
        } else {
            action = "user";
            mPAva.setVisibility(View.VISIBLE);
        }
        str = coreManager.getConfig().website + "?action=" + action + "&shikuId=" + userId;
        Log.e("zq", "二维码链接：" + str);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        // 生成二维码
        bitmap = CommonUtils.createQRCode(str, screenWidth - 200, screenWidth - 200);

        // 显示 二维码 与 头像
        if (isgroup) {// 群组头像
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), roomJid);
            if (friend != null) {
                mGAva.fillData(friend);
            }
        } else {// 用户头像
           /* Glide.with(mContext)
                    .load(AvatarHelper.getInstance().getAvatarUrl(userId, false))
                    .asBitmap()
                    .signature(new StringSignature(UserAvatarDao.getInstance().getUpdateTime(userId)))
                    .dontAnimate()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            bitmap = EncodingUtils.createQRCode(str, screenWidth - 200, screenWidth - 200,
                                    resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            bitmap = EncodingUtils.createQRCode(str, screenWidth - 200, screenWidth - 200,
                                    BitmapFactory.decodeResource(getResources(), R.drawable.avatar_normal));// 默认头像
                        }
                    });*/
            AvatarHelper.getInstance().displayAvatar(userId, mPAva);
        }
        qrcode.setImageBitmap(bitmap);
    }

    /**
     * 获取这个view的缓存bitmap,
     */
    private Bitmap getBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap result = Bitmap.createBitmap(view.getDrawingCache());
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);
        return result;
    }

    /**
     * Save image, update Gallery
     */
    public void saveImageToGallery(Bitmap bitmap) {
        if (bitmap == null) {
            ToastUtil.showToast(mContext, getString(R.string.creating_qr_code));
        }
        // 1.保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2.把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(this.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
            Toast.makeText(QRcodeActivity.this, R.string.tip_saved_qr_code, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 3.通知图库更新
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
    }
}
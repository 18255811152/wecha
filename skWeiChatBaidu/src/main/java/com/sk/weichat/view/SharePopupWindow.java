package com.sk.weichat.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;

public class SharePopupWindow extends PopupWindow implements OnClickListener {
    private Context mContent;

    public SharePopupWindow(Activity context) {
        this.mContent = context;
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mContent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mMenuView = inflater.inflate(R.layout.view_share, null);
        setContentView(mMenuView);
        setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

        setFocusable(true);
        setAnimationStyle(R.style.Buttom_Popwindow);

        // 因为某些机型是虚拟按键的,所以要加上以下设置防止挡住按键.
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mMenuView.findViewById(R.id.platformshare_wechat).setOnClickListener(this);
        mMenuView.findViewById(R.id.platformshare_moment).setOnClickListener(this);
        mMenuView.findViewById(R.id.cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        switch (v.getId()) {
            case R.id.platformshare_wechat:
                platformShare(ShareSDK.getPlatform(Wechat.NAME));
                break;
            case R.id.platformshare_moment:
                platformShare(ShareSDK.getPlatform(WechatMoments.NAME));
                break;
            case R.id.cancel:
                break;
        }
    }

    private void platformShare(Platform platform) {
        Platform.ShareParams params = new Platform.ShareParams();
        // 网页
        params.setShareType(Platform.SHARE_WEBPAGE);
       /* // 图片 ...其他类型
        params.setShareType(Platform.SHARE_IMAGE);*/
        Bitmap logo = BitmapFactory.decodeResource(mContent.getResources(), R.mipmap.icon);
        params.setImageData(logo);
       /* // else setting logo method
          params.setImagePath("file path");
          params.setImageUrl("image path");*/
        params.setTitle(MyApplication.getContext().getString(R.string.app_name) + mContent.getString(R.string.suffix_share_content));
        // 分享至朋友圈，该字段不显示
        params.setText(MyApplication.getContext().getString(R.string.app_name) + mContent.getString(R.string.suffix_share_content));
        /* if share image, you cannot setting Url*/
        params.setUrl("http://shiku.co/");
        platform.setPlatformActionListener(new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                Toast.makeText(MyApplication.getContext(), R.string.share_succes, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Platform platform, int i, Throwable throwable) {
                Toast.makeText(MyApplication.getContext(), R.string.share_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel(Platform platform, int i) {
                Toast.makeText(MyApplication.getContext(), R.string.share_cancel, Toast.LENGTH_SHORT).show();
            }
        });
        platform.share(params);
    }
}

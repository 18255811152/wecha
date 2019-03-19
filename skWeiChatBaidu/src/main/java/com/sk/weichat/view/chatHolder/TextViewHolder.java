package com.sk.weichat.view.chatHolder;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.ui.tool.WebViewActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.HtmlUtils;
import com.sk.weichat.util.HttpUtil;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.view.SelectURLDialog;

import java.util.List;

import static com.sk.weichat.ui.tool.WebViewActivity.EXTRA_URL;

public class TextViewHolder extends AChatHolderInterface {

    public TextView mTvContent;
    public TextView tvFireTime;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_text : R.layout.chat_to_item_text;
    }

    @Override
    public void initView(View view) {
        mTvContent = view.findViewById(R.id.chat_text);
        mRootView = view.findViewById(R.id.chat_warp_view);
        if (!isMysend) {
            tvFireTime = view.findViewById(R.id.tv_fire_time);
        }
    }

    @Override
    public void fillData(ChatMessage message) {
        // 修改字体功能
        int size = PreferenceUtils.getInt(mContext, Constants.FONT_SIZE) + 13;
        mTvContent.setTextSize(size);

        String content = StringUtils.replaceSpecialChar(message.getContent());

        mTvContent.setTextColor(mContext.getResources().getColor(R.color.black));

        CharSequence charSequence = HtmlUtils.transform200SpanString(content, true);
        if (message.getIsReadDel() && !isMysend) {// 阅后即焚
            if (!message.isGroup() && !message.isSendRead()) {
                mTvContent.setText(R.string.tip_click_to_read);
                mTvContent.setTextColor(mContext.getResources().getColor(R.color.redpacket_bg));
            } else {
                // 已经查看了，当适配器再次刷新的时候，不需要重新赋值
                mTvContent.setText(charSequence);
            }
        } else {
            mTvContent.setText(charSequence);
        }

        if (HttpUtil.isURL(mTvContent.getText().toString())) {
            mTvContent.setTextColor(mContext.getResources().getColor(R.color.link_color));
        }
    }

    @Override
    protected void onRootClick(View v) {
        // 点击跳转的处理
        if (HttpUtil.isURL(mTvContent.getText().toString())) {
            String str = mTvContent.getText().toString();

            List<String> mURLList = HttpUtil.getURLList(str);
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

    @Override
    public boolean enableFire() {
        return true;
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    private void intentWebView(String url) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        mContext.startActivity(intent);
    }

    public void showFireTime(boolean show) {
        if (tvFireTime != null) {
            tvFireTime.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}

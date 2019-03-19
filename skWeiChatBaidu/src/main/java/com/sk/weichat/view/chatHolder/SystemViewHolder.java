package com.sk.weichat.view.chatHolder;

import android.graphics.Color;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.util.StringUtils;


// 系统消息的holder
class SystemViewHolder extends AChatHolderInterface {

    TextView mTvContent;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return R.layout.chat_item_system;
    }

    @Override
    public void initView(View view) {
        mTvContent = view.findViewById(R.id.chat_content_tv);
        mRootView = mTvContent;
    }

    @Override
    public void fillData(ChatMessage message) {
        String sure = message.isDownload() ? getString(R.string.has_confirm) : getString(R.string.to_confirm);
        SpannableString content = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"), message.getContent(), sure);
        mTvContent.setText(content);
        mTvContent.setOnClickListener(this);

    }

    @Override
    protected void onRootClick(View v) {

    }

    @Override
    public void showTime(String time) {
        String chat = mTvContent.getText().toString().trim();
        StringBuffer sb = new StringBuffer(chat);
        sb.append("(").append(time).append(")");
        mTvContent.setText(sb);
    }

    @Override
    public boolean isLongClick() {
        return false;
    }

    @Override
    public boolean isOnClick() {
        return true;
    }

    @Override
    public boolean enableNormal() {
        return false;
    }
}

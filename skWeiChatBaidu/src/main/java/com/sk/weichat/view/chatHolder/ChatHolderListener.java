package com.sk.weichat.view.chatHolder;

import android.graphics.Bitmap;
import android.view.View;

import com.sk.weichat.bean.message.ChatMessage;

public interface ChatHolderListener {

    void onItemLongClick(View v, AChatHolderInterface aChatHolderInterface, ChatMessage mdata);

    void onItemClick(View v, AChatHolderInterface aChatHolderInterface, ChatMessage mdata);

    void onCompDownVoice(ChatMessage message);

    void onChangeInputText(String text);

    Bitmap onLoadBitmap(String key, int width, int height);
}

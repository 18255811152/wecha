package com.sk.weichat.bean;

import com.sk.weichat.bean.message.ChatMessage;

public class EventNewNotice {
    private String text;

    public EventNewNotice(ChatMessage chatMessage) {
        this.text = chatMessage.getContent();
    }

    public String getText() {
        return text;
    }
}

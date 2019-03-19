package com.sk.weichat.bean;

/**
 * Created by Administrator on 2016/4/29.
 */
public class PrivacySetting {

    private int allowAtt;
    private int allowGreet;
    private double chatRecordTimeOut;
    private int closeTelephoneFind;
    private int openService;

    // 消息漫游时长
    private double chatSyncTimeLen;

    public double getChatSyncTimeLen() {
        return chatSyncTimeLen;
    }

    public void setChatSyncTimeLen(double chatSyncTimeLen) {
        this.chatSyncTimeLen = chatSyncTimeLen;
    }

    // 需要好友验证
    private int friendsVerify;

    public int getFriendsVerify() {
        return friendsVerify;
    }

    public void setFriendsVerify(int friendsVerify) {
        this.friendsVerify = friendsVerify;
    }

    // 消息加密传输
    private int isEncrypt;
    // 消息来时振动
    private int isVibration;
    // 让对方知道我正在输入...
    private int isTyping;

    public int getIsEncrypt() {
        return isEncrypt;
    }

    public void setIsEncrypt(int isEncrypt) {
        this.isEncrypt = isEncrypt;
    }

    public int getIsVibration() {
        return isVibration;
    }

    public void setIsVibration(int isVibration) {
        this.isVibration = isVibration;
    }

    public int getIsTyping() {
        return isTyping;
    }

    public void setIsTyping(int isTyping) {
        this.isTyping = isTyping;
    }

    // 使用谷歌地图
    private int isUseGoogleMap;
    // 支持多设备登录
    private int multipleDevices;

    public int getIsUseGoogleMap() {
        return isUseGoogleMap;
    }

    public void setIsUseGoogleMap(int isUseGoogleMap) {
        this.isUseGoogleMap = isUseGoogleMap;
    }

    public int getMultipleDevices() {
        return multipleDevices;
    }

    public void setMultipleDevices(int multipleDevices) {
        this.multipleDevices = multipleDevices;
    }
}

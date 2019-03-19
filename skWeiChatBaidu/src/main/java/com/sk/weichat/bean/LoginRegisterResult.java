package com.sk.weichat.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class LoginRegisterResult {

    private String access_token;
    private String userId;
    @JSONField(name = "nickname")
    private String nickName;// 昵称
    private String telephone;
    private String password;
    private String areaCode;
    private int expires_in;
    private int friendCount;

    // 1=游客（用于后台浏览数据）；2=公众号 ；3=机器账号，由系统自动生成；4=客服账号;5=管理员；6=超级管理员；7=财务；
    private List<Integer> role; // 身份，
    private String myInviteCode;
    private Login login;
    private Settings settings;
    private int payPassword; // 是否已经设置了支付密码，

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(int expires_in) {
        this.expires_in = expires_in;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(int friendCount) {
        this.friendCount = friendCount;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public int getPayPassword() {
        return payPassword;
    }

    public void setPayPassword(int payPassword) {
        this.payPassword = payPassword;
    }

    public List<Integer> getRole() {
        return role;
    }

    public void setRole(List<Integer> role) {
        this.role = role;
    }

    public String getMyInviteCode() {
        return myInviteCode;
    }

    public void setMyInviteCode(String myInviteCode) {
        this.myInviteCode = myInviteCode;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static class Login {
        private int isFirstLogin;
        private long loginTime;
        private long offlineTime;
        private String model;
        private String osVersion;
        private String serial;
        private String latitude;
        private String longitude;

        public int getIsFirstLogin() {
            return isFirstLogin;
        }

        public void setIsFirstLogin(int isFirstLogin) {
            this.isFirstLogin = isFirstLogin;
        }

        public long getLoginTime() {
            return loginTime;
        }

        public void setLoginTime(long loginTime) {
            this.loginTime = loginTime;
        }

        public long getOfflineTime() {
            return offlineTime;
        }

        public void setOfflineTime(long offlineTime) {
            this.offlineTime = offlineTime;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getSerial() {
            return serial;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }
    }

    /**
     * User Private Settings
     */
    public static class Settings {
        private int allowAtt;
        private int allowGreet;
        private double chatRecordTimeOut;
        private int closeTelephoneFind;
        private int openService;

        // 消息漫游时长
        private double chatSyncTimeLen;
        // 需要好友验证
        private int friendsVerify;
        // 消息加密传输
        private int isEncrypt;
        // 消息来时振动
        private int isVibration;
        // 让对方知道我正在输入...
        private int isTyping;
        // 使用谷歌地图
        private int isUseGoogleMap;
        // 支持多设备登录
        private int multipleDevices;

        public double getChatSyncTimeLen() {
            return chatSyncTimeLen;
        }

        public void setChatSyncTimeLen(double chatSyncTimeLen) {
            this.chatSyncTimeLen = chatSyncTimeLen;
        }

        public int getFriendsVerify() {
            return friendsVerify;
        }

        public void setFriendsVerify(int friendsVerify) {
            this.friendsVerify = friendsVerify;
        }

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
}

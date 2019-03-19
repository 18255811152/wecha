package com.sk.weichat.bean.message;

import com.alibaba.fastjson.annotation.JSONField;

public class MucRoomMember {
    private String userId;// id
    @JSONField(name = "nickname")
    private String nickName;// 昵称
    private int createTime; // 加入时间
    /**
     * 4, 隐身人，
     * 5，监控人，
     * 隐身人和监控人：即群主设置某成员为这2个角色，则群员数量减1,其他人完全看不到他；隐身人和监控人的区别是，前者不可以说话，后者能说话。
     */
    private int role;// 1创建者，2管理员，3成员,
    private int talkTime;  // 禁言时间
    private int active;    // 最后一次互动时间
    private int sub;       // 0屏蔽消息，1不屏蔽
    private String call;// 音视频会议id 群聊
    private String videoMeetingNo;

    // 消息免打扰 1 是 0 否
    private int offlineNoPushMsg;

    private String remarkName;// 群主对群内成员的备注名 仅群主可见

    @Override
    public String toString() {
        return "MucRoomMember{" +
                "userId='" + userId + '\'' +
                ", nickName='" + nickName + '\'' +
                ", createTime=" + createTime +
                ", role=" + role +
                ", talkTime=" + talkTime +
                ", active=" + active +
                ", sub=" + sub +
                ", call='" + call + '\'' +
                '}';
    }

    /**
     * 全员禁言是否对此人生效，
     * {@link com.sk.weichat.bean.message.MucRoomMember#isAllBannedEffective}
     * {@link com.sk.weichat.bean.RoomMember#isAllBannedEffective}
     */
    public boolean isAllBannedEffective() {
        return getRole() == 3;
    }

    public int getOfflineNoPushMsg() {
        return offlineNoPushMsg;
    }

    public void setOfflineNoPushMsg(int offlineNoPushMsg) {
        this.offlineNoPushMsg = offlineNoPushMsg;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public String getVideoMeetingNo() {
        return videoMeetingNo;
    }

    public void setVideoMeetingNo(String videoMeetingNo) {
        this.videoMeetingNo = videoMeetingNo;
    }

    public String getCall() {
        return call;
    }

    public void setCall(String call) {
        this.call = call;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getTalkTime() {
        return talkTime;
    }

    public void setTalkTime(int talkTime) {
        this.talkTime = talkTime;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public int getSub() {
        return sub;
    }

    public void setSub(int sub) {
        this.sub = sub;
    }
}

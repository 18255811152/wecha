package com.sk.weichat.util;

public class Constants {

    public static final String VX_APP_ID = "wx373339ef4f3cd807";

    public static final String NOT_AUTHORIZED = "not_authorized";// XMPP登录时密码错误(密码与服务端不匹配)

    /*
    Other
     */
    // 国家区号
    public static final String MOBILE_PREFIX = "MOBILE_PREFIX";
    // 登录冲突，否，退出app，记录，下次进入历史登录界面
    public static final String LOGIN_CONFLICT = "login_conflict";
    // 当前设备离线时间
    public static final String OFFLINE_TIME = "offline_time";
    // App启动次数
    public static final String APP_LAUNCH_COUNT = "app_launch_count";
    public static final String IS_AUDIO_CONFERENCE = "is_audio_conference";
    public static final String LOCAL_CONTACTS = "local_contacts";
    public static final String NEW_CONTACTS_NUMBER = "new_contacts_number";
    public static final String NEW_CONTACTS_IDS = "new_contacts_ids";
    // 新消息数量
    public static final String NEW_MSG_NUMBER = "new_msg_number";
    // 通知栏进入
    public final static String IS_NOTIFICATION_BAR_COMING = "is_notification_bar_coming";
    // 刷新"消息"角标
    public final static String NOTIFY_MSG_SUBSCRIPT = "notify_msg_subscript";
    public final static String AREA_CODE_KEY = "areCode";

    public final static String UPDATE_ROOM = "update_room";

    public final static String BROWSER_SHARE_MOMENTS_CONTENT = "browser_share_moments_content";
    /*
    Chat Publish
     */
    // 最近一张屏幕截图的路径
    public final static String SCREEN_SHOTS = "screen_shots";
    // 删除
    public final static String CHAT_MESSAGE_DELETE_ACTION = "chat_message_delete";
    public final static String CHAT_REMOVE_MESSAGE_POSITION = "CHAT_REMOVE_MESSAGE_POSITION";
    // 多选
    public final static String SHOW_MORE_SELECT_MENU = "show_more_select_menu";
    public final static String CHAT_SHOW_MESSAGE_POSITION = "CHAT_SHOW_MESSAGE_POSITION";
    public final static String IS_MORE_SELECTED_INSTANT = "IS_MORE_SELECTED_INSTANT";// 是否为多选转发
    public final static String IS_SINGLE_OR_MERGE = "IS_SINGLE_OR_MERGE";// 逐条还是合并转发
    // 单、群聊 清空聊天记录
    public final static String CHAT_HISTORY_EMPTY = "chat_history_empty";
    // 更新消息过期时间的通知
    public final static String CHAT_TIME_OUT_ACTION = "chat_time_out_action";
    /*
    Person Set
     */
    // 阅后即焚
    public final static String MESSAGE_READ_FIRE = "message_read_fire";
    // 聊天背景
    public final static String SET_CHAT_BACKGROUND = "chat_background";
    public final static String SET_CHAT_BACKGROUND_PATH = "chat_background_path";
    /*
   Group Set
   */
    public final static String GROUP_JOIN_NOTICE = "group_join_notice";
    // 屏蔽群组消息
    public final static String SHIELD_GROUP_MSG = "shield_group_msg";
    // 全体禁言
    public final static String GROUP_ALL_SHUP_UP = "group_all_shut_up";
    // 是否开启群已读
    public final static String IS_SHOW_READ = "is_show_read";
    //是否允许普通群成员私聊
    public final static String IS_SEND_CARD = "is_send_card";
    // 是否允许普通成员召开会议
    public final static String IS_ALLOW_NORMAL_CONFERENCE = "is_allow_normal_conference";
    // 是否允许普通成员发送讲课
    public final static String IS_ALLOW_NORMAL_SEND_COURSE = "is_allow_normal_send_course";
    /*
    Set
     */
    // 字体大小
    public final static String FONT_SIZE = "font_size";
    /*
    Private Set
     */
    // 消息漫游时长 单位 天
    public final static String CHAT_SYNC_TIME_LEN = "chat_sync_time_len";
    // 消息加密传输
    public final static String IS_ENCRYPT = "isEncrypt";
    // 消息来时是否振动
    public final static String MSG_COME_VIBRATION = "msg_come_vibration";

    // 需要好友验证为服务端判断 本地不做处理
    // 让对方知道我正在输入...
    public final static String KNOW_ENTER_STATUS = "know_enter_status";
    // 是否使用google地图，
    public final static String IS_GOOGLE_MAP_KEY = "isGoogleMap";
    // 是否使用google地图，
    public final static String IS_PAY_PASSWORD_SET = "isPayPasswordSet";
    @SuppressWarnings("WeakerAccess")
    public static final String KEY_SKIN_COLOR = "KEY_SKIN_COLOR";
    public static boolean OFFLINE_TIME_IS_FROM_SERVICE = false;// 离线时间是否为服务端获取的
    public static boolean IS_SENDONG_COURSE_NOW = false;// 现在是否正在发送课程
}

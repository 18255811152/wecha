package com.sk.weichat.broadcast;

import android.content.Context;
import android.content.Intent;

import com.sk.weichat.AppConfig;

/**
 * 用于聊天消息的广播，更新MainActivity Tab栏显示的未读数量 和 消息界面数据的更新
 */
public class MsgBroadcast {
    public static final String ACTION_MSG_UI_UPDATE = AppConfig.sPackageName + ".action.msg_ui_update";// 消息界面的更新
    public static final String ACTION_MSG_NUM_UPDATE = AppConfig.sPackageName + ".intent.action.msg_num_update";// 未读数量的更新
    public static final String ACTION_MSG_NUM_UPDATE_NEW_FRIEND = AppConfig.sPackageName + ".intent.action.msg_num_update_new_friend";// 新的朋友 未读数量的更新
    public static final String ACTION_MSG_NUM_RESET = AppConfig.sPackageName + ".action.msg_num_reset";// 未读数量需要重置，即从数据库重新查
    public static final String EXTRA_NUM_COUNT = "count";
    public static final String EXTRA_NUM_OPERATION = "operation";
    public static final int NUM_ADD = 0;   // 消息加
    public static final int NUM_REDUCE = 1;// 消息减

    public static final String ACTION_MSG_STATE_UPDATE = AppConfig.sPackageName + ".action.CHANGE_MESSAGE_STATE"; // 通知某条消息改变
    public static final String ACTION_MSG_UPDATE_ROOM = AppConfig.sPackageName + ".action.msg_room_update";       // 聊天群组界面的更新
    public static final String ACTION_MSG_CLOSE_TRILL = AppConfig.sPackageName + ".action.colse_trill";  // 关闭抖音模块
    public static final String ACTION_MSG_ROLE_CHANGED = AppConfig.sPackageName + ".action.ROLE_CHANGED";
    public static final String ACTION_DISABLE_GROUP_BY_SERVICE = AppConfig.sPackageName + ".action.disable_group_by_service";// 群组被锁定/解锁

    /**
     * 更新消息Fragment的广播
     */
    public static void broadcastMsgUiUpdate(Context context) {
        context.sendBroadcast(new Intent(ACTION_MSG_UI_UPDATE));
    }

    public static void broadcastMsgNumUpdate(Context context, boolean add, int count) {
        Intent intent = new Intent(ACTION_MSG_NUM_UPDATE);
        intent.putExtra(EXTRA_NUM_COUNT, count);
        if (add) {
            intent.putExtra(EXTRA_NUM_OPERATION, NUM_ADD);
        } else {
            intent.putExtra(EXTRA_NUM_OPERATION, NUM_REDUCE);
        }
        context.sendBroadcast(intent);
    }

    public static void broadcastMsgNumUpdateNewFriend(Context context) {
        Intent intent = new Intent(ACTION_MSG_NUM_UPDATE_NEW_FRIEND);
        context.sendBroadcast(intent);
    }

    public static void broadcastMsgNumReset(Context context) {
        context.sendBroadcast(new Intent(ACTION_MSG_NUM_RESET));
    }

    public static void broadcastMsgReadUpdate(Context context, String packetId) {
        Intent intent = new Intent(ACTION_MSG_STATE_UPDATE);
        intent.putExtra("packetId", packetId);
        context.sendBroadcast(intent);
    }

    public static void broadcastMsgRoomUpdate(Context context) {
        context.sendBroadcast(new Intent(ACTION_MSG_UPDATE_ROOM));
    }

    public static void broadcastMsgColseTrill(Context context) {
        context.sendBroadcast(new Intent(ACTION_MSG_CLOSE_TRILL));
    }

    public static void broadcastMsgRoleChanged(Context context) {
        context.sendBroadcast(new Intent(ACTION_MSG_ROLE_CHANGED));
    }
}


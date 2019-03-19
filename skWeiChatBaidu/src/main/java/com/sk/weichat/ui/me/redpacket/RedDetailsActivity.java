package com.sk.weichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.RoomMember;
import com.sk.weichat.bean.redpacket.OpenRedpacket;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.RoomMemberDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * modify by zq
 * 单聊 普通红包、口令红包自己都不可领取
 * 群组 手气红包、口令红包自己可领取，普通红包不可领取
 */
public class RedDetailsActivity extends BaseActivity implements View.OnClickListener {
    LayoutInflater inflater;
    DecimalFormat df = new DecimalFormat("######0.00");
    private ImageView red_head_iv;
    private TextView red_nickname_tv;
    private TextView red_words_tv;
    private TextView red_money_tv;
    private TextView red_money_bit_tv;
    private TextView red_resultmsg_tv;
    private ListView red_details_lsv;
    private OpenRedpacket openRedpacket;
    private OpenRedpacket.PacketEntity packetEntity;
    private List<OpenRedpacket.ListEntity> list;
    private int redAction;  // 标记是抢到红包还是查看了红包
    private int timeOut;    // 标记红包是否已过时
    private boolean isGroup;// 是否为群组
    private String mToUserId; // userId || 群组jid
    private Friend mFriend; // 通过该mFriend，获取备注名、获取群成员表显示群内昵称
    private String resultMsg, redMsg;
    private Map<String, String> mGroupNickNameMap = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redpacket_details);
        Bundle bundle = getIntent().getExtras();
        openRedpacket = (OpenRedpacket) bundle.getSerializable("openRedpacket");
        redAction = bundle.getInt("redAction");
        timeOut = bundle.getInt("timeOut");
        isGroup = bundle.getBoolean("isGroup", false);
        mToUserId = bundle.getString("mToUserId");
        list = openRedpacket.getList();
        packetEntity = openRedpacket.getPacket();
        inflater = LayoutInflater.from(this);
        initView();
        showData();
    }

    private void initView() {
        getSupportActionBar().hide();

        red_head_iv = (ImageView) findViewById(R.id.red_head_iv);
        red_nickname_tv = (TextView) findViewById(R.id.red_nickname_tv);
        red_words_tv = (TextView) findViewById(R.id.red_words_tv);
        red_money_tv = (TextView) findViewById(R.id.get_money_tv);
        red_money_bit_tv = (TextView) findViewById(R.id.get_money_bit_tv);

        red_resultmsg_tv = (TextView) findViewById(R.id.red_resultmsg_tv);
        red_details_lsv = (ListView) findViewById(R.id.red_details_lsv);

        mFriend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), mToUserId);
        if (isGroup && mFriend != null) {// 群组红包 获取群内昵称 之后显示
            List<RoomMember> mRoomMemberList = RoomMemberDao.getInstance().getRoomMember(mFriend.getRoomId());
            if (mRoomMemberList != null && mRoomMemberList.size() > 0) {
                for (int i = 0; i < mRoomMemberList.size(); i++) {
                    RoomMember mRoomMember = mRoomMemberList.get(i);
                    mGroupNickNameMap.put(mRoomMember.getUserId(), mRoomMember.getUserName());
                }
            }
        }

        findViewById(R.id.red_back_tv).setOnClickListener(this);
        findViewById(R.id.get_redlist_tv).setOnClickListener(this);
    }

    private void showData() {
        if (list == null) {
            list = new ArrayList<>();
        }
        AvatarHelper.getInstance().displayAvatar(packetEntity.getUserId(), red_head_iv, true);
        red_nickname_tv.setText(getString(R.string.someone_s_red_packet_place_holder, packetEntity.getUserName()));
        red_words_tv.setText(packetEntity.getGreetings());

        for (OpenRedpacket.ListEntity entity : list) {
            if (entity.getUserId().equals(coreManager.getSelf().getUserId())) {
                red_money_tv.setText(df.format(entity.getMoney()));
                if (!TextUtils.isEmpty(df.format(entity.getMoney()))) {
                    red_money_bit_tv.setText(R.string.rmb);
                }
            }
        }

        resultMsg = getString(R.string.red_packet_receipt_place_holder, list.size(), packetEntity.getCount(),
                df.format(packetEntity.getMoney() - packetEntity.getOver()),
                df.format(packetEntity.getMoney()));

        if (list.size() == packetEntity.getCount()) {
            redMsg = getString(R.string.red_packet_receipt_suffix_all);
        } else if (timeOut == 1) {
            redMsg = getString(R.string.red_packet_receipt_suffix_over);
        } else {
            redMsg = getString(R.string.red_packet_receipt_suffix_remain);
        }

        red_resultmsg_tv.setText(resultMsg + redMsg);
        red_details_lsv.setAdapter(new RedAdapter());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_title_left || v.getId() == R.id.red_back_tv) {
            finish();
        } else if (v.getId() == R.id.get_redlist_tv) {
            Intent intent = new Intent(RedDetailsActivity.this, RedListActivity.class);
            startActivity(intent);
        }
    }

    private class RedAdapter extends BaseAdapter {
        View view;

        @Override
        public int getCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            OpenRedpacket.ListEntity listEntity = list.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            long lcc_time = Long.valueOf(listEntity.getTime());
            String StrTime = sdf.format(new Date(lcc_time * 1000L));

            view = inflater.inflate(R.layout.reditem_layout, null);
            AvatarHelper.getInstance().displayAvatar(listEntity.getUserId(), (ImageView) view.findViewById(R.id.red_head_iv), true);
            if (isGroup) {
                if (mGroupNickNameMap.size() > 0 && mGroupNickNameMap.containsKey(listEntity.getUserId())) {
                    ((TextView) view.findViewById(R.id.username_tv)).setText(mGroupNickNameMap.get(listEntity.getUserId()));
                } else {
                    ((TextView) view.findViewById(R.id.username_tv)).setText(listEntity.getUserName());
                }
            } else {
                if (listEntity.getUserId().equals(coreManager.getSelf().getUserId())) {// 自己领取了
                    ((TextView) view.findViewById(R.id.username_tv)).setText(listEntity.getUserName());
                } else {
                    if (mFriend != null) {
                        // ((TextView) view.findViewById(R.id.username_tv)).setText(listEntity.getUserName());
                        ((TextView) view.findViewById(R.id.username_tv)).setText(TextUtils.isEmpty(mFriend.getRemarkName())
                                ? mFriend.getNickName() : mFriend.getRemarkName());
                    } else {
                        ((TextView) view.findViewById(R.id.username_tv)).setText(listEntity.getUserName());
                    }
                }
            }
            ((TextView) view.findViewById(R.id.opentime_tv)).setText(StrTime);
            ((TextView) view.findViewById(R.id.money_tv)).setText(df.format(listEntity.getMoney()) + getString(R.string.rmb));
            return view;
        }
    }
}

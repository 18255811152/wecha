package com.sk.weichat.ui.message.multi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.RoomMember;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.RoomMemberDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.sortlist.BaseComparator;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewHolder;
import com.sk.weichat.view.BannedDialog;
import com.sk.weichat.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * Features: 展示群成员 && 删除群成员 && 禁言
 * Features: 群主对群内成员备注
 * <p>
 * 因为管理员也可以进入该界面进行前三种操作，且管理员需要显示userName 群主显示cardName 所以需要区分下
 * // Todo 当群组人数过多时，排序需要很久，先干掉排序功能，考虑替换排序规则
 */
public class GroupMoreFeaturesActivity extends BaseActivity {
    private EditText mEditText;
    private boolean isSearch;

    private ListView mListView;
    private GroupMoreFeaturesAdapter mAdapter;
    // private List<BaseSortModel<RoomMember>> mSortRoomMember;
    // private List<BaseSortModel<RoomMember>> mSearchSortRoomMember;
    private List<RoomMember> mSortRoomMember;
    private List<RoomMember> mSearchSortRoomMember;
    private BaseComparator<RoomMember> mBaseComparator;

    private TextView mTextDialog;

    private String mRoomId;
    private boolean isDelete;
    private boolean isBanned;
    private boolean isSetRemark;

    private RoomMember mRoomMember;
    private Map<String, String> mRemarksMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_all_member);
        mRoomId = getIntent().getStringExtra("roomId");
        isDelete = getIntent().getBooleanExtra("isDelete", false);
        isBanned = getIntent().getBooleanExtra("isBanned", false);
        isSetRemark = getIntent().getBooleanExtra("isSetRemark", false);

        initActionBar();
        initData();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.group_member);

    }

    private void initData() {
        AsyncUtils.doAsync(this, c -> {
            List<Friend> mFriendList = FriendDao.getInstance().getAllFriends(coreManager.getSelf().getUserId());
            for (int i = 0; i < mFriendList.size(); i++) {
                if (!TextUtils.isEmpty(mFriendList.get(i).getRemarkName())) {// 针对该好友进行了备注
                    mRemarksMap.put(mFriendList.get(i).getUserId(), mFriendList.get(i).getRemarkName());
                }
            }
            c.uiThread(r -> {
                mAdapter.notifyDataSetChanged();// 刷新页面
            });
        });

        mSortRoomMember = new ArrayList<>();
        mSearchSortRoomMember = new ArrayList<>();
        mBaseComparator = new BaseComparator<>();

        List<RoomMember> data = RoomMemberDao.getInstance().getRoomMember(mRoomId);
        for (RoomMember roomMember : data) {
            if (Objects.equals(roomMember.getUserId(), coreManager.getSelf().getUserId())) {
                mRoomMember = roomMember;
            }
        }
        mSortRoomMember.addAll(data);
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.list_view);
        mAdapter = new GroupMoreFeaturesAdapter(mSortRoomMember);
        mListView.setAdapter(mAdapter);

        mEditText = (EditText) findViewById(R.id.search_et);
        mEditText.setHint(InternationalizationHelper.getString("JX_Seach"));
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                isSearch = true;
                mSearchSortRoomMember.clear();
                String str = mEditText.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    isSearch = false;
                    mAdapter.setData(mSortRoomMember);
                    return;
                }
                for (int i = 0; i < mSortRoomMember.size(); i++) {
/*
                    if (getName(mSortRoomMember.get(i).getBean()).contains(str)) { // 符合搜索条件的好友
                        mSearchSortRoomMember.add((mSortRoomMember.get(i)));
                    }
*/
                    if (getName(mSortRoomMember.get(i)).contains(str)) { // 符合搜索条件的好友
                        mSearchSortRoomMember.add((mSortRoomMember.get(i)));
                    }
                }
                mAdapter.setData(mSearchSortRoomMember);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                final BaseSortModel<RoomMember> baseSortModel;
                final RoomMember roomMember;
                if (isSearch) {
                    // baseSortModel = mSearchSortRoomMember.get(position);
                    roomMember = mSearchSortRoomMember.get(position);
                } else {
                    // baseSortModel = mSortRoomMember.get(position);
                    roomMember = mSortRoomMember.get(position);
                }

                if (isDelete) {// 踢人
                    if (roomMember.getUserId().equals(coreManager.getSelf().getUserId())) {
                        ToastUtil.showToast(mContext, R.string.can_not_remove_self);
                        return;
                    }
                    if (roomMember.getRole() == 1) {
                        ToastUtil.showToast(mContext, getString(R.string.tip_cannot_remove_owner));
                        return;
                    }

                    if (roomMember.getRole() == 2 && mRoomMember.getRole() != 1) {
                        ToastUtil.showToast(mContext, getString(R.string.tip_cannot_remove_manager));
                        return;
                    }

                    SelectionFrame mSF = new SelectionFrame(GroupMoreFeaturesActivity.this);
                    mSF.setSomething(null, getString(R.string.sure_remove_member_for_group, getName(roomMember)),
                            new SelectionFrame.OnSelectionFrameClickListener() {
                                @Override
                                public void cancelClick() {

                                }

                                @Override
                                public void confirmClick() {
                                    deleteMember(roomMember, roomMember.getUserId());
                                }
                            });
                    mSF.show();
                } else if (isBanned) {// 禁言
                    if (roomMember.getUserId().equals(coreManager.getSelf().getUserId())) {
                        ToastUtil.showToast(mContext, R.string.can_not_banned_self);
                        return;
                    }

                    if (roomMember.getRole() == 1) {
                        ToastUtil.showToast(mContext, getString(R.string.tip_cannot_ban_owner));
                        return;
                    }

                    if (roomMember.getRole() == 2) {
                        ToastUtil.showToast(mContext, getString(R.string.tip_cannot_ban_manager));
                        return;
                    }

                    showBannedDialog(roomMember.getUserId());
                } else if (isSetRemark) {// 备注
                    if (roomMember.getUserId().equals(coreManager.getSelf().getUserId())) {
                        ToastUtil.showToast(mContext, R.string.can_not_remark_self);
                        return;
                    }
                    setRemarkName(roomMember.getUserId(), getName(roomMember));
                } else {
                    Intent intent = new Intent(mContext, BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, roomMember.getUserId());
                    startActivity(intent);
                }
            }
        });
    }

    private String getName(RoomMember member) {
        if (mRoomMember != null && mRoomMember.getRole() == 1) {
            if (!TextUtils.equals(member.getUserName(), member.getCardName())) {// 当userName与cardName不一致时，我们认为群主有设置群内备注
                return member.getCardName();
            } else {
                if (mRemarksMap.containsKey(member.getUserId())) {
                    return mRemarksMap.get(member.getUserId());
                } else {
                    return member.getUserName();
                }
            }
        } else {
            if (mRemarksMap.containsKey(member.getUserId())) {
                return mRemarksMap.get(member.getUserId());
            } else {
                return member.getUserName();
            }
        }
    }

    private void showBannedDialog(final String userId) {
        final int daySeconds = 24 * 60 * 60;
        BannedDialog bannedDialog = new BannedDialog(mContext, new BannedDialog.OnBannedDialogClickListener() {

            @Override
            public void tv1Click() {
                bannedVoice(userId, 0);
            }

            @Override
            public void tv2Click() {
                bannedVoice(userId, TimeUtils.sk_time_current_time() + daySeconds / 48);
            }

            @Override
            public void tv3Click() {
                bannedVoice(userId, TimeUtils.sk_time_current_time() + daySeconds / 24);
            }

            @Override
            public void tv4Click() {
                bannedVoice(userId, TimeUtils.sk_time_current_time() + daySeconds);
            }

            @Override
            public void tv5Click() {
                bannedVoice(userId, TimeUtils.sk_time_current_time() + daySeconds * 3);
            }

            @Override
            public void tv6Click() {
                bannedVoice(userId, TimeUtils.sk_time_current_time() + daySeconds * 7);
            }

            @Override
            public void tv7Click() {
                bannedVoice(userId, TimeUtils.sk_time_current_time() + daySeconds * 15);
            }
        });
        bannedDialog.show();
    }

    /**
     * 删除群成员
     */
    private void deleteMember(final RoomMember baseSortModel, final String userId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("userId", userId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, R.string.remove_success, Toast.LENGTH_SHORT).show();
                            mSortRoomMember.remove(baseSortModel);
                            mEditText.setText("");

                            RoomMemberDao.getInstance().deleteRoomMember(mRoomId, userId);
                            EventBus.getDefault().post(new EventGroupStatus(10001, Integer.valueOf(userId)));
                        } else {
                            Toast.makeText(mContext, R.string.remove_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 禁言
     */
    private void bannedVoice(String userId, final long time) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("userId", userId);
        params.put("talkTime", String.valueOf(time));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (time > TimeUtils.sk_time_current_time()) {
                                ToastUtil.showToast(mContext, R.string.banned_succ);
                            } else {
                                ToastUtil.showToast(mContext, R.string.canle_banned_succ);
                            }
                        } else {
                            ToastUtil.showErrorData(mContext);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 对群内成员备注
     */
    private void setRemarkName(final String userId, final String name) {
        DialogHelper.showLimitSingleInputDialog(this, getString(R.string.change_remark), name, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String newName = ((EditText) v).getText().toString().trim();
                if (TextUtils.isEmpty(newName) || newName.equals(name)) {
                    return;
                }

                Map<String, String> params = new HashMap<>();
                params.put("access_token", coreManager.getSelfStatus().accessToken);
                params.put("roomId", mRoomId);
                params.put("userId", userId);
                params.put("remarkName", newName);
                DialogHelper.showDefaulteMessageProgressDialog(GroupMoreFeaturesActivity.this);

                HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_UPDATE)
                        .params(params)
                        .build()
                        .execute(new BaseCallback<Void>(Void.class) {

                            @Override
                            public void onResponse(ObjectResult<Void> result) {
                                DialogHelper.dismissProgressDialog();
                                if (result.getResultCode() == 1) {
                                    ToastUtil.showToast(mContext, R.string.modify_succ);
                                    RoomMemberDao.getInstance().updateRoomMemberCardName(mRoomId, userId, newName);

                                    for (int i = 0; i < mSortRoomMember.size(); i++) {
/*
                                                if (mSortRoomMember.get(i).getBean().getUserId().equals(userId)) {
                                                    mSortRoomMember.get(i).getBean().setCardName(newName);
                                                }
*/
                                        if (mSortRoomMember.get(i).getUserId().equals(userId)) {
                                            mSortRoomMember.get(i).setCardName(newName);
                                        }
                                    }
                                    if (!TextUtils.isEmpty(mEditText.getText().toString())) {// 清空mEditText
                                        mEditText.setText("");
                                    } else {
                                        mAdapter.setData(mSortRoomMember);
                                    }
                                    // 更新群组信息页面
                                    EventBus.getDefault().post(new EventGroupStatus(10003, 0));
                                } else {
                                    ToastUtil.showToast(mContext, result.getResultMsg());
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                DialogHelper.dismissProgressDialog();
                                ToastUtil.showErrorNet(mContext);
                            }
                        });
            }
        });
    }

    class GroupMoreFeaturesAdapter extends BaseAdapter {
        // List<BaseSortModel<RoomMember>> mSortRoomMember;
        List<RoomMember> mSortRoomMember;

/*
        GroupMoreFeaturesAdapter(List<BaseSortModel<RoomMember>> sortRoomMember) {
            this.mSortRoomMember = new ArrayList<>();
            this.mSortRoomMember = sortRoomMember;
        }

        public void setData(List<BaseSortModel<RoomMember>> sortRoomMember) {
            this.mSortRoomMember = sortRoomMember;
            notifyDataSetChanged();
        }
*/

        GroupMoreFeaturesAdapter(List<RoomMember> sortRoomMember) {
            this.mSortRoomMember = new ArrayList<>();
            this.mSortRoomMember = sortRoomMember;
        }

        public void setData(List<RoomMember> sortRoomMember) {
            this.mSortRoomMember = sortRoomMember;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSortRoomMember.size();
        }

        @Override
        public Object getItem(int position) {
            return mSortRoomMember.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_room_all_member, parent, false);
            }
            TextView catagoryTitleTv = ViewHolder.get(convertView, R.id.catagory_title);
            ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
            TextView roleS = ViewHolder.get(convertView, R.id.roles);
            TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);

/*
            // 根据position获取分类的首字母的Char ascii值
            int section = getSectionForPosition(position);
            // 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == getPositionForSection(section)) {
                catagoryTitleTv.setVisibility(View.VISIBLE);
                catagoryTitleTv.setText(mSortRoomMember.get(position).getFirstLetter());
            } else {
                catagoryTitleTv.setVisibility(View.GONE);
            }
*/
            catagoryTitleTv.setVisibility(View.GONE);

            // RoomMember member = mSortRoomMember.get(position).getBean();
            RoomMember member = mSortRoomMember.get(position);
            if (member != null) {
                AvatarHelper.getInstance().displayAvatar(member.getUserId(), avatarImg, true);
                if (member.getRole() == 1) {
                    roleS.setBackgroundResource(R.drawable.bg_role1);
                    roleS.setText(InternationalizationHelper.getString("JXGroup_Owner"));
                } else if (member.getRole() == 2) {
                    roleS.setBackgroundResource(R.drawable.bg_role2);
                    roleS.setText(InternationalizationHelper.getString("JXGroup_Admin"));
                } else {
                    roleS.setBackgroundResource(R.drawable.bg_role3);
                    roleS.setText(InternationalizationHelper.getString("JXGroup_RoleNormal"));
                }
                userNameTv.setText(getName(member));
            }
            return convertView;
        }

/*
        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int section) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortRoomMember.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == section) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortRoomMember.get(position).getFirstLetter().charAt(0);
        }
*/
    }
}

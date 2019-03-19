package com.sk.weichat.ui.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.adapter.FriendSortAdapter;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.sortlist.BaseComparator;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.sortlist.SideBar;
import com.sk.weichat.sortlist.SortHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 黑名单列表
 */
public class BlackActivity extends BaseActivity {
    private ListView mPullToRefreshListView;
    private FriendSortAdapter mAdapter;
    private List<BaseSortModel<Friend>> mSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private SideBar mSideBar;
    private TextView mTextDialog;
    private String mLoginUserId;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
                loadData();
                mAdapter.setData(mSortFriends);
            }
        }
    };

    public BlackActivity() {
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.black_list);
    }

    private void initView() {
        mPullToRefreshListView = (ListView) findViewById(R.id.pull_refresh_list);
        mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Friend friend = mSortFriends.get(position).getBean();
                if (friend != null) {
                    Intent intent = new Intent(BlackActivity.this, BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                    startActivity(intent);
                }
            }
        });

        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mPullToRefreshListView.setSelection(position);
                }
            }
        });

        mAdapter = new FriendSortAdapter(this, mSortFriends);
        mPullToRefreshListView.setAdapter(mAdapter);
        loadData();

        registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
    }

    private void loadData() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            final List<Friend> friends = FriendDao.getInstance().getAllBlacklists(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
                if (friends.size() == 0) {
                    findViewById(R.id.fl_empty).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.fl_empty).setVisibility(View.GONE);
                }
            });
        });
    }

    //////////////其他操作///////////////////
   /* private CardcastActivity mActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (CardcastActivity) getActivity();
    }*/

   /* private BaseSortModel<Friend> mmsortFriend = null;
    private NewFriendMessage mmessage = null;
    private String deletepacketid = null;

    private void showLongClickOperationDialog(final BaseSortModel<Friend> sortFriend) {
        Friend friend = sortFriend.getBean();
        if (friend.getStatus() != Friend.STATUS_BLACKLIST && friend.getStatus() == Friend.STATUS_ATTENTION
                && friend.getStatus() == Friend.STATUS_FRIEND) {
            return;
        }
        CharSequence[] items = new CharSequence[3];
        items[0] = getString(R.string.set_remark_name);// 设置备注名
        if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
            items[1] = getString(R.string.remove_blacklist);
        } else {
            // items[1] = getString(R.string.add_blacklist);
        }
        // items[2] = getString(R.string.cancel_attention);
        items[2] = getString(R.string.delete_all);

        new AlertDialog.Builder(getActivity()).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:// 设置备注名
                        showRemarkDialog(sortFriend);
                        break;
                    case 1:// 加入黑名单，或者移除黑名单
                        showBlacklistDialog(sortFriend);
                        break;
                    case 2:// 解除关注关系或者解除好友关系
                        showDeleteAllDialog(sortFriend);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    *//**
     * 设置备注名称
     *//*
    private void showRemarkDialog(final BaseSortModel<Friend> sortFriend) {
        DialogHelper.showSingleInputDialog(getActivity(), getString(R.string.set_remark_name), sortFriend.getBean().getShowName(), 2, 2, new InputFilter[]{new InputFilter.LengthFilter(20)}, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = ((EditText) v).getText().toString().trim();
                if (input.equals(sortFriend.getBean().getShowName())) {// 备注名没变
                    return;
                }
                *//*if (!StringUtils.isNickName(input)) {// 不符合昵称
                    if (input.length() != 0) {
                        ToastUtil.showToast(getActivity(), R.string.remark_name_format_error);
                        return;
                    } else {// 不符合昵称，因为长度为0，但是可以做备注名操作，操作就是清除备注名
                        // 判断之前有没有备注名
                        if (TextUtils.isEmpty(sortFriend.getBean().getRemarkName())) {// 如果没有备注名，就不需要清除
                            return;
                        }
                    }
                }*//*
                remarkFriend(sortFriend, input);
            }
        });
    }

    private void remarkFriend(final BaseSortModel<Friend> sortFriend, final String remarkName) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", sortFriend.getBean().getUserId());
        params.put("remarkName", remarkName);
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_REMARK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        String firstLetter = sortFriend.getFirstLetter();
                        mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
                        sortFriend.getBean().setRemarkName(remarkName);// 修改备注名称
                        setSortCondition(sortFriend);
                        Collections.sort(mSortFriends, mBaseComparator);
                        mAdapter.notifyDataSetChanged();
                        // 更新到数据库
                        FriendDao.getInstance().setRemarkName(mLoginUserId, sortFriend.getBean().getUserId(), remarkName);
                        // 更新消息界面（因为昵称变了，所有要更新）
                        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    *//**
     * 移除黑名单
     *//*
    private void showBlacklistDialog(final BaseSortModel<Friend> sortFriend) {
        removeBlacklist(sortFriend);
    }

    private void removeBlacklist(final BaseSortModel<Friend> sortFriend) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", sortFriend.getBean().getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(getActivity(), "移出黑名单成功");
                        FriendDao.getInstance().updateFriendStatus(sortFriend.getBean().getOwnerId(), sortFriend.getBean().getUserId(), Friend.STATUS_FRIEND);

                        ChatMessage removeChatMessage = new ChatMessage();
                        removeChatMessage.setContent(coreManager.getSelf().getNickName() + InternationalizationHelper.getString("REMOVE"));
                        removeChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                        FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);

                        mSortFriends.remove(sortFriend);
                        String firstLetter = sortFriend.getFirstLetter();
                        mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
                        mAdapter.notifyDataSetInvalidated();

                        CardcastUiUpdateUtil.broadcastUpdateUi(getActivity());
                        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    *//**
     * 彻底删除
     *//*
    private void showDeleteAllDialog(final BaseSortModel<Friend> sortFriend) {
        if (sortFriend.getBean().getStatus() == Friend.STATUS_UNKNOW) {
            return;
        }
        deleteFriend(sortFriend, 1);
    }

    private void deleteFriend(final BaseSortModel<Friend> sortFriend, final int type) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", sortFriend.getBean().getUserId());
        String url = mActivity.coreManager.getConfig().FRIENDS_DELETE;
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(getActivity(), R.string.delete_all_succ);
                        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                                XmppMessage.TYPE_DELALL, null, sortFriend.getBean());

                        mActivity.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);
                        deletepacketid = message.getPacketId();
                        mmessage = message;
                        mmsortFriend = sortFriend;
                        FriendHelper.removeAttentionOrFriend(mLoginUserId, mmsortFriend.getBean().getUserId());

                        mSortFriends.remove(mmsortFriend);
                        String firstLetter = mmsortFriend.getFirstLetter();
                        // 移除之前设置的首字母
                        mSideBar.removeExist(firstLetter);
                        mAdapter.notifyDataSetInvalidated();

                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setContent(InternationalizationHelper.getString("JXAlert_DeleteFirend") + " " + mmsortFriend.getBean().getNickName());
                        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                        FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, chatMessage);

                        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                        NewFriendDao.getInstance().changeNewFriendState(mmsortFriend.getBean().getUserId(), Friend.STATUS_16);
                        ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mmessage, true);

                        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }*/
}

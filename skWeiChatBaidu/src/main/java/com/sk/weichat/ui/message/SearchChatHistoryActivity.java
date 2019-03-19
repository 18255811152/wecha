package com.sk.weichat.ui.message;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.CommonAdapter;
import com.sk.weichat.util.CommonViewHolder;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * 单、群 聊查找聊天记录
 */
public class SearchChatHistoryActivity extends BaseActivity {
    private ClearEditText mEditText;
    private ListView mSearchListView;
    private SearchAdapter mSearchAdapter;
    private List<ChatMessage> mSearchChatMessageList;

    private String mLoginUserId;
    private boolean isSearchSingle;// 单聊 || 群聊
    private String mFriendId;
    private Friend mFriend;
    private String mFriendName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_chat_history);
        mLoginUserId = coreManager.getSelf().getUserId();
        isSearchSingle = getIntent().getBooleanExtra("isSearchSingle", false);
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);
        if (mFriend == null) {
            ToastUtil.showErrorData(this);
            return;
        }
        mFriendName = TextUtils.isEmpty(mFriend.getRemarkName()) ? mFriend.getNickName() : mFriend.getRemarkName();
        initActionBar();
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
        if (isSearchSingle) {
            tvTitle.setText(getString(R.string.find_friend_chat_history_place_holder, mFriendName));
        } else {
            tvTitle.setText(getString(R.string.find_room_chat_history_place_holder, mFriendName));
        }
    }

    private void initView() {
        mSearchChatMessageList = new ArrayList<>();

        mEditText = (ClearEditText) findViewById(R.id.search_edit);
        mSearchListView = (ListView) findViewById(R.id.chat_history_lv);
        mSearchAdapter = new SearchAdapter(this, mSearchChatMessageList);
        mSearchListView.setAdapter(mSearchAdapter);
        mSearchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage chatMessage = mSearchChatMessageList.get(position);
                if (chatMessage != null) {
                    // 在跳转之前关闭软键盘
                    InputMethodManager inputManager = (InputMethodManager) SearchChatHistoryActivity.this.getSystemService(INPUT_METHOD_SERVICE);
                    if (inputManager != null) {
                        inputManager.hideSoftInputFromWindow(findViewById(R.id.main_content).getWindowToken(), 0); //强制隐藏键盘
                    }

                    Intent intent;
                    if (isSearchSingle) {
                        intent = new Intent(SearchChatHistoryActivity.this, ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND, mFriend);
                    } else {
                        intent = new Intent(SearchChatHistoryActivity.this, MucChatActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                        intent.putExtra(AppConstant.EXTRA_NICK_NAME, mFriendName);
                    }
                    intent.putExtra("isserch", true);
                    intent.putExtra("jilu_id", chatMessage.getTimeSend());
                    startActivity(intent);
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mSearchChatMessageList.clear();
                String keyWord = s.toString();
                if (!TextUtils.isEmpty(keyWord)) {
                    List<ChatMessage> messages = ChatMessageDao.getInstance().queryChatMessageByContent(mLoginUserId, mFriendId, keyWord);
                    mSearchChatMessageList.addAll(messages);
                }
                mSearchAdapter.notifyDataSetChanged();

                if (mSearchChatMessageList.size() > 0) {
                    findViewById(R.id.empty).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.empty).setVisibility(View.VISIBLE);
                }
                if (TextUtils.isEmpty(keyWord)) {
                    findViewById(R.id.empty).setVisibility(View.GONE);
                }
            }
        });
    }

    class SearchAdapter extends CommonAdapter<ChatMessage> {

        public SearchAdapter(Context context, List<ChatMessage> data) {
            super(context, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.row_search_chat_history, position);
            ImageView avatar_iv = viewHolder.getView(R.id.avatar_img);
            TextView nick_name_tv = viewHolder.getView(R.id.nick_name_tv);
            TextView time_tv = viewHolder.getView(R.id.time_tv);
            TextView content_tv = viewHolder.getView(R.id.content_tv);
            ChatMessage chatMessage = data.get(position);
            if (chatMessage != null) {
                AvatarHelper.getInstance().displayAvatar(chatMessage.getFromUserId(), avatar_iv);
                if (isSearchSingle) {
                    if (chatMessage.getFromUserId().equals(mLoginUserId)) {
                        nick_name_tv.setText(coreManager.getSelf().getNickName());
                    } else {
                        nick_name_tv.setText(mFriendName);
                    }
                } else {
                    nick_name_tv.setText(chatMessage.getFromUserName());
                }
                time_tv.setText(TimeUtils.getFriendlyTimeDesc(SearchChatHistoryActivity.this, chatMessage.getTimeSend()));
                content_tv.setText(chatMessage.getContent());
            }
            return viewHolder.getConvertView();
        }
    }
}

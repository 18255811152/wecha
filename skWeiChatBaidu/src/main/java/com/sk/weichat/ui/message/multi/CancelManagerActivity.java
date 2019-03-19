package com.sk.weichat.ui.message.multi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.RoomMember;
import com.sk.weichat.bean.SetManager;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.RoomMemberDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewHolder;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.greenrobot.event.EventBus;
import okhttp3.Call;


/**
 * 取消管理员界面
 * 取消隐身人监控人也是这里处理，
 */
public class CancelManagerActivity extends BaseActivity {
    private String roomId;
    private int role;
    private String roomJid;

    private EditText mEditText;
    private ListView mListView;
    private SetManagerAdapter mSetManagerAdapter;
    private List<SetManager> setManagerList;

    public static void start(Context ctx, String roomId, String roomJid, int role) {
        Intent intent = new Intent(ctx, CancelManagerActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("role", role);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_manager);
        if (getIntent() != null) {
            roomId = getIntent().getStringExtra("roomId");
            role = getIntent().getIntExtra("role", RoomMember.ROLE_MANAGER);
            roomJid = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        }
        initActionBar();
        loadData();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        int titleId;
        switch (role) {
            case RoomMember.ROLE_MANAGER:
                titleId = R.string.cancel_admin;
                break;
            case RoomMember.ROLE_INVISIBLE:
                titleId = R.string.cancel_invisible;
                break;
            case RoomMember.ROLE_GUARDIAN:
                titleId = R.string.cancel_guardian;
                break;
            default:
                Reporter.unreachable();
                return;
        }
        TextView textView = findViewById(R.id.tv_title_center);
        textView.setText(titleId);
    }

    private void loadData() {
        setManagerList = new ArrayList<>();
        List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);
        Log.e("zq", String.valueOf(roomMember.size()));

        // 保留旧代码，只列出已经是当前身份的人，
        for (int i = 0; i < roomMember.size(); i++) {
            SetManager setManager = new SetManager();
            if (roomMember.get(i).getRole() == role) {
                setManager.setRole(roomMember.get(i).getRole());
                setManager.setCreateTime(roomMember.get(i).getCreateTime());
                setManager.setUserId(roomMember.get(i).getUserId());
                setManager.setNickName(roomMember.get(i).getCardName());
                setManagerList.add(setManager);
                Log.d("zq", String.valueOf(setManagerList.size()));
            }
        }
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.set_manager_lv);
        mSetManagerAdapter = new SetManagerAdapter(this);
        mSetManagerAdapter.setData(setManagerList);
        mListView.setAdapter(mSetManagerAdapter);

        /**
         * 群内邀请好友搜索功能
         */
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
                String mContent = mEditText.getText().toString();
                List<SetManager> setManagers = new ArrayList<SetManager>();
                if (TextUtils.isEmpty(mContent)) {
                    mSetManagerAdapter.setData(setManagerList);
                }
                for (int i = 0; i < setManagerList.size(); i++) {
                    if (setManagerList.get(i).getNickName().contains(mContent)) {
                        // 符合搜索条件的好友
                        setManagers.add((setManagerList.get(i)));
                    }
                }
                mSetManagerAdapter.setData(setManagers);
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SetManager setManager = (SetManager) mSetManagerAdapter.getItem(i);
                String userId = setManager.getUserId();
                if (role == RoomMember.ROLE_MANAGER) {
                    // 取消管理员，保留旧代码，
                    cancelManager(roomId, userId);
                } else {
                    // 取消隐身人，监控人，
                    cancelRole(roomId, userId, role);
                }
            }
        });
    }

    private void cancelManager(String roomId, final String userId) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("touserId", userId);
        params.put("type", String.valueOf(3));

        HttpUtils.get().url(coreManager.getConfig().ROOM_MANAGER)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(CancelManagerActivity.this, InternationalizationHelper.getString("JXRoomMemberVC_CancelAdministratorSuccess"), Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new EventGroupStatus(10000, 0));
                            finish();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(CancelManagerActivity.this, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(CancelManagerActivity.this, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(CancelManagerActivity.this, getString(R.string.check_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cancelRole(String roomId, final String userId, final int role) {
        Integer type;
        switch (role) {
            case RoomMember.ROLE_INVISIBLE:
                type = -1;
                break;
            case RoomMember.ROLE_GUARDIAN:
                type = 0;
                break;
            default:
                Reporter.unreachable();
                return;
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("touserId", userId);
        params.put("type", type.toString());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE_ROLE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            int tipContent;
                            switch (role) {
                                case RoomMember.ROLE_INVISIBLE: // 隐身人
                                    tipContent = R.string.tip_cancel_invisible_success;
                                    break;
                                case RoomMember.ROLE_GUARDIAN: // 监控人
                                    tipContent = R.string.tip_cancel_guardian_success;
                                    break;
                                default:
                                    Reporter.unreachable();
                                    return;
                            }
                            ToastUtil.showToast(CancelManagerActivity.this, tipContent);
                            // 保留旧代码，抛出去RoomInfoActivity统一处理，
                            EventBus.getDefault().post(new EventGroupStatus(10000, 0));
                            finish();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(CancelManagerActivity.this, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(CancelManagerActivity.this, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(CancelManagerActivity.this);
                    }
                });
    }

    private class SetManagerAdapter extends BaseAdapter {
        private List<SetManager> mSetManager;
        private Context mContext;

        public SetManagerAdapter(Context context) {
            mSetManager = new ArrayList<>();
            mContext = context;
        }

        public void setData(List<SetManager> setManager) {
            this.mSetManager = setManager;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSetManager.size();
        }

        @Override
        public Object getItem(int i) {
            return mSetManager.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.a_item_set_manager, viewGroup, false);
            }
            ImageView avatar_img = ViewHolder.get(view, R.id.set_manager_iv);
            TextView roleS = ViewHolder.get(view, R.id.roles);
            TextView nick_name_tv = ViewHolder.get(view, R.id.set_manager_tv);
            // 设置头像
            AvatarHelper.getInstance().displayAvatar(mSetManager.get(i).getUserId(), avatar_img, true);
            if (mSetManager.get(i).getRole() == 1) {
                roleS.setBackgroundResource(R.drawable.bg_role1);
                roleS.setText(InternationalizationHelper.getString("JXGroup_Owner"));
            } else if (mSetManager.get(i).getRole() == 2) {
                roleS.setBackgroundResource(R.drawable.bg_role2);
                roleS.setText(InternationalizationHelper.getString("JXGroup_Admin"));
            } else {
                roleS.setBackgroundResource(R.drawable.bg_role3);
                roleS.setText(InternationalizationHelper.getString("JXGroup_RoleNormal"));
            }
            // 设置昵称
            nick_name_tv.setText(mSetManager.get(i).getNickName());
            return view;
        }
    }
}

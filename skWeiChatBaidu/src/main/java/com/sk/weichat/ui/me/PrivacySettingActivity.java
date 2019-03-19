package com.sk.weichat.ui.me;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.PrivacySetting;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.map.MapHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.MsgSyncDaysDialog;
import com.suke.widget.SwitchButton;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

import static com.sk.weichat.util.Constants.IS_GOOGLE_MAP_KEY;

/**
 * 隐私设置
 **/
public class PrivacySettingActivity extends BaseActivity implements View.OnClickListener {
    private TextView mMsgSyncDays;

    private SwitchButton mSbVerify;    // 需要朋友验证

    private SwitchButton mSbEncrypt;   // 消息加密传输
    private SwitchButton mSbVibration; // 消息来时振动
    private SwitchButton mSbInputState;// 让对方知道我正在输入...

    private SwitchButton mSbUseGoogleMap;// 使用谷歌地图
    private SwitchButton mSbSupport;     // 支持多设备登录

    private TextView addFriendTv, isEncryptTv, inputStateTv;

    private String mLoginUserId;
    MsgSyncDaysDialog.OnMsgSaveDaysDialogClickListener onMsgSyncDaysDialogClickListener = new MsgSyncDaysDialog.OnMsgSaveDaysDialogClickListener() {
        @Override
        public void tv1Click() {
            updateMsgSyncTimeLen(-1);
        }

        @Override
        public void tv8Click() {
            updateMsgSyncTimeLen(-2);
        }

        @Override
        public void tv2Click() {
            updateMsgSyncTimeLen(0.04);
        }

        @Override
        public void tv3Click() {
            updateMsgSyncTimeLen(1);
        }

        @Override
        public void tv4Click() {
            updateMsgSyncTimeLen(7);
        }

        @Override
        public void tv5Click() {
            updateMsgSyncTimeLen(30);
        }

        @Override
        public void tv6Click() {
            updateMsgSyncTimeLen(90);
        }

        @Override
        public void tv7Click() {
            updateMsgSyncTimeLen(365);
        }
    };
    SwitchButton.OnCheckedChangeListener onCheckedChangeListener = new SwitchButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(SwitchButton view, boolean isChecked) {
            switch (view.getId()) {
                case R.id.mSbVerify:
                    submitPrivacySetting(1, isChecked);
                    break;
                case R.id.mSbEncrypt:
                    submitPrivacySetting(2, isChecked);
                    break;
                case R.id.mSbzhendong:
                    submitPrivacySetting(3, isChecked);
                    break;
                case R.id.mSbInputState:
                    submitPrivacySetting(4, isChecked);
                    break;
                case R.id.sb_google_map:
                    submitPrivacySetting(5, isChecked);
                    break;
                case R.id.mSbSupport:
                    submitPrivacySetting(6, isChecked);
                    break;
            }
        }
    };
    private int friendsVerify = 0;// 是否开启验证

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_setting);

        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        getPrivacySetting();
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
        tvTitle.setText(InternationalizationHelper.getString("JX_PrivacySettings"));
    }

    private void initView() {
        mMsgSyncDays = (TextView) findViewById(R.id.msg_sync_days_tv);

        mSbVerify = (SwitchButton) findViewById(R.id.mSbVerify);

        mSbEncrypt = (SwitchButton) findViewById(R.id.mSbEncrypt);
        mSbVibration = (SwitchButton) findViewById(R.id.mSbzhendong);
        mSbInputState = (SwitchButton) findViewById(R.id.mSbInputState);

        mSbUseGoogleMap = (SwitchButton) findViewById(R.id.sb_google_map);
        mSbSupport = (SwitchButton) findViewById(R.id.mSbSupport);

        addFriendTv = (TextView) findViewById(R.id.addFriend_text);
        addFriendTv.setText(InternationalizationHelper.getString("JXSettings_FirendVerify"));
        isEncryptTv = (TextView) findViewById(R.id.isEncrypt_text);
        isEncryptTv.setText(InternationalizationHelper.getString("DES_CHAT"));
        inputStateTv = (TextView) findViewById(R.id.tv_input_state);
        inputStateTv.setText(InternationalizationHelper.getString("LET_OTHER_KNOW"));
    }

    // 获取用户的设置状态
    private void getPrivacySetting() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_PRIVACY_SETTING)
                .params(params)
                .build()
                .execute(new BaseCallback<PrivacySetting>(PrivacySetting.class) {

                    @Override
                    public void onResponse(ObjectResult<PrivacySetting> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            PrivacySetting settings = result.getData();
                            MyApplication.getInstance().initPrivateSettingStatus(mLoginUserId, settings.getChatSyncTimeLen(),
                                    settings.getIsEncrypt(), settings.getIsVibration(), settings.getIsTyping(),
                                    settings.getIsUseGoogleMap(), settings.getMultipleDevices());

                            friendsVerify = settings.getFriendsVerify();
                        }

                        initStatus();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(PrivacySettingActivity.this);

                        initStatus();
                    }
                });
    }

    private void initStatus() {
        String chatSyncTimeLen = PreferenceUtils.getString(this, Constants.CHAT_SYNC_TIME_LEN + mLoginUserId, "1");
        mMsgSyncDays.setText(conversion(Double.parseDouble(chatSyncTimeLen)));

        mSbVerify.setChecked(friendsVerify == 1);

        // 获得加密状态
        boolean isEncrypt = PreferenceUtils.getBoolean(this, Constants.IS_ENCRYPT + mLoginUserId, false);
        mSbEncrypt.setChecked(isEncrypt);
        // 获得振动状态
        boolean vibration = PreferenceUtils.getBoolean(this, Constants.MSG_COME_VIBRATION + mLoginUserId, false);
        mSbVibration.setChecked(vibration);
        // 获得输入状态
        boolean inputStatus = PreferenceUtils.getBoolean(this, Constants.KNOW_ENTER_STATUS + mLoginUserId, false);
        mSbInputState.setChecked(inputStatus);

        // 获得使用状态
        boolean iSGoogleMap = PreferenceUtils.getBoolean(this, Constants.IS_GOOGLE_MAP_KEY, false);
        mSbUseGoogleMap.setChecked(iSGoogleMap);

        // 获得支持状态
        boolean isSupport = PreferenceUtils.getBoolean(this, "RESOURCE_TYPE", true);
        mSbSupport.setChecked(isSupport);

        findViewById(R.id.msg_sync_days_rl).setOnClickListener(this);

        mSbSupport.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSbVerify.setOnCheckedChangeListener(onCheckedChangeListener);

                mSbEncrypt.setOnCheckedChangeListener(onCheckedChangeListener);
                mSbVibration.setOnCheckedChangeListener(onCheckedChangeListener);
                mSbInputState.setOnCheckedChangeListener(onCheckedChangeListener);

                mSbUseGoogleMap.setOnCheckedChangeListener(onCheckedChangeListener);
                mSbSupport.setOnCheckedChangeListener(onCheckedChangeListener);
            }
        }, 200);
    }

    @Override
    public void onClick(View v) {
        MsgSyncDaysDialog msgSyncDaysDialog = new MsgSyncDaysDialog(this, onMsgSyncDaysDialogClickListener);
        msgSyncDaysDialog.show();
    }

    private String conversion(double outTime) {
        String outTimeStr;
        if (outTime == -1 || outTime == 0) {
            outTimeStr = getString(R.string.permanent);
        } else if (outTime == -2) {
            outTimeStr = getString(R.string.no_sync);
        } else if (outTime == 0.04) {
            outTimeStr = getString(R.string.one_hour);
        } else if (outTime == 1) {
            outTimeStr = getString(R.string.one_day);
        } else if (outTime == 7) {
            outTimeStr = getString(R.string.one_week);
        } else if (outTime == 30) {
            outTimeStr = getString(R.string.one_month);
        } else if (outTime == 90) {
            outTimeStr = getString(R.string.one_season);
        } else {
            outTimeStr = getString(R.string.one_year);
        }
        return outTimeStr;
    }

    // 更新消息漫游时长
    private void updateMsgSyncTimeLen(final double syncTime) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        params.put("chatSyncTimeLen", String.valueOf(syncTime));

        HttpUtils.get().url(coreManager.getConfig().USER_SET_PRIVACY_SETTING)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(PrivacySettingActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            PreferenceUtils.putString(PrivacySettingActivity.this, Constants.CHAT_SYNC_TIME_LEN + mLoginUserId, String.valueOf(syncTime));
                            mMsgSyncDays.setText(conversion(syncTime));
                        } else {
                            Toast.makeText(PrivacySettingActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 提交设置用户的状态
    private void submitPrivacySetting(final int index, final boolean isChecked) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        String status = isChecked ? "1" : "0";
        if (index == 1) {
            params.put("friendsVerify", status);
        } else if (index == 2) {
            params.put("isEncrypt", status);
        } else if (index == 3) {
            params.put("isVibration", status);
        } else if (index == 4) {
            params.put("isTyping", status);
        } else if (index == 5) {
            params.put("isUseGoogleMap", status);
        } else if (index == 6) {
            params.put("multipleDevices", status);
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_SET_PRIVACY_SETTING)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(PrivacySettingActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            if (index == 2) {
                                PreferenceUtils.putBoolean(PrivacySettingActivity.this, Constants.IS_ENCRYPT + mLoginUserId, isChecked);
                            } else if (index == 3) {
                                PreferenceUtils.putBoolean(PrivacySettingActivity.this, Constants.MSG_COME_VIBRATION + mLoginUserId, isChecked);
                            } else if (index == 4) {
                                PreferenceUtils.putBoolean(PrivacySettingActivity.this, Constants.KNOW_ENTER_STATUS + mLoginUserId, isChecked);
                            } else if (index == 5) {
                                PreferenceUtils.putBoolean(PrivacySettingActivity.this, IS_GOOGLE_MAP_KEY, isChecked);
                                if (isChecked) {
                                    MapHelper.setMapType(MapHelper.MapType.GOOGLE);
                                } else {
                                    MapHelper.setMapType(MapHelper.MapType.BAIDU);
                                }
                            } else if (index == 6) {
                                PreferenceUtils.putBoolean(PrivacySettingActivity.this, "RESOURCE_TYPE", isChecked);
                                DialogHelper.tip(PrivacySettingActivity.this, getString(R.string.multi_login_need_reboot));
                            }
                        } else {
                            ToastUtil.showErrorData(PrivacySettingActivity.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(PrivacySettingActivity.this);
                    }
                });
    }
}

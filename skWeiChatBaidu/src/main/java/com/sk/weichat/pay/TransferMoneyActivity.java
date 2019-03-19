package com.sk.weichat.pay;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.EventTransfer;
import com.sk.weichat.bean.Transfer;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.me.redpacket.ChangePayPasswordActivity;
import com.sk.weichat.ui.me.redpacket.PayPasswordVerifyDialog;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.KeyboardxView;
import com.sk.weichat.view.VerifyDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class TransferMoneyActivity extends BaseActivity {
    private String mTransferredUserId, mTransferredName;

    private ImageView mTransferredIv;
    private TextView mTransferredTv;

    private String money, words;// 转账金额与转账说明

    private TextView mTransferDescTv, mTransferDescClickTv;
    private TextView mMoneyTv;
    private KeyboardxView mKeyboardxView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_money);
        mTransferredUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mTransferredName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
        initActionBar();
        initView();
        initEvent();
        checkHasPayPassword();
    }

    private void checkHasPayPassword() {
        boolean hasPayPassword = PreferenceUtils.getBoolean(this, Constants.IS_PAY_PASSWORD_SET + coreManager.getSelf().getUserId(), true);
        if (!hasPayPassword) {
            ToastUtil.showToast(this, R.string.tip_no_pay_password);
            Intent intent = new Intent(this, ChangePayPasswordActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.transfer_money));
    }

    private void initView() {
        mTransferredIv = findViewById(R.id.tm_iv);
        mTransferredTv = findViewById(R.id.tm_tv);
        AvatarHelper.getInstance().displayAvatar(mTransferredUserId, mTransferredIv);
        mTransferredTv.setText(mTransferredName);

        mTransferDescTv = findViewById(R.id.transfer_desc_tv);
        mTransferDescClickTv = findViewById(R.id.transfer_edit_desc_tv);
        mMoneyTv = findViewById(R.id.transfer_je_tv);
        mMoneyTv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);// 允许输入数字与小数点

        mKeyboardxView = findViewById(R.id.transfer_keyboard);
        mKeyboardxView.setMaxLength(10);
    }

    private void initEvent() {
        mMoneyTv.setOnClickListener(v -> mKeyboardxView.setVisibility(View.VISIBLE));

        mTransferDescClickTv.setOnClickListener(v -> {
            VerifyDialog verifyDialog = new VerifyDialog(mContext);
            verifyDialog.setVerifyClickListener(getString(R.string.transfer_money_desc), getString(R.string.transfer_desc_max_length_10),
                    words, 10, new VerifyDialog.VerifyClickListener() {
                        @Override
                        public void cancel() {
                            mKeyboardxView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void send(String str) {
                            words = str;
                            if (TextUtils.isEmpty(words)) {
                                mTransferDescTv.setText("");
                                mTransferDescTv.setVisibility(View.GONE);
                            } else {
                                mTransferDescTv.setText(str);
                                mTransferDescTv.setVisibility(View.VISIBLE);
                            }
                            mKeyboardxView.setVisibility(View.VISIBLE);
                        }
                    });
            verifyDialog.setOkButton(R.string.sure);
            mKeyboardxView.setVisibility(View.GONE);
            Window window = verifyDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); // 软键盘弹起
            }
            verifyDialog.show();
        });

        findViewById(R.id.transfer_btn).setOnClickListener(v -> {
            if (TextUtils.isEmpty(money)) {
                Toast.makeText(mContext, getString(R.string.transfer_input_money), Toast.LENGTH_SHORT).show();
                return;
            }
            PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
            dialog.setAction(getString(R.string.transfer_money_to_someone, mTransferredName));
            dialog.setMoney(money);
            dialog.setOnInputFinishListener(password -> transfer(money, words, password));
            dialog.show();
        });

        mKeyboardxView.addOnInputTextListener(text -> {
            money = text;
            mMoneyTv.setText(text);
        });
    }

    public void transfer(String money, final String words, String payPassword) {
        if (!coreManager.isLogin()) {
            return;
        }
        Map<String, String> params = new HashMap();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mTransferredUserId);
        params.put("money", money);
        params.put("remark", words);

        HttpUtils.get().url(coreManager.getConfig().SKTRANSFER_SEND_TRANSFER)
                .params(params)
                .addSecret(payPassword, money)
                .build()
                .execute(new BaseCallback<Transfer>(Transfer.class) {

                    @Override
                    public void onResponse(ObjectResult<Transfer> result) {
                        Transfer transfer = result.getData();
                        if (result.getResultCode() != 1) {
                            // 发送红包失败，
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            String objectId = transfer.getId();
                            ChatMessage message = new ChatMessage();
                            message.setType(XmppMessage.TYPE_TRANSFER);
                            message.setFromUserId(coreManager.getSelf().getUserId());
                            message.setFromUserName(coreManager.getSelf().getNickName());
                            message.setToUserId(mTransferredUserId);
                            message.setContent(money);// 转账金额
                            message.setFilePath(words); // 转账说明
                            message.setObjectId(objectId); // 红包id
                            CoreManager.updateMyBalance();

                            EventBus.getDefault().post(new EventTransfer(message));
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

}

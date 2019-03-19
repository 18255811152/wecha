package com.sk.weichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.me.redpacket.alipay.AlipayHelper;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.text.DecimalFormat;

public class QuXianActivity extends BaseActivity {
    private IWXAPI api;

    private EditText mMentionMoneyEdit;
    private TextView mBalanceTv;
    private TextView mAllMentionTv;
    private TextView mSureMentionTv;
    private TextView tvAlipay;

    private DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public static String amount;// 提现金额 单位:分

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qu_xian);

        api = WXAPIFactory.createWXAPI(QuXianActivity.this, Constants.VX_APP_ID, false);
        api.registerApp(Constants.VX_APP_ID);

        initActionbar();
        initView();
        intEvent();

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

    private void initActionbar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.withdraw));
/*
        TextView mTvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        mTvTitleRight.setText(getString(R.string.withdrawal_instructions));
*/
    }

    private void initView() {
        mMentionMoneyEdit = (EditText) findViewById(R.id.tixianmoney);
        mBalanceTv = (TextView) findViewById(R.id.blance_weixin);
        mBalanceTv.setText("￥" + decimalFormat.format(coreManager.getSelf().getBalance()));
        mAllMentionTv = (TextView) findViewById(R.id.tixianall);
        mSureMentionTv = (TextView) findViewById(R.id.tixian);
        tvAlipay = (TextView) findViewById(R.id.withdraw_alipay);
    }

    private void intEvent() {
        mMentionMoneyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (TextUtils.isEmpty(moneyStr)) {
                    mSureMentionTv.setBackgroundResource(R.drawable.weixin_text_yuanjiao_no);
                    tvAlipay.setBackgroundResource(R.drawable.weixin_text_yuanjiao_no);
                } else {
                    mSureMentionTv.setBackgroundResource(R.drawable.weixin_text_yuanjiao);
                    tvAlipay.setBackgroundResource(R.drawable.weixin_text_yuanjiao);
                }
            }
        });

        mAllMentionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMentionMoneyEdit.setText(decimalFormat.format(coreManager.getSelf().getBalance()) + "");
            }
        });

        mSureMentionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (checkMoney(moneyStr)) {
                    amount = String.valueOf(Integer.valueOf(moneyStr) * 100);

                    SendAuth.Req req = new SendAuth.Req();
                    req.scope = "snsapi_userinfo";
                    req.state = "wechat_sdk_demo_test";
                    api.sendReq(req);

                    finish();
                }
            }
        });

        tvAlipay.setOnClickListener(v -> {
            String moneyStr = mMentionMoneyEdit.getText().toString();
            if (checkMoney(moneyStr)) {
                amount = moneyStr;
                AlipayHelper.auth(this, coreManager, userId -> {
                    AlipayHelper.withdraw(this, coreManager, amount, userId);
                });
            }
        });
    }

    private boolean checkMoney(String moneyStr) {
        if (TextUtils.isEmpty(moneyStr)) {
            DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_empty));
        } else {
            if (Double.valueOf(moneyStr) < 1) {
                DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_too_little));
            } else if (Double.valueOf(moneyStr) > coreManager.getSelf().getBalance()) {
                DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_balance_not_enough));
            } else {// 获取用户code
                return true;
            }
        }
        return false;
    }
}

package com.sk.weichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.redpacket.Balance;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.text.DecimalFormat;
import java.util.HashMap;

import okhttp3.Call;

public class WxPayBlance extends BaseActivity {

    public static final String RSA_PRIVATE = "";
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;
    private TextView mBalanceTv;
    private TextView mRechargeTv;
    private TextView mWithdrawTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wx_pay_blance);
        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
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
        mTvTitle.setText(getString(R.string.my_purse));
        TextView mTvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        mTvTitleRight.setText(getString(R.string.expenses_record));
        mTvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 访问接口 获取记录
                Intent intent = new Intent(WxPayBlance.this, MyConsumeRecord.class);
                startActivity(intent);
            }
        });
    }

    private void initView() {
        mBalanceTv = (TextView) findViewById(R.id.myblance);
        mRechargeTv = (TextView) findViewById(R.id.chongzhi);
        mWithdrawTv = (TextView) findViewById(R.id.quxian);

        mRechargeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WxPayBlance.this, WxPayAdd.class);
                startActivity(intent);
            }
        });

        mWithdrawTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WxPayBlance.this, QuXianActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.tvPayPassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WxPayBlance.this, ChangePayPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initData() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            DecimalFormat decimalFormat = new DecimalFormat("0.00");
                            Balance balance = result.getData();
                            coreManager.getSelf().setBalance(Double.parseDouble(decimalFormat.format(balance.getBalance())));
                            mBalanceTv.setText("￥" + decimalFormat.format(Double.parseDouble(decimalFormat.format(balance.getBalance()))));
                        } else {
                            ToastUtil.showErrorData(WxPayBlance.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(WxPayBlance.this);
                    }
                });
    }
}

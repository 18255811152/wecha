package com.sk.weichat.pay;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qrcode.utils.CommonUtils;
import com.sk.weichat.R;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.ScreenUtil;

/**
 * 付款
 */
public class PaymentActivity extends BaseActivity {

    private ImageView mPayQrCodeIv;
    private ImageView mPayBarCodeIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initActionBar();
        initView();
        initEvent();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.receipt_payment));
    }

    private void initView() {
        mPayQrCodeIv = findViewById(R.id.pm_qr_code_iv);
        mPayBarCodeIv = findViewById(R.id.pm_bar_code_iv);

        String code = generateReceiptCode();

        Bitmap bitmap1 = CommonUtils.createQRCode(code, DisplayUtil.dip2px(mContext, 160),
                DisplayUtil.dip2px(mContext, 160));
        Bitmap bitmap2 = CommonUtils.createBarCode(code, ScreenUtil.getScreenWidth(mContext) - DisplayUtil.dip2px(mContext, 40),
                DisplayUtil.dip2px(mContext, 80));
        mPayQrCodeIv.setImageBitmap(bitmap1);
        mPayBarCodeIv.setImageBitmap(bitmap2);
    }

    private void initEvent() {
        mPayQrCodeIv.setOnClickListener(v -> {// 刷新付款码
            String code = generateReceiptCode();

            Bitmap bitmap1 = CommonUtils.createQRCode(code, DisplayUtil.dip2px(mContext, 160),
                    DisplayUtil.dip2px(mContext, 160));
            Bitmap bitmap2 = CommonUtils.createBarCode(code, ScreenUtil.getScreenWidth(mContext) - DisplayUtil.dip2px(mContext, 40),
                    DisplayUtil.dip2px(mContext, 80));
            mPayQrCodeIv.setImageBitmap(bitmap1);
            mPayBarCodeIv.setImageBitmap(bitmap2);
        });

        findViewById(R.id.receipt_tv).setOnClickListener(v ->   startActivity(new Intent(mContext, ReceiptActivity.class)));

    }

    public String generateReceiptCode() {
        /**
         *  规则
         *  (userId+n+opt)的长度+(userId+n+opt)+opt+(time/opt)
         */
        String barCode;
        int type = 1;// 支付类型  1：账户余额    2：银行卡1  3：银行卡2  4：银行卡3  ....

        int n = 9;
        int userId = Integer.valueOf(coreManager.getSelf().getUserId());
        String accessToken = coreManager.getSelfStatus().accessToken;
        long time = System.currentTimeMillis() / 1000;

        // byte[] sha = DigestUtils.sha(accessToken + time + AppConfig.apiKey);
        // int opt = Math.abs(sha[0]);
        int opt = 127;
        if (100 > opt) {
            opt = opt + 100;
        }

        String userCode = String.valueOf(userId + n + opt);
        int userCodeLen = userCode.length();
        barCode = String.valueOf(userCodeLen) + userCode + String.valueOf(opt);

        long timeCode = (time / (opt));
        if (String.valueOf(timeCode).length() < 8) {
            timeCode = (time / (opt - 100));
        }
        barCode += String.valueOf(timeCode);
        Log.e("zq", barCode);
        Log.e("zq", "Len-->" + barCode.length());
        return barCode;
    }
}

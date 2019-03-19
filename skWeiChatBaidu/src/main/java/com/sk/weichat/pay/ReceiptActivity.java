package com.sk.weichat.pay;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qrcode.utils.CommonUtils;
import com.sk.weichat.R;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.DisplayUtil;

/**
 * 收款
 */
public class ReceiptActivity extends BaseActivity {

    private ImageView mReceiptQrCodeIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.rp_receipt));
    }

    private void initView() {
        mReceiptQrCodeIv = findViewById(R.id.rp_qr_code_iv);

        Bitmap bitmap = CommonUtils.createQRCode(coreManager.getSelf().getUserId(), DisplayUtil.dip2px(mContext, 160),
                DisplayUtil.dip2px(mContext, 160));
        mReceiptQrCodeIv.setImageBitmap(bitmap);
    }
}

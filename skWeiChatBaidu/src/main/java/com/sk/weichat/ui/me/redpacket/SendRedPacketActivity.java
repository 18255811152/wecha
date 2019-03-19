package com.sk.weichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.bean.redpacket.RedPacket;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.message.ChatActivity;
import com.sk.weichat.ui.smarttab.SmartTabLayout;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.InputChangeListener;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

import static com.sk.weichat.ui.MainActivity.isAuthenticated;

/**
 * Created by 魏正旺 on 2016/9/9.
 */
public class SendRedPacketActivity extends BaseActivity implements View.OnClickListener {
    LayoutInflater inflater;
    private SmartTabLayout smartTabLayout;
    private ViewPager viewPager;
    private List<View> views;
    private List<String> mTitleList;
    private EditText editTextPt;  // 普通红包的金额输入框
    private EditText editTextKl;  // 口令红包的金额输入框
    private EditText editTextPwd; // 口令输入框
    private EditText editTextGre; // 祝福语输入框
    String toUserId = "";
    PayPasswordVerifyDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redpacket);
        inflater = LayoutInflater.from(this);
        initView();

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

    /**
     * 初始化布局
     */
    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.tv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_SendGift"));

        viewPager = (ViewPager) findViewById(R.id.viewpagert_redpacket);
        smartTabLayout = (SmartTabLayout) findViewById(R.id.smarttablayout_redpacket);
        views = new ArrayList<View>();
        mTitleList = new ArrayList<String>();
        mTitleList.add(InternationalizationHelper.getString("JX_UsualGift"));
        mTitleList.add(InternationalizationHelper.getString("JX_MesGift"));
        View v1, v2;
        v1 = inflater.inflate(R.layout.redpacket_pager_pt, null);
        v2 = inflater.inflate(R.layout.redpacket_pager_kl, null);
        views.add(v1);
        views.add(v2);

        // 获取EditText
        editTextPt = (EditText) v1.findViewById(R.id.edit_money);
        editTextGre = (EditText) v1.findViewById(R.id.edit_blessing);
        editTextKl = (EditText) v2.findViewById(R.id.edit_money);
        editTextPwd = (EditText) v2.findViewById(R.id.edit_password);

        TextView jineTv, tipTv, sumjineTv, yuan1, yuan2;
        jineTv = (TextView) v1.findViewById(R.id.JinETv);
        tipTv = (TextView) v2.findViewById(R.id.textviewtishi);
        sumjineTv = (TextView) v2.findViewById(R.id.sumMoneyTv);
        yuan1 = (TextView) v1.findViewById(R.id.yuanTv);
        yuan2 = (TextView) v2.findViewById(R.id.yuanTv);
        jineTv.setText(InternationalizationHelper.getString("AMOUNT_OF_MONEY"));
        tipTv.setText(InternationalizationHelper.getString("SMALL_PARTNERS"));
        sumjineTv.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        yuan1.setText(InternationalizationHelper.getString("YUAN"));
        yuan2.setText(InternationalizationHelper.getString("YUAN"));

        editTextPt.setHint(InternationalizationHelper.getString("JX_InputGiftCount"));
        editTextGre.setHint(InternationalizationHelper.getString("JX_GiftText"));

        editTextKl.setHint(InternationalizationHelper.getString("JX_InputGiftCount"));
        editTextPwd.setHint(InternationalizationHelper.getString("JX_WantOpenGift"));

        TextView koulinTv;
        koulinTv = (TextView) v2.findViewById(R.id.setKouLinTv);
        koulinTv.setText(InternationalizationHelper.getString("JX_Message"));

        Button b1 = (Button) v1.findViewById(R.id.btn_sendRed);
        b1.setText(InternationalizationHelper.getString("GIVE_MONEY"));
        b1.setOnClickListener(this);
        Button b2 = (Button) v2.findViewById(R.id.btn_sendRed);
        b2.setText(InternationalizationHelper.getString("GIVE_MONEY"));
        b2.setOnClickListener(this);

        InputChangeListener inputChangeListenerPt = new InputChangeListener(editTextPt);
        InputChangeListener inputChangeListenerKl = new InputChangeListener(editTextKl);

        editTextPt.addTextChangedListener(inputChangeListenerPt);
        editTextKl.addTextChangedListener(inputChangeListenerKl);

        //设置值允许输入数字和小数点
        editTextPt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editTextKl.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        viewPager.setAdapter(new PagerAdapter());
        inflater = LayoutInflater.from(this);
        smartTabLayout.setViewPager(viewPager);

        /**
         * 为了实现点击Tab栏切换的时候不出现动画
         * 为每个Tab重新设置点击事件
         */
        for (int i = 0; i < mTitleList.size(); i++) {
            View view = smartTabLayout.getTabAt(i);
            view.setTag(i + "");
            view.setOnClickListener(this);
        }
        toUserId = getIntent().getStringExtra("toUserId");
    }

    String money = null, words = null;

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_sendRed) {
            SendRed();
        } else {
            // 根据Tab按钮传递的Tag来判断是那个页面，设置到相应的界面并且去掉动画
            int index = Integer.parseInt(v.getTag().toString());
            viewPager.setCurrentItem(index, false);
        }
    }

    private void SendRed() {
        final Bundle bundle = new Bundle();
        final Intent data = new Intent(this, ChatActivity.class);
        //根据Tab的Item来判断当前发送的是那种红包
        final int item = viewPager.getCurrentItem();
        //获取金额和文字信息(口令或者祝福语)
        if (item == 0) {
            money = editTextPt.getText().toString();
            words = editTextGre.getText().toString();
            if (StringUtils.isNullOrEmpty(words)) {
                words = editTextGre.getHint().toString();
            }
        } else if (item == 1) {
            money = editTextKl.getText().toString();
            words = editTextPwd.getText().toString();
            if (StringUtils.isNullOrEmpty(words)) {
                words = editTextPwd.getHint().toString();
                words = words.substring(1, words.length());
            }
        }
        if (StringUtils.isNullOrEmpty(money)) {
            ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_InputGiftCount"));
        } else if (Double.parseDouble(money) > 500 || Double.parseDouble(money) <= 0) {
            ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXRechargeVC_MoneyCount"));
        } else if (Double.parseDouble(money) > coreManager.getSelf().getBalance()) {
            ToastUtil.showToast(mContext, InternationalizationHelper.getString("JX_NotEnough"));
        } else {
            dialog = new PayPasswordVerifyDialog(this);
            dialog.setAction(getString(R.string.chat_redpacket));
            dialog.setMoney(money);
            final String finalMoney = money;
            final String finalWords = words;
            final String finalwords = words;
            String finalMoney1 = money;
            dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
                @Override
                public void onInputFinish(final String password) {
                    // 回传信息
                    String word = item == 0 ? "greetings" : "password", finalwords;
                    String type = item == 0 ? "1" : "3";
                    bundle.putString("money", finalMoney);
                    bundle.putString(item == 0 ? "greetings" : "password", finalWords);
                    bundle.putString("type", item == 0 ? "1" : "3"); // 类型
                    bundle.putString("count", "1"); // 因为是单聊，所以个数必须是一
                    bundle.putString("payPassword", password);

                    Map<String, String> params = new HashMap();
                    params.put("access_token", coreManager.getSelfStatus().accessToken);
                    params.put("type", type);
                    params.put("moneyStr", finalMoney1);
                    params.put("count", "1");
                    params.put("greetings", word);
                    params.put("toUserId", toUserId);

                    HttpUtils.get().url(coreManager.getConfig().REDPACKET_SEND)
                            .params(params)
                            .addSecret(password, finalMoney1)
                            .build()
                            .execute(new BaseCallback<RedPacket>(RedPacket.class) {
                                @Override
                                public void onResponse(ObjectResult<RedPacket> result) {
                                    if (result.getResultCode() != 1) {
                                        ToastUtil.showToast(mContext, result.getResultMsg());
                                        SendRed();
                                    } else {
                                        data.putExtras(bundle);
                                        setResult(item == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PT : ChatActivity.REQUEST_CODE_SEND_RED_KL, data);
                                        finish();
                                    }
                                }

                                @Override
                                public void onError(Call call, Exception e) {
                                }
                            });

                }
            });
            dialog.show();
        }
    }

    private class PagerAdapter extends android.support.v4.view.PagerAdapter {

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ((ViewGroup) container).addView(views.get(position));
            return views.get(position);
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            return mTitleList.get(position);
        }
    }
}

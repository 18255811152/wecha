package com.sk.weichat.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.message.ChatActivity;
import com.sk.weichat.ui.message.MucChatActivity;
import com.sk.weichat.ui.smarttab.SmartTabLayout;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.InputChangeListener;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ToastUtil;

import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 魏正旺 on 2016/9/8.
 */
public class MucSendRedPacketActivity extends BaseActivity implements View.OnClickListener {
    private SmartTabLayout smartTabLayout;
    private ViewPager viewPager;
    private List<View> views;
    private List<String> mTitleList;
    LayoutInflater inflater;

    private EditText edit_count_pt;
    private EditText edit_money_pt;
    private EditText edit_words_pt;

    private EditText edit_count_psq;
    private EditText edit_money_psq;
    private EditText edit_words_psq;

    private EditText edit_count_kl;
    private EditText edit_money_kl;
    private EditText edit_words_kl;

    private TextView hbgs, ge, zje, yuan, xhb;
    private Button sq;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_muc_redpacket);
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

        smartTabLayout = (SmartTabLayout) findViewById(R.id.muc_smarttablayout_redpacket);
        viewPager = (ViewPager) findViewById(R.id.muc_viewpagert_redpacket);
        views = new ArrayList<View>();
        mTitleList = new ArrayList<String>();
        mTitleList.add(InternationalizationHelper.getString("JX_UsualGift"));
        mTitleList.add(InternationalizationHelper.getString("JX_LuckGift"));
        mTitleList.add(InternationalizationHelper.getString("JX_MesGift"));

        views.add(inflater.inflate(R.layout.muc_redpacket_pager_pt, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_sq, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_kl, null));

        View temp_view = views.get(0);
        edit_count_pt = (EditText) temp_view.findViewById(R.id.edit_redcount);
        edit_money_pt = (EditText) temp_view.findViewById(R.id.edit_money);
        edit_words_pt = (EditText) temp_view.findViewById(R.id.edit_blessing);
        hbgs = (TextView) temp_view.findViewById(R.id.hbgs);
        ge = (TextView) temp_view.findViewById(R.id.ge);
        zje = (TextView) temp_view.findViewById(R.id.zje);
        yuan = (TextView) temp_view.findViewById(R.id.yuan);
        xhb = (TextView) temp_view.findViewById(R.id.textviewtishi);
        sq = (Button) temp_view.findViewById(R.id.btn_sendRed);
        hbgs.setText(InternationalizationHelper.getString("NUMBER_OF_ENVELOPES"));
        ge.setText(InternationalizationHelper.getString("INDIVIDUAL"));
        zje.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        edit_money_pt.setHint(InternationalizationHelper.getString("INPUT_AMOUNT"));
        yuan.setText(InternationalizationHelper.getString("YUAN"));
        xhb.setText(InternationalizationHelper.getString("SAME_AMOUNT"));
        edit_words_pt.setHint(InternationalizationHelper.getString("JX_GiftText"));
        sq.setText(InternationalizationHelper.getString("GIVE_MONEY"));
        sq.setOnClickListener(this);

        temp_view = views.get(1);
        edit_count_psq = (EditText) temp_view.findViewById(R.id.edit_redcount);
        edit_money_psq = (EditText) temp_view.findViewById(R.id.edit_money);
        edit_words_psq = (EditText) temp_view.findViewById(R.id.edit_blessing);
        hbgs = (TextView) temp_view.findViewById(R.id.hbgs);
        ge = (TextView) temp_view.findViewById(R.id.ge);
        zje = (TextView) temp_view.findViewById(R.id.zje);
        yuan = (TextView) temp_view.findViewById(R.id.yuan);
        xhb = (TextView) temp_view.findViewById(R.id.textviewtishi);
        sq = (Button) temp_view.findViewById(R.id.btn_sendRed);
        hbgs.setText(InternationalizationHelper.getString("NUMBER_OF_ENVELOPES"));
        ge.setText(InternationalizationHelper.getString("INDIVIDUAL"));
        zje.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        edit_money_psq.setHint(InternationalizationHelper.getString("INPUT_AMOUNT"));
        yuan.setText(InternationalizationHelper.getString("YUAN"));
        xhb.setText(InternationalizationHelper.getString("RONDOM_AMOUNT"));
        edit_words_psq.setHint(InternationalizationHelper.getString("JX_GiftText"));
        sq.setText(InternationalizationHelper.getString("GIVE_MONEY"));
        sq.setOnClickListener(this);

        temp_view = views.get(2);
        edit_count_kl = (EditText) temp_view.findViewById(R.id.edit_redcount);
        edit_money_kl = (EditText) temp_view.findViewById(R.id.edit_money);
        edit_words_kl = (EditText) temp_view.findViewById(R.id.edit_password);
        EditText edit_compatible = (EditText) temp_view.findViewById(R.id.edit_compatible);
        edit_compatible.requestFocus();

        hbgs = (TextView) temp_view.findViewById(R.id.hbgs);
        ge = (TextView) temp_view.findViewById(R.id.ge);
        zje = (TextView) temp_view.findViewById(R.id.zje);
        yuan = (TextView) temp_view.findViewById(R.id.yuan);
        xhb = (TextView) temp_view.findViewById(R.id.textviewtishi);
        sq = (Button) temp_view.findViewById(R.id.btn_sendRed);
        TextView kl = (TextView) temp_view.findViewById(R.id.kl);
        kl.setText(InternationalizationHelper.getString("JX_Message"));
        hbgs.setText(InternationalizationHelper.getString("NUMBER_OF_ENVELOPES"));
        ge.setText(InternationalizationHelper.getString("INDIVIDUAL"));
        zje.setText(InternationalizationHelper.getString("TOTAL_AMOUNT"));
        edit_money_kl.setHint(InternationalizationHelper.getString("INPUT_AMOUNT"));
        yuan.setText(InternationalizationHelper.getString("YUAN"));
        xhb.setText(InternationalizationHelper.getString("REPLY_GRAB"));
        edit_words_kl.setHint(InternationalizationHelper.getString("BIG_ENVELOPE"));
        sq.setText(InternationalizationHelper.getString("GIVE_MONEY"));
        sq.setOnClickListener(this);

        InputChangeListener inputChangeListenerPt = new InputChangeListener(edit_money_pt);
        InputChangeListener inputChangeListenerPsq = new InputChangeListener(edit_money_psq);
        InputChangeListener inputChangeListenerKl = new InputChangeListener(edit_money_kl);

        // 添加输入监听
        edit_money_pt.addTextChangedListener(inputChangeListenerPt);
        edit_money_psq.addTextChangedListener(inputChangeListenerPsq);
        edit_money_kl.addTextChangedListener(inputChangeListenerKl);
        // 只允许输入小数点和数字
        edit_money_pt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_money_psq.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_money_kl.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        viewPager.setAdapter(new PagerAdapter());
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
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_sendRed) {
            final int item = viewPager.getCurrentItem();
            final Bundle bundle = new Bundle();
            final Intent intent = new Intent(this, MucChatActivity.class);
            String money = null, words = null, count = null;
            int resultCode = 0;
            switch (item) {
                case 0: {
                    money = edit_money_pt.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_pt.getText().toString()) ?
                            edit_words_pt.getHint().toString() : edit_words_pt.getText().toString();
                    count = edit_count_pt.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_PT;
                }
                break;

                case 1: {
                    money = edit_money_psq.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_psq.getText().toString()) ?
                            edit_words_psq.getHint().toString() : edit_words_psq.getText().toString();
                    count = edit_count_psq.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_PSQ;
                }
                break;

                case 2: {
                    money = edit_money_kl.getText().toString();
                    words = StringUtils.isNullOrEmpty(edit_words_kl.getText().toString()) ?
                            edit_words_kl.getHint().toString() : edit_words_kl.getText().toString();
                    count = edit_count_kl.getText().toString();
                    resultCode = ChatActivity.REQUEST_CODE_SEND_RED_KL;
                }
                break;
            }

            // 当金额过小，红包个数过多的情况下会出现不够分的情况
            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > 100) {
                Toast.makeText(this, R.string.tip_red_packet_too_much, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(money) &&
                    !TextUtils.isEmpty(count) &&
                    Double.parseDouble(money) / Integer.parseInt(count) < 0.01) {
                Toast.makeText(this, R.string.tip_money_too_less, Toast.LENGTH_SHORT).show();
                return;
            }

            if (eqData(money, count, words)) {
                PayPasswordVerifyDialog dialog = new PayPasswordVerifyDialog(this);
                dialog.setAction(getString(R.string.chat_redpacket));
                dialog.setMoney(money);
                final String finalMoney = money;
                final String finalWords = words;
                final String finalCount = count;
                dialog.setOnInputFinishListener(new PayPasswordVerifyDialog.OnInputFinishListener() {
                    @Override
                    public void onInputFinish(final String password) {
                        // 回传信息
                        bundle.putString("money", finalMoney);
                        bundle.putString("count", finalCount);
                        bundle.putString("words", finalWords);
                        bundle.putString("type", (item + 1) + "");
                        bundle.putString("payPassword", password);
                        intent.putExtras(bundle);
                        setResult(item == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PT : ChatActivity.REQUEST_CODE_SEND_RED_KL, intent);
                        finish();
                    }
                });
                dialog.show();
            }
        } else {
            int index = Integer.parseInt(v.getTag().toString());
            viewPager.setCurrentItem(index, false);
        }
    }

    private boolean eqData(String money, String count, String words) {
        if (StringUtils.isNullOrEmpty(money)) {
            ToastUtil.showToast(mContext, getString(R.string.need_input_money));
            return false;
        } else if (Double.parseDouble(money) > 500 || Double.parseDouble(money) <= 0) {
            ToastUtil.showToast(mContext, getString(R.string.red_packet_range));
            return false;
        } else if (Double.parseDouble(money) > coreManager.getSelf().getBalance()) {
            ToastUtil.showToast(mContext, getString(R.string.balance_not_enough));
            return false;
        } else if (StringUtils.isNullOrEmpty(count)) {
            ToastUtil.showToast(mContext, getString(R.string.need_red_packet_count));
            return false;
        } else if (StringUtils.isNullOrEmpty(words)) {
            return false;
        }
        return true;
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
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView(views.get(position));
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

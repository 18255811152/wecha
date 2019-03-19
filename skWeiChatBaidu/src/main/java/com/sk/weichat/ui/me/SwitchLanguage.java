package com.sk.weichat.ui.me;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.ActivityStack;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.CommonAdapter;
import com.sk.weichat.util.CommonViewHolder;
import com.sk.weichat.util.LocaleHelper;
import com.sk.weichat.view.TipDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zq on 2017/8/26 0026.
 * <p>
 * 切换语言
 */
public class SwitchLanguage extends BaseActivity {
    private ListView mListView;
    private LanguageAdapter languageAdapter;
    private List<Language> languages;
    private String currentLanguage;
    private TipDialog tipDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_switch_language);
        initView();
    }

    protected void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_LanguageSwitching"));

        // 当前语言
        currentLanguage = LocaleHelper.getLanguage(this);
        Log.e("zq", "当前语言:" + currentLanguage);
        // 初始化语言数据
        languages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Language l = new Language();
            if (i == 0) {
                l.setFullName("简体中文");
                l.setAbbreviation("zh");
            } else if (i == 1) {
                l.setFullName("繁體中文");
                l.setAbbreviation("TW");
            } else if (i == 2) {
                l.setFullName("English");
                l.setAbbreviation("en");
            }
            languages.add(l);
        }

        initUI();
    }

    void initUI() {
        mListView = (ListView) findViewById(R.id.lg_lv);
        languageAdapter = new LanguageAdapter(this, languages);
        mListView.setAdapter(languageAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 切换选中语言
                switchLanguage(languages.get(position).getAbbreviation());
                currentLanguage = LocaleHelper.getLanguage(SwitchLanguage.this);
                languageAdapter.notifyDataSetInvalidated();
            }
        });
    }

    private void switchLanguage(String language) {
        // 切换语言
        LocaleHelper.setLocale(this, language);
        // 设置语言
        LocaleHelper.onAttach(this, "zh");
        tipDialog = new TipDialog(this);
        tipDialog.setmConfirmOnClickListener(getString(R.string.tip_change_language_success), new TipDialog.ConfirmOnClickListener() {
            @Override
            public void confirm() {
                ActivityStack.getInstance().exit();
                MyApplication.getInstance().destory();
            }
        });
        tipDialog.show();
    }


    class LanguageAdapter extends CommonAdapter<Language> {

        LanguageAdapter(Context context, List<Language> data) {
            super(context, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.item_switch_language, position);
            TextView language = viewHolder.getView(R.id.language);
            language.setText(data.get(position).getFullName());
            ImageView check = viewHolder.getView(R.id.check);
            if (data.get(position).getAbbreviation().equals(currentLanguage)) {
                check.setVisibility(View.VISIBLE);
            } else {
                check.setVisibility(View.GONE);
            }
            return viewHolder.getConvertView();
        }
    }

    class Language {
        String FullName;    // 全称
        String Abbreviation;// 简称

        public String getFullName() {
            return FullName;
        }

        public void setFullName(String fullName) {
            FullName = fullName;
        }

        public String getAbbreviation() {
            return Abbreviation;
        }

        public void setAbbreviation(String abbreviation) {
            Abbreviation = abbreviation;
        }
    }
}

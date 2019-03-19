package com.sk.weichat.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.groupchat.RoomFragment;
import com.sk.weichat.ui.groupchat.SelectContactsActivity;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends BaseActivity {
    private TabLayout tabLayout;
    private ViewPager mViewPager;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, RoomActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        initActionBar();
        initView();
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
        mTvTitle.setText(InternationalizationHelper.getString("GROUP"));
        ImageView mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        if (coreManager.getLimit().cannotCreateGroup()) {
            mIvTitleRight.setVisibility(View.GONE);
        }
        mIvTitleRight.setImageResource(R.drawable.ic_app_add);
        mIvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!UiUtils.isNormalClick()){
                    return;
                }
                startActivity(new Intent(RoomActivity.this, SelectContactsActivity.class));
            }
        });
    }

    private void initView() {
        mViewPager = (ViewPager) findViewById(R.id.tab1_vp);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new RoomFragment());
        // 取消全部群组，
        // fragments.add(new AllRoomFragment());
        mViewPager.setAdapter(new MyTabAdapter(getSupportFragmentManager(), fragments));

        tabLayout = (TabLayout) findViewById(R.id.tab1_layout);
        // 取消全部群组，只剩一个，不要标签栏，
        tabLayout.setVisibility(View.GONE);
        tabLayout.setTabTextColors(getResources().getColor(R.color.text_black), SkinUtils.getSkin(this).getAccentColor());
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }

    class MyTabAdapter extends FragmentPagerAdapter {
        List<String> listTitle = new ArrayList<>();
        private List<Fragment> mFragments;

        MyTabAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;

            listTitle.add(InternationalizationHelper.getString("MY_GROUP"));
            listTitle.add(InternationalizationHelper.getString("ALL_GROUP"));
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            if (mFragments != null) {
                return mFragments.size();
            }
            return 0;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (listTitle != null) {
                return listTitle.get(position);
            }
            return "";
        }
    }
}

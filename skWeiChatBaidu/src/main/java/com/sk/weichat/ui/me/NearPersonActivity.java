package com.sk.weichat.ui.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.nearby.NearbyGridFragment;
import com.sk.weichat.ui.nearby.NearbyMapFragment;
import com.sk.weichat.ui.nearby.UserSearchActivity;
import com.sk.weichat.util.MyFragmentManager;
import com.sk.weichat.view.TabView;

/**
 * 附近的人
 */

public class NearPersonActivity extends BaseActivity {
    private TabView tabView;
    private MyFragmentManager mMyFragmentManager;
    // 列表
    private NearbyGridFragment mGridFragment;
    // 地图
    private NearbyMapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardcast);

        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXNearVC_NearPer"));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.search_near);
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NearPersonActivity.this, UserSearchActivity.class);
                startActivity(intent);
            }
        });

        tabView = new TabView(this);
        tabView.getAttention_each_tv().setText(InternationalizationHelper.getString("JXNearVC_NearPer"));
        tabView.getAttention_single_tv().setText(InternationalizationHelper.getString("MAP"));
        ((LinearLayout) findViewById(R.id.ll_content)).addView(tabView.getView(), 0);

        mGridFragment = new NearbyGridFragment();
        mMapFragment = new NearbyMapFragment();
        mMyFragmentManager = new MyFragmentManager(this, R.id.fl_fragments);
        mMyFragmentManager.add(mGridFragment, mMapFragment);
        tabView.setOnTabSelectedLisenter(new TabView.OnTabSelectedLisenter() {
            @Override
            public void onSelected(int index) {
                mMyFragmentManager.show(index);
            }
        });
        mMyFragmentManager.show(0);
    }
}

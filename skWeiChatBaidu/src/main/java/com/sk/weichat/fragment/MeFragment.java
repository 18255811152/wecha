package com.sk.weichat.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.course.LocalCourseActivity;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.circle.BusinessCircleActivity;
import com.sk.weichat.ui.contacts.RoomActivity;
import com.sk.weichat.ui.me.BasicInfoEditActivity;
import com.sk.weichat.ui.me.MyCollection;
import com.sk.weichat.ui.me.SettingActivity;
import com.sk.weichat.ui.me.redpacket.WxPayBlance;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;

public class MeFragment extends EasyFragment implements View.OnClickListener {
    private TextView mTvTitle;

    private ImageView mAvatarImg;
    private TextView mNickNameTv;
    private TextView mPhoneNumTv;
    private TextView skyTv, setTv;

    public MeFragment() {
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_me;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void initView() {
        skyTv = (TextView) findViewById(R.id.MySky);
        setTv = (TextView) findViewById(R.id.SettingTv);
        skyTv.setText(getString(R.string.my_moments));
        TextView mCollectionTv = (TextView) findViewById(R.id.my_collection_tv);
        mCollectionTv.setText(InternationalizationHelper.getString("JX_MyCollection"));
        TextView mCourseTv = (TextView) findViewById(R.id.my_course_tv);
        mCourseTv.setText(InternationalizationHelper.getString("JX_MyLecture"));
        setTv.setText(InternationalizationHelper.getString("JXSettingVC_Set"));
        findViewById(R.id.info_rl).setOnClickListener(this);

        findViewById(R.id.meeting_rl).setOnClickListener(this);
        findViewById(R.id.live_rl).setOnClickListener(this);
        findViewById(R.id.douyin_rl).setOnClickListener(this);
        if (coreManager.getConfig().newUi) { // 切换新旧两种ui对应我的页面是否显示视频会议和直播，
            findViewById(R.id.meeting_rl).setVisibility(View.GONE);
            findViewById(R.id.live_rl).setVisibility(View.GONE);
            findViewById(R.id.douyin_rl).setVisibility(View.GONE);
        }

        findViewById(R.id.my_space_rl).setOnClickListener(this);
        findViewById(R.id.my_collection_rl).setOnClickListener(this);
        findViewById(R.id.local_course_rl).setOnClickListener(this);
        findViewById(R.id.my_monry).setOnClickListener(this);
        findViewById(R.id.setting_rl).setOnClickListener(this);

        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
        mPhoneNumTv = (TextView) findViewById(R.id.phone_number_tv);
        String loginUserId = coreManager.getSelf().getUserId();
        AvatarHelper.getInstance().displayAvatar(loginUserId, mAvatarImg, false);
        mNickNameTv.setText(coreManager.getSelf().getNickName());

        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SingleImagePreviewActivity.class);
                intent.putExtra(AppConstant.EXTRA_IMAGE_URI, coreManager.getSelf().getUserId());
                startActivity(intent);
            }
        });

        findViewById(R.id.llFriend).setOnClickListener(v -> {
            MainActivity activity = (MainActivity) requireActivity();
            activity.changeTab(R.id.rb_tab_2);
        });

        findViewById(R.id.llGroup).setOnClickListener(v -> RoomActivity.start(requireContext()));

        initTitleBackground();
    }

    private void initTitleBackground() {
        SkinUtils.Skin skin = SkinUtils.getSkin(requireContext());
        int primaryColor = skin.getPrimaryColor();
        int brighterColor = skin.getBrighterPrimaryColor();
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{primaryColor, brighterColor});
        gd.setCornerRadius(0f);
        findViewById(R.id.tool_bar).setBackground(gd);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (!UiUtils.isNormalClick()){
            return;
        }
        switch (id) {
            case R.id.info_rl:
                // 我的资料
                startActivityForResult(new Intent(getActivity(), BasicInfoEditActivity.class), 1);
                break;

            case R.id.my_space_rl:
                // 我的动态
                Intent intent = new Intent(getActivity(), BusinessCircleActivity.class);
                intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                startActivity(intent);
                break;
            case R.id.my_collection_rl:
                // 我的收藏
                startActivity(new Intent(getActivity(), MyCollection.class));
                break;
            case R.id.local_course_rl:
                // 我的课件
                startActivity(new Intent(getActivity(), LocalCourseActivity.class));
                break;
            case R.id.my_monry:
                // 我的钱包
                startActivity(new Intent(getActivity(), WxPayBlance.class));
                break;
            case R.id.setting_rl:
                // 设置
                startActivity(new Intent(getActivity(), SettingActivity.class));
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 || resultCode == Activity.RESULT_OK) {// 个人资料更新了
            updateUI();
        }
    }

    /**
     * 用户的信息更改的时候，ui更新
     */
    private void updateUI() {
        if (mAvatarImg != null) {
            AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), mAvatarImg, true);
        }
        if (mNickNameTv != null) {
            mNickNameTv.setText(coreManager.getSelf().getNickName());
        }

        if (mPhoneNumTv != null) {
            String phoneNumber = coreManager.getSelf().getTelephone();
            int mobilePrefix = PreferenceUtils.getInt(getContext(), Constants.AREA_CODE_KEY, -1);
            String sPrefix = String.valueOf(mobilePrefix);
            // 删除开头的区号，
            if (phoneNumber.startsWith(sPrefix)) {
                phoneNumber = phoneNumber.substring(sPrefix.length());
            }
            mPhoneNumTv.setText(phoneNumber);
        }


        AsyncUtils.doAsync(this, t -> {
            Reporter.post("获取好友数量失败，", t);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ToastUtil.showToast(requireContext(), R.string.tip_me_query_friend_failed);
                });
            }
        }, ctx -> {
            long count = FriendDao.getInstance().getFriendsCount(coreManager.getSelf().getUserId());
            ctx.uiThread(ref -> {
                TextView tvColleague = findViewById(R.id.tvFriend);
                tvColleague.setText(String.valueOf(count));
            });
        });

        AsyncUtils.doAsync(this, t -> {
            Reporter.post("获取群组数量失败，", t);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ToastUtil.showToast(requireContext(), R.string.tip_me_query_friend_failed);
                });
            }
        }, ctx -> {
            long count = FriendDao.getInstance().getGroupsCount(coreManager.getSelf().getUserId());
            ctx.uiThread(ref -> {
                TextView tvGroup = findViewById(R.id.tvGroup);
                tvGroup.setText(String.valueOf(count));
            });
        });

    }
}

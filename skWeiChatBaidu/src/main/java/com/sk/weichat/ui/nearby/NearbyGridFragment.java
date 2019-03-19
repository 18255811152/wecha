package com.sk.weichat.ui.nearby;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.User;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseGridFragment;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.CircleImageView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

/**
 * 附近的人-列表模式
 */
public class NearbyGridFragment extends BaseGridFragment<NearbyGridFragment.NearbyGridHolder> {
    double latitude;
    double longitude;
    private List<User> mUsers = new ArrayList<>();
    private boolean isPullDwonToRefersh;

    @Override
    public void initDatas(int pager) {
        if (pager == 0) {
            isPullDwonToRefersh = true;
        } else {
            isPullDwonToRefersh = false;
        }

        latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();

        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(pager));
        params.put("pageSize", "20");
        params.put("latitude", String.valueOf(latitude));
        params.put("longitude", String.valueOf(longitude));
        requestData(params);
    }

    private void requestData(HashMap<String, String> params) {
        HttpUtils.get().url(coreManager.getConfig().NEARBY_USER)
                .params(params)
                .build()
                .execute(new ListCallback<User>(User.class) {
                    @Override
                    public void onResponse(ArrayResult<User> result) {

                        if (isPullDwonToRefersh) {
                            mUsers.clear();
                        }

                        List<User> data = result.getData();
                        if (data != null && data.size() > 0) {
                            mUsers.addAll(data);
                        }

                        if (mUsers.size() > 0) {
                            update(mUsers);
                        }

                        //                        AsyncUtils.doAsync(this, new AsyncUtils.Function<AsyncUtils.AsyncContext<ListCallback<User>>>() {
                        //                            @Override
                        //                            public void apply(AsyncUtils.AsyncContext<ListCallback<User>> listCallbackAsyncContext) throws Exception {
                        //                                List<User> data = result.getData();
                        //                                if (data != null && data.size() > 0) {
                        //                                    mUsers.addAll(data);
                        //                                }
                        //
                        //
                        //                                if (mUsers.size() > 0){
                        //                                    update(mUsers);
                        //                                }
                        //
                        ////                                AsyncUtils.runOnUiThread(this, new AsyncUtils.Function<AsyncUtils.Function<AsyncUtils.AsyncContext<ListCallback<User>>>>() {
                        ////                                    @Override
                        ////                                    public void apply(AsyncUtils.Function<AsyncUtils.AsyncContext<ListCallback<User>>> asyncContextFunction) throws Exception {
                        ////
                        ////                                    }
                        ////                                });
                        //                            }
                        //                        });
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    @Override
    public NearbyGridHolder initHolder(ViewGroup parent) {
        View v = mInflater.inflate(R.layout.item_nearby_grid, parent, false);
        return new NearbyGridHolder(v);
    }

    @Override
    public void fillData(NearbyGridHolder holder, int position) {
        if (mUsers != null && mUsers.size() > 0) {
            User data = mUsers.get(position);
            AvatarHelper.getInstance().displayAvatar(data.getUserId(), holder.ivHead, true);
            AvatarHelper.getInstance().displayAvatar(data.getUserId(), holder.ivBgImg, false);
            String distance = DisplayUtil.getDistance(latitude, longitude, data);
            holder.tvDistance.setText(distance);
            holder.tvName.setText(data.getNickName());
            holder.tvTime.setText(TimeUtils.skNearbyTimeString(data.getCreateTime()));
        }
    }

    public void onItemClick(int position) {
        String userId = mUsers.get(position).getUserId();
        Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    class NearbyGridHolder extends RecyclerView.ViewHolder {
        LinearLayout rootView;
        ImageView ivBgImg;
        TextView tvDistance;
        TextView tvName;
        TextView tvTime;
        CircleImageView ivHead;

        NearbyGridHolder(View itemView) {
            super(itemView);
            rootView = (LinearLayout) itemView.findViewById(R.id.ll_nearby_grid_root);
            ivBgImg = (ImageView) itemView.findViewById(R.id.iv_nearby_img);
            tvDistance = (TextView) itemView.findViewById(R.id.tv_nearby_distance);
            tvName = (TextView) itemView.findViewById(R.id.tv_nearby_name);
            tvTime = (TextView) itemView.findViewById(R.id.tv_nearby_time);
            ivHead = (CircleImageView) itemView.findViewById(R.id.iv_nearby_head);
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(getLayoutPosition());
                }
            });
        }
    }
}

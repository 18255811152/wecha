package com.sk.weichat.ui.nearby;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.bumptech.glide.Glide;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.MarkerPagerAdapter;
import com.sk.weichat.bean.User;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.CircleImageView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

/**
 * 附近的人-地图模式
 */
public class NearbyMapFragment extends EasyFragment implements View.OnClickListener {
    private static final String TAG = "map";
    TextureMapView mMapView;
    BaiduMap mBaiduMap;
    ImageView ivLocation;
    ViewPager mViewPager;
    NearbyCardAdapter mAdapter;
    Map<String, User> hashMap = new HashMap<>();
    double latitude;
    double longitude;
    LatLng cuttLatLng;

    // 解决头像闪与OOM的问题 addMaker是取这里的数据，而不是hashMap内的数据
    Map<String, User> mNewMakerMap = new HashMap<>();

    private ImageView daohang;
    private List<User> mCurrentData = new ArrayList<>();
    /**
     * MarkerCLickListener
     */
    BaiduMap.OnMarkerClickListener markerListener = new BaiduMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            String userId = marker.getExtraInfo().getString("userId");
            int index = 0;
            if (!TextUtils.isEmpty(userId)) {
                for (int i = 0; i < mCurrentData.size(); i++) {
                    if (mCurrentData.get(i).getUserId().equals(userId)) {
                        index = i;
                    }
                }
            }
            mViewPager.setCurrentItem(index);
            showViewPager();
            return false;
        }
    };
    /**
     * 地图移动监听
     */
    BaiduMap.OnMapStatusChangeListener moveListener = new BaiduMap.OnMapStatusChangeListener() {
        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus, int i) {

        }

        @Override
        public void onMapStatusChange(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChangeFinish(MapStatus mapStatus) {
            // 在这里地图改变完成后就获取经纬度,
            // 每次移动地图后隐藏信息ViewPager
            hideViewPager();
            LatLng latLng = mapStatus.target;
            double distance = DistanceUtil.getDistance(latLng, cuttLatLng);
            if (distance > 8000) {
                mBaiduMap.clear();
                hashMap.clear();
            }
            cuttLatLng = latLng;
            loadDatas(latLng.latitude, latLng.longitude);
        }
    };

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_nearby_map;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            ivLocation = (ImageView) findViewById(R.id.iv_location);
            mMapView = (TextureMapView) findViewById(R.id.mTexturemap);
            mViewPager = (ViewPager) findViewById(R.id.vp_nearby);
            daohang = (ImageView) findViewById(R.id.daohang);
            daohang.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 弹出底部抽屉
                    final Dialog bottomDialog = new Dialog(getActivity(), R.style.BottomDialog);
                    View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.map_dialog, null);
                    bottomDialog.setContentView(contentView);
                    ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
                    layoutParams.width = getResources().getDisplayMetrics().widthPixels;
                    contentView.setLayoutParams(layoutParams);
                    bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
                    bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
                    bottomDialog.show();
                    bottomDialog.findViewById(R.id.bdmap).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (AppUtils.isAppInstalled(getActivity(), "com.baidu.BaiduMap")) {
                                try {
                                    Intent intent01 = Intent.getIntent("intent://map/direction?" +
                                            //"origin=latlng:"+"34.264642646862,108.95108518068&" +   //起点  此处不传值默认选择当前位置
                                            "destination=latlng:" + latitude + "," + longitude + "|name:我的目的地" +        // 终点
                                            "&mode=driving&" +        // 导航路线方式
                                            "region=北京" +           //
                                            "&src=慧医#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end");
                                    startActivity(intent01); //启动调用
                                } catch (URISyntaxException e) {
                                }
                            } else {// 未安装
                                // market为路径，id为包名
                                // 显示手机上所有的market商店
                                Toast.makeText(getActivity(), R.string.tip_no_baidu_map, Toast.LENGTH_LONG).show();
                                try {// Nokia N1 平板测试 此处崩溃ActivityNotFoundException try catch 处理
                                    Uri uri = Uri.parse("market://details?id=com.baidu.BaiduMap");
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    bottomDialog.findViewById(R.id.gdmap).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (AppUtils.isAppInstalled(getActivity(), "com.autonavi.minimap")) {
                                try {
                                    Intent intent = Intent.getIntent("androidamap://navi?sourceApplication=慧医&poiname=我的目的地&lat=" + latitude + "&lon=" + longitude + "&dev=0");
                                    startActivity(intent);
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Toast.makeText(getActivity(), R.string.tip_no_amap, Toast.LENGTH_LONG).show();
                                try {
                                    Uri uri = Uri.parse("market://details?id=com.autonavi.minimap");
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    bottomDialog.findViewById(R.id.ggmap).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (AppUtils.isAppInstalled(getActivity(), "com.google.android.apps.maps")) {
                                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude + ", + Sydney +Australia");
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                startActivity(mapIntent);
                            } else {
                                Toast.makeText(getActivity(), R.string.tip_no_google_map, Toast.LENGTH_LONG).show();
                                try {
                                    Uri uri = Uri.parse("market://details?id=com.google.android.apps.maps");
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            });
            ivLocation.setOnClickListener(this);
            mBaiduMap = mMapView.getMap();
            UiSettings settings = mBaiduMap.getUiSettings();
            settings.setOverlookingGesturesEnabled(false);
            // 禁用旋转
            settings.setRotateGesturesEnabled(false);
            // 隐藏百度logo
            mMapView.getChildAt(1).setVisibility(View.GONE);
            /** 请求定位权限 */
            requestPermission(ACCESS_COARSE_LOCATION);
            /** 地图移动监听 */
            mBaiduMap.setOnMapStatusChangeListener(moveListener);
            /** marker 点击监听 */
            mBaiduMap.setOnMarkerClickListener(markerListener);
            mAdapter = new NearbyCardAdapter();
            mViewPager.setAdapter(mAdapter);
        }
    }

    private void requestPermission(String accessCoarseLocation) {
        latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        cuttLatLng = new LatLng(latitude, longitude);
        /** 加载数据 */
        loadDatas(latitude, longitude);
        MyLocationConfiguration config =
                new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null);
        mBaiduMap.setMyLocationConfigeration(config);
        mBaiduMap.setMyLocationEnabled(true);
        // 这里偷个懒  就直接用别人的定位数据就好了
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(20.0f)
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(0).latitude(latitude)
                .longitude(longitude).build();
        mBaiduMap.setMyLocationData(locData);
        moveMap(latitude, longitude);
    }

    @Override
    public void onClick(View v) {
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        moveMap(latitude, longitude);
    }

    public void loadDatas(double latitude, double longitude) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("pageIndex", "0");
        params.put("pageSize", "20");
        params.put("latitude", String.valueOf(latitude));
        params.put("longitude", String.valueOf(longitude));
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().NEARBY_USER)
                .params(params)
                .build()
                .execute(new ListCallback<User>(User.class) {
                    @Override
                    public void onResponse(ArrayResult<User> result) {
                        List<User> datas = result.getData();
                        if (datas != null && datas.size() > 0) {
                            update(datas);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    private void update(List<User> datas) {
        AsyncUtils.doAsync(this, new AsyncUtils.Function<AsyncUtils.AsyncContext<NearbyMapFragment>>() {
            @Override
            public void apply(AsyncUtils.AsyncContext<NearbyMapFragment> nearbyMapFragmentAsyncContext) throws Exception {
                mNewMakerMap.clear();// 清空之前的
                for (User user : datas) {
                    if (hashMap.containsKey(user.getUserId())) {
                        // 重复数据
                    } else {
                        // 新数据
                        hashMap.put(user.getUserId(), user);
                        mNewMakerMap.put(user.getUserId(), user);// 添加新数据

                    }
                }
                AsyncUtils.runOnUiThread(this, new AsyncUtils.Function<AsyncUtils.Function<AsyncUtils.AsyncContext<NearbyMapFragment>>>() {
                    @Override
                    public void apply(AsyncUtils.Function<AsyncUtils.AsyncContext<NearbyMapFragment>> asyncContextFunction) throws Exception {
                        mAdapter.setData(hashMap);
                    }
                });
            }
        });
    }

    public void moveMap(double x, double y) {
        Log.e(TAG, "x  " + x + "  y  " + y);
        if (mBaiduMap != null && x > 0.1 && y > 0.1) {
            //设定中心点坐标
            LatLng cenpt = new LatLng(x, y);
            //定义地图状态
            MapStatus mMapStatus = new MapStatus.Builder()
                    // 要移动的点
                    .target(cenpt)
                    // 放大地图到20倍
                    .zoom(18)
                    .build();
            //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
            MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
            //改变地图状态
            mBaiduMap.animateMapStatus(mMapStatusUpdate);
        }
    }

    public void showViewPager() {
        if (mViewPager.getVisibility() == View.GONE) {
            mViewPager.setVisibility(View.VISIBLE);
            ivLocation.setVisibility(View.GONE);
            mMapView.showZoomControls(false);
        }
    }

    public void hideViewPager() {
        if (mViewPager.getVisibility() == View.VISIBLE) {
            mViewPager.setVisibility(View.GONE);
            ivLocation.setVisibility(View.VISIBLE);
            mMapView.showZoomControls(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    public void addMarker(final double lat, final double lng, final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = AvatarHelper.getAvatarUrl(id, true);
                    Bitmap mBitmap = Glide.with(getActivity())
                            .load(url)
                            .asBitmap()
                            .centerCrop()
                            .into(120, 120)
                            .get();
                    MarkerOptions marker = getMarker(lat, lng, mBitmap, id);
                    mBaiduMap.addOverlay(marker);
                } catch (ExecutionException executionException) {// 部分用户为默认头像，URL为空，导致ExecutionException
                    try {
                        Bitmap mBitmap = Glide.with(getActivity())
                                .load(R.drawable.avatar_normal)// 将本地资源转换为bitmap
                                .asBitmap()
                                .centerCrop()
                                .into(120, 120)
                                .get();
                        MarkerOptions marker = getMarker(lat, lng, mBitmap, id);
                        mBaiduMap.addOverlay(marker);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    public MarkerOptions getMarker(double lat, double lng, Bitmap bitmap, String id) {
        LatLng loc = new LatLng(lat, lng);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory
                .fromBitmap(getRoundedCornerBitmap(bitmap));
        Bundle bundle = new Bundle();
        bundle.putString("userId", id);
        MarkerOptions marker = new MarkerOptions().position(loc).icon(bitmapDescriptor)
                .zIndex(9).draggable(true).extraInfo(bundle);
        return marker;
    }

    public Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outBitmap);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPX = bitmap.getWidth() / 2;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPX, roundPX, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return outBitmap;
    }

    class NearbyCardAdapter extends MarkerPagerAdapter {

        private List<User> data = new ArrayList<>();

        @Override
        public View getView(View convertView, final int position) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = View.inflate(getActivity(), R.layout.item_nearby_card, null);
                viewHolder = new ViewHolder();
                viewHolder.layout = (LinearLayout) convertView.findViewById(R.id.layout);
                viewHolder.tvName = (TextView) convertView.findViewById(R.id.job_name_tv);
                viewHolder.ivHead = (CircleImageView) convertView.findViewById(R.id.iv_head);
                viewHolder.tvPhone = (TextView) convertView.findViewById(R.id.job_money_tv);
                viewHolder.tvDist = (TextView) convertView.findViewById(R.id.juli_tv);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final User user = data.get(position);
            viewHolder.tvName.setText(user.getNickName());
            AvatarHelper.getInstance().displayAvatar(user.getUserId(), viewHolder.ivHead, true);
            viewHolder.tvPhone.setText(user.getTelephone());
            String distance = DisplayUtil.getDistance(latitude, longitude, user);
            viewHolder.tvDist.setText(distance);
            viewHolder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userId = user.getUserId();
                    Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
                    startActivity(intent);
                }
            });
            return convertView;
        }

        @Override
        public int myGetCount() {
            return data.size();
        }

        public void setData(Map<String, User> hashMap) {
            data.clear();
            mCurrentData.clear();
            Iterator iterator = hashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                User user = (User) entry.getValue();
                if (user != null && user.getLoc() != null) {
                    data.add(user);
                    // Todo 头像闪动的原因在这里，即界面内已经有该maker了，还在里面不停的add(有时OOM可能也是这里引起的)
                    // addMarker(user.getLoc().getLat(), user.getLoc().getLng(), user.getUserId(), index++);
                }
            }

            mCurrentData = data;

            Iterator mNewIterator = mNewMakerMap.entrySet().iterator();
            while (mNewIterator.hasNext()) {
                Map.Entry entry = (Map.Entry) mNewIterator.next();
                User user = (User) entry.getValue();
                if (user != null && user.getLoc() != null) {
                    // 已添加的maker不会重复添加了，即index已经不起做用了，现记录当前data(mCurrentData)，最后通过对比userId获取其index
                    // addMarker(user.getLoc().getLat(), user.getLoc().getLng(), user.getUserId(), index++);
                    addMarker(user.getLoc().getLat(), user.getLoc().getLng(), user.getUserId());
                }
            }
            notifyDataSetChanged();
        }
    }

    class ViewHolder {
        LinearLayout layout;
        TextView tvName;
        ImageView ivHead;
        TextView tvPhone;
        TextView tvDist;
    }
}

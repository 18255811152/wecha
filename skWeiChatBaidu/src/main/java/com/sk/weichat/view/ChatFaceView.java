package com.sk.weichat.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.collection.Collectiion;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.message.ManagerEmojiActivity;
import com.sk.weichat.util.SmileyParser;
import com.sk.weichat.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;


/**
 * 表情界面
 *
 * @author Administrator
 */
public class ChatFaceView extends RelativeLayout {
    private Context mContext;
    private ViewPager mViewPager;
    private RadioGroup mFaceRadioGroup;// 切换不同组表情的RadioGroup
    private boolean mHasGif;
    // 表情总数据
    private List<Collectiion> mCollection;
    private EmotionClickListener mEmotionClickListener;
    private BroadcastReceiver refreshCollectionListBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getMyCollectionList(true);
        }
    };

    public ChatFaceView(Context context) {
        super(context);
        init(context);
    }

    public ChatFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        init(context);
    }

    public ChatFaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
        init(context);
    }

    private static int dip_To_px(Context context, int dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private void initAttrs(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChatFaceView);// TypedArray是一个数组容器
        mHasGif = a.getBoolean(R.styleable.ChatFaceView_hasGif, true);
        a.recycle();
    }

    @SuppressWarnings("deprecation")
    private void init(Context context) {
        mContext = context;
        mCollection = new ArrayList<>();
        LayoutInflater.from(mContext).inflate(R.layout.chat_face_view, this);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        ((TabLayout) findViewById(R.id.tabDots)).setupWithViewPager(mViewPager, false);
        mFaceRadioGroup = (RadioGroup) findViewById(R.id.face_btn_layout);
        RadioButton rg1 = (RadioButton) findViewById(R.id.default_face);
        RadioButton rg2 = (RadioButton) findViewById(R.id.moya_face_gif);
        rg1.setText(InternationalizationHelper.getString("emojiVC_Emoji"));
        rg2.setText(InternationalizationHelper.getString("emojiVC_Anma"));
        mFaceRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.default_face:
                        // 表情
                        switchViewPager1();
                        break;
                    case R.id.moya_face_gif:
                        // gif
                        switchViewPager2();
                        break;
                    default:
                        // 自定义表情
                        switchViewPager3();
                        break;
                }
            }
        });
        mFaceRadioGroup.check(R.id.default_face);

        if (!mHasGif) {
            mFaceRadioGroup.setVisibility(View.GONE);
        }
        // 获取添加的表情列表
        getMyCollectionList(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().registerReceiver(
                refreshCollectionListBroadcast, new IntentFilter("CollectionRefresh"));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(refreshCollectionListBroadcast);
    }

    public void getMyCollectionList(final boolean isSwitch) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(getContext()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(getContext()).Collection_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Collectiion>(Collectiion.class) {
                    @Override
                    public void onResponse(ArrayResult<Collectiion> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            mCollection = result.getData();
                            Collectiion collection = new Collectiion();
                            collection.setType(7);
                            mCollection.add(0, collection);
                            if (isSwitch) {
                                switchViewPager3();
                            }
                        } else {
                            Toast.makeText(MyApplication.getContext(), result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MyApplication.getContext());
                    }
                });
    }

    /*
    Emotion
     */
    private void switchViewPager1() {
        mViewPager.setAdapter(new EmojiPager1Adapter(
                getContext(),
                SmileyParser.Smilies.getIds(),
                SmileyParser.Smilies.getTexts(),
                ss -> {
                    mEmotionClickListener.onNormalFaceClick(ss);
                }
        ));
    }

    /*
    Gif
     */
    public void switchViewPager2() {
        String[][] strArray = SmileyParser.Gifs.getTexts();
        int[][] pngId = SmileyParser.Gifs.getPngIds();
        mViewPager.setAdapter(new EmojiPager2Adapter(
                getContext(),
                pngId,
                strArray,
                text -> {
                    mEmotionClickListener.onGifFaceClick(text);
                }
        ));
    }

    /*
    Collections
     */
    public void switchViewPager3() {
        if (mCollection == null || mCollection.size() == 0) {
            Toast.makeText(mContext, R.string.tip_emoji_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        mViewPager.setAdapter(new EmojiPager3Adapter(
                getContext(),
                mCollection,
                c -> {
                    if (c.getType() == 7) {
                        Intent intent = new Intent(getContext(), ManagerEmojiActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("list", (Serializable) mCollection);
                        intent.putExtras(bundle);
                        mContext.startActivity(intent);
                    } else {
                        // 发送自定义表情
                        mEmotionClickListener.onCollecionClick(c.getUrl());
                    }
                }
        ));
    }

    public void setEmotionClickListener(EmotionClickListener listener) {
        mEmotionClickListener = listener;
    }

    public interface EmotionClickListener {
        void onNormalFaceClick(SpannableString ss);

        void onGifFaceClick(String resName);

        void onCollecionClick(String collection);
    }

    interface OnEmojiClickListener {
        void onEmojiClick(SpannableString ss);
    }

    interface OnGifClickListener {
        void onGifClick(String text);
    }

    interface OnCollectionClickListener {
        void onCollectionClick(Collectiion c);
    }

    static class EmojiAdapter extends BaseAdapter {
        private final Context ctx;
        private final int[] idList;

        EmojiAdapter(Context ctx, int[] idList) {
            this.ctx = ctx;
            this.idList = idList;
        }

        @Override
        public int getCount() {
            return idList.length;
        }

        @Override
        public Object getItem(int position) {
            return idList[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new ImageView(ctx);
                // 宽高间距计算保留旧代码，
                AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                convertView.setLayoutParams(layoutParams);
                convertView.setPadding(dip_To_px(ctx, 13), dip_To_px(ctx, 7), 0, 0);
            }
            ImageView ivEmoji = (ImageView) convertView;
            int res = idList[position];
            ivEmoji.setImageResource(res);
            return convertView;
        }
    }

    static class GifAdapter extends BaseAdapter {
        private final Context ctx;
        private final int[] idList;

        GifAdapter(Context ctx, int[] idList) {
            this.ctx = ctx;
            this.idList = idList;
        }

        @Override
        public int getCount() {
            return idList.length;
        }

        @Override
        public Object getItem(int position) {
            return idList[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new ImageView(ctx);
                // 宽高间距计算保留旧代码，
                AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT, dip_To_px(ctx, 164 / 2) - 30 + 7);
                convertView.setLayoutParams(layoutParams);
                convertView.setPadding(dip_To_px(ctx, 13), dip_To_px(ctx, 7), 0, 0);
            }
            ImageView ivEmoji = (ImageView) convertView;
            int res = idList[position];
            ivEmoji.setImageResource(res);
            return convertView;
        }
    }

    static class CollectionAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<Collectiion> collectionList;

        CollectionAdapter(Context ctx, List<Collectiion> collectionList) {
            this.ctx = ctx;
            this.collectionList = collectionList;
        }

        @Override
        public int getCount() {
            return collectionList.size();
        }

        @Override
        public Object getItem(int position) {
            return collectionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                ImageView ivEmoji = new ImageView(ctx);
                convertView = ivEmoji;
                ivEmoji.setScaleType(ImageView.ScaleType.FIT_CENTER);
                // 宽高间距计算保留旧代码，
                AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, dip_To_px(ctx, 164 / 2) - 30 + 7);
                convertView.setLayoutParams(layoutParams);
                convertView.setPadding(dip_To_px(ctx, 13), dip_To_px(ctx, 7), 0, 0);
            }
            ImageView ivEmoji = (ImageView) convertView;
            Collectiion c = collectionList.get(position);
            // 保留旧代码，
            if (c.getType() == 7) {
                ivEmoji.setImageResource(R.drawable.ic_collection_set);
            } else {
                String url = c.getUrl();
                if (url.endsWith(".gif")) {
                    Glide.with(ctx)
                            .load(url)
                            .asGif()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(ivEmoji);
                } else {
                    Glide.with(ctx)
                            .load(url)
                            .placeholder(R.drawable.ffb)
                            .error(R.drawable.fez)
                            .dontAnimate()
                            .into(ivEmoji);
                }
            }
            return convertView;
        }
    }

    static class EmojiPager1Adapter extends PagerAdapter {
        // 弱引用缓存表情第一页，用来加速加载，
        private static SoftReference<GridView> softFirstPage = new SoftReference<>(null);
        private int[][] idMatrix;
        // 表情符号所代表的英文字符
        private String[][] strMatrix;
        private OnEmojiClickListener listener;
        private Context ctx;

        EmojiPager1Adapter(Context ctx, int[][] idMatrix, String[][] strMatrix, OnEmojiClickListener listener) {
            this.ctx = ctx;
            this.idMatrix = idMatrix;
            this.strMatrix = strMatrix;
            this.listener = listener;
        }

        @Override
        public int getCount() {
            return idMatrix.length;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int pagePosition) {
            int[] idList = idMatrix[pagePosition];
            String[] strList = strMatrix[pagePosition];
            GridView gridView = null;
            if (0 == pagePosition) {
                gridView = softFirstPage.get();
            }
            if (gridView == null) {
                gridView = (GridView) LayoutInflater.from(ctx).inflate(R.layout.emotion_gridview, container, false);
                gridView.setSelector(R.drawable.chat_face_bg);
                ListAdapter adapter = new EmojiAdapter(ctx, idList);
                gridView.setAdapter(adapter);
                gridView.setPadding(0, 0, dip_To_px(ctx, 12), 0);
                if (0 == pagePosition) {
                    softFirstPage = new SoftReference<>(gridView);
                }
            }
            container.addView(gridView);
            gridView.setOnItemClickListener((parent, view, itemPosition, id) -> {
                if (listener != null) {
                    int res = idList[itemPosition];
                    String text = strList[itemPosition];
                    SpannableString ss = new SpannableString(text);
                    Drawable d = ctx.getResources().getDrawable(res);
                    // 设置表情图片的显示大小
                    d.setBounds(0, 0, (int) (d.getIntrinsicWidth() / 1.95), (int) (d.getIntrinsicHeight() / 1.95));
                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                    ss.setSpan(span, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    listener.onEmojiClick(ss);
                }
            });
            return gridView;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    static class EmojiPager2Adapter extends PagerAdapter {
        private int[][] idMatrix;
        // 表情符号所代表的英文字符
        private String[][] strMatrix;
        private OnGifClickListener listener;
        private Context ctx;

        EmojiPager2Adapter(Context ctx, int[][] idMatrix, String[][] strMatrix, OnGifClickListener listener) {
            this.ctx = ctx;
            this.idMatrix = idMatrix;
            this.strMatrix = strMatrix;
            this.listener = listener;
        }

        @Override
        public int getCount() {
            return idMatrix.length;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int pagePosition) {
            int[] idList = idMatrix[pagePosition];
            String[] strList = strMatrix[pagePosition];
            GridView gridView = (GridView) LayoutInflater.from(ctx).inflate(R.layout.chat_face_gridview, container, false);
            container.addView(gridView);
            gridView.setSelector(R.drawable.chat_face_bg);
            ListAdapter adapter = new GifAdapter(ctx, idList);
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener((parent, view, itemPosition, id) -> {
                if (listener != null) {
                    String text = strList[itemPosition];
                    listener.onGifClick(text);
                }
            });
            gridView.setPadding(0, 0, dip_To_px(ctx, 12), 0);
            return gridView;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    class EmojiPager3Adapter extends PagerAdapter {
        // collections_gridview里的列数乘以两行，
        private final static int size = 10;
        private List<Collectiion> collectionList;
        private OnCollectionClickListener listener;
        private Context ctx;

        EmojiPager3Adapter(Context ctx, List<Collectiion> collectionList, OnCollectionClickListener listener) {
            this.ctx = ctx;
            this.collectionList = collectionList;
            this.listener = listener;
        }

        @Override
        public int getCount() {
            // 编辑按钮已经加在list开头了，
            // 0舍1入，除以每页10个，
            return (collectionList.size() + (size - 1)) / size;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int pagePosition) {
            GridView gridView = (GridView) LayoutInflater.from(ctx).inflate(R.layout.collections_gridview, container, false);
            container.addView(gridView);
            gridView.setSelector(R.drawable.chat_face_bg);
            List<Collectiion> currentPageItemList = collectionList.subList(
                    pagePosition * size,
                    Math.min((pagePosition + 1) * size, collectionList.size())
            );
            ListAdapter adapter = new CollectionAdapter(mContext, currentPageItemList);
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener((parent, view, itemPosition, id) -> {
                if (listener != null) {
                    listener.onCollectionClick(currentPageItemList.get(itemPosition));
                }
            });
            gridView.setPadding(0, 0, dip_To_px(ctx, 12), 0);
            return gridView;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}

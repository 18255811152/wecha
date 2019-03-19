package com.sk.weichat.view;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.util.DisplayUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

interface PagerGridAdapterFactory<T> {
    ListAdapter createPagerGridAdapter(List<T> data);
}

interface OnItemClickListener<T> {
    void onItemClick(T item);
}

/**
 * Created by Administrator on 2016/9/8.
 */
public class ChatToolsView extends RelativeLayout {
    private ViewPager viewPagerTools;
    private GridPagerAdapter gridPagerAdapter;
    private ChatBottomView.ChatBottomListener listener;
    private boolean isGroup;

    public ChatToolsView(Context context) {
        super(context);
        init(context);
    }

    public ChatToolsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatToolsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private List<Item> loadData() {
        // 隐藏个别按钮是通过删除指定drawable对应按钮实现，
        // 所以这里的drawable应该唯一，
        return Arrays.asList(
                new Item(R.drawable.im_tool_photo_button_bg, R.string.chat_poto, () -> {
                    listener.clickPhoto();
                }),
                new Item(R.drawable.im_tool_camera_button_bg, R.string.chat_camera_record, () -> {
                    listener.clickCamera();
                }),
                // 现拍照录像ui和二为一
/*
                new Item(R.drawable.im_tool_local_button_bg, R.string.video, () -> {
                    Dialog bottomDialog = new Dialog(getContext(), R.style.BottomDialog);
                    View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_video, null);
                    bottomDialog.setContentView(contentView);
                    ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
                    layoutParams.width = getResources().getDisplayMetrics().widthPixels;
                    contentView.setLayoutParams(layoutParams);
                    bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
                    bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
                    bottomDialog.show();
                    contentView.findViewById(R.id.dialog_select_cancel).setOnClickListener(v -> bottomDialog.dismiss());
                    contentView.findViewById(R.id.dialog_select_local_video).setOnClickListener(v -> {
                        bottomDialog.dismiss();
                        listener.clickLocalVideo();
                    });
                    contentView.findViewById(R.id.dialog_select_start_record).setOnClickListener(v -> {
                        bottomDialog.dismiss();
                        listener.clickStartRecord();
                    });
                }),
*/
                new Item(R.drawable.im_tool_loc_button_bg, R.string.chat_loc, () -> {
                    listener.clickLocation();
                }),
                new Item(R.drawable.im_tool_redpacket_button_bg, R.string.chat_redpacket, () -> {
                    listener.clickRedpacket();
                }),
                new Item(R.drawable.im_tool_transfer_button_bg, R.string.transfer_money, () -> {
                    listener.clickTransferMoney();
                }),
                new Item(R.drawable.im_tool_collection, R.string.chat_collection, () -> {
                    listener.clickCollection();
                }),
                new Item(R.drawable.im_tool_card_button_bg, R.string.chat_card, () -> {
                    listener.clickCard();
                }),
                new Item(R.drawable.im_tool_file_button_bg, R.string.chat_file, () -> {
                    listener.clickFile();
                }),
                new Item(R.drawable.im_tool_shake, R.string.chat_shake, () -> {
                    listener.clickShake();
                })
        );
    }

    private void init(Context mContext) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(R.layout.chat_tools_view_all, this);

        viewPagerTools = (ViewPager) findViewById(R.id.view_pager_tools);
        ((TabLayout) findViewById(R.id.tabDots)).setupWithViewPager(viewPagerTools, true);

        gridPagerAdapter = new GridPagerAdapter(
                loadData(), 4, 2,
                data -> new PagerGridAdapter(data, viewPagerTools)
        );
        viewPagerTools.setAdapter(gridPagerAdapter);
    }

    public void init(
            ChatBottomView.ChatBottomListener listener,
            boolean isEquipment,
            boolean isGroup,
            boolean disableLocationServer
    ) {
        setBottomListener(listener);
        setEquipment(isEquipment);
        setGroup(isGroup);
        setPosition(disableLocationServer);
    }

    public void setBottomListener(ChatBottomView.ChatBottomListener listener) {
        this.listener = listener;
    }

    // 事实上只在初始化ChatToolsViews时调用，所以不需要更新小红点indicator，
    public void setEquipment(boolean isEquipment) {
        if (isEquipment) {// 我的设备 需要隐藏音视频通话、红包、戳一戳
            gridPagerAdapter.removeAll(
                    R.drawable.im_tool_audio_button_bg,
                    R.drawable.im_tool_redpacket_button_bg,
                    R.drawable.im_tool_shake
            );
        }
    }

    // 事实上只在初始化ChatToolsViews时调用，所以不需要更新小红点indicator，
    public void setGroup(boolean isGroup) {
        this.isGroup = isGroup;
        if (isGroup) {// 群组 将通话修改为会议，隐藏戳一戳
            gridPagerAdapter.doEach(item -> {
                if (item.icon == R.drawable.im_tool_audio_button_bg) {
                    item.text = R.string.chat_audio_conference;
                }
            });
            // 这里面notify了，
            gridPagerAdapter.removeAll(
                    R.drawable.im_tool_transfer_button_bg
                    , R.drawable.im_tool_shake);
        } else {// 隐藏转账功能
            gridPagerAdapter.removeAll(
                    R.drawable.im_tool_transfer_button_bg);
        }
    }

    // 事实上只在初始化ChatToolsViews时调用，所以不需要更新小红点indicator，
    public void setPosition(boolean disableLocationServer) {
        if (disableLocationServer) {
            gridPagerAdapter.removeAll(R.drawable.im_tool_loc_button_bg);
        }
    }

    static class GridPagerAdapter extends PagerAdapter {
        private final int columnCount;
        private final PagerGridAdapterFactory<Item> factory;
        private final int pageSize;
        private List<Item> data;

        GridPagerAdapter(
                List<Item> data,
                int columnCount,
                int rowCount,
                PagerGridAdapterFactory<Item> factory
        ) {
            this.data = new ArrayList<>(data);
            this.columnCount = columnCount;
            this.factory = factory;

            pageSize = rowCount * columnCount;
        }

        void removeAll(@DrawableRes Integer... ids) {
            Set<Integer> idSet = new HashSet<>(ids.length);
            idSet.addAll(Arrays.asList(ids));
            final Iterator<Item> each = data.iterator();
            while (each.hasNext()) {
                if (idSet.contains(each.next().icon)) {
                    each.remove();
                }
            }
            notifyDataSetChanged();
        }

        void doEach(Function<Item> block) {
            for (Item item : data) {
                block.apply(item);
            }
        }

        private List<Item> getPageData(int page) {
            return data.subList(page * pageSize, Math.min(((page + 1) * pageSize), data.size()));
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            GridView grid = new GridView(container.getContext());
            grid.setNumColumns(columnCount);
            grid.setAdapter(factory.createPagerGridAdapter(getPageData(position)));
            container.addView(grid, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // 没有设置itemClick和item background，但还是会有点击效果，
            return grid;

        }

        @Override
        public int getCount() {
            return (data.size() + (pageSize - 1)) / pageSize;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        public interface Function<T> {
            void apply(T t);
        }
    }

    static class PagerGridAdapter extends BaseAdapter {
        private final List<Item> data;
        private final ViewPager viewPager;

        PagerGridAdapter(List<Item> data, ViewPager viewPager) {
            this.data = data;
            this.viewPager = viewPager;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Item getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View ret = convertView;
            if (ret == null) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_tools_item, parent, false);
                // 高度手动铺满2行，
                // 没必要自动，还容易出意外，
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = DisplayUtil.dip2px(parent.getContext(), 100f);
                view.setLayoutParams(lp);
                ret = view;
            }
            TextView tvItem = ret.findViewById(R.id.tvItem);
            Item item = getItem(position);
            tvItem.setText(item.text);
            tvItem.setCompoundDrawablesWithIntrinsicBounds(null, parent.getContext().getResources().getDrawable(item.icon), null, null);
            tvItem.setOnClickListener(v -> {
                item.runnable.run();
            });
            return ret;
        }
    }
}

class Item {
    @StringRes
    int text;
    @DrawableRes
    int icon;
    Runnable runnable;

    public Item(int icon, int text, Runnable runnable) {
        this.icon = icon;
        this.text = text;
        this.runnable = runnable;
    }
}

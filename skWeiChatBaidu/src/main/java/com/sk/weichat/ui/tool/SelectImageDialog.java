package com.sk.weichat.ui.tool;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sk.weichat.R;
import com.sk.weichat.util.ViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/8/18.
 */
public class SelectImageDialog extends Dialog {
    private Context mContext;
    private LayoutInflater mInflater;
    private OptionListener mlistener;
    private View mRootView;
    private GridView mGridView;
    private TextView mTvTitle;
    private List<String> mUrls;

    public SelectImageDialog(Context context, List<String> urls, OptionListener listener) {
        this(context, 0, LayoutInflater.from(context));
        mContext = context;
        mlistener = listener;
        if (urls == null) {
            mUrls = new ArrayList<>();
        } else {
            mUrls = urls;
        }
        initView();
    }

    private SelectImageDialog(Context context, int themeResId, LayoutInflater mInflater) {
        super(context, R.style.full_dialog_style);
    }

    private void initView() {
        mInflater = LayoutInflater.from(mContext);
        mRootView = mInflater.inflate(R.layout.dialog_select_image, null);
        setContentView(mRootView);

        mRootView.findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mTvTitle = (TextView) mRootView.findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.tip_select_photo);
        mGridView = (GridView) mRootView.findViewById(R.id.dialog_select_gv);
        mGridView.setAdapter(new SelectImgAdapter());
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismiss();
                mlistener.option(mUrls.get(position));
            }
        });
    }

    public interface OptionListener {
        void option(String url);
    }

    class SelectImgAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mUrls.size();
        }

        @Override
        public Object getItem(int position) {
            return mUrls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_image, null, false);
            }
            ImageView iv = ViewHolder.get(convertView, R.id.iv);
            Glide.with(mContext).load(mUrls.get(position)).placeholder(R.drawable.dialog_loading).error(R.drawable.defaultpic).into(iv);
            return convertView;
        }
    }
}

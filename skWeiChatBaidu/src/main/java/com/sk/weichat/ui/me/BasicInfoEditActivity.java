package com.sk.weichat.ui.me;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.EventAvatarUploadSuccess;
import com.sk.weichat.bean.User;
import com.sk.weichat.db.InternationalizationHelper;
import com.sk.weichat.db.dao.UserAvatarDao;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.helper.UsernameHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.other.QRcodeActivity;
import com.sk.weichat.ui.tool.SelectAreaActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.TipDialog;
import com.sk.weichat.volley.Result;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * 编辑个人资料
 */
public class BasicInfoEditActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CAPTURE_CROP_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_CROP_PHOTO = 2;
    private static final int REQUEST_CODE_CROP_PHOTO = 3;
    // widget
    private ImageView mAvatarImg;
    private EditText mNameEdit;
    private TextView mSexTv;
    private TextView mBirthdayTv;
    private TextView mCityTv;
    private TextView mTvDiyName;
    private Button mNextStepBtn;
    private TextView nickNameTv, sexTv, birthdayTv, cityTv, shiledTv;
    private User mUser;
    // Temp
    private User mTempData;
    private File mCurrentFile;
    private Uri mNewPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = coreManager.getSelf();
        if (!LoginHelper.isUserValidation(mUser)) {
            return;
        }
        setContentView(R.layout.activity_basic_info_edit);
        initView();
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_BaseInfo"));

        TextView tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        UsernameHelper.initTextView(tvPhoneNumber, coreManager.getConfig().registerUsername);

        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        mNameEdit = (EditText) findViewById(R.id.name_edit);
        mSexTv = (TextView) findViewById(R.id.sex_tv);
        mBirthdayTv = (TextView) findViewById(R.id.birthday_tv);
        mCityTv = (TextView) findViewById(R.id.city_tv);
        mTvDiyName = (TextView) findViewById(R.id.tv_diy_name);
        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
        mNextStepBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());

        nickNameTv = (TextView) findViewById(R.id.name_text);
        sexTv = (TextView) findViewById(R.id.sex_text);
        birthdayTv = (TextView) findViewById(R.id.birthday_text);
        cityTv = (TextView) findViewById(R.id.city_text);
        shiledTv = (TextView) findViewById(R.id.iv_diy_name);
        TextView mQRCode = (TextView) findViewById(R.id.city_text_02);

        mQRCode.setText(InternationalizationHelper.getString("JX_MyQRImage"));
        nickNameTv.setText(InternationalizationHelper.getString("JX_NickName"));
        sexTv.setText(InternationalizationHelper.getString("JX_Sex"));
        birthdayTv.setText(InternationalizationHelper.getString("JX_BirthDay"));
        cityTv.setText(InternationalizationHelper.getString("JX_Address"));
        shiledTv.setText(InternationalizationHelper.getString("PERSONALIZED_SIGNATURE"));
        mNameEdit.setHint(InternationalizationHelper.getString("JX_InputName"));
        mTvDiyName.setHint(InternationalizationHelper.getString("ENTER_PERSONALIZED_SIGNATURE"));
        mNextStepBtn.setText(InternationalizationHelper.getString("JX_Finish"));

        mAvatarImg.setOnClickListener(this);
        findViewById(R.id.sex_select_rl).setOnClickListener(this);
        findViewById(R.id.birthday_select_rl).setOnClickListener(this);
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.city_select_rl).setVisibility(View.GONE);
        } else {
            findViewById(R.id.city_select_rl).setOnClickListener(this);
        }
        findViewById(R.id.diy_name_rl).setOnClickListener(this);
        findViewById(R.id.qccodeforshiku).setOnClickListener(this);
        mNextStepBtn.setOnClickListener(this);

        if (coreManager.getConfig().registerInviteCode == 2
                && !TextUtils.isEmpty(coreManager.getSelf().getMyInviteCode())) {
            TextView tvInviteCode = findViewById(R.id.invite_code_tv);
            tvInviteCode.setText(coreManager.getSelf().getMyInviteCode());
        } else {
            findViewById(R.id.rlInviteCode).setVisibility(View.GONE);
        }

        updateUI();
    }

    private void updateUI() {
        // clone一份临时数据，用来存数变化的值，返回的时候对比有无变化
        try {
            mTempData = (User) mUser.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        AvatarHelper.getInstance().updateAvatar(mTempData.getUserId());
        // AvatarHelper.getInstance().displayAvatar(mTempData.getUserId(), mAvatarImg, false);
        displayAvatar(mTempData.getUserId());

        mNameEdit.setText(mTempData.getNickName());
        if (mTempData.getSex() == 1) {
            mSexTv.setText(InternationalizationHelper.getString("JX_Man"));
        } else {
            mSexTv.setText(InternationalizationHelper.getString("JX_Wuman"));
        }
        mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str(mTempData.getBirthday()));
        mCityTv.setText(Area.getProvinceCityString(mTempData.getCityId(), mTempData.getAreaId()));
        mTvDiyName.setText(mTempData.getDescription());

        TextView mPhoneTv = (TextView) findViewById(R.id.phone_tv);
        String phoneNumber = coreManager.getSelf().getTelephone();
        int mobilePrefix = PreferenceUtils.getInt(this, Constants.AREA_CODE_KEY, -1);
        String sPrefix = String.valueOf(mobilePrefix);
        // 删除开头的区号，
        if (phoneNumber.startsWith(sPrefix)) {
            phoneNumber = phoneNumber.substring(sPrefix.length());
        }
        mPhoneTv.setText(phoneNumber);
    }

    public void displayAvatar(final String userId) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(userId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(userId);

            Glide.with(MyApplication.getContext())
                    .load(mOriginalUrl)
                    .placeholder(R.drawable.avatar_normal)
                    .signature(new StringSignature(time))
                    .dontAnimate()
                    .error(R.drawable.avatar_normal)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            DialogHelper.dismissProgressDialog();
                            mAvatarImg.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            DialogHelper.dismissProgressDialog();
                            Log.e("zq", "加载原图失败：" + mOriginalUrl);// 该用户未设置头像，网页访问该URL也是404
                        }
                    });
        } else {
            DialogHelper.dismissProgressDialog();
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.avatar_img:
                showSelectAvatarDialog();
                break;
            case R.id.sex_select_rl:
                showSelectSexDialog();
                break;
            case R.id.birthday_select_rl:
                showSelectBirthdayDialog();
                break;
            case R.id.city_select_rl:
                Intent intent = new Intent(BasicInfoEditActivity.this, SelectAreaActivity.class);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_TYPE, Area.AREA_TYPE_PROVINCE);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_PARENT_ID, Area.AREA_DATA_CHINA_ID);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_DEEP, Area.AREA_TYPE_CITY);
                startActivityForResult(intent, 4);
                break;
            case R.id.diy_name_rl:
                inputDiyName();
                break;
            case R.id.qccodeforshiku:
                Intent intent2 = new Intent(BasicInfoEditActivity.this, QRcodeActivity.class);
                intent2.putExtra("isgroup", false);
                intent2.putExtra("userid", mUser.getUserId());
                startActivity(intent2);
                break;
            case R.id.next_step_btn:
                next();
                break;
        }
    }

    private void showSelectAvatarDialog() {
        String[] items = new String[]{InternationalizationHelper.getString("PHOTOGRAPH"), InternationalizationHelper.getString("ALBUM")};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("SELECT_AVATARS")).setSingleChoiceItems(items, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            takePhoto();
                        } else {
                            selectPhoto();
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void takePhoto() {
        mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_CROP_PHOTO);
    }

    private void selectPhoto() {
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_CROP_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CAPTURE_CROP_PHOTO) {// 拍照返回再去裁减
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    Uri o = mNewPhotoUri;
                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 300, 300);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_CROP_PHOTO) {// 选择一张图片,然后立即调用裁减
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    String path = CameraUtil.getImagePathFromUri(this, data.getData());
                    Uri o = Uri.fromFile(new File(path));
                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 300, 300);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_CROP_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    AvatarHelper.getInstance().displayUrl(mNewPhotoUri.toString(), mAvatarImg);
                    // 上传头像
                    uploadAvatar(mCurrentFile);
                } else {
                    ToastUtil.showToast(this, R.string.c_crop_failed);
                }
            }
        } else if (requestCode == 4) {// 选择城市
            if (resultCode == RESULT_OK && data != null) {
                int countryId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTRY_ID, 0);
                int provinceId = data.getIntExtra(SelectAreaActivity.EXTRA_PROVINCE_ID, 0);
                int cityId = data.getIntExtra(SelectAreaActivity.EXTRA_CITY_ID, 0);
                int countyId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTY_ID, 0);

                String province_name = data.getStringExtra(SelectAreaActivity.EXTRA_PROVINCE_NAME);
                String city_name = data.getStringExtra(SelectAreaActivity.EXTRA_CITY_NAME);
                /*String county_name = data.getStringExtra(SelectAreaActivity.EXTRA_COUNTY_ID);*/
                mCityTv.setText(province_name + "-" + city_name);

                mTempData.setCountryId(countryId);
                mTempData.setProvinceId(provinceId);
                mTempData.setCityId(cityId);
                mTempData.setAreaId(countyId);
            }
        }
    }

    private void uploadAvatar(File file) {
        if (!file.exists()) {
            // 文件不存在
            return;
        }
        // 显示正在上传的ProgressDialog
        DialogHelper.showDefaulteMessageProgressDialog(this);
        RequestParams params = new RequestParams();
        final String loginUserId = coreManager.getSelf().getUserId();
        params.put("userId", loginUserId);
        try {
            params.put("file1", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(coreManager.getConfig().AVATAR_UPLOAD_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
                DialogHelper.dismissProgressDialog();
                boolean success = false;
                if (arg0 == 200) {
                    Result result = null;
                    try {
                        result = JSON.parseObject(new String(arg2), Result.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (result != null && result.getResultCode() == Result.CODE_SUCCESS) {
                        success = true;
                    }
                }

                if (success) {
                    ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_success);
                    AvatarHelper.getInstance().updateAvatar(loginUserId);// 更新时间
                    EventBus.getDefault().post(new EventAvatarUploadSuccess(true));
                } else {
                    ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_failed);
                }
            }

            @Override
            public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_failed);
            }
        });
    }

    private void showSelectSexDialog() {
        String[] sexs = new String[]{InternationalizationHelper.getString("JX_Man"), InternationalizationHelper.getString("JX_Wuman")};
        new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("GENDER_SELECTION"))
                .setSingleChoiceItems(sexs, mTempData.getSex() == 1 ? 0 : 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            mTempData.setSex(1);
                            mSexTv.setText(InternationalizationHelper.getString("JX_Man"));
                        } else {
                            mTempData.setSex(0);
                            mSexTv.setText(InternationalizationHelper.getString("JX_Wuman"));
                        }
                        dialog.dismiss();
                    }
                }).setCancelable(true).create().show();
    }

    @SuppressWarnings("deprecation")
    private void showSelectBirthdayDialog() {
        Date date = new Date(mTempData.getBirthday() * 1000);
        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                mTempData.setBirthday(TimeUtils.getSpecialBeginTime(mBirthdayTv, calendar.getTime().getTime() / 1000));
                long currentTime = System.currentTimeMillis() / 1000;
                long birthdayTime = calendar.getTime().getTime() / 1000;
                if (birthdayTime > currentTime) {
                    ToastUtil.showToast(mContext, R.string.data_of_birth);
                }
            }
        }, date.getYear() + 1900, date.getMonth(), date.getDate());
        dialog.show();
    }

    private void inputDiyName() {
        final EditText inputServer = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(InternationalizationHelper.getString("PERSONALIZED_SIGNATURE")).setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                .setNegativeButton(InternationalizationHelper.getString("JX_Cencal"), null);
        builder.setPositiveButton(InternationalizationHelper.getString("JX_Confirm"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String diyName = inputServer.getText().toString();
                mTvDiyName.setText(diyName);
                mUser.setDescription(diyName);
            }
        });
        builder.show();
    }

    private void loadPageData() {
        mTempData.setNickName(mNameEdit.getText().toString().trim());
    }

    private void next() {
        if (!MyApplication.getInstance().isNetworkActive()) {
            ToastUtil.showToast(this, R.string.net_exception);
            return;
        }

        loadPageData();

        if (TextUtils.isEmpty(mTempData.getNickName())) {
            mNameEdit.requestFocus();
            mNameEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.name_empty_error));
            return;
        }

        if (!coreManager.getConfig().disableLocationServer) {
            if (mTempData.getCityId() <= 0) {
                TipDialog tipDialog = new TipDialog(this);
                tipDialog.setTip(getString(R.string.live_address_empty_error));
                tipDialog.show();
                return;
            }
        }

        if (mUser != null && !mUser.equals(mTempData)) {// 数据改变了，提交数据
            updateData();
        } else {
            finish();
        }
    }

    private void updateData() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        if (!mUser.getNickName().equals(mTempData.getNickName())) {
            params.put("nickname", mTempData.getNickName());
        }
        if (mUser.getSex() != mTempData.getSex()) {
            params.put("sex", String.valueOf(mTempData.getSex()));
        }
        if (mUser.getBirthday() != mTempData.getBirthday()) {
            params.put("birthday", String.valueOf(mTempData.getBirthday()));
        }
        if (mUser.getCountryId() != mTempData.getCountryId()) {
            params.put("countryId", String.valueOf(mTempData.getCountryId()));
        }
        if (mUser.getProvinceId() != mTempData.getProvinceId()) {
            params.put("provinceId", String.valueOf(mTempData.getProvinceId()));
        }
        if (mUser.getCityId() != mTempData.getCityId()) {
            params.put("cityId", String.valueOf(mTempData.getCityId()));
        }
        if (mUser.getAreaId() != mTempData.getAreaId()) {
            params.put("areaId", String.valueOf(mTempData.getAreaId()));
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        saveData();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(BasicInfoEditActivity.this);
                    }
                });
    }

    private void saveData() {
        if (!mUser.getNickName().equals(mTempData.getNickName())) {
            coreManager.getSelf().setNickName(mTempData.getNickName());
            UserDao.getInstance().updateNickName(mTempData.getUserId(), mTempData.getNickName());     // 更新数据库
        }
        if (mUser.getSex() != mTempData.getSex()) {
            coreManager.getSelf().setSex(mTempData.getSex());
            UserDao.getInstance().updateSex(mTempData.getUserId(), mTempData.getSex() + "");          // 更新数据库
        }
        if (mUser.getBirthday() != mTempData.getBirthday()) {
            coreManager.getSelf().setBirthday(mTempData.getBirthday());
            UserDao.getInstance().updateBirthday(mTempData.getUserId(), mTempData.getBirthday() + "");// 更新数据库
        }

        if (mUser.getCountryId() != mTempData.getCountryId()) {
            coreManager.getSelf().setCountryId(mTempData.getCountryId());
            UserDao.getInstance().updateCountryId(mTempData.getUserId(), mTempData.getCountryId());
        }
        if (mUser.getProvinceId() != mTempData.getProvinceId()) {
            coreManager.getSelf().setProvinceId(mTempData.getProvinceId());
            UserDao.getInstance().updateProvinceId(mTempData.getUserId(), mTempData.getProvinceId());
        }
        if (mUser.getCityId() != mTempData.getCityId()) {
            coreManager.getSelf().setCityId(mTempData.getCityId());
            UserDao.getInstance().updateCityId(mTempData.getUserId(), mTempData.getCityId());
        }
        if (mUser.getAreaId() != mTempData.getAreaId()) {
            coreManager.getSelf().setAreaId(mTempData.getAreaId());
            UserDao.getInstance().updateAreaId(mTempData.getUserId(), mTempData.getAreaId());
        }

        setResult(RESULT_OK);
        finish();
    }
}

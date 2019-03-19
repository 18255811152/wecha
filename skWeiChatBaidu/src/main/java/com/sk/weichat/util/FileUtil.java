package com.sk.weichat.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.User;
import com.sk.weichat.ui.base.CoreManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class FileUtil {

    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_ADUIO = 2;
    private static final int TYPE_VIDEO = 3;

    /**
     * {@link #TYPE_IMAGE}<br/>
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    private static String getPublicFilePath(int type) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = MyApplication.getInstance().mVoicesDir;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = MyApplication.getInstance().mVideosDir;
                fileSuffix = ".mp4";
                break;
            case TYPE_IMAGE:
                fileDir = MyApplication.getInstance().mPicturesDir;
                fileSuffix = ".jpg";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        return fileDir + File.separator + UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

    /**
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    private static String getPrivateFilePath(int type, String userId) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = MyApplication.getInstance().mAppDir + File.separator + userId + File.separator + Environment.DIRECTORY_MUSIC;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = MyApplication.getInstance().mAppDir + File.separator + userId + File.separator + Environment.DIRECTORY_MOVIES;
                fileSuffix = ".mp4";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        return fileDir + File.separator + UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

    public static String getRandomImageFilePath() {
        return getPublicFilePath(TYPE_IMAGE);
    }

    public static String getRandomAudioFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            return getPrivateFilePath(TYPE_ADUIO, user.getUserId());
        } else {
            return getPublicFilePath(TYPE_ADUIO);
        }
    }

    public static String getRandomAudioAmrFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        String filePath = null;
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            filePath = getPrivateFilePath(TYPE_ADUIO, user.getUserId());
        } else {
            filePath = getPublicFilePath(TYPE_ADUIO);
        }
        if (!TextUtils.isEmpty(filePath)) {
            return filePath.replace(".mp3", ".amr");
        } else {
            return null;
        }
    }

    public static String getRandomVideoFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            return getPrivateFilePath(TYPE_VIDEO, user.getUserId());
        } else {
            return getPublicFilePath(TYPE_VIDEO);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static void createFileDir(String fileDir) {
        File fd = new File(fileDir);
        if (!fd.exists()) {
            fd.mkdirs();
        }
    }

    /**
     * @param fullName
     */
    public static void delFile(String fullName) {
        File file = new File(fullName);
        if (file.exists()) {
            if (file.isFile()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件夹里面的所有文件
     *
     * @param path String 文件夹路径 如 /sdcard/data/
     */
    public static void delAllFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            System.out.println(path + tempList[i]);
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]); // 先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]); // 再删除空文件夹
            }
        }
    }

    /**
     * 删除文件夹
     * <p>
     * String 文件夹路径及名称 如/sdcard/data/
     * String
     *
     * @return boolean
     */
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // 删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); // 删除空文件夹
        } catch (Exception e) {
            System.out.println("删除文件夹操作出错");
            e.printStackTrace();
        }
    }

    public static File saveFileByBitmap(Bitmap bitmap, String url) {

        String fileName = null;

        if (null != url) {
            String[] sss = url.split("/");
            if (sss != null && sss.length >= 1) {
                fileName = sss[sss.length - 1];
            }
        }

        if (fileName == null) {
            return null;
        }


        String path = MyApplication.getInstance().mPicturesDir;

        File dirFile = new File(path);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }

        File myCaptureFile = new File(path + fileName);
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e("xuan", "saveFileByBitmap: " + myCaptureFile.getAbsolutePath());
        return myCaptureFile;
    }

    public static String getSaveDirectory(String str) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + str + "/";
            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }
            return rootDir;
        } else {
            return null;
        }
    }

    private static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private static void saveImageToGallery(Context content, Bitmap bitmap) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 通知图库更新
        content.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
    }

    /**
     * 将图片(本地/网络)保存至相册
     */
    public static void downImageToGallery(final Context context, String url) {
        if (url.toLowerCase().endsWith("gif")) {
            File file = new File(url);
            if (file.exists()) {
                // 将Gif拷贝到指定路径
                String imagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".gif";
                copyFile(url, imagePath);
                Toast.makeText(context, R.string.tip_save_gif_success, Toast.LENGTH_SHORT).show();
                Intent intentBroadcast = new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File mGifFile = new File(imagePath);
                intentBroadcast.setData(Uri.fromFile(mGifFile));
                context.sendBroadcast(intentBroadcast);
            } else {
                ToastUtil.showToast(context, context.getString(R.string.tip_save_gif_failed));
            }
        } else {
            Glide.with(context)
                    .load(url)
                    .asBitmap()
                    .dontAnimate()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            saveImageToGallery(context, resource);
                            Toast.makeText(context, R.string.tip_save_image_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            ToastUtil.showToast(context, context.getString(R.string.tip_save_image_failed));
                        }
                    });
        }
    }

    /**
     * 保存bitmap到本地
     */
    public static String saveBitmap(Bitmap bitmap) {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(imageDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file.getAbsolutePath();
    }

    public static File createImageFileForEdit() {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        return new File(imageDir, fileName);
    }


    public static boolean isExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        return file.exists();
    }
}

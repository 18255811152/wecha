package com.sk.weichat.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sk.weichat.R;
import com.sk.weichat.bean.Company;
import com.sk.weichat.bean.Contact;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.Label;
import com.sk.weichat.bean.MsgRoamTask;
import com.sk.weichat.bean.MyPhoto;
import com.sk.weichat.bean.MyZan;
import com.sk.weichat.bean.UploadingFile;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.UserAvatar;
import com.sk.weichat.bean.VideoFile;
import com.sk.weichat.bean.circle.CircleMessage;
import com.sk.weichat.bean.message.NewFriendMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;


public class SQLiteHelper extends OrmLiteSqliteOpenHelper {
    public static final String DATABASE_NAME = "shiku.db";
    private static final int DATABASE_VERSION = 6;

    // public static final String DATABASE_PATH = Config.SDCARD_PATH +
    // File.separator + "shiku" + File.separator + "shiku.db";
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * 重建数据库，
     */
    public static void rebuildDatabase(Context ctx) {
        ctx.deleteDatabase(SQLiteHelper.DATABASE_NAME);
        copyDatabaseFile(ctx);
    }

    /**
     * 如果在data目录下没有该项目数据库，则拷贝数据库
     */
    public static void copyDatabaseFile(Context context) {
        File dbFile = context.getDatabasePath(SQLiteHelper.DATABASE_NAME);
        if (dbFile.exists()) {
            return;
        }
        File parentFile = dbFile.getParentFile();
        if (!parentFile.exists()) {
            try {
                parentFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        InputStream in = null;
        FileOutputStream out = null;
        try {
            dbFile.createNewFile();
            in = context.getResources().openRawResource(R.raw.shiku);
            int size = in.available();
            byte buf[] = new byte[size];
            in.read(buf);
            out = new FileOutputStream(dbFile);
            out.write(buf);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connSource) {
        createTables(connSource);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connSource, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            version2Drop(connSource);
            createTables(connSource);
            return;
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE User ADD msgBackGroundUrl text default NULL;");
        }
    }

    private void createTables(ConnectionSource connSource) {
        try {
            TableUtils.createTableIfNotExists(connSource, Company.class);
            TableUtils.createTableIfNotExists(connSource, User.class);
            TableUtils.createTableIfNotExists(connSource, Friend.class);
            TableUtils.createTableIfNotExists(connSource, NewFriendMessage.class);
            TableUtils.createTableIfNotExists(connSource, VideoFile.class);
            TableUtils.createTableIfNotExists(connSource, MyPhoto.class);
            TableUtils.createTableIfNotExists(connSource, CircleMessage.class);
            TableUtils.createTableIfNotExists(connSource, MyZan.class);
            TableUtils.createTableIfNotExists(connSource, UserAvatar.class);
            TableUtils.createTableIfNotExists(connSource, Label.class);
            TableUtils.createTableIfNotExists(connSource, Contact.class);
            TableUtils.createTableIfNotExists(connSource, MsgRoamTask.class);
            TableUtils.createTableIfNotExists(connSource, UploadingFile.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void version2Drop(ConnectionSource connSource) {
        try {
            TableUtils.dropTable(connSource, Company.class, false);
            TableUtils.dropTable(connSource, User.class, false);
            TableUtils.dropTable(connSource, Friend.class, false);
            TableUtils.dropTable(connSource, NewFriendMessage.class, false);
            TableUtils.dropTable(connSource, VideoFile.class, false);
            TableUtils.dropTable(connSource, MyPhoto.class, false);
            TableUtils.dropTable(connSource, CircleMessage.class, false);
            TableUtils.dropTable(connSource, MyZan.class, false);
            TableUtils.dropTable(connSource, UserAvatar.class, false);
            TableUtils.dropTable(connSource, Label.class, false);
            TableUtils.dropTable(connSource, Contact.class, false);
            TableUtils.dropTable(connSource, MsgRoamTask.class, false);
            TableUtils.dropTable(connSource, UploadingFile.class, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

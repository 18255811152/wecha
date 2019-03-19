package com.sk.weichat.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sk.weichat.R;

import java.util.Arrays;
import java.util.List;

/**
 * Created by zq on 2017/10/23 0023.
 */

public class SkinUtils {
    // 添加皮肤在这里加一行就可以，
    // 然后字符串要国际化，改皮肤页面显示用，
    // 保存primaryColor作本地持久化，所以primaryColor必须唯一，
    public static List<Skin> defaultSkins = Arrays.asList(
            // 小蚁logo色#14a399，淡钴绿  Viridian green
            new Skin(R.string.skin_viridian_green, 0x14a399),
            // 粉叶绿#BAE019，粉叶绿  Leaf green
            new Skin(R.string.skin_leaf_green, 0xBAE019),
            // 粉天蓝#7ED1FB，粉天蓝  Powder azure
            new Skin(R.string.skin_powder_azure, 0x7ED1FB),
            // facebook蓝#3C589B， 商务蓝  business blue
            new Skin(R.string.skin_business_blue, 0x3C589B),
            // 大海蓝，
            new Skin(R.string.skin_sea_blue, 0x099FDE),
            // 粉红#FA99A0，感性粉
            new Skin(R.string.skin_perceptual_pink, 0xFA99A0),
            // 中国红,
            new Skin(R.string.skin_chinese_red, 0xDE3031),
            // 琥珀黄 #FFC400, 琥珀黄  Amber yellow
            new Skin(R.string.skin_amber_yellow, 0xFFC400),
            // 桔色#FE7B21， 橘黄色   orange
            new Skin(R.string.skin_orange, 0xFE7B21),
            // 马卡龙让JA色#C17E61，浅咖色 light coffee
            new Skin(R.string.skin_light_coffe, 0xC17E61),
            // 蓝灰#547A8C，蓝灰色  blue gray
            new Skin(R.string.skin_blue_gray, 0x547A8C),
            // 深茶色#4E2505， 深茶色  burnt Umber
            new Skin(R.string.skin_burnt_umber, 0x4E2505)
    );
    // 默认淡钴绿,
    private static final Skin DEFAULT_SKIN = defaultSkins.get(0);

    @Nullable
    private static Skin currentSkin = null;

    @NonNull
    private static Skin requireSkin(Context ctx) {
        if (currentSkin != null) {
            return currentSkin;
        }
        synchronized (SkinUtils.class) {
            if (currentSkin == null) {
                int savedSkinColor = PreferenceUtils.getInt(ctx, Constants.KEY_SKIN_COLOR, DEFAULT_SKIN.getPrimaryColor());
                for (Skin skin : defaultSkins) {
                    if (skin.getPrimaryColor() == savedSkinColor) {
                        currentSkin = skin;
                        break;
                    }
                }
                if (currentSkin == null) {
                    // 本地保存的皮肤数据出了异常，比如高版本删除了低版本存在的皮肤，
                    currentSkin = DEFAULT_SKIN;
                }
            }
        }
        return currentSkin;
    }

    public static Skin getSkin(Context ctx) {
        return requireSkin(ctx);
    }

    public static void setSkin(Context ctx, Skin skin) {
        currentSkin = skin;
        PreferenceUtils.putInt(ctx, Constants.KEY_SKIN_COLOR, skin.getPrimaryColor());
    }


    public static class Skin {
        // 表示未激活的灰色，
        private static final int normalColor = 0xffd5d4d9;
        private int colorName;
        // 主色，也就是标题栏的颜色，
        private int primaryColor;
        // 活跃的颜色，也就是各种按钮控件激活状态的颜色，
        private int accentColor;

        Skin(int colorName, int color) {
            this(colorName, color, color);
        }

        Skin(int colorName, int primaryColor, int accentColor) {
            this.colorName = colorName;
            this.primaryColor = 0xff000000 | primaryColor;
            this.accentColor = 0xff000000 | accentColor;
        }

        public int getColorName() {
            return colorName;
        }

        public int getPrimaryColor() {
            return primaryColor;
        }

        public int getAccentColor() {
            return accentColor;
        }

        public ColorStateList getTabColorState() {
            int[][] states = new int[][]{
                    new int[]{-android.R.attr.state_checked},
                    new int[]{android.R.attr.state_checked}
            };

            int[] colors = new int[]{
                    normalColor,
                    getAccentColor()
            };

            return new ColorStateList(states, colors);
        }

        /**
         * copy from java.awt.Color.brighter,
         */
        public int getBrighterPrimaryColor() {
            int color = primaryColor;
            int var1 = Color.red(color);
            int var2 = Color.green(color);
            int var3 = Color.blue(color);
            int var4 = Color.alpha(color);
            byte var5 = 3;
            if (var1 == 0 && var2 == 0 && var3 == 0) {
                return Color.argb(var4, var5, var5, var5);
            } else {
                if (var1 > 0 && var1 < var5) {
                    var1 = var5;
                }

                if (var2 > 0 && var2 < var5) {
                    var2 = var5;
                }

                if (var3 > 0 && var3 < var5) {
                    var3 = var5;
                }

                return Color.argb(var4, Math.min((int) ((double) var1 / 0.7D), 255), Math.min((int) ((double) var2 / 0.7D), 255), Math.min((int) ((double) var3 / 0.7D), 255));
            }
        }
    }
}

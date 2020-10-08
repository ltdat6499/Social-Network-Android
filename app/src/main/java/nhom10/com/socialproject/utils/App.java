package nhom10.com.socialproject.utils;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        this.context = this;
        super.onCreate();

    }
    public static Context getContext() {
        return context;
    }

    public static int getNumCommentOfPost(String pComment) {
        if(TextUtils.isEmpty(pComment))return 0;
        String[] nums = pComment.split(",");
        return nums.length;
    }

    /**
     * @param pLike ,chuỗi chứa số lượng uid like
     * @return số like
     */
    public static int getNumLikeOfPost(String pLike) {
        if(TextUtils.isEmpty(pLike))return 0;
        String[] nums = pLike.split(",");
        return nums.length;
    }

    public static String ID = null;
}

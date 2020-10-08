package nhom10.com.socialproject.services;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import java.util.List;

import nhom10.com.socialproject.models.Post;
import nhom10.com.socialproject.models.User;

public class SocialNetwork {

    public static boolean isDarkMode = false;

    private static Context mContext = null;

    public static SocialServices socialServices = null;

    public static Intent intentService = null;

    public static void startService(Context context, ServiceConnection serviceConnection) {
        if(mContext == null) mContext = context;
        if(intentService == null) {
            intentService = new Intent(context, SocialServices.class);
            context.startService(intentService);
        }
    }

    public static void navigateProfile(String uid){
        socialServices.senBroadcastToNavigateUid(uid);
    }

    public static void navigateCommentListOf(Post post){
        socialServices.senBroadcastToNavigateCommentOf(post);
    }

    public static List<User> getUserListCurrent(){
        return socialServices.getUserListCurrent();
    }

    public static User getUser(String uid){
        return socialServices.findUserById(uid);
    }

    public static void addUserForListCurrent(User user){
        socialServices.addUser(user);
    }

    public static String findNameById(String uid){
        return socialServices.findName(uid);
    }

    public static String findImageById(String uid){
        return socialServices.findImage(uid);
    }

    public static List<Post> getPostListCurrent(){
        return socialServices.getPostListCurrent();
    }

    public static boolean isReceiveDataSuccessfully(){
        if(socialServices != null) {
            return socialServices.isReceiveDataSuccessfully();
        }
        return false;
    }

    public static void getDatabaseFromFirebase(){
        if(socialServices != null){
            socialServices.getDatabaseFromFirebase();
        }
    }

}


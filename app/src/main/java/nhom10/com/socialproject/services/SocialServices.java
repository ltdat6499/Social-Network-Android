package nhom10.com.socialproject.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import nhom10.com.socialproject.models.Comment;
import nhom10.com.socialproject.models.Post;
import nhom10.com.socialproject.models.User;

public class SocialServices extends Service {

    //constants
    public static final String VIEW_TYPE = "VIEW_TYPE";


    public static final String VIEW_PROFILE = "VIEW_PROFILE";

    public static final String VIEW_COMMENT_POST = "VIEW_COMMENT_POST";

    public static final String DATA_CHANGE = "DATA_CHANGE";

    public static final String USER_DATA_CHANGES = "USER_DATA_CHANGES";

    public static final String POST_DATA_CHANGES = "POST_DATA_CHANGES";

    public static final String COMMENT_DELETED = "COMMENT_DELETED";

    public static final String POST_DELETED = "POST_DELETED";

    public static final String NEW_COMMENTS = "NEW_COMMENTS";

    public static final String NEW_POSTS= "NEW_POSTS";
    // Nhóm fire base
    private FirebaseUser mUser;

    private final List<User> userListCurrent = new ArrayList<>();

    private final List<Post> postListCurrent = new ArrayList<>();

    private final List<Comment> commentListCurrent = new ArrayList<>();
    //

    private boolean isHaveUsers = false;

    private boolean isHavePosts = false;

    private boolean isHaveComments = false;

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }


    public class LocalBinder extends Binder {
        public SocialServices getService() {
            return SocialServices.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getDatabaseFromFirebase();
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Hàm nhận tất cả dữ liệu từ firebase và lắng nghe mỗi khi fire base có sự thây đổi
     */
    public void getDatabaseFromFirebase() {
        getAllUsers();
        getAllComments();
        getAllPosts();
        listenWhenChildPostChangeData();
        listenWhenChildUserChangeData();
        listenWhenChildCommentChangeData();
    }

    /**
     * @param uid , uid của người cần xem thông tin
     *            Hàm gửi broadcast tới dashboard activity để điều hướng tới thông tin của người
     *            dùng
     */
    public void senBroadcastToNavigateUid(String uid) {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, VIEW_PROFILE);
        intent.putExtra("uid", uid);
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    /**
     * @param post , post
     *            Hàm gửi broadcast tới dashboard activity để điều hướng tới comment của post này
     */
    public void senBroadcastToNavigateCommentOf(Post post) {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, VIEW_COMMENT_POST);
        intent.putExtra("uid", post.getUid());
        intent.putExtra("pId", post.getpId());
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    /**
     * @param type , loại dữ liệu thay đổi
     * @param object, đối tượng bị thay đổi
     *                 Hàm gửi broadcast tới dashboard activity để activity này cập nhật dữ liệu
     *                cần thay đổi
     */
    public void senBroadcastToUpdateData(String type, Serializable object) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("OBJECT_VALE",object);
        intent.putExtra("DATA",bundle);
        intent.putExtra(VIEW_TYPE, type);
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    public FirebaseUser getCurrentUser() {
        return mUser;
    }

    /**
     * @return true nếu nhận dữ liệu thành công từ firebase, ngược lại false
     */
    public boolean isReceiveDataSuccessfully() {
        return isHaveUsers && isHaveComments && isHavePosts;
    }

    /**
     * Hàm nhận tất cả user từ firebase ở lần đầu tiên hàm được gọi và nhận tất cả thay đổi mới
     * nhất nếu fire base có sự thay đổi dữ liệu user
     */
    private void getAllUsers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("User");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userListCurrent.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    userListCurrent.add(user);

                }
                isHaveUsers = true;
                senBroadcastToUpdateData(DATA_CHANGE,null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public List<User> getUserListCurrent() {
        return userListCurrent;
    }

    public User findUserById(String uid) {
        for (User us : userListCurrent) {
            if (us.getUid().equals(uid)) return us;
        }
        return null;
    }

    public void addUser(User user) {
        if (findUserById(user.getUid()) == null) {
            userListCurrent.add(user);
        }
    }

    public String findName(String uid) {
        for (User us : userListCurrent) {
            if (us.getUid().equals(uid)) return us.getName();
        }
        return "";
    }

    public String findImage(String uid) {
        for (User us : userListCurrent) {
            if (us.getUid().equals(uid)) return us.getImage();
        }
        return "";
    }

    /**
     * Hàm nhận tất cả post từ firebase ở lần đầu tiên hàm được gọi và nhận tất cả thay đổi mới
     * nhất nếu fire base có sự thay đổi dữ liệu post
     */
    private void getAllPosts() {
        // Đường dẫn tới tất cả các post
        FirebaseDatabase.getInstance()
                .getReference("Posts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        postListCurrent.clear();
                        // Load tất cả bài viết từ fire base, đồng thời lắng nghe sự thay đổi
                        // để cập nhật lại post
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Post post = ds.getValue(Post.class);
                            postListCurrent.add(post);
                        }
                        isHavePosts = true;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    public List<Post> getPostListCurrent() {
        return postListCurrent;
    }

    /**
     * Hàm nhận tất cả comment từ firebase ở lần đầu tiên hàm được gọi và nhận tất cả thay đổi mới
     * nhất nếu fire base có sự thay đổi dữ liệu comment
     */
    private void getAllComments() {
        // Đường dẫn tới tất cả các post
        FirebaseDatabase.getInstance()
                .getReference("Comments")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        commentListCurrent.clear();
                        // Load tất cả bài viết từ fire base, đồng thời lắng nghe sự thay đổi
                        // để cập nhật lại post
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Comment comment = ds.getValue(Comment.class);
                            commentListCurrent.add(comment);
                        }
                        isHaveComments = true;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    /**
     * Hàm lắng nghe sự thay đổi mỗi khi dữ liệu user thay đổi trên firebase
     */
    private void listenWhenChildUserChangeData() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("User");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User user = dataSnapshot.getValue(User.class);
                senBroadcastToUpdateData(USER_DATA_CHANGES,user);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Hàm lắng nghe sự thay đổi mỗi khi dữ liệu post thay đổi trên firebase
     */
    private void listenWhenChildPostChangeData() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Post post = dataSnapshot.getValue(Post.class);
                senBroadcastToUpdateData(NEW_POSTS,post);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Post post = dataSnapshot.getValue(Post.class);
                senBroadcastToUpdateData(POST_DATA_CHANGES,post);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Post post = dataSnapshot.getValue(Post.class);
                senBroadcastToUpdateData(POST_DELETED,post);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Hàm lắng nghe sự thay đổi mỗi khi dữ liệu comment thay đổi trên firebase
     */
    private void listenWhenChildCommentChangeData() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Comments");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Comment comment = dataSnapshot.getValue(Comment.class);
                senBroadcastToUpdateData(NEW_COMMENTS,comment);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Comment comment = dataSnapshot.getValue(Comment.class);
                senBroadcastToUpdateData(COMMENT_DELETED,comment);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}

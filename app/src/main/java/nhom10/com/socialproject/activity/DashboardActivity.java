package nhom10.com.socialproject.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.widget.CompoundButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;

import nhom10.com.socialproject.R;
import nhom10.com.socialproject.adapters.AdapterComment;
import nhom10.com.socialproject.adapters.ViewPagerAdapter;
import nhom10.com.socialproject.fragments.NotificationFragment;
import nhom10.com.socialproject.fragments.HomeFragment;
import nhom10.com.socialproject.fragments.ProfileFragment;
import nhom10.com.socialproject.fragments.UsersFragment;
import nhom10.com.socialproject.models.Comment;
import nhom10.com.socialproject.notifications.Token;
import nhom10.com.socialproject.services.SocialNetwork;
import nhom10.com.socialproject.services.SocialServices;
import nhom10.com.socialproject.services.SocialStateListener;

public class DashboardActivity extends AppCompatActivity
        implements SocialStateListener{

    private List<SocialStateListener> socialStateListeners = new ArrayList<>();

    private BroadcastListener broadcastListener;

    // Nhóm fire base
    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private DatabaseReference referencePost;

    private String mUID;

    //
    private BottomSheetBehavior bottomSheetBehavior;

    private RecyclerView recyclerViewComments;

    private AdapterComment adapterComment;

    private List<Comment> commentList;

    private TabLayout tabLayout;

    private ViewPager viewPager;

    private ViewPagerAdapter viewPagerAdapter;

    private SwitchCompat switchCompat;

    private SwipeRefreshLayout swipeRefreshLayout;

    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard2);
        if(!SocialNetwork.isReceiveDataSuccessfully()){
            SocialNetwork.getDatabaseFromFirebase();
        }
        initializeUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastListener);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcast();
        if(!SocialNetwork.isReceiveDataSuccessfully()){
            SocialNetwork.getDatabaseFromFirebase();
        }
    }

    /**
     * Hàm ánh xạ vắt bắt sự kiện cho views
     */
    private void initializeUI() {
        toolbar = findViewById(R.id.toolbarDashboard);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tabLayout    = findViewById(R.id.tabLayoutOptions);
        viewPager    = findViewById(R.id.viewPager);
        switchCompat = findViewById(R.id.switchCompat);

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    onDarkMode(true);
                }else{
                    onDarkMode(false);
                }
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        referencePost = FirebaseDatabase.getInstance().getReference("Comments");

        commentList = new ArrayList<>();
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        recyclerViewComments.setHasFixedSize(true);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));

        bottomSheetBehavior = BottomSheetBehavior.from(recyclerViewComments);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new HomeFragment(),"");
        viewPagerAdapter.addFragment(new ProfileFragment(),"");
        viewPagerAdapter.addFragment(new UsersFragment(),"");
        viewPagerAdapter.addFragment(new NotificationFragment(),"");
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_tab_newsfeed);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_tab_user);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_tab_friends);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_bell);


        checkUserStatus();
        updateToken(FirebaseInstanceId.getInstance().getToken());

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onRefreshApp();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                },800);
            }
        });
    }

    private void refreshApp() {
        Intent i = new Intent(getApplicationContext(), DashboardActivity.class);
        startActivity(i);
        finish();
    }

    public void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        reference.child(mUID).setValue(mToken);
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus() {
        // Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // User đã đăng nhập
            mUID = user.getUid();
            // Lưu lại id của người dùng đăng nhập vào shared preferences
            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.putString("Current_USEREMAIL",mUser.getEmail());
            editor.apply();
        } else {
            // User chưa đăng nhập, quay về main activity
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * @param uid , uid của người cần tìm thông tin
     *            Hàm điều hướng tới thông tin của một người dùng bất kì
     */
    public void navigateProfile(String uid) {
        onNavigate(SocialServices.VIEW_PROFILE,uid);
        viewPager.setCurrentItem(1);

    }

    @Override
    public void onMetaChanged(String type, Object sender) {
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onMetaChanged(type,sender);
            }
        }
    }

    @Override
    public void onNavigate(String type, String idType) {
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onNavigate(type,idType);
            }
        }
    }

    @Override
    public void onDarkMode(boolean change) {
        if(change) {
            SocialNetwork.isDarkMode = true;
            changeThemeDarkMode();
        }else{
            SocialNetwork.isDarkMode = false;
            changeThemeDefault();
        }
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onDarkMode(change);
            }
        }
    }

    @Override
    public void onRefreshApp() {
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onRefreshApp();
            }
        }
    }

    /*
     * Thay đổi giao diện về dạng light mode
     */
    private void changeThemeDefault() {
        tabLayout.setBackgroundColor(Color.WHITE);
        viewPager.setBackgroundColor(Color.WHITE);
    }

    /**
     * Thay đổi giao diện về dạng dark mode
     */
    private void changeThemeDarkMode() {
        recyclerViewComments.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
        toolbar.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
        tabLayout.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
        viewPager.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
    }


    /**
     * @param stateListener , những lớp khai triển callback này
     */
    public void setSocialStateListener(SocialStateListener stateListener){
        if(stateListener == this) return;
        if(stateListener != null){
            socialStateListeners.add(stateListener);
        }
    }

    /**
     * Phương thức đăng kí nhận thông báo từ dịch vụ để xử lý khi dữ liệu trên fire base thay đổi
     */
    private void registerBroadcast() {

        broadcastListener = new BroadcastListener();
        IntentFilter intentFilter = new IntentFilter("metaChanged.Broadcast");
        registerReceiver(broadcastListener, intentFilter);
    }

    /**
     * @param uid, uid của người đăng post
     * @param pId, id của post
     *            Hàm di chuyển đến tất comment của bài viết
     */
    private void navigateComment(String uid, final String pId) {
        referencePost.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Comment comment = ds.getValue(Comment.class);

                    // Comment này chứa pId và uid của người dùng hiện tại thì thêm vào commentList
                    if (comment.getpId().equals(pId)) {
                        commentList.add(comment);
                    }
                }

                // Thêm một comment ảo để làm nơi comment row của người dùng
                Comment comment = new Comment(pId, "", "", mUser.getUid());
                commentList.add(comment);

                //if(adapterComment == null) {
                    adapterComment = new AdapterComment(DashboardActivity.this, commentList);
                    adapterComment.setCommentList(commentList);
                    recyclerViewComments.setAdapter(adapterComment);
                    adapterComment.notifyDataSetChanged();
                /*}else{
                    adapterComment = new AdapterComment(DashboardActivity.this, commentList);
                   // adapterComment.setCommentList(commentList);
                    adapterComment.notifyDataSetChanged();
                }*/

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * @param comment , comment có sự thay đổi
     *                Hàm cập nhật comment mới
     */
    private void updateComment(Comment comment) {
        adapterComment = new AdapterComment(this,commentList);
        recyclerViewComments.setAdapter(adapterComment);
    }

    /**
     * Lớp broadcast để đăng kí xử lý thông báo từ dịch vụ
     */
    public class BroadcastListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(SocialServices.VIEW_TYPE);
            if (type.equals(SocialServices.VIEW_PROFILE)) {
                String uid = intent.getStringExtra("uid");
                navigateProfile(uid);
            } else if (type.equals(SocialServices.VIEW_COMMENT_POST)) {
                String uid = intent.getStringExtra("uid");
                String pId = intent.getStringExtra("pId");
                navigateComment(uid, pId);
            } else if (type.equals(SocialServices.COMMENT_DELETED)) {
                Comment comment = (Comment) intent.getBundleExtra("DATA")
                                .getSerializable("OBJECT_VALE");
                updateComment(comment);
            } else if (type.equals(SocialServices.USER_DATA_CHANGES)) {
                Object args =
                        intent.getBundleExtra("DATA").getSerializable("OBJECT_VALE");
                onMetaChanged(SocialServices.USER_DATA_CHANGES, args);
            } else if (type.equals(SocialServices.POST_DATA_CHANGES)) {
                Object args =
                        intent.getBundleExtra("DATA").getSerializable("OBJECT_VALE");
                onMetaChanged(SocialServices.POST_DATA_CHANGES, args);
            }else if (type.equals(SocialServices.NEW_POSTS) ||
                      type.equals(SocialServices.POST_DELETED)) {
                Object args =
                        intent.getBundleExtra("DATA").getSerializable("OBJECT_VALE");
                onMetaChanged(SocialServices.NEW_POSTS, args);
            }
        }
    }

}

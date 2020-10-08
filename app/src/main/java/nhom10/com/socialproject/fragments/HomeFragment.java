package nhom10.com.socialproject.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import nhom10.com.socialproject.activity.DashboardActivity;
import nhom10.com.socialproject.activity.MainActivity;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.adapters.AdapterPost;
import nhom10.com.socialproject.models.Post;
import nhom10.com.socialproject.models.User;
import nhom10.com.socialproject.services.SocialNetwork;
import nhom10.com.socialproject.services.SocialServices;
import nhom10.com.socialproject.services.SocialStateListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements SocialStateListener {
    //fire base
    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private DatabaseReference mReference;

    private RecyclerView recyclerViewPosts;

    private List<Post> postList;

    private AdapterPost adapterPost;



    public HomeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ((DashboardActivity) getActivity()).setSocialStateListener(this);

        // Init fire base
        mAuth      = FirebaseAuth.getInstance();
        mReference = FirebaseDatabase.getInstance().getReference("User");
        mUser      = mAuth.getCurrentUser();

        // Init views
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewPosts.setLayoutManager(layoutManager);
        recyclerViewPosts.setHasFixedSize(true);

        // Init post list
        postList = new ArrayList<>();

        if(SocialNetwork.isReceiveDataSuccessfully()){
            loadAllPosts();
        }
        return view;
    }

    private void loadAllPosts() {
        postList.clear();
        List<Post> pl = SocialNetwork.getPostListCurrent();
        for (int i = 0; i < pl.size(); i++) {
            if (!pl.get(i).getpMode().equals("Only me") && isPostRelateToWithMyself(pl.get(i))) {
                postList.add(pl.get(i));
            }
        }

        Post p = new Post();
        p.setUid(mUser.getUid());
        postList.add(p);

        if (adapterPost == null) {
            adapterPost = new AdapterPost(getActivity(), postList);
            recyclerViewPosts.setAdapter(adapterPost);
        } else {
            adapterPost.setPostList(postList);
            adapterPost.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapterPost != null){
            recyclerViewPosts.setAdapter(adapterPost);
            adapterPost.notifyDataSetChanged();
            loadAllPosts();
        }

    }

    private boolean isInvitation(String friends) {
        String[] friendList = friends.split(",");
        for (int i = 0; i < friendList.length; i++) {
            if (friendList[i].contains(mUser.getUid())) {
                if (friendList[i].contains("@")) return true;
            }
        }
        return false;
    }

    /**
     * @param post ,
     * @return true nếu post này có quan hệ với người dùng hiện tại, ngược lại false
     */
    private boolean isPostRelateToWithMyself(Post post) {
        User user = SocialNetwork.getUser(post.getUid());
        return isUserRelateToWithMyself(user);
    }

    /**
     * @param user
     * @return true nếu user này có quan hệ với người dùng hiện tại, ngược lại false
     */
    private boolean isUserRelateToWithMyself(User user) {
        if (user.getUid().equals(mUser.getUid())) return true;
        if (user.getFriends().contains(mUser.getUid())) {
            if (!isInvitation(user.getFriends())) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * @param searchQuery , chuỗi cần tìm kiếm
     *                    Hàm tiếm kiếm nội dung post có liền quan tới searchQuery
     */
    private void searchPosts(final String searchQuery) {
        postList.clear();
        List<Post> list = SocialNetwork.getPostListCurrent();
        for(int i = 0;i<list.size();i++){
            if(isPostRelateToWithMyself(list.get(i))){
                if(list.get(i).getpStatus().toLowerCase().contains(searchQuery.toLowerCase())
                  || (SocialNetwork.getUser(list.get(i).getUid())
                                   .getName()
                                   .toLowerCase().contains(searchQuery.toLowerCase()))){
                    postList.add(list.get(i));
                }
            }
        }


        Post p = new Post();
        p.setUid(mUser.getUid());
        postList.add(p);

        if(adapterPost == null){
            adapterPost = new AdapterPost(getActivity(),postList);
            recyclerViewPosts.setAdapter(adapterPost);
        }else{
            adapterPost.setPostList(postList);
            adapterPost.notifyDataSetChanged();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//Để hiện menu
        super.onCreate(savedInstanceState);
    }

    /**
     * Hàm tạo option menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.menuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //gọi khi có sự thay đổi kí tự
                //gọi khi người dùng nhấn tìm kiếm
                if (!TextUtils.isEmpty(s)) {
                    searchPosts(s);
                } else {
                    loadAllPosts();
                }
                return false;
            }
        });
    }


    /**
     * Bắt sự kiện cho menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuLogout) {
            logoutUser();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hàm logout user, quay về main activity
     */
    private void logoutUser() {
        mAuth.signOut();
        checkUserStatus();
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus() {
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            //user đã đăng nhập
        } else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onMetaChanged(String type, Object sender) {
        if (type.equals(SocialServices.USER_DATA_CHANGES)) {
            User user = (User) sender;
            if (isUserRelateToWithMyself(user)) {
                loadAllPosts();
            }
        } else if (type.equals(SocialServices.POST_DATA_CHANGES)) {
            Post post = (Post) sender;
            if (isPostRelateToWithMyself(post)) {
                loadAllPosts();
            }
        } else if (type.equals(SocialServices.NEW_POSTS )||
                   type.equals(SocialServices.POST_DELETED)) {
            loadAllPosts();
        }
    }


    @Override
    public void onNavigate(String type, String idType) {

    }

    @Override
    public void onDarkMode(boolean change) {
        if(change){
            changeThemeDarkMode();
        }else{
            changeThemeDefault();
        }
    }

    @Override
    public void onRefreshApp() {
        loadAllPosts();
    }

    private void changeThemeDarkMode(){
        if(adapterPost != null) {
            recyclerViewPosts.setBackgroundResource(R.drawable.custom_background_dark_mode_main);
            adapterPost.changeThemeDarkMode();
        }
    }

    private void changeThemeDefault(){
        if(adapterPost != null) {
            recyclerViewPosts.setBackgroundColor(Color.WHITE);
            adapterPost.changeThemeDefault();
        }
    }
}

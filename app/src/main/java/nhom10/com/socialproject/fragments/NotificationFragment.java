package nhom10.com.socialproject.fragments;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nhom10.com.socialproject.R;
import nhom10.com.socialproject.activity.DashboardActivity;
import nhom10.com.socialproject.adapters.AdapterNotification;
import nhom10.com.socialproject.models.User;
import nhom10.com.socialproject.services.SocialNetwork;
import nhom10.com.socialproject.services.SocialStateListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class NotificationFragment extends Fragment implements SocialStateListener {

    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private RecyclerView recyclerViewNotifications;

    private AdapterNotification adapterNotification;

    private List<User> userList;

    private TextView txtNotifications;

    private FrameLayout frameNotificationLayout;

    public NotificationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        frameNotificationLayout = view.findViewById(R.id.frameNotificationLayout);
        ((DashboardActivity)getActivity()).setSocialStateListener(this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        //Initialize recycler view
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewNotifications.setLayoutManager(layoutManager);
        recyclerViewNotifications.setHasFixedSize(true);

        txtNotifications = view.findViewById(R.id.txtNotifications);
        //initialize user list
        userList = new ArrayList<>();
        getNotifications();
        return view;
    }

    /**
     * Hàm nhận danh sách user tồn tại trên firebase
     */
    private void getNotifications() {

        FirebaseDatabase.getInstance()
                .getReference("User")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        userList.clear();
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            User user = ds.getValue(User.class);
                            // Nhận danh sách bạn của user
                            String friends = user.getFriends();

                            // Kiểm tra xem uset này có phải là người dùng hiện tại hay không
                            if(user.getUid().equals(mUser.getUid())) {
                                if (friends.contains("@")) {
                                    for(User us:SocialNetwork.getUserListCurrent()){
                                        if(friends.contains(("@"+us.getUid()))){
                                            userList.add(us);
                                        }
                                    }
                                    txtNotifications.setVisibility(View.GONE);
                                    adapterNotification =
                                            new AdapterNotification(getActivity(),
                                                    userList);
                                    recyclerViewNotifications
                                            .setAdapter(adapterNotification);

                                } else {
                                    txtNotifications.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


    }

    private boolean isRequestAddFriend(String friends) {
        String[] friendList = friends.split(",");
        for(int i =0;i<friendList.length;i++){
            if(friendList[i].contains(mAuth.getCurrentUser().getUid())){
                if(friendList[i].contains("@")) return true;
            }
        }
        return false;
    }

    @Override
    public void onMetaChanged(String type, Object sender) {
    }

    @Override
    public void onNavigate(String type, String idType) {

    }

    @Override
    public void onDarkMode(boolean change) {
        if(change) {
            frameNotificationLayout
                    .setBackgroundResource(R.drawable.custom_background_dark_mode_main);
        }else{
            frameNotificationLayout
                    .setBackgroundResource(R.drawable.custom_background_notifications);
        }
    }

    @Override
    public void onRefreshApp() {
        getNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

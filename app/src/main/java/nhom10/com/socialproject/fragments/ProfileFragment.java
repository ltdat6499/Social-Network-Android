package nhom10.com.socialproject.fragments;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nhom10.com.socialproject.activity.ChatActivity;
import nhom10.com.socialproject.activity.DashboardActivity;
import nhom10.com.socialproject.activity.MainActivity;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.adapters.AdapterProfiles;
import nhom10.com.socialproject.models.Post;
import nhom10.com.socialproject.models.User;
import nhom10.com.socialproject.services.SocialNetwork;
import nhom10.com.socialproject.services.SocialServices;
import nhom10.com.socialproject.services.SocialStateListener;

import static com.google.firebase.storage.FirebaseStorage.getInstance;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment implements SocialStateListener {

    // Nhóm firebase
    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private FirebaseDatabase mDatabase;

    private DatabaseReference mReference;

    private DatabaseReference databaseReference;

    // Storage
    private StorageReference storageReference;

    // Đường dẫn nơi ảnh đại diện hay cover được lưu trữ
    private String storagePath = "Users_Profile_Cover_Imgs/";

    // Nhóm UI
    private ImageView imgAvatar;

    private ImageView imgCoverPhoto;

    private TextView txtName, txtEmail, txtPhone;

    private FloatingActionButton floatingActionButton;

    private ProgressDialog progressDialog;

    private BottomNavigationView bottomNavigationView;

    private RelativeLayout relativeLayout;

    private LinearLayout linearLayout;

    private LinearLayoutManager layoutManager;

    //Nhóm constants permissions
    private static final int CAMERA_REQUEST_CODE = 100;

    private static final int STORAGE_REQUEST_CODE = 200;

    private static final int IMAGE_PIC_CAMERA_REQUEST_CODE = 300;

    private static final int IMAGE_PIC_GALLERY_REQUEST_CODE = 400;

    //Thông số xin cấp quyền
    private String cameraPermissions[];

    private String storagePermissions[];

    private Uri imageUri;

    // Để kiểm tra là loại ảnh đại diện hay là ảnh cover
    private String isProfileOrCover;

    private String uid;

    private RecyclerView recyclerViewProfiles;

    private List<Post> postList;

    private AdapterProfiles adapterProfiles;

    public ProfileFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile,container,false);

        ((DashboardActivity)getActivity()).setSocialStateListener(this);

        // Initialize firebase
        mAuth                = FirebaseAuth.getInstance();
        mUser                = mAuth.getCurrentUser();
        mDatabase            = FirebaseDatabase.getInstance();
        mReference           = mDatabase.getReference("User");
        databaseReference    = mDatabase.getReference("User");

        // Firebase storage reference
        storageReference     = getInstance().getReference();

        // Initialize permissions
        cameraPermissions =
                new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions =
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // Initialize UI
        recyclerViewProfiles = view.findViewById(R.id.recyclerViewProfiles);
        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewProfiles.setLayoutManager(layoutManager);
        recyclerViewProfiles.setHasFixedSize(true);

        recyclerViewProfiles.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                if (!recyclerView.canScrollVertically(-1) &&
                        newState == RecyclerView.SCROLL_STATE_IDLE) {
                    relativeLayout.setVisibility(View.VISIBLE);

                }
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    relativeLayout.setVisibility(View.GONE);
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });


        linearLayout         = view.findViewById(R.id.linearLayoutProfile);
        relativeLayout       = view.findViewById(R.id.relativeLayout);
        bottomNavigationView = view.findViewById(R.id.navigationView);
        floatingActionButton = view.findViewById(R.id.floatingActionButton);
        imgCoverPhoto        = view.findViewById(R.id.imgCoverPhoto);
        imgAvatar            = view.findViewById(R.id.imgAvatar);
        txtName              = view.findViewById(R.id.txtName);
        txtEmail             = view.findViewById(R.id.txtEmail);
        txtPhone             = view.findViewById(R.id.txtPhone);
        progressDialog       = new ProgressDialog(getActivity());

        //Sự kiện click floating point action
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        uid = mUser.getUid();

        //Optimize code
        //getInfoUser(uid);

        // Tạm thời ẩn đi navigation view
        bottomNavigationView.setVisibility(View.GONE);
        // Bắt sự kiện cho bottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(selectedListener);
        postList = new ArrayList<>();

        //Optimize code
        if(SocialNetwork.isReceiveDataSuccessfully()){
            getInfoUser(uid);
            loadAllPosts();
        }
        return view;
    }

    private void loadAllPosts() {
        postList.clear();
        List<Post> pl = SocialNetwork.getPostListCurrent();
        if (uid.equals(mUser.getUid())) {
            // Nếu thông tin trùng với người đăng nhập và bài post do người dùng đăng
            // Nhận cái post của người dùng
            for (int i = 0; i < pl.size(); i++) {
                if (uid.equals(pl.get(i).getUid())) {
                    postList.add(pl.get(i));
                }
            }
        } else {
            // Tìm người viết bài này
            for (Post p2 : pl) {
                if (uid.equals(p2.getUid())) {
                    User user = SocialNetwork.getUser(uid);
                    if (isUserRelateToWithMyself(user)) {
                        postList.add(p2);
                    }
                }
            }
        }

        if (adapterProfiles == null) {
            adapterProfiles = new AdapterProfiles(getActivity(), postList);
            recyclerViewProfiles.setAdapter(adapterProfiles);
        } else {
            adapterProfiles.setPostList(postList);
            adapterProfiles.notifyDataSetChanged();
        }

        relativeLayout.setVisibility(View.VISIBLE);
        relativeLayout.setFocusable(true);
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

    private boolean isPostRelateToWithMyself(Post post) {
        User user = SocialNetwork.getUser(post.getUid());
        return isUserRelateToWithMyself(user);
    }

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

    @SuppressLint("RestrictedApi")
    private void getInfoUser(String uid) {
        // Nếu thông tin người dùng chứa uid này khác với người đăng nhập hiện tại
        // thì đó là một người khác. Ta hiện bottom navigation view
        if (!uid.equals(mUser.getUid())) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            floatingActionButton.setVisibility(View.GONE);
        } else {
            bottomNavigationView.setVisibility(View.GONE);
            floatingActionButton.setVisibility(View.VISIBLE);
        }

        User user      = SocialNetwork.getUser(uid);
        String name    = user.getName();
        String email   = user.getEmail();
        String phone   = user.getPhone();
        String image   = user.getImage();
        String cover   = user.getCover();
        String follow  = user.getFollow();
        String friends = user.getFriends();

        if (follow.contains(mUser.getUid())) {
            MenuItem menuItem =
                    bottomNavigationView.getMenu().findItem(R.id.itemFollow);
            menuItem.setTitle("Following");
        }

        if (friends.contains(mUser.getUid())) {
            MenuItem menuItem =
                    bottomNavigationView.getMenu().findItem(R.id.itemAddFriend);
            if (!isInvitation(friends)) {
                menuItem.setTitle("Friend");
            } else {
                menuItem.setTitle("Requested");
            }
        }
        //Thiết lập dữ liệu
        txtName.setText(name);
        txtEmail.setText(email);
        txtPhone.setText(phone);

        try {
            // Thiết lập image nếu nhận được hình ảnh từ firebase storage
            Glide.with(getActivity()).load(image).placeholder(R.drawable.ic_tab_user).into(imgAvatar);
        } catch (Exception e) {

        }
        try {
            // Thiết lập image nếu nhận được hình ảnh từ firebase storage
            Glide.with(getActivity()).load(cover).into(imgCoverPhoto);

        } catch (Exception e) {
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapterProfiles != null) {
            recyclerViewProfiles.setAdapter(adapterProfiles);
            adapterProfiles.notifyDataSetChanged();
        }
    }

    /*
     * Hàm xử lý sự kiện click các item trên bottom navigation view
     */
    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    //Xử lý item click
                    switch (menuItem.getItemId()){
                        case R.id.itemAddFriend:
                            //Xử lý kết bạn
                            addFriends(uid,menuItem);
                            return true;
                        case R.id.itemFollow:
                            //Xử lý follow
                            followPerson(uid,menuItem);
                            return true;
                        case R.id.itemMessage:
                            // Xử lý chat
                            // StartActivity by putting UID
                            // Sử dụng UID để nhận diện người chat cùng
                            Intent intent = new Intent(getActivity(), ChatActivity.class);
                            intent.putExtra("hisUid",uid);
                            getActivity().startActivity(intent);
                            return true;
                        case R.id.itemMore:

                            return true;
                    }
                    return false;
                }
            };

    /**
     * @param uid ,uid của người cần add friend
     *            Hàm friend theo uid của người dùng
     */
    private void addFriends(final String uid, final MenuItem menuItem) {

        // Thêm chính người dùng hiện tại vào danh sách bạn của người khác của người khác
        databaseReference.orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {

                            final String friendArr = ds.child("friends").getValue().toString();
                            final HashMap<String, Object> hashMap = new HashMap<>();

                            //Nếu danh sách bạn của người khác đang có chứa người dùng này
                            if (friendArr.contains(mUser.getUid())) {
                                menuItem.setTitle("Add Friend");
                                //Nếu đang gửi yêu cầu, cho phép hủy
                                if (isInvitation(friendArr)) {

                                    String newFriendArr = friendArr
                                            .replace("@" + mUser.getUid() + ",",
                                                    "");
                                    hashMap.put("friends", newFriendArr);
                                    ds.getRef().updateChildren(hashMap);
                                }else{
                                    // Đã là bạn thì xóa
                                    String newFriendArr = friendArr
                                            .replace(mUser.getUid() + ",",
                                                    "");
                                    hashMap.put("friends", newFriendArr);
                                    ds.getRef().updateChildren(hashMap);

                                    mReference.orderByChild("uid").equalTo(mUser.getUid())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                        String frs = ds.child("friends").getValue().toString();
                                                        frs = frs.replace(uid+",","");
                                                        HashMap<String,Object> hs = new HashMap<>();
                                                        hashMap.put("friends",frs);
                                                        ds.getRef().updateChildren(hashMap);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                }
                            }else {
                                // Thêm một yêu cầu kết bạn
                                menuItem.setTitle("Requested");
                                String fr = ds.child("friends").getValue().toString();
                                HashMap<String, Object> hm = new HashMap<>();
                                fr += "@" + mUser.getUid() + ",";
                                hashMap.put("friends", fr);
                                ds.getRef().updateChildren(hashMap);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }


    /**
     * @param uid , uid của người cần follow
     *            Hàm follow theo uid của người dùng
     */
    private void followPerson(final String uid, final MenuItem menuItem) {

        //Thêm chính người dùng hiện tại vào danh sách follow của người khác
        databaseReference.orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds: dataSnapshot.getChildren()){
                            String follow = ds.child("follow").getValue().toString();
                            HashMap<String,Object> hashMap = new HashMap<>();
                            if(follow.contains(mUser.getUid())){
                                menuItem.setTitle("Follow");
                                follow = follow.replace(mUser.getUid()+",","");
                            }else {
                                follow += mUser.getUid() + ",";
                                menuItem.setTitle("Following");
                            }
                            hashMap.put("follow",follow);
                            ds.getRef().updateChildren(hashMap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    /**
     * Show dialog contains options
     * Edit profile picture
     * Edit cover picture
     * Edit name
     * Edit phone
     */
    private void showEditProfileDialog() {
        String options[] = new String[]{"Edit profile picture",
                "Edit cover photo",
                "Edit name",
                "Edit phone"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //Set title
        builder.setTitle("Choose action");
        //Set items
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        //Edit profile clicked
                        progressDialog.setMessage("Updating profile picture");
                        isProfileOrCover = "image";
                        showImagePicDialog();
                        break;
                    case 1:
                        //Edit cover photo clicked
                        progressDialog.setMessage("Updating cover photo");
                        isProfileOrCover = "cover";
                        showImagePicDialog();
                        break;
                    case 2:
                        //Edit name clicked
                        progressDialog.setMessage("Updating name");
                        showNamePhoneUpdateDialog("name");
                        break;
                    case 3:
                        //Edit phone clicked
                        progressDialog.setMessage("Updating phone number");
                        showNamePhoneUpdateDialog("phone");
                        break;
                    default:break;
                }
            }
        }).create().show();
    }

    /**
     * @param key , cho biết loại cần update là name hay phone
     *             Hàm update name và phone vào trong user's database in firebase
     */
    private void showNamePhoneUpdateDialog(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+key);

        //Set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);

        //add edit text
        final EditText editText = new EditText(getActivity());
        editText.setHint("Ennter "+key);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = editText.getText().toString().trim();
                if(!TextUtils.isEmpty(value)){
                    progressDialog.show();
                    HashMap<String,Object> result = new HashMap<>();
                    result.put(key,value);

                    //Cập nhật user's database theo uid
                    mReference.child(mUser.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //Updated, dimiss progress
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(),
                                            "Updated...",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(),e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {
                    Toast.makeText(getActivity(),"Please enter "+key,Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.dismiss();
            }
        });

        builder.create().show();
    }

    /**
     * Hàm hiển thị dialog cho người dùng chọn phương thức lấy ảnh từ camera hoặc gallery
     */
    private void showImagePicDialog() {
        String options[] = new String[]{"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //Set title
        builder.setTitle("Browse");
        //Set items
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        //Camera clicked
                        if(!checkCameraPermissions()){
                            requestCameraPermissions();
                        }else {
                            pickFromCamera();
                        }
                        break;
                    case 1:
                        //Gallery clicked
                        if(!checkStoragePermissions()){
                            requestStoragePermissions();
                        }else{
                            pickFromGallery();
                        }
                        break;
                    default:break;
                }
            }
        }).create().show();
    }

    /**
     * @return true ứng dụng có quyền thao tác với file của phone, ngược lại false
     */
    private boolean checkStoragePermissions(){
        ///Nếu ứng dụng có quyền này thì trả về PackageManager.PERMISSION_GRANTED
        boolean result = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    /**
     * @return true nếu xin quyền thao tác thành công, ngược lại false
     */
    private void requestStoragePermissions(){
        requestPermissions(storagePermissions,STORAGE_REQUEST_CODE);
    }


    /**
     * @return true ứng dụng có quyền sử dụng camera, ngược lại false
     */
    private boolean checkCameraPermissions(){
        //Nếu ứng dụng có quyền này thì trả về PackageManager.PERMISSION_GRANTED
        boolean result1 = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean result2 = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result1 && result2;
    }

    /**
     * @return true nếu xin quyền sử dụng camera thành công, ngược lại false
     */
    private void requestCameraPermissions(){
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    /*
     * Phương thức này được gọi khi người dùng click chấp nhận hoặc từ chối xin cấp quyền từ
     * hộp thoại dialog
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode){
            //Chấp nhận quyền sử dụng camera
            case CAMERA_REQUEST_CODE:
                //Bật camera, kiểm tra camera và storage permissions có được chấp nhận
                if(grantResults.length>0){
                    boolean cameraAccept = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccept = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccept && storageAccept){
                        //permissions enabled
                        pickFromCamera();
                    }else{
                        ////permissions denied
                        Toast.makeText(getActivity(),
                                "Please accept camera & storage permissions",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            //Chấp nhận quyền sử dụng file
            case STORAGE_REQUEST_CODE:

                //Bật gallery, kiểm tra storage permissions có được chấp nhận
                if(grantResults.length>0){
                    boolean writeStorageAccept = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(writeStorageAccept){
                        //permissions enabled
                        pickFromGallery();
                    }else{
                        ////permissions denied
                        Toast.makeText(getActivity(),
                                "Please accept storage permissions",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                break;
        }
    }

    /*
     * Phương thức này sẽ được gọi sau khi chọn image từ camera hoặc gallery
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK){
            //Image được chọn từ gallery, nhận Uri của image
            if(requestCode == IMAGE_PIC_GALLERY_REQUEST_CODE){
                imageUri = data.getData();
                uploadProfileCoverPhoto(imageUri);
            }
            //Image được chọn từ camera, nhận Uri của image
            if(requestCode == IMAGE_PIC_CAMERA_REQUEST_CODE){
                uploadProfileCoverPhoto(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * @param uri ,đường dẫn tới ảnh
     *                 Hàm cập nhật ảnh cover photo
     */
    private void uploadProfileCoverPhoto(Uri uri) {
        progressDialog.show();
        //Đường dẫn và tên của ảnh
        //e.g Users_Profile_Cover_Imgs/image_123456.jpg
        //e.g Users_Profile_Cover_Imgs/cover_123456.jpg
        String filePathAndName = storagePath+""+isProfileOrCover+"_"+mUser.getUid();

        final StorageReference storageReference2nd = storageReference.child(filePathAndName);
        //
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Ảnh được upload tới storage, nhận nó và lưu trữ trong database của user
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();

                        //Nếu ảnh được upload thành công
                        if(uriTask.isSuccessful()){
                            //Thêm hoặc cập nhật vào user's database
                             /*Tham số đầu tiên là isProfileOrCover có giá trị 'image' hoặc 'cover'
                             là các key trong user's database nơi url của hình ảnh sẽ được lưu.

                             Tham số thứ hai chứa url của hình ảnh được lưu trữ trong bộ lưu trữ firebase,
                              url này sẽ được lưu dưới dạng string so với khóa 'image' hoặc 'cover'*/
                            HashMap<String,Object> results = new HashMap<>();
                            results.put(isProfileOrCover,downloadUri.toString());

                            mReference.child(mUser.getUid())
                                    .updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //url in database of user is added successfully
                                            //dismiss progress bar
                                            progressDialog.dismiss();
                                            Toast.makeText(getActivity(),
                                                    "Image updated...",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(),
                                            "Error updating image...",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                        }else {
                            //Nếu ảnh được upload thất bại
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(),"Some error occured",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getActivity(),e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PIC_GALLERY_REQUEST_CODE);
    }

    private void pickFromCamera() {
        //Khởi tạo đường dẫn đến file hình

        //Khởi tạo giá trị cần lưu vào dữ liệu
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp description");

        //Chèn giá trị vào cơ sở dữ liệu lưu trữ ảnh, trả về Uri trỏ đến vị trí đang lưu
        imageUri = getActivity().getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //Tạo intent để yêu cầu hệ thống start Camera chụp hình
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Chỉ định vị trí lưu ảnh: ta gán đường dẫn uri ở trên
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(cameraIntent,IMAGE_PIC_CAMERA_REQUEST_CODE);
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
        inflater.inflate(R.menu.menu_main,menu);
        super.onCreateOptionsMenu(menu,inflater);
    }


    /**
     * Bắt sự kiện cho menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menuLogout){
            logoutUser();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hàm logout user, quay về main activity
     */
    private void logoutUser(){
        mAuth.signOut();
        checkUserStatus();
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus(){
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //user đã đăng nhập
        }else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onMetaChanged(String type, Object sender) {
        getInfoUser(uid);
        //loadAllPosts();

        if(type.equals(SocialServices.USER_DATA_CHANGES)){
            User user = (User)sender;
            if(uid.equals(user.getUid())){
                if(isUserRelateToWithMyself(user)){
                    getInfoUser(uid);
                    loadAllPosts();
                    updateProfile();
                }
            }
        }else if(type.equals(SocialServices.POST_DATA_CHANGES)){
           Post post = (Post)sender;
           if(isPostRelateToWithMyself(post)){
               loadAllPosts();
           }
        }else if(type.equals(SocialServices.NEW_POSTS )||
                type.equals(SocialServices.POST_DELETED)){
            loadAllPosts();
        }
    }



    private void updateProfile() {
        if (uid != null) {
            User user = SocialNetwork.getUser(uid);
            String friend = user.getFriends();
            if (friend.contains(mUser.getUid())) {
                if (isInvitation(friend)) {
                    bottomNavigationView.getMenu().findItem(R.id.itemAddFriend).setTitle("Requested");
                } else {
                    bottomNavigationView.getMenu().findItem(R.id.itemAddFriend).setTitle("Friend");
                }
            } else {
                bottomNavigationView.getMenu().findItem(R.id.itemAddFriend).setTitle("Add Friend");
            }
        }
    }


    @Override
    public void onNavigate(String type, String idType) {
        if(type.equals(SocialServices.VIEW_PROFILE)){
            uid = idType;
            getInfoUser(uid);
            loadAllPosts();
            updateProfile();
        }
    }

    @Override
    public void onDarkMode(boolean change) {
        if (change) {
            changeThemeDarkMode();
        }else {
            changeThemeDefault();
        }
    }

    @Override
    public void onRefreshApp() {
        loadAllPosts();
    }

    private void changeThemeDarkMode(){
        if(adapterProfiles != null) {
            recyclerViewProfiles.setBackgroundResource(R.drawable.custom_background_dark_mode_main);
            floatingActionButton.setBackgroundResource(R.drawable.custom_background_dark_mode_main);
            linearLayout.setBackgroundResource(R.drawable.custom_background_dark_mode_main);
            txtName.setTextColor(Color.WHITE);
            txtEmail.setTextColor(Color.WHITE);
            txtPhone.setTextColor(Color.WHITE);
            adapterProfiles.changeThemeDarkMode();
        }
    }

    private void changeThemeDefault(){
        if(adapterProfiles != null) {
            recyclerViewProfiles.setBackgroundColor(Color.WHITE);
            floatingActionButton.setBackgroundColor(Color.WHITE);
            linearLayout.setBackgroundColor(Color.WHITE);
            txtName.setTextColor(txtName.getTextColors().getDefaultColor());
            txtEmail.setTextColor(txtEmail.getTextColors().getDefaultColor());
            txtPhone.setTextColor(txtPhone.getTextColors().getDefaultColor());
            adapterProfiles.changeThemeDefault();
        }
    }
}

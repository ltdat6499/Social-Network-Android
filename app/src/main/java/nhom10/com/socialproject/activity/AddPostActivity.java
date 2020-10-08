package nhom10.com.socialproject.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import nhom10.com.socialproject.R;

public class AddPostActivity extends AppCompatActivity {

    //permissions constant
    private static final int CAMERA_REQUEST_CODE = 100;

    private static final int STORAGE_REQUEST_CODE = 200;

    //image pick constants
    private static final int IMAGE_PICK_FROM_CAMERA_CODE = 300;

    private static final int IMAGE_PICK_FROM_GALLERY_CODE = 400;

    //permissions array
    private String[] cameraPermissions;

    private String[] storagePermissions;

    //
    public static final int REQUEST_PRIVACY = 1;

    private LinearLayout linearChoosePrivacy, lnPickPhotoVideo, lnPickCamera;

    private ImageView imgPrivacy, imgProfile, imgStatus;

    private TextView txtPrivacy, txtUserName;

    private EditText edtStatus;

    private Toolbar toolbar;

    //firebase
    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private DatabaseReference databaseReference;
    //
    private Uri imageUri = null;

    private ProgressDialog progressDialog;

    //user info
    private String name, email, uid, dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);
        initializeUI();
    }

    /**
     * Khởi tạo và ánh xạ các views
     */
    private void initializeUI(){
        toolbar = findViewById(R.id.toolbarAddPost);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundResource(R.drawable.custom_background_profile2);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imgPrivacy  = findViewById(R.id.imgPrivacy);
        imgProfile  = findViewById(R.id.circularAvatar);
        imgStatus   = findViewById(R.id.imgStatus);
        txtPrivacy  = findViewById(R.id.txtPrivacy);
        txtUserName = findViewById(R.id.txtUserName);
        edtStatus   = findViewById(R.id.edtStatus);

        linearChoosePrivacy = findViewById(R.id.linearChoosePrivacy);
        lnPickPhotoVideo    = findViewById(R.id.lnPickPhotoVideo);
        lnPickCamera        = findViewById(R.id.lnPickCamera);

        progressDialog = new ProgressDialog(this);
        linearChoosePrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(AddPostActivity.this,
                        PrivacyActivity.class),
                        REQUEST_PRIVACY);
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mUser  = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("User");

        lnPickCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get image from camera
                //check permission ở đây
                if(!checkCameraPermissions()){
                    requestCameraPermissions();
                }else{
                    pickFromCamera();
                }
            }
        });

        lnPickPhotoVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get image/video from gallery
                if(!checkStoragePermissions()){
                    requestStoragePermissions();
                }else{
                    pickFromGallery();
                }
            }
        });

        updateInfoUser();

        //init permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    /**
     * Chọn ảnh từ gallery
     */
    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_FROM_GALLERY_CODE);
    }

    /**
     * Chọn ảnh từ camera
     */
    private void pickFromCamera() {
        //Khởi tạo đường dẫn đến file hình
        //Khởi tạo giá trị cần lưu vào dữ liệu
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp description");

        //Chèn giá trị vào cơ sở dữ liệu lưu trữ ảnh, trả về Uri trỏ đến vị trí đang lưu
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //Tạo intent để yêu cầu hệ thống start Camera chụp hình
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Chỉ định vị trí lưu ảnh: ta gán đường dẫn uri ở trên
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(cameraIntent,IMAGE_PICK_FROM_CAMERA_CODE);
    }

    /**
     * @return true nếu quyền lưu trữ đã được cấp phép, ngược lại false
     */
    private boolean checkStoragePermissions(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    /**
     * Hàm xin cấp quyền sử dụng bộ lưu trữ
     */
    private void requestStoragePermissions(){
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }

    /**
     * @return true nếu quyền camera đã được cấp phép, ngược lại false
     */
    private boolean checkCameraPermissions(){

        boolean result2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return result1 && result2;
    }

    /**
     * Hàm xin cấp quyền sử dụng camera
     */
    private void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    /**
     * Hàm cập nhật thông tin user
     */
    private void updateInfoUser(){
        Query query = databaseReference.orderByChild("email").equalTo(mUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            //Hàm này luôn được gọi khi có sự thay đổi dữ liệu trên firebase
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Kiểm tra cho đến khi nhận được dữ liệu
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //Nhận dữ liệu
                    name  = ds.child("name").getValue().toString();
                    email = ds.child("email").getValue().toString();
                    dp    = ds.child("image").getValue().toString();
                    uid   = ds.child("uid").getValue().toString();

                    //Thiết lập dữ liệu
                    txtUserName.setText(name);

                    try {
                        //Thiết lập image nếu nhận được hình ảnh từ firebase storage
                        Picasso.get().load(dp).into(imgProfile);
                    } catch (Exception e) {
                        //Ngược lại, thiết lập hình mặc định
                        Picasso.get().load(R.drawable.ic_add_image).into(imgProfile);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddPostActivity.this,
                        databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.itemPost) {
            //Nhận dữ liệu từ edit text
            String status = edtStatus.getText().toString().trim();
            if (imageUri == null) {
                //Đăng status không hình ảnh
                if(TextUtils.isEmpty(status))return false;
                uploadData(status,"noImage");

            } else {
                edtStatus.setHint("Say something about this photo...");
                uploadData(status,String.valueOf(imageUri));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @param status , tiêu đề của post
     * @param uri, uri ảnh của post
     *                 Upload post lên database firebase
     */
    private void uploadData(final String status, final String uri) {
        progressDialog.setMessage("Creating new post...");
        progressDialog.show();

        //mỗi post được đăng sẽ có một thời gian xác định, lấy mốc này làm id của post
        final String timestamp = String.valueOf(System.currentTimeMillis());

        //đường dẫn image của post trên storage firebase nếu post có ảnh
        String filePathAndName = "Posts/post_" + timestamp;
        if (!uri.equals("noImage")) {
            //post with image
            StorageReference storageReference =
                    FirebaseStorage.getInstance().getReference().child(filePathAndName);
            storageReference.putFile(Uri.parse(uri))
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Ảnh được upload tới storage, nhận nó và lưu trữ trong database của user
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful()) ;
                            String downloadUri = uriTask.getResult().toString();

                            //Nếu ảnh được upload thành công
                            if (uriTask.isSuccessful()) {
                                /* Thêm hoặc cập nhật vào user's database
                                * Tham số đầu tiên là isProfileOrCover có giá trị 'image' hoặc 'cover'
                                * là các key trong user's database nơi url của hình ảnh sẽ được lưu.

                                * Tham số thứ hai chứa url của hình ảnh được lưu trữ trong bộ lưu trữ firebase,
                                * url này sẽ được lưu dưới dạng string so với khóa 'image' hoặc 'cover'*/
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("uid", uid);
                                hashMap.put("pId", timestamp);
                                hashMap.put("pStatus", status);
                                hashMap.put("pImage", downloadUri);
                                hashMap.put("pTime",timestamp);
                                hashMap.put("pMode",txtPrivacy.getText());
                                hashMap.put("pLike","");
                                hashMap.put("pComment","");

                                //path to store data
                                DatabaseReference ref =
                                        FirebaseDatabase.getInstance().getReference("Posts");
                                ref.child(timestamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Toast.makeText(AddPostActivity.this,
                                                        "Posted...",
                                                        Toast.LENGTH_SHORT).show();

                                                //reset views
                                                edtStatus.setText("");
                                                imgStatus.setImageURI(null);
                                                imageUri = null;
                                                imgStatus.setVisibility(View.GONE);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failed adding post in data
                                        progressDialog.dismiss();
                                        Toast.makeText(AddPostActivity.this,
                                                e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

        }else{
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid", uid);
            hashMap.put("pId", timestamp);
            hashMap.put("pStatus", status);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime",timestamp);
            hashMap.put("pMode",txtPrivacy.getText());
            hashMap.put("pLike","");
            hashMap.put("pComment","");

            //path to store data
            DatabaseReference ref =
                    FirebaseDatabase.getInstance().getReference("Posts");
            ref.child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this,
                                    "Posted...",
                                    Toast.LENGTH_SHORT).show();
                            //reset views
                            edtStatus.setText("");
                            imgStatus.setImageURI(null);
                            imageUri = null;
                            imgStatus.setVisibility(View.GONE);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed adding post in data
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_PRIVACY){
                String typePrivacy = data.getStringExtra("PRIVACY");
                if(typePrivacy.equals("Public")){
                    imgPrivacy.setImageResource(R.drawable.ic_public);
                }else if(typePrivacy.equals("Friends")){
                    imgPrivacy.setImageResource(R.drawable.ic_friends_all);
                }else if(typePrivacy.equals("Only Me")){
                    imgPrivacy.setImageResource(R.drawable.ic_private);
                }
                txtPrivacy.setText(typePrivacy);
            }
            else if(requestCode == IMAGE_PICK_FROM_CAMERA_CODE){
                //Nhận image từ uri
                if(imageUri != null){
                    imgStatus.setImageURI(imageUri);
                    imgStatus.setVisibility(View.VISIBLE);
                }

            }else if(requestCode == IMAGE_PICK_FROM_GALLERY_CODE){
                //Nhận image từ uri
                imageUri = data.getData();
                if(imageUri != null){
                    imgStatus.setImageURI(imageUri);
                    imgStatus.setVisibility(View.VISIBLE);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
     * Hàm xử lý kết quả xin cấp quyền của người dùng
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            // check kết quả quyền camera
            case CAMERA_REQUEST_CODE:
                if(grantResults.length>0){
                    boolean cameraAccept = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccept = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccept && storageAccept){
                        pickFromCamera();
                    }else{
                        Toast.makeText(this,
                                "Please grant camera & storage permission...",
                                Toast.LENGTH_SHORT).show();
                    }
                }else{

                }
                break;
                // check kết quả quyền lưu trữ
            case STORAGE_REQUEST_CODE:
                //
                if(grantResults.length>0){
                    boolean storageAccept = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccept){
                        pickFromGallery();
                    }else{
                        Toast.makeText(this,
                                "Please grant storage permission...",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
                default:break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}

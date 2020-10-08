package nhom10.com.socialproject.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nhom10.com.socialproject.R;
import nhom10.com.socialproject.adapters.AdapterChat;
import nhom10.com.socialproject.models.Chat;
import nhom10.com.socialproject.models.Room;
import nhom10.com.socialproject.models.User;
import nhom10.com.socialproject.notifications.APIService;
import nhom10.com.socialproject.notifications.Client;
import nhom10.com.socialproject.notifications.Data;
import nhom10.com.socialproject.notifications.Response;
import nhom10.com.socialproject.notifications.Sender;
import nhom10.com.socialproject.notifications.Token;
import nhom10.com.socialproject.widgets.TypingVisualizer;
import retrofit2.Call;
import retrofit2.Callback;

public class    ChatActivity extends AppCompatActivity {

    private static final int REQUEST_VIDEO_CALL = 100;

    private  static final int GALLERY_PICK = 1;

    private static final int CAMERA_REQUEST_CODE = 10;

    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS =
            {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
             Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //Firebase
    private FirebaseAuth mAuth;

    private FirebaseDatabase firebaseDatabase;

    private DatabaseReference databaseReference;

    private StorageReference firebaseStorage;
    //

    //Kiểm tra bật tắt tính năng voice
    private boolean isVoice = true;

    //Sử dụng tính năng voice
    private MediaRecorder mediaRecorder;

    private static final String outputFileRecord =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

    //Để kiểm tra xem người dùng có nhìn thấy tin nhắn hay không
    private ValueEventListener valueEventListener;

    private DatabaseReference referenceForSeen;
    //
    private List<Chat> chatList;

    private AdapterChat adapterChat;

    //Views
    private RecyclerView recyclerView;

    private ImageView imgProfile;

    private TextView txtName, txtStatus;

    private EditText edtMessage;

    private ImageButton btnSend, btnVideoCall, btnSendImg;

    private LinearLayout typingLinearLayout;

    private TypingVisualizer typingVisualizer;

    //
    private String hisUid;

    private String hisImage;

    private String myUid;

    //
    private APIService apiService;

    private boolean notify = false;

    private boolean isCalling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ActivityCompat.requestPermissions(ChatActivity.this,
                REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        initializeUI();
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }


    @Override
    protected void onPause() {
        super.onPause();
        //get time stamp
        if(!isCalling) {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            //set offline with last seen time stamp
            checkOnlineStatus(timeStamp);
        }

        //set typing
        checkTypingStatus("noOne");

        //Người dùng tạm dừng chương trình thì kết thúc việc lắng nghe
        referenceForSeen.removeEventListener(valueEventListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }

    /**
     * Hàm khởi tạo và ánh xạ các views
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initializeUI(){

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolBarChat);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        typingLinearLayout = findViewById(R.id.typingLayout);
        typingVisualizer   = findViewById(R.id.typingVisualizer);
        recyclerView = findViewById(R.id.recyclerViewChats);
        imgProfile   = findViewById(R.id.circularProfile);
        txtName      = findViewById(R.id.txtName);
        txtStatus    = findViewById(R.id.txtStatus);
        edtMessage   = findViewById(R.id.edtMessage);
        btnSend      = findViewById(R.id.btnSend);
        btnVideoCall = findViewById(R.id.btnVideoCall);
        btnSendImg   = findViewById(R.id.btn_Img_Send);

        //Layout linear cho recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        //create API service
        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);

        mAuth        = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("User");
        firebaseStorage = FirebaseStorage.getInstance().getReference();

        //Khi click vào một user ta có uid. Chúng ta sử dụng uid này để có được hình ảnh và bắt
        //đầu trò chuyện cùng người đó
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //Tìm kiếm thông tin bạn chat
        Query query = databaseReference.orderByChild("uid").equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Kiểm tra cho đến khi nhận được thông tin từ firebase
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //Nhận tên, uri hình ảnh bạn chat
                    String name   = "" + ds.child("name").getValue();
                    hisImage      = "" + ds.child("image").getValue();

                    //get value of typing
                    String typing = "" + ds.child("typingTo").getValue();
                    if(typing.equals(myUid)){
                        typingVisualizer.setColor(Color.BLUE);
                        typingLinearLayout.setVisibility(View.VISIBLE);
                    }else{
                        typingLinearLayout.setVisibility(View.GONE);
                    }

                    //get value of online status
                    String onlineStatus = ""+ds.child("onlineStatus").getValue();
                    if(onlineStatus.equals("online")) {
                        btnVideoCall.setImageResource(R.drawable.ic_video_call);
                    }else if(onlineStatus.equals("Video calling")){
                        txtStatus.setText("Calling to you");
                        btnVideoCall.setImageResource(R.drawable.ic_video_call_on);
                    } else {
                        btnVideoCall.setImageResource(R.drawable.ic_video_call);
                        //format time
                        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                        calendar.setTimeInMillis(Long.parseLong(onlineStatus));
                        String dateTime =
                                DateFormat.format("yyyy/MM/dd hh:mm aa",calendar).toString();
                        txtStatus.setText("Last seen at: "+dateTime);
                    }
                    //Set data
                    txtName.setText(name);
                    try{
                        Glide.with(ChatActivity.this) .load(hisImage)
                                .placeholder(R.drawable.ic_user_anonymous)
                                .into(imgProfile);
                    }catch (Exception e){
                        Glide.with(ChatActivity.this)
                                .load(R.drawable.ic_user_anonymous)
                                .into(imgProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        btnSend.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        if(!isVoice) return false;
                        try {
                            mediaRecorder = new MediaRecorder();
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                            mediaRecorder.setOutputFile(outputFileRecord);
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                            btnSend.setImageResource(R.drawable.ic_recording);
                            Toast.makeText(getApplicationContext(), "Recording...", Toast.LENGTH_LONG).show();
                        } catch (IllegalStateException ise) {
                            //Toast.makeText(getApplicationContext(), "Error ise Occurred : " + ise.getMessage(), Toast.LENGTH_LONG).show();
                        } catch (IOException ioe) {
                            mediaRecorder.stop();
                            mediaRecorder.release();
                            mediaRecorder = null;
                            //Toast.makeText(getApplicationContext(), "Error ioe Occurred : " + ioe.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(!isVoice){
                            notify = true;
                            //Nhận nội dung từ edit text
                            String message = edtMessage.getText().toString().trim();

                            if (TextUtils.isEmpty(message)) {
                                //Handle text is empty
                            } else {
                                sendMessage(message);
                            }
                            //Reset edit text
                            edtMessage.setText("");
                        }else {
                            btnSend.setImageResource(R.drawable.ic_voice);
                            try {
                                mediaRecorder.stop();
                                mediaRecorder.release();
                                mediaRecorder = null;
                                Toast.makeText(getApplicationContext(), "Sending Record", Toast.LENGTH_LONG).show();
                                sendVoiceMessage();
                                return true;
                            }catch (Exception ex){
                                Toast.makeText(getApplicationContext(), "Record too short", Toast.LENGTH_LONG).show();
                            }
                        }
                }
                return false;
            }
        });

        btnSendImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] actions = {"Location", "Picture", "Camera"};

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Option");
                builder.setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]
                        switch (which){
                            case 0:
                                break;
                            case 1:
                                Intent galleryIntent = new Intent();
                                galleryIntent.setType("image/*");
                                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
                                break;
                            case 2:
                                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                startActivityForResult(intent,CAMERA_REQUEST_CODE);
                                break;
                        }
                    }
                });
                builder.show();

            }
        });

        btnVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOnlineStatus("Video calling");
                setupToCallVideo();

            }
        });

        //check edit text change listener
        edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals("")){
                    isVoice = true;
                    btnSend.setImageResource(R.drawable.ic_voice);
                }else{
                    isVoice = false;
                    btnSend.setImageResource(R.drawable.ic_send);
                }
                if(s.toString().trim().length() == 0){
                    checkTypingStatus("noOne");
                }else{
                    //uid of receiver
                    checkTypingStatus(hisUid);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        readMessages();

        seenMessages();
    }


    /**
     * Hàm lắng nghe cuộc hội thoại giữa 2 người, cho biết bên nào đã xem
     */
    private void seenMessages() {
        referenceForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        valueEventListener = referenceForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    //Kiểm tra xem đoạn chat cuối, người nhận là chính bạn và người gửi là bạn của
                    //bạn thì bạn của bạn đã xem tin nhắn
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("isSeen",true);
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
     * Hàm lấy nội dung đoạn chat trên database của firebase sau khi activity chat được gọi
     * và lắng nghe cuộc hội thoại giữa 2 người, khi có sự thay đổi, dữ liệu trên firebase sẽ được
     * cập nhật
     */
    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    //Kiểm tra cho đến khi nhận được nội dung đoạn chat giữa người dùng và bạn chat
                    if((chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid))
                    ||(chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid))){
                        chatList.add(chat);
                    }
                }
                Collections.reverse((chatList));
                if(adapterChat == null){
                    adapterChat = new AdapterChat(ChatActivity.this,chatList,hisImage);
                    recyclerView.setAdapter(adapterChat);
                }else{
                    adapterChat.setChatList(chatList);
                    adapterChat.notifyDataSetChanged();
                };

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * @param message , nội dung tin nhắn cần gửi
     *                Hàm gửi nội dung tin nhắn đến bạn chat
     */
    private void sendMessage(final String message) {
        /* "Chats" node will be created that will contains all chats
         * Whenever user send message it will create new child in "Chats" note and that will contain
         * the following key values :
         * sender: UID of sender
         * receiver: UID of receiver
         * message: content of conversation
         */

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        //Lấy thời gian hiện tại
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("sender",myUid);
        hashMap.put("receiver",hisUid);
        hashMap.put("message",message);
        hashMap.put("timestamp",timestamp);
        hashMap.put("isSeen",false);
        hashMap.put("type","text");

        //Tạo node Chats và set dữ liệu
        reference.child("Chats").push().setValue(hashMap);


        String msg = message;
        DatabaseReference databaseReference =
                FirebaseDatabase.getInstance().getReference("User").child(myUid);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User  user = dataSnapshot.getValue(User.class);
                if(notify){
                    sendNotifications(hisUid,user.getName(),message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Hàm gửi âm thanh cho bạn chat
     */
    private void sendVoiceMessage() {
        final String mCurrentUserId = mAuth.getCurrentUser().getUid();
        final String nameRandom = myUid + hisUid + SystemClock.currentThreadTimeMillis() + ".3gp";
        final StorageReference filepath = firebaseStorage.child("Message_Audio").child(nameRandom);

        Uri uriAudio = Uri.fromFile(new File(outputFileRecord).getAbsoluteFile());
        filepath.putFile(uriAudio).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String timestamp = String.valueOf(System.currentTimeMillis());

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                            Map messageMap = new HashMap();
                            messageMap.put("sender", mCurrentUserId);
                            messageMap.put("receiver", hisUid);
                            messageMap.put("message", uri.toString());
                            messageMap.put("timestamp", timestamp);
                            messageMap.put("isSeen", false);
                            messageMap.put("type", "audio");

                            reference.child("Chats").push().setValue(messageMap);

                            DatabaseReference dataRef =
                                    FirebaseDatabase.getInstance().getReference("User").child(myUid);
                            dataRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    if (notify) {
                                        sendNotifications(hisUid, user.getName(), "You have an audio message");
                                    }
                                    notify = false;
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //Toast.makeText(ChatActivity.this, "Error : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * @param imageUri, đường dẫn của ảnh
     *                  Hàm gửi hình ảnh cho bạn chat
     */
    private void sendImageMessage(Uri imageUri){
        final String mCurrentUserId = mAuth.getCurrentUser().getUid();
        final String nameRandom = myUid + hisUid + SystemClock.currentThreadTimeMillis() + ".jpg";
        final StorageReference filepath = firebaseStorage.child("Message_Images").child(nameRandom);
        filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String timestamp = String.valueOf(System.currentTimeMillis());

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                            Map messageMap = new HashMap();
                            messageMap.put("sender", mCurrentUserId);
                            messageMap.put("receiver", hisUid);
                            messageMap.put("message", uri.toString());
                            messageMap.put("timestamp", timestamp);
                            messageMap.put("isSeen", false);
                            messageMap.put("type", "image");

                            reference.child("Chats").push().setValue(messageMap);

                            DatabaseReference dataRef =
                                    FirebaseDatabase.getInstance().getReference("User").child(myUid);
                            dataRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    if (notify) {
                                        sendNotifications(hisUid, user.getName(), "You have a image message");
                                    }
                                    notify = false;
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //Toast.makeText(ChatActivity.this, "Error : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * @param hisUid , uid của bạn chat
     * @param name, tên của bạn chat
     * @param message, nội đung tin nhắn của bạn chat
     *                 Hàm gửi notification cho điện thoại khi bạn chat gửi tin nhắn đến người dùng
     */
    private void sendNotifications(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid,
                            name+":"+message,
                            "New Message",
                            hisUid,
                            R.drawable.ic_user_anonymous);
                    Sender sender = new Sender(data,token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<Response>() {
                                @Override
                                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                                    Toast.makeText(ChatActivity.this,
                                            response.message(),
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<Response> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus(){
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //user đã đăng nhập
            myUid = user.getUid();
        }else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(ChatActivity.this, MainActivity.class));
            finish();
        }
    }

    /**
     * @param status , trạng thái của người dùng đang online hay offline
     *               Hàm thay đổi trạng thái của người dùng hiện tại
     */
    private void checkOnlineStatus(String status){
        DatabaseReference reference =
                FirebaseDatabase.getInstance().getReference("User").child(myUid);
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus",status);

        //Cập nhật giá trị vào trong uid của current user(
        reference.updateChildren(hashMap);
    }

    /**
     * @param typing , trạng thái của người dùng đang gõ tin nhắn
     *               Hàm thay đổi trạng thái của người dùng đang gõ tin nhắn
     */
    private void checkTypingStatus(String typing){
        DatabaseReference reference =
                FirebaseDatabase.getInstance().getReference("User").child(myUid);
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("typingTo",typing);

        //Cập nhật giá trị vào trong uid của current user(
        reference.updateChildren(hashMap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hàm tạo room với bạn chat để để gọi video call
     */
    private void setupToCallVideo() {

        // Tìm phòng
        FirebaseDatabase.getInstance()
                .getReference("Rooms")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean findRoomSuccess = false;
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            Room room = ds.getValue(Room.class);
                            if(room.getUser1().equals(hisUid) &&
                               room.getUser2().equals(myUid)){
                                //Đã có room

                                Intent intent = new Intent(ChatActivity.this,VideoCallActivity.class);
                                intent.putExtra("hisUid",hisUid);
                                intent.putExtra("myUid",myUid);
                                intent.putExtra("room",room.getRoomId());
                                startActivityForResult(intent,REQUEST_VIDEO_CALL);
                                isCalling = true;
                                findRoomSuccess = true;
                                break;
                            }
                        }
                        if(!findRoomSuccess){
                            Room room = new Room(timestamp,myUid,hisUid);
                            FirebaseDatabase.getInstance()
                                    .getReference("Rooms")
                                    .child(timestamp).setValue(room);
                            Intent intent = new Intent(ChatActivity.this,VideoCallActivity.class);
                            intent.putExtra("hisUid",hisUid);
                            intent.putExtra("myUid",myUid);
                            intent.putExtra("room",timestamp);
                            isCalling = true;
                            startActivityForResult(intent,REQUEST_VIDEO_CALL);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        //Toast.makeText(ChatActivity.this,"Not call",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_VIDEO_CALL){
            // Cuộc gọi kết thúc
            FirebaseDatabase.getInstance()
                    .getReference("Rooms")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot ds:dataSnapshot.getChildren()){
                                Room room = ds.getValue(Room.class);
                                if(room.getUser1().equals(myUid)){
                                    ds.getRef().removeValue();
                                    isCalling = false;
                                    checkOnlineStatus("online");
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

        }else if(resultCode == RESULT_OK && (requestCode == GALLERY_PICK )){
            if (data != null) {
                Uri imageUri = data.getData();
                sendImageMessage(imageUri);
            }else {
                Toast.makeText(ChatActivity.this, "Can't take image !", Toast.LENGTH_SHORT).show();
            }
        }else if(resultCode == RESULT_OK && (requestCode == CAMERA_REQUEST_CODE )){
            if (data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Uri photoUri = getImageUri(getApplicationContext(), photo);
                sendImageMessage(photoUri);
            }else {
                Toast.makeText(ChatActivity.this, "Can't take photo !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}


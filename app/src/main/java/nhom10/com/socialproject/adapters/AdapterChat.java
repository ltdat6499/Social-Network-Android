package nhom10.com.socialproject.adapters;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.models.Chat;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.Holder> {

    private static final int MESSAGE_TYPE_ON_LEFT = 0;

    private static final int MESSAGE_TYPE_ON_RIGHT = 1;

    private boolean audioPlaying = false;

    private Context mContext;

    private List<Chat> chatList;

    private String imageUrl;

    private FirebaseUser mUser;

    public void setChatList(List<Chat> chatList) {
        this.chatList = chatList;
    }

    public AdapterChat(Context mContext, List<Chat> chatList, String imageUrl) {
        this.mContext = mContext;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    /*
     * Dựa vào type để tạo ViewHolder tương ứng.
     */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if(i == MESSAGE_TYPE_ON_RIGHT){
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.row_contains_chat_right,viewGroup,false);
            return new Holder(view);
        }else{
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.row_contains_chat_left,viewGroup,false);
            return new Holder(view);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull final Holder holder, final int i) {

        final Chat c = chatList.get(i);
        final String message_type = c.getType();

        // get data
        final String message = chatList.get(i).getMessage();
        String timestamp = chatList.get(i).getTimestamp();

        // format time
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
            calendar.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime = DateFormat.format("yyyy/MM/dd hh:mm aa",calendar).toString();

        // set data
        if(message_type.equals("text")) {
            holder.txtMessage.setVisibility(View.VISIBLE);
            holder.imgMessage.setVisibility(View.GONE);
            holder.btnAudioMessage.setVisibility(View.GONE);
            holder.txtMessage.setText(message);
        } else if(message_type.equals("image")){
            holder.imgMessage.setVisibility(View.VISIBLE);
            holder.txtMessage.setVisibility(View.GONE);
            holder.btnAudioMessage.setVisibility(View.GONE);
            try {
                Glide.with(mContext)
                        .load(message)
                        .into(holder.imgMessage);
            }catch (Exception e){
                Toast.makeText(mContext, "Can't load image !", Toast.LENGTH_SHORT).show();
            }
        }else if(message_type.equals("audio")){
            holder.imgMessage.setVisibility(View.GONE);
            holder.txtMessage.setVisibility(View.GONE);
            holder.btnAudioMessage.setVisibility(View.VISIBLE);
            holder.btnAudioMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    {
                        String url = c.getMessage();
                        if(audioPlaying){
                            audioPlaying = false;
                            holder.btnAudioMessage.setImageResource(R.drawable.ic_play);
                        }else{
                            audioPlaying = true;
                            holder.btnAudioMessage.setImageResource(R.drawable.ic_stop);
                            MediaPlayer mediaPlayer = new MediaPlayer();
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    audioPlaying = false;
                                    holder.btnAudioMessage.setImageResource(R.drawable.ic_play);
                                    mp.stop();
                                    mp.release();
                                }
                            });
                            try {
                                mediaPlayer.setDataSource(url);
                                mediaPlayer.prepare();
                                mediaPlayer.start();
                                Toast.makeText(mContext, "Playing Audio", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                audioPlaying = false;
                                holder.btnAudioMessage.setImageResource(R.drawable.ic_play);
                                mediaPlayer.stop();
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                        }
                    }
                }
            });
        }
        holder.txtTimeSend.setText(dateTime);
        try{
            Picasso.get().load(imageUrl).into(holder.imgProfile);
        }catch (Exception e){

        }

        //handle click message
        holder.messageLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(message_type.equals("text")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("Delete");
                    builder.setMessage("Do you want delete this message?");

                    //yes button
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteMessages(i);
                        }
                    });

                    //no button
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    return true;
                }else{
                    String[] actions = {"Save", "Delete"};

                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(mContext);
                    builder.setTitle("Option");
                    builder.setItems(actions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                    downloadFile(Uri.parse(message), message_type);
                                    break;
                                case 1:
                                    AlertDialog.Builder builderDelete = new AlertDialog.Builder(mContext);
                                    builderDelete.setTitle("Delete");
                                    builderDelete.setMessage("Do you want delete this message?");
                                    builderDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            deleteMessages(i);
                                        }
                                    });
                                    builderDelete.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    builderDelete.create().show();
                                    break;
                            }
                        }
                    });
                    builder.show();
                    return true;
                }
            }
        });

        // Set seen/delivered state of message
        // Kiểm tra xem tin nhắn cuối cùng trong danh sách
        // TH1: của row left thì isSeen đã GONE trong xml
        // TH2: của row right thì kiểm tra xem đã nhận được tin nhắn chưa
        if(i == chatList.size() - 1){
            if(chatList.get(i).isSeen()){
                holder.txtIsSeen.setText("Seen");
            }else{
                holder.txtIsSeen.setText("Delivered");
            }
        }else{
            holder.txtIsSeen.setVisibility(View.GONE);
        }
    }

    //Cho phép tải ảnh hoặc audio từ tin nhắn
    private void downloadFile(Uri uri, String type){
        long lastDownload =-1;
        String suffixes = "";
        if(type.equals("image")){
            suffixes = ".jpg";
        }else if(type.equals("audio")){
            suffixes = ".3gp";
        }
        else{
            return;
        }
        DownloadManager mgr = null;
        mgr = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .mkdirs();

        lastDownload = mgr.enqueue(new DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                        DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("Downloading "+ type)
                .setDescription("Downloading, Please Wait...")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        SystemClock.currentThreadTimeMillis() + suffixes));
    }

    /**
     * @param position , vị trí cần xóa
     *          Hàm xóa tin nhắn
     */
    private void deleteMessages(int position) {
        final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        /*
         * Logic:
         * Nhận timestamp của message được click
         * So sánh với tất cả message in Chats node
         * Nếu cả hai trùng nhau thì xóa
         */
        String msg = chatList.get(position).getTimestamp();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = reference.orderByChild("timestamp").equalTo(msg);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /* Có 2 cách xóa tin nhắn
                 * Cách 1: Xóa luôn node child trên firebase
                 * Cách 2: Đánh dấu tin nhắn thành đã xóa
                 */
                for(DataSnapshot ds:dataSnapshot.getChildren()){

                    if(ds.child("sender").getValue().equals(myUid)) {
                        // Xóa trên firebase
                        // ds.getRef().removeValue();

                        // Thay đổi nội dung
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "The message has been deleted");
                        ds.getRef().updateChildren(hashMap);

                        Toast.makeText(mContext,"Message deleted...",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(mContext,
                                "You can delete only your message...",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    /*
    * Lấy về type tương ứng với từng position trong collection, position từ cao đến thấp
    */
    @Override
    public int getItemViewType(int position) {
        //Hiện đang đăng nhập người dùng
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(chatList.get(position).getSender().equals(mUser.getUid())){
            return MESSAGE_TYPE_ON_RIGHT;
        }
        return  MESSAGE_TYPE_ON_LEFT;
    }

    // ViewHolder

    class Holder extends RecyclerView.ViewHolder{

        // Views
        CircleImageView imgProfile;

        ImageView imgMessage;

        TextView txtMessage, txtTimeSend, txtIsSeen;

        LinearLayout messageLinearLayout;

        ImageButton btnAudioMessage;

        public Holder(@NonNull View itemView) {
            super(itemView);

            // Ánh xạ
            imgProfile          = itemView.findViewById(R.id.imageProfile);
            txtMessage          = itemView.findViewById(R.id.txtMessage);
            txtTimeSend         = itemView.findViewById(R.id.txtTimeSend);
            txtIsSeen           = itemView.findViewById(R.id.txtSeenMessage);
            messageLinearLayout = itemView.findViewById(R.id.messageLayout);
            imgMessage          = itemView.findViewById(R.id.imgMessage);
            btnAudioMessage     = itemView.findViewById(R.id.btnAudioMessage);
        }
    }
}

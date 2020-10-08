package nhom10.com.socialproject.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.models.Comment;
import nhom10.com.socialproject.services.SocialNetwork;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.Holder> {

    private Context mContext;

    private List<Comment> commentList;

    public AdapterComment(Context mContext, List<Comment> commentList) {
        this.mContext = mContext;
        this.commentList = commentList;
    }

    public List<Comment> getCommentList() {
        return commentList;
    }

    public void setCommentList(List<Comment> commentList) {
        this.commentList = commentList;
    }


    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //Tạo row news feed


        View view = LayoutInflater.from(mContext).inflate(R.layout.row_comment,
                viewGroup, false);
        return new Holder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, final int i) {
        //Nếu là row dùng để comment
        if (i == (commentList.size() - 1)) {
            try {
                Picasso
                        .get()
                        .load(SocialNetwork.findImageById(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                        .placeholder(R.drawable.ic_user_anonymous).into(holder.imgProfile);
            }catch (Exception e){

            }
            holder.txtName.setVisibility(View.GONE);
            holder.btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(holder.edtComment.getText().toString().trim().equals("")){
                        Toast.makeText(mContext,
                                "You have not entered your message!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    Comment comment =
                            new Comment(commentList.get(i).getpId(),
                                    holder.edtComment.getText().toString(),
                                    timestamp,uid);

                    //Lưu vào đường dẫn có tên là comment;
                    //Tạo đường dẫn pId, đặt dữ liệu vào database
                    FirebaseDatabase.getInstance()
                            .getReference("Comments")
                            .child(timestamp).setValue(comment);

                    // Thêm người comment vào danh sách comment
                    FirebaseDatabase.getInstance().getReference("Posts")
                            .orderByChild("pId").equalTo(commentList.get(i).getpId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        String pComment = ds.child("pComment").getValue().toString();
                                        pComment += uid + ",";
                                        HashMap<String, Object> hashMap = new HashMap<>();
                                        hashMap.put("pComment", pComment);
                                        ds.getRef().updateChildren(hashMap);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                    holder.edtComment.setText("");
                }
            });
        }else{

            holder.btnSend.setVisibility(View.GONE);
            holder.txtTime.setVisibility(View.VISIBLE);
            //format time
            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
            calendar.setTimeInMillis(Long.parseLong(commentList.get(i).getcTime()));
            String dateTime =
                    DateFormat.format("yyyy/MM/dd hh:mm aa",calendar).toString();
            String id = commentList.get(i).getUid();
            holder.txtTime.setText(dateTime);
            holder.edtComment.setText(commentList.get(i).getcContent());
            holder.edtComment.setEnabled(false);

            holder.txtName.setText(SocialNetwork.findNameById(id));
            try {
                Picasso.get().load(SocialNetwork.findImageById(id))
                        .placeholder(R.drawable.ic_user_anonymous).into(holder.imgProfile);
            }catch (Exception e){

            }
            holder.linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(commentList.get(i)
                            .getUid()
                            .equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                        showDeleteDialog(commentList.get(i).getcTime(),
                                commentList.get(i).getpId(),
                                commentList.get(i).getUid());
                    }
                    return false;
                }
            });
        }
    }

    private void showDeleteDialog(final String cTime, final String pId, final String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Notification");
        builder.setMessage("Are you sure you want to delete this comment?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    deleteComment(cTime, pId, uid);
                }
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void deleteComment(String cTime, final String pId, final String uid) {
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Deleting...");

        //image deleted, delete from database
        FirebaseDatabase.getInstance()
                .getReference("Comments")
                .orderByChild("cTime").equalTo(cTime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue();

                    FirebaseDatabase.getInstance()
                            .getReference("Posts")
                            .orderByChild("pId").equalTo(pId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for(DataSnapshot ds: dataSnapshot.getChildren()){
                                        String pComment = ds.child("pComment").getValue().toString();
                                        pComment = removeUidFromPost(pComment,uid);
                                        HashMap<String,Object> hashMap = new HashMap<>();
                                        hashMap.put("pComment",pComment);
                                        ds.getRef().updateChildren(hashMap);
                                        Toast.makeText(mContext,
                                                "Deleted successfully",
                                                Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    progressDialog.dismiss();
                                }
                            });

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });

    }

    /**
     * @param pComment , comment của post
     * @param uid, uid của người dùng
     * @return
     */
    private String removeUidFromPost(String pComment, String uid) {
        String[] s = pComment.split(",");
        int k = -1;
        for(int i =0;i<s.length;i++){
            if(s[i].equals(uid)) {
                k = i;
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        if(k != -1){
            for(int j =0;j<s.length;j++){
                if(j != k){
                    sb.append(s[j]);
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        return pComment;
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    //View holder
    class Holder extends RecyclerView.ViewHolder{

        //Nhóm views của người dùng đăng nhập hiện tại để comment
        CircleImageView imgProfile;

        EditText edtComment;

        TextView txtName, txtTime;

        ImageButton btnSend;

        LinearLayout linearLayout;
        //

        public Holder(@NonNull View itemView) {
            super(itemView);

            //init views
            imgProfile    = itemView.findViewById(R.id.imgProfileComment);
            txtName       = itemView.findViewById(R.id.txtNameComment);
            txtTime       = itemView.findViewById(R.id.txtTimeComment);
            edtComment    = itemView.findViewById(R.id.edtComment);
            btnSend       = itemView.findViewById(R.id.btnSendComment);
            linearLayout  = itemView.findViewById(R.id.linearLayoutComment);
        }
    }

}

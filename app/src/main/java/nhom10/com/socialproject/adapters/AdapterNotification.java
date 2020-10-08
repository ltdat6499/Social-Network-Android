package nhom10.com.socialproject.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.models.User;

public class AdapterNotification extends RecyclerView.Adapter<AdapterNotification.Holder>  {

    FirebaseAuth mAuth;

    FirebaseUser mUser;

    DatabaseReference mReferenceFriend;

    DatabaseReference mReferenceMe;

    Context mContext;

    List<User> userList;

    public AdapterNotification(Context mContext, List<User> userList) {
        this.mContext = mContext;
        this.userList = userList;
    }


    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layout (row_user.xml)
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.row_accept_friend,viewGroup,false);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mReferenceFriend = FirebaseDatabase.getInstance().getReference("User");
        mReferenceMe = FirebaseDatabase.getInstance().getReference("User");

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int i) {
        //get data
        final String hisUid = userList.get(i).getUid();
        String userImage    = userList.get(i).getImage();
        String userName     = userList.get(i).getName();

        //set data
        holder.txtName.setText(userName);

        try {
            Glide.with(mContext).load(userImage)
                    .placeholder(R.drawable.ic_user_anonymous)
                    .into(holder.imgAvatar);
        }catch (Exception e){

        }

        //handle item clicked
        holder.btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptInvitation(hisUid,holder.btnDelete,holder.btnConfirm);
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                declineInvitation(hisUid,holder.btnDelete,holder.btnConfirm);
            }
        });
    }

    /**
     * Từ chối lời mời kết bạn
     */
    private void declineInvitation(final String hisUid, Button btnDelete, Button btnConfirm) {

        mReferenceMe.orderByChild("uid").equalTo(mUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            String friends = ds.child("friends").getValue().toString();

                            //Xác nhận hisUid  vào trong friends child của uid người dùng hiện tại
                            HashMap<String,Object> hashMap = new HashMap<>();
                            hashMap.put("friends",
                                    friends.replace("@"+hisUid+",",""));
                            ds.getRef().updateChildren(hashMap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        btnDelete.setText("Declined");
        btnDelete.setEnabled(false);
        btnConfirm.setVisibility(View.GONE);
    }


    /**
     * Chấp nhận lời mời kết bạn
     */
    private void acceptInvitation(final String hisUid, Button btnDelete, Button btnConfirm) {

        //Thêm uid của người dùng đăng nhập hiện tại vào trong friends child chứa hisUid
        mReferenceFriend.orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            String friends = ds.child("friends").getValue().toString();

                            friends = friends+ "" + mUser.getUid()+",";
                            //Xác nhận uid của người dùng hiện tại vào trong friends child của hisUid
                            HashMap<String,Object> hashMap = new HashMap<>();
                            hashMap.put("friends", friends);
                            ds.getRef().updateChildren(hashMap);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        mReferenceMe.orderByChild("uid").equalTo(mUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            String friends = ds.child("friends").getValue().toString();

                            //Xác nhận hisUid  vào trong friends child của uid người dùng hiện tại
                            HashMap<String,Object> hashMap = new HashMap<>();
                            hashMap.put("friends",
                                    friends.replace("@"+hisUid,hisUid.toString()));
                            ds.getRef().updateChildren(hashMap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
        btnConfirm.setText("Confirmed");
        btnConfirm.setEnabled(false);
        btnDelete.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    //View holder class
    class Holder extends RecyclerView.ViewHolder{

        CircleImageView imgAvatar;

        TextView txtName;

        Button btnConfirm, btnDelete;

        public Holder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.circularAvatar);
            txtName     = itemView.findViewById(R.id.txtUserName);
            btnConfirm  = itemView.findViewById(R.id.btnConfirm);
            btnDelete   = itemView.findViewById(R.id.btnDelete);
        }
    }

}

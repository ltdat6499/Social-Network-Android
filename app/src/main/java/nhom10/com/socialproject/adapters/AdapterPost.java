package nhom10.com.socialproject.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nhom10.com.socialproject.R;
import nhom10.com.socialproject.activity.AddPostActivity;
import nhom10.com.socialproject.models.Post;
import nhom10.com.socialproject.models.User;
import nhom10.com.socialproject.services.SocialNetwork;

public class AdapterPost extends RecyclerView.Adapter<AdapterPost.Holder> {

    private static final int LAYOUT_CREATE_POST_REQUEST = 0;

    private static final int LAYOUT_VIEW_POST_REQUEST = 1;

    private List<Holder> holderList = new ArrayList<>();

    private Context mContext;

    private List<Post> postList;

    public void setPostList(List<Post> postList) {
        this.postList = postList;
    }

    public AdapterPost(Context mContext, List<Post> postList) {
        this.mContext = mContext;
        this.postList = postList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //Tạo row news feed
        if(i == LAYOUT_VIEW_POST_REQUEST ) {
            View view1 = LayoutInflater.from(mContext).inflate(R.layout.row_news_feed, viewGroup, false);
            return new Holder(view1);
        }else {
            View view2 = LayoutInflater.from(mContext).inflate(R.layout.row_poster,viewGroup,false);
            return new Holder(view2);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == postList.size() - 1 ){
            return LAYOUT_CREATE_POST_REQUEST;
        }
        return LAYOUT_VIEW_POST_REQUEST;
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int i) {

        final Post post = postList.get(i);

        //get data
        final String uid = postList.get(i).getUid();
        if (i != (postList.size() - 1)) {
            User user = SocialNetwork.getUser(uid);
            String uEmail = user.getEmail();
            String uName = user.getName();
            String uDp = user.getImage();
            final String pId = postList.get(i).getpId();
            String pStatus = postList.get(i).getpStatus();
            final String pImage = postList.get(i).getpImage();
            String pTime = postList.get(i).getpTime();
            String pMode = postList.get(i).getpMode();
            final String pLike = postList.get(i).getpLike();
            String pComment = postList.get(i).getpComment();

            //format time
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTimeInMillis(Long.parseLong(pTime));
            String timePost = DateFormat.format("yyyy/MM/dd hh:mm aa", calendar).toString();

            //Kiểm tra người dùng đã like hay chưa
            if(pLike.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                holder.btnLike.setImageResource(R.drawable.ic_like_on);
                holder.txtLikePost.setTextColor(Color.BLUE);
            }else{
                holder.btnLike.setImageResource(R.drawable.ic_like_off);
                holder.txtLikePost.setTextColor(Color.BLACK);
            }

            // Đếm số lượng like
            int likeCount = getNumLikeOfPost(pLike);
            holder.txtLikeCount.setText(likeCount+"");

            // Đếm số lượng comment
            int commentCount = getNumCommentOfPost(pComment);
            holder.txtCmtCount.setText(commentCount+" comments 0 share");
            //set user image
            try {
                Picasso.get().load(uDp)
                        .placeholder(R.drawable.ic_user_anonymous).into(holder.imgProfile);
            } catch (Exception e) {

            }

            //set data
            holder.txtName.setText(uName);
            holder.txtTime.setText(timePost);
            holder.txtStatus.setText(pStatus);

            //set post image
            if (pImage.equals("noImage")) {
                holder.imgPost.setVisibility(View.GONE);
            } else {
                    try {
                        Picasso.get().load(pImage).into(holder.imgPost);
                        holder.imgPost.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        holder.imgPost.setVisibility(View.GONE);
                    }
            }

            holder.btnMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMoreOptions(holder.btnMore, uid, pId, pImage);
                }
            });

            //handle button like click
            holder.linearLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseDatabase.getInstance()
                                .getReference("Posts")
                                .orderByChild("pId").equalTo(pId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                            String pLike = ds.child("pLike").getValue().toString();
                                            //Người dùng đã like, ta unlike
                                            if (pLike.contains(uid)) {
                                                pLike = pLike.replace(uid + ",", "");
                                            } else {
                                                pLike += uid + ",";
                                            }
                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            hashMap.put("pLike", pLike);
                                            ds.getRef().updateChildren(hashMap);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                }
            });

            //handle button share click
            holder.linearShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //chưa phát triển
                    Toast.makeText(mContext, "Coming soon", Toast.LENGTH_SHORT).show();
                }
            });

            //handle button comment click
            holder.linearComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SocialNetwork.navigateCommentListOf(post);
                }
            });

            holder.imgProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SocialNetwork.navigateProfile(uid);
                }
            });

        } else {
            try {
                Picasso
                        .get()
                        .load(SocialNetwork.findImageById(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                        .placeholder(R.drawable.ic_user_anonymous).into(holder.btnViewProfile);
            }catch (Exception e){

            }
            holder.btnViewProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SocialNetwork.navigateProfile(uid);
                }
            });

            //Xử lý đăng status lấy ảnh từ gallery
            holder.btnPickGallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            //Di chuyển đến activity post status
            holder.btnPostStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(new Intent(mContext, AddPostActivity.class));
                }
            });
        }

        if(SocialNetwork.isDarkMode) changeThemeDarkMode();

    }

    /**
     * @param pComment , danh sách comment
     * @return số lượng comment của post
     */
    private int getNumCommentOfPost(String pComment) {
        if(TextUtils.isEmpty(pComment))return 0;
        String[] nums = pComment.split(",");
        return nums.length;
    }

    /**
     * @param pLike ,chuỗi chứa số lượng uid like
     * @return số like
     */
    private int getNumLikeOfPost(String pLike) {
        if(TextUtils.isEmpty(pLike))return 0;
        String[] nums = pLike.split(",");
        return nums.length;
    }


    /**
     * @param btnMore , view sẽ hiển thị popup
     * @param uid, uid của người dùng
     * @param pId, uid của bài viết
     * @param pImage, image của bài viết
     *                Hàm hiển thị popup menu cho phép người dùng xóa bài viết của mình
     */
    private void showMoreOptions(ImageView btnMore, String uid,
                                 final String pId, final String pImage) {
        PopupMenu popupMenu = new PopupMenu(mContext,btnMore, Gravity.END);
        if(uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if(menuItem.getItemId() == 0){
                    deletePost(pId,pImage);
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * @param pId , id của post
     * @param pImage, đường dẫn image của bài viết
     *                Hàm kiểm tra để xóa bài viết có hình hoặc không có hình
     */
    private void deletePost(String pId, String pImage) {
        if(pImage.equals("noImage")){
            deletePostWithoutImage(pId,pImage);
        }else{
            deletePostWithImage(pId,pImage);
        }
    }

    /**
     * @param pId , id của bài viết
     * @param pImage, đường dẫn image của bài viết
     *                Hàm xóa bài viết với bài viết không có hình ảnh
     */
    private void deletePostWithImage(final String pId, String pImage) {
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Deleting...");
        //Steps:
        //1) Delete image using url
        //2) Delete from database using post id
        StorageReference storageReference =
                FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image deleted, delete from database
                        Query query =
                                FirebaseDatabase.getInstance().getReference("Posts")
                                        .orderByChild("pId").equalTo(pId);
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot ds:dataSnapshot.getChildren()){
                                    ds.getRef().removeValue();
                                }
                                Toast.makeText(mContext,
                                        "Deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
            }
        });

    }

    /**
     * @param pId , id của bài viết
     * @param pImage, đường dẫn image của bài viết
     *                Hàm xóa bài viết với bài viết có hình ảnh
     */
    private void deletePostWithoutImage(String pId, String pImage) {
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Deleting...");

        //image deleted, delete from database
        Query query = FirebaseDatabase.getInstance().getReference("Posts")
                .orderByChild("pId").equalTo(pId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                Toast.makeText(mContext,
                        "Deleted successfully",
                        Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //View holder
    class Holder extends RecyclerView.ViewHolder{

        //UI from row_news_feed
        ImageView imgProfile,imgPost, btnMore;

        TextView txtName, txtTime, txtStatus, txtLikeCount, txtCmtCount,txtLikePost, txtShare,
                txtCommentPost;

        LinearLayout linearLike, linearComment, linearShare;

        RelativeLayout relativeNewsFeedLayout;

        ImageView btnLike,btnComment,btnShare;

        //UI poster
        EditText btnPostStatus;

        ImageView btnPickGallery, btnViewProfile;

        public Holder(@NonNull View itemView) {
            super(itemView);

            //init views
            imgProfile    = itemView.findViewById(R.id.imgProfile);
            imgPost       = itemView.findViewById(R.id.imgPost);
            btnMore       = itemView.findViewById(R.id.btnMore);
            txtName       = itemView.findViewById(R.id.txtNameOfPoster);
            txtTime       = itemView.findViewById(R.id.txtTimePost);
            txtStatus     = itemView.findViewById(R.id.txtStatus);
            txtLikeCount  = itemView.findViewById(R.id.txtLikeCount);
            txtLikePost   = itemView.findViewById(R.id.txtLikePost);
            txtCmtCount   = itemView.findViewById(R.id.txtCmtCount);
            linearLike    = itemView.findViewById(R.id.linearLike);
            txtShare      = itemView.findViewById(R.id.txtShare);
            btnLike       = itemView.findViewById(R.id.btnLikePost);
            btnComment    = itemView.findViewById(R.id.btnComment);
            btnShare      = itemView.findViewById(R.id.btnShare);
            linearComment = itemView.findViewById(R.id.linearComment);
            linearShare   = itemView.findViewById(R.id.linearShare);
            txtCommentPost = itemView.findViewById(R.id.txtCommentPost);
            relativeNewsFeedLayout = itemView.findViewById(R.id.relativeNewsFeedLayout);
            //init views
            btnPostStatus  = itemView.findViewById(R.id.btnPostStatus);
            btnPickGallery = itemView.findViewById(R.id.btnPickGallery);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
        }
    }

    /**
     * Hàm thay đổi giao diện dark mode
     */
    public void changeThemeDarkMode(){
        for(int i = 0;i<holderList.size();i++){
            holderList.get(i)
                    .relativeNewsFeedLayout
                    .setBackgroundResource(R.drawable.custom_background_view_post);

            holderList.get(i).txtCmtCount.setTextColor(Color.WHITE);
            holderList.get(i).txtLikeCount.setTextColor(Color.WHITE);
            holderList.get(i).txtLikePost.setTextColor(Color.WHITE);
            holderList.get(i).txtStatus.setTextColor(Color.WHITE);
            holderList.get(i).txtName.setTextColor(Color.WHITE);
            holderList.get(i).txtTime.setTextColor(Color.WHITE);
            holderList.get(i).txtShare.setTextColor(Color.WHITE);
            holderList.get(i).txtCommentPost.setTextColor(Color.WHITE);

            Picasso.get().load(R.drawable.ic_like_dark_off).into(holderList.get(i).btnLike);
            Picasso.get().load(R.drawable.ic_comment_dark).into(holderList.get(i).btnComment);
            Picasso.get().load(R.drawable.ic_share_dark).into(holderList.get(i).btnShare);
        }
    }

    /**
     * Hàm đổi giao diện light mode
     */
    public void changeThemeDefault(){
        for(int i = 0;i<holderList.size();i++){
            holderList.get(i)
                    .relativeNewsFeedLayout
                    .setBackgroundColor(Color.WHITE);

            holderList.get(i).txtCmtCount
                    .setTextColor(holderList.get(i).txtCmtCount.getTextColors().getDefaultColor());

            holderList.get(i).txtLikeCount
                    .setTextColor(holderList.get(i).txtLikeCount.getTextColors().getDefaultColor());

            holderList.get(i).txtLikePost
                    .setTextColor(holderList.get(i).txtLikePost.getTextColors().getDefaultColor());

            holderList.get(i).txtStatus
                    .setTextColor(holderList.get(i).txtStatus.getTextColors().getDefaultColor());

            holderList.get(i).txtName
                    .setTextColor(holderList.get(i).txtName.getTextColors().getDefaultColor());

            holderList.get(i).txtTime
                    .setTextColor(holderList.get(i).txtTime.getTextColors().getDefaultColor());

            holderList.get(i).txtShare
                    .setTextColor(holderList.get(i).txtShare.getTextColors().getDefaultColor());

            holderList.get(i).txtCommentPost
                    .setTextColor(holderList.get(i).txtCommentPost.getTextColors().getDefaultColor());

            Picasso.get().load(R.drawable.ic_like_on).into(holderList.get(i).btnLike);
            Picasso.get().load(R.drawable.ic_comment).into(holderList.get(i).btnComment);
            Picasso.get().load(R.drawable.ic_share).into(holderList.get(i).btnShare);
        }
    }
}

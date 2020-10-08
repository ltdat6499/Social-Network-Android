package nhom10.com.socialproject.adapters;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.models.User;
import nhom10.com.socialproject.services.SocialNetwork;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.Holder> {

    private Context mContext;

    private List<User> userList;

    private List<Holder> holderList = new ArrayList<>();

    public AdapterUser(Context mContext, List<User> userList) {
        this.mContext = mContext;
        this.userList = userList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layout (row_user.xml)
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.row_users,viewGroup,false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int i) {
        holderList.add(holder);
        //get data
        final String hisUID = userList.get(i).getUid();
        String userImage    = userList.get(i).getImage();
        String userName     = userList.get(i).getName();
        String userEmail    = userList.get(i).getEmail();

        //set data
        holder.txtName.setText(userName);
        holder.txtEmail.setText(userEmail);

        try {
            Glide.with(mContext)
                    .load(userImage)
                    .placeholder(R.drawable.ic_user_anonymous)
                    .into(holder.imgAvatar);
        }catch (Exception e){
            Glide.with(mContext).load(R.drawable.ic_user_anonymous).into(holder.imgAvatar);
        }

        //handle item clicked
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SocialNetwork.navigateProfile(hisUID);

            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    //View holder class
    class Holder extends RecyclerView.ViewHolder{
        CircleImageView imgAvatar;

        TextView txtName, txtEmail;

        CardView cardView;

        public Holder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.circularAvatar);
            txtEmail  = itemView.findViewById(R.id.txtEmail);
            txtName   = itemView.findViewById(R.id.txtUserName);
            cardView  = itemView.findViewById(R.id.cardView);
        }
    }

    public void changeThemeDarkMode() {
        for(int i = 0;i<holderList.size();i++){
            holderList.get(i).cardView
                    .setCardBackgroundColor(R.drawable.custom_background_row_user_dark);
            holderList.get(i).txtEmail.setTextColor(Color.WHITE);
            holderList.get(i).txtName.setTextColor(Color.WHITE);
        }
    }

    public void changeThemeDefault() {
        for(int i = 0;i<holderList.size();i++){
            holderList.get(i).cardView.setCardBackgroundColor(R.drawable.custom_card_view);
            holderList.get(i).txtEmail
                    .setTextColor(holderList.get(i).txtEmail.getTextColors().getDefaultColor());
            holderList.get(i).txtName
                    .setTextColor(holderList.get(i).txtName.getTextColors().getDefaultColor());
        }
    }
}

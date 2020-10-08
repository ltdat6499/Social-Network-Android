package nhom10.com.socialproject.activity;

import android.app.Activity;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.RadioButton;

import nhom10.com.socialproject.R;

/**
 * Lớp chọn chế độ cho post mỗi khi người dùng đăng post
 */
public class PrivacyActivity extends AppCompatActivity implements View.OnClickListener {

    private RadioButton rdPublic, rdFriendsAll, rdFriendsExcept, rdOnlyMe;

    private String typeDisplay = "Public";

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);
        initializeUI();
    }

    /**
     * Khời tạo và ánh xạ các views
     */
    private void initializeUI(){
        toolbar = findViewById(R.id.toolbarAddPost);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundResource(R.drawable.custom_background_profile2);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rdPublic = findViewById(R.id.rdPublic);
        rdFriendsAll = findViewById(R.id.rdFriendsAll);
        rdFriendsExcept = findViewById(R.id.rdFriendsExcept);
        rdOnlyMe = findViewById(R.id.rdOnlyMe);
        rdPublic.setOnClickListener(this);
        rdFriendsAll.setOnClickListener(this);
        rdFriendsExcept.setOnClickListener(this);
        rdOnlyMe.setOnClickListener(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        responseResult();
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        responseResult();
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.rdPublic){
            typeDisplay = "Public";
        }else if(v.getId() == R.id.rdFriendsAll){
            typeDisplay = "Friends";
        }else if(v.getId() == R.id.rdFriendsExcept){
            //Xử lý sau
        }else if(v.getId() == R.id.rdOnlyMe){
            typeDisplay = "Only Me";
        }
    }

    /**
     * Trả về kết quả của chế độ đăng bài
     */
    private void responseResult(){
        Intent intent = new Intent();
        intent.putExtra("PRIVACY",typeDisplay);
        setResult(Activity.RESULT_OK,intent);
        finish();
    }
}

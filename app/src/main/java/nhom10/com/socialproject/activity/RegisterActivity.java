package nhom10.com.socialproject.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import nhom10.com.socialproject.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail,edtPassword,edtPasswordRetype;

    private Button btnRegister;

    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initializeUI();
    }


    /**
     * Hàm ánh xạ vắt bắt sự kiện cho views
     */
    private void initializeUI() {
        edtEmail          = findViewById(R.id.edtEmail);
        edtPassword       = findViewById(R.id.edtPassword);
        edtPasswordRetype = findViewById(R.id.edtPasswordRetype);
        btnRegister       = findViewById(R.id.btnRegisterAccount);
        progressDialog    = new ProgressDialog(this);
        progressDialog.setMessage("Creating user...");
        //Khởi tạo firebase authentication
        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                String passwordRetype = edtPasswordRetype.getText().toString().trim();

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    edtEmail.setError("Invalid email");
                    edtEmail.setFocusable(true);
                    return;
                }
                if (password.length() < 6 ) {
                    edtPassword.setError("Password length at least 6 characters");
                    edtPassword.setFocusable(true);
                    return;
                }

                if (!password.equals(passwordRetype)) {
                    edtPasswordRetype.setError("Current password does not match");
                    edtPasswordRetype.setFocusable(true);
                    return;
                }
                registerUser(email,password);
            }
        });
    }

    /**
     * @param email
     * @param password
     *                Hàm đăng kí tài khoản user
     *
     */
    private void registerUser(String email, String password){
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng kí thành công, cập nhật giao diện với thông tin người dùng
                            FirebaseUser user = mAuth.getCurrentUser();

                            //Get info user
                            String email = user.getEmail();
                            String uid = user.getUid();
                            HashMap<Object,String> hashMap = new HashMap<>();
                            hashMap.put("email",email);
                            hashMap.put("uid",uid);
                            hashMap.put("name","");
                            hashMap.put("onlineStatus","online");
                            hashMap.put("typingTo","noOne");
                            hashMap.put("phone","");
                            hashMap.put("image","");
                            hashMap.put("cover","");
                            hashMap.put("friends","");
                            hashMap.put("follow","");

                            FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
                            //Lưu vào đường dẫn có tên là User
                            DatabaseReference reference = mDatabase.getReference("User");
                            //Tạo đường dẫn uid, đặt dữ liệu vào database
                            reference.child(uid).setValue(hashMap);
                            Toast.makeText(RegisterActivity.this,
                                    "Registed "+user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this,
                                    DashboardActivity.class));
                            finish();
                        } else {
                            // Đăp kí thất bại, báo lỗi cho người dùng
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        progressDialog.dismiss();
                        // ...
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RegisterActivity.this, e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}

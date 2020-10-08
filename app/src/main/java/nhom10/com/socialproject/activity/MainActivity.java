package nhom10.com.socialproject.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import nhom10.com.socialproject.R;
import nhom10.com.socialproject.services.SocialNetwork;
import nhom10.com.socialproject.services.SocialServices;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int DELAY_TIME = 300;

    private static final int RC_SIGN_IN = 6969;

    private Button btnLogin, btnRegister;

    private EditText edtMail, edtPassword;

    private TextView txtForgotPassword, txtOpen;

    private CircleImageView imgOpen;

    private ImageView imageViewTransaction;

    private RelativeLayout relativeLayoutOpen, relativeLayoutLogin;

    // UI đăng nhập bằng google
    private SignInButton btnLoginEmail;

    private GoogleSignInClient mGoogleSignInClient;
    //
    private FirebaseAuth mAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapViews();
        initializeService();
        initAnimation();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser() != null){
            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            finish();
        }
    }

    /**
     * Hàm khởi tạo animation load app
     */
    private void initAnimation(){
        final Animation animTransaction = AnimationUtils.loadAnimation(this,R.anim.anim_transaction);
        final Animation animRotate = AnimationUtils.loadAnimation(this,R.anim.anim_rotate);
        txtOpen.startAnimation(animTransaction);
        imageViewTransaction.startAnimation(animTransaction);
        imgOpen.startAnimation(animRotate);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                relativeLayoutOpen.setVisibility(View.GONE);
                relativeLayoutLogin.setVisibility(View.VISIBLE);
                animTransaction.cancel();
                animRotate.cancel();
            }
        },2600);
    }

    /**
     * Hàm ánh xạ vắt bắt sự kiện cho views
     */
    private void mapViews() {
        imageViewTransaction = findViewById(R.id.imageViewTransaction);
        relativeLayoutOpen   = findViewById(R.id.relativeLayoutOpen);
        relativeLayoutLogin  = findViewById(R.id.relativeLayoutLogin);
        imgOpen              = findViewById(R.id.imageViewOpen);
        txtOpen              = findViewById(R.id.textViewOpen);
        btnLogin             = findViewById(R.id.btnLogin);
        btnRegister          = findViewById(R.id.btnRegister);
        edtMail              = findViewById(R.id.edtEmail);
        edtPassword          = findViewById(R.id.edtPassword);
        txtForgotPassword    = findViewById(R.id.txtForgotPasword);
        progressDialog       = new ProgressDialog(this);
        btnLoginEmail        = findViewById(R.id.btnLoginGmail);
        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
        txtForgotPassword.setOnClickListener(this);
        btnLoginEmail.setOnClickListener(this);
        progressDialog.setMessage("Loading");

        // Cấu hình để đăng nhập bằng google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnRegister) {
            //Đăng tài khoản
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        } else if (v.getId() == R.id.btnLogin) {
            //Đăng nhập
            String email = edtMail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtMail.setError("Invalid email");
                edtMail.setFocusable(true);
            } else {
                loginUser(email, password);
            }
        } else if (v.getId() == R.id.txtForgotPasword) {
            showRecoverPasswordDialog();
        } else if (v.getId() == R.id.btnLoginGmail) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    /**
     * @param email     , địa chỉ email
     * @param password, mật khẩu
     *                  Hàm đăng nhập tài khoản với tài khoản đã tồn tại
     */
    private void loginUser(String email, String password) {
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            new Handler().postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            startActivity(new Intent(MainActivity.this,
                                                    DashboardActivity.class));
                                            finish();
                                        }
                                    }, DELAY_TIME);


                        } else {
                            // Sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    /**
     * Hàm khởi tạo dialog lấy lại mật khẩu
     */
    private void showRecoverPasswordDialog() {
        // Tạo dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");

        LinearLayout linearLayout = new LinearLayout(this);
        final EditText editText = new EditText(this);
        editText.setHint("Your Email");
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        linearLayout.addView(editText);
        linearLayout.setPadding(10, 10, 10, 10);
        builder.setView(linearLayout);

        // Tạo button cho dialog và bắt sự kiện
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = editText.getText().toString().trim();
                recoverUser(email);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /**
     * @param email , email cần lấy
     *              Hàm lấy lại mật khẩu
     */
    private void recoverUser(String email) {
        progressDialog.setMessage("Sending email...");
        progressDialog.show();
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed...", Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hàm nhận kết quả trả về khi đăng nhập bằng google
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                progressDialog.setMessage("Loading...");
                progressDialog.show();
                fireBaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
    }

    private void fireBaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            //Nếu là tài khoản mới
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                //Get info user
                                String email = user.getEmail();
                                String uid = user.getUid();
                                HashMap<Object, String> hashMap = new HashMap<>();
                                hashMap.put("email", email);
                                hashMap.put("uid", uid);
                                hashMap.put("name", "");
                                hashMap.put("onlineStatus", "online");
                                hashMap.put("typingTo", "noOne");
                                hashMap.put("phone", "");
                                hashMap.put("image", "");
                                hashMap.put("cover", "");
                                hashMap.put("friends", "");
                                hashMap.put("follow", "");

                                FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
                                //Lưu vào đường dẫn có tên là User
                                DatabaseReference reference = mDatabase.getReference("User");
                                //Tạo đường dẫn uid, đặt dữ liệu vào database
                                reference.child(uid).setValue(hashMap);
                            }
                            new Handler().postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            startActivity(new Intent(MainActivity.this,
                                                    DashboardActivity.class));
                                            finish();
                                        }
                                    }, DELAY_TIME);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this,
                                    "Failed...",
                                    Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SocialServices.LocalBinder binder = (SocialServices.LocalBinder) service;
            SocialNetwork.socialServices = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * Hàm gọi khởi tạo dịch vụ
     */
    private void initializeService(){
        if(SocialNetwork.socialServices == null) {
            SocialNetwork.startService(this, serviceConnection);
        }
        // Bound Service
        Intent intent = new Intent(this, SocialServices.class);
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onStop() {
        super.onStop();
        //this.unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unbindService(serviceConnection);
    }
}

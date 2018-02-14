package migueldaipre.com.acaiapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Model.User;

public class SignUp extends AppCompatActivity {

    MaterialEditText edtName,edtPhone,edtPassword,edtSecureCode;
    Button btnSignUp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edtName = (MaterialEditText)findViewById(R.id.edtName);
        edtPhone = (MaterialEditText)findViewById(R.id.edtPhone);
        edtPassword = (MaterialEditText)findViewById(R.id.edtPassword);
        edtSecureCode = (MaterialEditText)findViewById(R.id.edtSecureCode);

        btnSignUp = (Button)findViewById(R.id.btnSignUp);

        //init firebase
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("User");

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Common.isConnectedToInternet(getBaseContext())) {

                    if (TextUtils.isEmpty(edtPhone.getText().toString())) {
                        Toast.makeText(SignUp.this, "Please Enter Your Phone!!!", Toast.LENGTH_SHORT).show();
                    }
                    else if (TextUtils.isEmpty(edtName.getText().toString())) {
                        Toast.makeText(SignUp.this, "Please Enter Your Name!!!", Toast.LENGTH_SHORT).show();
                    }
                    else if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                        Toast.makeText(SignUp.this, "Please Enter Your Password!!!", Toast.LENGTH_SHORT).show();
                    }
                    else if (TextUtils.isEmpty(edtSecureCode.getText().toString())) {
                        Toast.makeText(SignUp.this, "Please Enter Your Secure-Code!!!", Toast.LENGTH_SHORT).show();
                    }
                    else if (edtPassword.getText().toString().length() < 8) {
                        Toast.makeText(SignUp.this, "Password Too Small!!! Min 8 Chars...", Toast.LENGTH_SHORT).show();
                    }
                    else if (edtSecureCode.getText().toString().length() < 6) {
                        Toast.makeText(SignUp.this, "Secure-Code Too Small!!! Min 6 Chars...", Toast.LENGTH_SHORT).show();
                    }
                    else {

                        //add progressBar
                        final ProgressDialog mDialog = new ProgressDialog(SignUp.this);
                        mDialog.setTitle("USER SIGN-UP");
                        mDialog.setMessage("Please wait! while we Register Your Account!!");
                        mDialog.setCanceledOnTouchOutside(false);
                        mDialog.show();

                        table_user.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                //check if user already exists
                                if (dataSnapshot.child(edtPhone.getText().toString()).exists()) {
                                    mDialog.dismiss();
                                    Toast.makeText(SignUp.this, "Already Registered!!!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                                else {
                                    mDialog.dismiss();
                                    User user = new User(edtName.getText().toString(), edtPassword.getText().toString(),
                                            edtSecureCode.getText().toString());

                                    table_user.child(edtPhone.getText().toString()).setValue(user);

                                    Toast.makeText(SignUp.this, "You are good to Go..", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                else {
                    Toast.makeText(SignUp.this, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });
    }
}

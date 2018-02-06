package migueldaipre.com.acaiapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rey.material.widget.CheckBox;

import io.paperdb.Paper;
import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Model.User;

public class SignIn extends AppCompatActivity {

    EditText edtPhone,edtPassword;
    Button btnSignIn;
    TextView txtForgotPwd;

    CheckBox ckbRemember;

    FirebaseDatabase database;
    DatabaseReference table_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        edtPhone = (MaterialEditText)findViewById(R.id.edtPhone);
        edtPassword = (MaterialEditText)findViewById(R.id.edtPassword);
        txtForgotPwd = (TextView)findViewById(R.id.txtForgotPwd);

        btnSignIn = (Button)findViewById(R.id.btnSignIn);
        ckbRemember = (CheckBox)findViewById(R.id.ckbRemember);

        //init paper
        Paper.init(this);

        database = FirebaseDatabase.getInstance();
        table_user = database.getReference("User");

        txtForgotPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showForgotPwdDialog();
            }
        });


        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Common.isConnectedToInternet(getBaseContext())) {

                    //save user and password
                    if (ckbRemember.isChecked()) {
                        Paper.book().write(Common.USER_KEY, edtPhone.getText().toString());
                        Paper.book().write(Common.PWD_KEY, edtPassword.getText().toString());
                    }

                    if (TextUtils.isEmpty(edtPhone.getText().toString())) {
                        Toast.makeText(SignIn.this, "Please Enter Your Phone!!!", Toast.LENGTH_SHORT).show();
                    }
                    else if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                        Toast.makeText(SignIn.this, "Please Enter Your Password!!!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        //add progressBar
                        final ProgressDialog mDialog = new ProgressDialog(SignIn.this);
                        mDialog.setTitle("USER LOG-IN");
                        mDialog.setMessage("Please wait! while we check your credential!!");
                        mDialog.setCanceledOnTouchOutside(false);
                        mDialog.show();

                        table_user.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                table_user.addValueEventListener(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        //check if user doesn't exist in database
                                        if (dataSnapshot.child(edtPhone.getText().toString()).exists()) {

                                            //get user information
                                            mDialog.dismiss();
                                            User user = dataSnapshot.child(edtPhone.getText().toString()).getValue(User.class);
                                            user.setPhone(edtPhone.getText().toString());//set phone

                                            if (user.getPassword().equals(edtPassword.getText().toString())) {

                                                mDialog.dismiss();
                                                Toast.makeText(SignIn.this, "Welcome! Sign In Successful!!", Toast.LENGTH_SHORT).show();

                                                Intent homeIntent = new Intent(SignIn.this, Home.class);
                                                Common.currentUser = user;
                                                startActivity(homeIntent);
                                                finish();

                                                table_user.removeEventListener(this);
                                            }
                                            else {
                                                mDialog.dismiss();
                                                Toast.makeText(SignIn.this, "Wrong Password!!!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        else {
                                            mDialog.dismiss();
                                            Toast.makeText(SignIn.this, "Not Registered Yet! Kindly Register", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                else{
                        Toast.makeText(SignIn.this, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }

            }
        });
    }

    private void showForgotPwdDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");
        builder.setMessage("Enter Your Secure Code");

        LayoutInflater inflater = this.getLayoutInflater();
        View forgot_view = inflater.inflate(R.layout.forgot_password_layout,null);

        builder.setView(forgot_view);
        builder.setIcon(R.drawable.ic_security_black_24dp);

        final MaterialEditText edtPhone = (MaterialEditText)forgot_view.findViewById(R.id.edtPhone);
        final MaterialEditText edtSecureCode = (MaterialEditText)forgot_view.findViewById(R.id.txtSecureCode);

        builder.setPositiveButton("SHOW", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                table_user.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User user = dataSnapshot.child(edtPhone.getText().toString()).getValue(User.class);

                         if (user.getSecureCode().equals(edtSecureCode.getText().toString())) {
                                Toast.makeText(SignIn.this, "Your Password : " + user.getPassword(), Toast.LENGTH_LONG).show();
                            }
                            else {
                                Toast.makeText(SignIn.this, "Wrong Secure-Code!!", Toast.LENGTH_SHORT).show();
                            }
                        }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }
}

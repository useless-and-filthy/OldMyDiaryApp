package com.akigon.mydiary;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    private ImageView back;
    private Button signUp;
    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText passwordagain;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        progressDialog = new ProgressDialog(SignUpActivity.this);

        back = findViewById(R.id.back);
        signUp = findViewById(R.id.signup);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        passwordagain = findViewById(R.id.passwordagain);

        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && !(source.charAt(i) == '@') && !(source.charAt(i) == '#') && !(source.charAt(i) == '_')) {
                        return "";
                    }
                }
                return null;
            }
        };
        password.setFilters(new InputFilter[]{filter});
        passwordagain.setFilters(new InputFilter[]{filter});
        signUp.setOnClickListener(v -> {
            if (password.getText().toString().equals(passwordagain.getText().toString()) && !TextUtils.isEmpty(email.getText().toString()) && !TextUtils.isEmpty(password.getText().toString()))
                signUp(email.getText().toString(), password.getText().toString());
            else
                Toast.makeText(this, "Make sure that the values you entered are correct.", Toast.LENGTH_SHORT).show();
        });

        back.setOnClickListener(v -> finish());

    }

    private void signUp(String email, String password) {
        progressDialog.show();
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    showAlert("Sign Up Successful", "You are signed in. Let's get started!");
                } else {
                    Toast.makeText(SignUpActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
        AlertDialog ok = builder.create();
        ok.show();
    }
}


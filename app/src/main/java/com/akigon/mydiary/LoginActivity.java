package com.akigon.mydiary;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import static com.akigon.mydiary.BFunctions.LOGIN_BLOCKED_TILL;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText email;
    private TextInputEditText password;
    private Button login;
    private TextView navigatesignup;
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(LoginActivity.this);
        prefs = getSharedPreferences("meta_data", Context.MODE_PRIVATE);
        email = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        navigatesignup = findViewById(R.id.navigatesignup);

        login.setOnClickListener(v -> performLogin(email.getText().toString(), password.getText().toString()));

        navigatesignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        findViewById(R.id.textForgotPassword).setOnClickListener(v -> {
            if (!Objects.requireNonNull(email.getText()).toString().isEmpty()) {
                progressDialog.show();
                FirebaseAuth.getInstance().sendPasswordResetEmail(email.getText().toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        showToast("Reset Email Sent");
                    }
                });
            } else {
                showToast("Fill email address and then click Forgot password");
            }
        });


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
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void performLogin(String username, String password) {
        if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
            progressDialog.show();
            mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        showAlert("Error", task.getException().getMessage());
                    }
                }
            });
        } else {
            showToast("Invalid credentials");
        }
    }


    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog ok = builder.create();
        ok.show();
    }
}


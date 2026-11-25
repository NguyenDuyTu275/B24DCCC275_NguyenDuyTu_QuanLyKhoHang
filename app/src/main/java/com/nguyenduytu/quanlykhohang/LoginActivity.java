package com.nguyenduytu.quanlykhohang;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Màn hình 1: Đăng nhập. Tài khoản được kiểm tra bằng truy vấn bảng users trong SQLite.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvError;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_login);
        AppUtils.applySystemBarInsets(findViewById(R.id.main));

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> checkLogin());

        // Nhấn "Xong" trên bàn phím ở ô mật khẩu cũng đăng nhập
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkLogin();
                return true;
            }
            return false;
        });

        // Ẩn thông báo lỗi khi người dùng sửa lại thông tin
        TextWatcher hideErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        etUsername.addTextChangedListener(hideErrorWatcher);
        etPassword.addTextChangedListener(hideErrorWatcher);
    }

    /** Kiểm tra thông tin đăng nhập với bảng users trong cơ sở dữ liệu. */
    private void checkLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.trim().isEmpty()) {
            showError(getString(R.string.error_login_empty));
            return;
        }

        if (dbHelper.checkLogin(username, password)) {
            // Đúng tài khoản: sang danh sách sản phẩm, finish() để nút Back không quay lại đây
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            showError(getString(R.string.error_login_wrong));
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}

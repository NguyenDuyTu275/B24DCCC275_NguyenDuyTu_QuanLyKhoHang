package com.nguyenduytu.quanlykhohang;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Màn hình 3: Thêm sản phẩm. Kiểm tra dữ liệu nhập, lưu vào SQLite và trả về màn hình danh sách.
 */
public class AddProductActivity extends AppCompatActivity {

    /** Khóa Intent trả id sản phẩm vừa thêm về MainActivity. */
    public static final String EXTRA_NEW_PRODUCT_ID = "new_product_id";

    private EditText etName;
    private EditText etPrice;
    private EditText etQuantity;
    private Spinner spCategory;
    private SwitchCompat swActive;
    private EditText etDescription;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_add_product);
        AppUtils.applySystemBarInsets(findViewById(R.id.main));

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etQuantity = findViewById(R.id.etQuantity);
        spCategory = findViewById(R.id.spCategory);
        swActive = findViewById(R.id.swActive);
        etDescription = findViewById(R.id.etDescription);
        Button btnSave = findViewById(R.id.btnSave);

        // Spinner loại sản phẩm: Điện tử, Thực phẩm, Văn phòng phẩm, Gia dụng, Khác
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.product_categories, R.layout.item_spinner);
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spCategory.setAdapter(categoryAdapter);

        btnSave.setOnClickListener(v -> saveProduct());
    }

    /** Kiểm tra dữ liệu, lưu sản phẩm vào cơ sở dữ liệu và quay về danh sách. */
    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        boolean active = swActive.isChecked();
        String description = etDescription.getText().toString().trim();

        // 1. Tên sản phẩm không được để trống
        if (name.isEmpty()) {
            showFieldError(etName, getString(R.string.error_name_empty));
            return;
        }

        // 2. Giá bán: không rỗng, là số, lớn hơn 0
        if (priceText.isEmpty()) {
            showFieldError(etPrice, getString(R.string.error_price_empty));
            return;
        }
        long price;
        try {
            price = Long.parseLong(priceText);
        } catch (NumberFormatException e) {
            showFieldError(etPrice, getString(R.string.error_price_invalid));
            return;
        }
        if (price <= 0) {
            showFieldError(etPrice, getString(R.string.error_price_not_positive));
            return;
        }

        // 3. Số lượng: không rỗng, là số nguyên, không âm
        if (quantityText.isEmpty()) {
            showFieldError(etQuantity, getString(R.string.error_quantity_empty));
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            showFieldError(etQuantity, getString(R.string.error_quantity_invalid));
            return;
        }
        if (quantity < 0) {
            showFieldError(etQuantity, getString(R.string.error_quantity_negative));
            return;
        }

        // 4. Lưu vào SQLite
        Product product = new Product(name, price, quantity, category, active, description);
        long newId = dbHelper.insertProduct(product);
        if (newId == -1) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.msg_product_added, Toast.LENGTH_SHORT).show();

        // Trả id về MainActivity; danh sách và thống kê được tải lại trong onResume()
        Intent result = new Intent();
        result.putExtra(EXTRA_NEW_PRODUCT_ID, (int) newId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}

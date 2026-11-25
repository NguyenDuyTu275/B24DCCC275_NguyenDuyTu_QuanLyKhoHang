package com.nguyenduytu.quanlykhohang;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Màn hình 4: Chi tiết sản phẩm. Nhận id qua Intent, truy vấn lại SQLite theo id để hiển thị;
 * cho phép nhập thêm 1, bán 1 và xóa sản phẩm.
 */
public class ProductDetailActivity extends AppCompatActivity {

    /** Khóa Intent truyền id sản phẩm từ MainActivity sang. */
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private DatabaseHelper dbHelper;
    private int productId;
    private Product product;

    private TextView tvName;
    private TextView tvCategory;
    private TextView tvStatus;
    private TextView tvPrice;
    private TextView tvQuantity;
    private TextView tvLowStock;
    private TextView tvStockValue;
    private TextView tvDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_product_detail);
        AppUtils.applySystemBarInsets(findViewById(R.id.main));

        dbHelper = new DatabaseHelper(this);
        productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tvName);
        tvCategory = findViewById(R.id.tvCategory);
        tvStatus = findViewById(R.id.tvStatus);
        tvPrice = findViewById(R.id.tvPrice);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvLowStock = findViewById(R.id.tvLowStock);
        tvStockValue = findViewById(R.id.tvStockValue);
        tvDescription = findViewById(R.id.tvDescription);
        Button btnImport = findViewById(R.id.btnImport);
        Button btnSell = findViewById(R.id.btnSell);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnDelete = findViewById(R.id.btnDelete);

        btnImport.setOnClickListener(v -> importOne());
        btnSell.setOnClickListener(v -> sellOne());
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> confirmDelete());

        loadProduct();
    }

    /** Truy vấn lại sản phẩm theo id và hiển thị. */
    private void loadProduct() {
        product = dbHelper.getProductById(productId);
        if (product == null) {
            Toast.makeText(this, R.string.error_product_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName.setText(product.getName());
        tvCategory.setText(product.getCategory());

        if (product.isActive()) {
            tvStatus.setText(R.string.status_active);
            tvStatus.setBackgroundResource(R.drawable.bg_chip_active);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            tvStatus.setText(R.string.status_inactive);
            tvStatus.setBackgroundResource(R.drawable.bg_chip_inactive);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.inactive_text));
        }

        tvPrice.setText(AppUtils.formatMoney(product.getPrice()));
        tvQuantity.setText(String.valueOf(product.getQuantity()));
        tvStockValue.setText(AppUtils.formatMoney(product.getStockValue()));

        boolean low = product.isLowStock();
        tvLowStock.setVisibility(low ? View.VISIBLE : View.GONE);
        tvQuantity.setTextColor(ContextCompat.getColor(this,
                low ? R.color.danger : R.color.text_primary));

        String description = product.getDescription();
        if (description == null || description.trim().isEmpty()) {
            tvDescription.setText(R.string.no_description);
            tvDescription.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        } else {
            tvDescription.setText(description);
            tvDescription.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    /** Nút "Nhập thêm 1": tăng số lượng thêm 1 và ghi vào cơ sở dữ liệu. */
    private void importOne() {
        if (dbHelper.updateQuantity(productId, product.getQuantity() + 1)) {
            loadProduct();
        } else {
            Toast.makeText(this, R.string.error_update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Nút "Bán 1": giảm số lượng đi 1; nếu đã bằng 0 thì báo "Hết hàng" và không giảm nữa. */
    private void sellOne() {
        if (product.getQuantity() == 0) {
            Toast.makeText(this, R.string.msg_out_of_stock, Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.updateQuantity(productId, product.getQuantity() - 1)) {
            loadProduct();
        } else {
            Toast.makeText(this, R.string.error_update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Hỏi xác nhận trước khi xóa sản phẩm. */
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, product.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteProduct())
                .show();
    }

    /** Xóa sản phẩm khỏi cơ sở dữ liệu rồi quay lại danh sách (MainActivity tự tải lại). */
    private void deleteProduct() {
        if (dbHelper.deleteProduct(productId)) {
            Toast.makeText(this, R.string.msg_product_deleted, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.error_delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}

package com.nguyenduytu.quanlykhohang;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Màn hình 2: Danh sách sản phẩm và thống kê kho.
 * Dữ liệu được tải lại từ SQLite trong onResume() mỗi khi quay về màn hình này
 * (sau khi thêm, nhập/bán hoặc xóa sản phẩm).
 */
public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ProductAdapter adapter;

    private RecyclerView rvProducts;
    private TextView tvEmpty;
    private TextView tvHint;
    private TextView tvTotalProducts;
    private TextView tvTotalValue;
    private TextView tvLowStock;

    /** id sản phẩm vừa thêm, dùng để cuộn tới sản phẩm đó sau khi tải lại danh sách. */
    private int pendingScrollToId = -1;

    private final ActivityResultLauncher<Intent> addProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    pendingScrollToId = result.getData()
                            .getIntExtra(AddProductActivity.EXTRA_NEW_PRODUCT_ID, -1);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_main);
        AppUtils.applySystemBarInsets(findViewById(R.id.main));

        dbHelper = new DatabaseHelper(this);

        rvProducts = findViewById(R.id.rvProducts);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvHint = findViewById(R.id.tvHint);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvLowStock = findViewById(R.id.tvLowStock);
        Button btnAddProduct = findViewById(R.id.btnAddProduct);

        // Nhấn vào một dòng: mở chi tiết, truyền id qua Intent
        adapter = new ProductAdapter(product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(adapter);

        btnAddProduct.setOnClickListener(v ->
                addProductLauncher.launch(new Intent(this, AddProductActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    /** Đọc lại danh sách và thống kê từ cơ sở dữ liệu. */
    private void loadData() {
        List<Product> products = dbHelper.getAllProducts();
        adapter.setProducts(products);

        boolean isEmpty = products.isEmpty();
        rvProducts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvHint.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        // Thống kê
        tvTotalProducts.setText(String.valueOf(dbHelper.countProducts()));
        tvTotalValue.setText(AppUtils.formatMoney(dbHelper.getTotalValue()));
        int lowStock = dbHelper.countLowStock();
        tvLowStock.setText(String.valueOf(lowStock));
        tvLowStock.setTextColor(ContextCompat.getColor(this,
                lowStock > 0 ? R.color.stats_warning : R.color.white));

        // Cuộn tới sản phẩm vừa thêm
        if (pendingScrollToId != -1) {
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getId() == pendingScrollToId) {
                    rvProducts.scrollToPosition(i);
                    break;
                }
            }
            pendingScrollToId = -1;
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}

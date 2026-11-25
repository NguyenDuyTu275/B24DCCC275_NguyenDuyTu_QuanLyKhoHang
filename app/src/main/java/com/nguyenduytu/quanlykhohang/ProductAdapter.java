package com.nguyenduytu.quanlykhohang;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách sản phẩm trên RecyclerView.
 * Danh sách ở đây chỉ chứa kết quả truy vấn từ SQLite để hiển thị.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final List<Product> products = new ArrayList<>();
    private final OnProductClickListener listener;

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    /** Thay toàn bộ danh sách bằng kết quả truy vấn mới (danh sách nhỏ nên vẽ lại toàn bộ). */
    @SuppressLint("NotifyDataSetChanged")
    public void setProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvQuantity;
        private final TextView tvPrice;
        private final TextView tvCategory;
        private final TextView tvLowStock;
        private final TextView tvInactive;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLowStock = itemView.findViewById(R.id.tvLowStock);
            tvInactive = itemView.findViewById(R.id.tvInactive);
        }

        void bind(Product product) {
            tvName.setText(product.getName());
            tvQuantity.setText(itemView.getContext()
                    .getString(R.string.quantity_short, product.getQuantity()));
            tvPrice.setText(itemView.getContext()
                    .getString(R.string.price_label, AppUtils.formatMoney(product.getPrice())));
            tvCategory.setText(product.getCategory());

            // Sắp hết hàng (SL < 5): hiện nhãn cảnh báo và tô đỏ số lượng
            boolean low = product.isLowStock();
            tvLowStock.setVisibility(low ? View.VISIBLE : View.GONE);
            tvQuantity.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    low ? R.color.danger : R.color.text_primary));

            tvInactive.setVisibility(product.isActive() ? View.GONE : View.VISIBLE);
        }
    }
}

package com.nguyenduytu.quanlykhohang;

import android.graphics.Color;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Các hàm tiện ích dùng chung cho nhiều màn hình.
 */
public final class AppUtils {

    private AppUtils() {
    }

    /** Định dạng tiền có dấu phân cách hàng nghìn, ví dụ 450000 -> "450.000 đ". */
    public static String formatMoney(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount) + " đ";
    }

    /**
     * Bật chế độ tràn viền với biểu tượng thanh trạng thái màu sáng (nền tiêu đề màu xanh đậm).
     * Gọi trước setContentView().
     */
    public static void enableEdgeToEdge(ComponentActivity activity) {
        EdgeToEdge.enable(activity,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
    }

    /**
     * Chừa khoảng trống cho thanh trạng thái, thanh điều hướng và bàn phím
     * (ứng dụng chạy chế độ tràn viền - edge-to-edge).
     */
    public static void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}

package com.nguyenduytu.quanlykhohang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test cho lớp Product và hàm định dạng tiền (chạy trên JVM, không cần máy ảo).
 */
public class ProductTest {

    @Test
    public void lowStock_whenQuantityBelowFive() {
        assertTrue(new Product("A", 1000, 0, "Khác", true, "").isLowStock());
        assertTrue(new Product("A", 1000, 4, "Khác", true, "").isLowStock());
        assertFalse(new Product("A", 1000, 5, "Khác", true, "").isLowStock());
        assertFalse(new Product("A", 1000, 20, "Khác", true, "").isLowStock());
    }

    @Test
    public void stockValue_isPriceTimesQuantity() {
        assertEquals(1_350_000L, new Product("Bàn phím cơ", 450_000, 3, "Điện tử", true, "")
                .getStockValue());
        assertEquals(0L, new Product("A", 450_000, 0, "Khác", true, "").getStockValue());
    }

    @Test
    public void stockValue_doesNotOverflowInt() {
        // 10.000.000 x 1.000 = 10 tỷ > Integer.MAX_VALUE
        assertEquals(10_000_000_000L, new Product("A", 10_000_000, 1000, "Khác", true, "")
                .getStockValue());
    }

    @Test
    public void formatMoney_usesDotThousandSeparator() {
        assertEquals("0 đ", AppUtils.formatMoney(0));
        assertEquals("25.000 đ", AppUtils.formatMoney(25_000));
        assertEquals("450.000 đ", AppUtils.formatMoney(450_000));
        assertEquals("5.350.000 đ", AppUtils.formatMoney(5_350_000));
    }
}

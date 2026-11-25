package com.nguyenduytu.quanlykhohang;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp quản lý cơ sở dữ liệu SQLite warehouse.db (phiên bản 1).
 * Toàn bộ dữ liệu của ứng dụng (tài khoản, sản phẩm) được đọc/ghi qua lớp này.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "warehouse.db";
    public static final int DATABASE_VERSION = 1;

    // ----- Bảng users -----
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // ----- Bảng products -----
    public static final String TABLE_PRODUCTS = "products";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_PRICE = "price";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_CATEGORY = "category";
    public static final String COL_ACTIVE = "active";
    public static final String COL_DESCRIPTION = "description";

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " ("
                    + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_USERNAME + " TEXT NOT NULL UNIQUE, "
                    + COL_PASSWORD + " TEXT NOT NULL)";

    private static final String SQL_CREATE_PRODUCTS =
            "CREATE TABLE " + TABLE_PRODUCTS + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_NAME + " TEXT NOT NULL, "
                    + COL_PRICE + " INTEGER NOT NULL CHECK (" + COL_PRICE + " > 0), "
                    + COL_QUANTITY + " INTEGER NOT NULL CHECK (" + COL_QUANTITY + " >= 0), "
                    + COL_CATEGORY + " TEXT NOT NULL, "
                    + COL_ACTIVE + " INTEGER NOT NULL, "
                    + COL_DESCRIPTION + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_PRODUCTS);

        // Tài khoản mẫu
        insertUser(db, "admin", "123456");

        // 3 sản phẩm mẫu (active = 1)
        insertProduct(db, new Product("Chuột không dây", 150000, 20, "Điện tử", true,
                "Chuột không dây 2.4GHz, pin AA"));
        insertProduct(db, new Product("Bàn phím cơ", 450000, 3, "Điện tử", true,
                "Bàn phím cơ switch xanh, có đèn nền"));
        insertProduct(db, new Product("Sổ tay A5", 25000, 40, "Văn phòng phẩm", true,
                "Sổ tay khổ A5, 200 trang"));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // =====================================================================
    //  Bảng users
    // =====================================================================

    /** Trả về true nếu có dòng khớp username và password trong bảng users. */
    public boolean checkLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    /** Thêm tài khoản mới vào bảng users. Trả về id vừa thêm, hoặc -1 nếu lỗi (vd: trùng tên). */
    public long insertUser(String username, String password) {
        return insertUser(getWritableDatabase(), username, password);
    }

    /**
     * Bản dùng chung cho cả onCreate() — trong onCreate() không được gọi getWritableDatabase()
     * (sẽ lỗi gọi đệ quy), nên truyền thẳng đối tượng db vào.
     */
    private long insertUser(SQLiteDatabase db, String username, String password) {
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values);
    }

    // =====================================================================
    //  Bảng products
    // =====================================================================

    /** Thêm sản phẩm, trả về id vừa thêm (hoặc -1 nếu lỗi). */
    public long insertProduct(Product p) {
        long id = insertProduct(getWritableDatabase(), p);
        if (id != -1) {
            p.setId((int) id);
        }
        return id;
    }

    private long insertProduct(SQLiteDatabase db, Product p) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, p.getName());
        values.put(COL_PRICE, p.getPrice());
        values.put(COL_QUANTITY, p.getQuantity());
        values.put(COL_CATEGORY, p.getCategory());
        values.put(COL_ACTIVE, p.isActive() ? 1 : 0);
        values.put(COL_DESCRIPTION, p.getDescription());
        return db.insert(TABLE_PRODUCTS, null, values);
    }

    /**
     * Lấy toàn bộ sản phẩm để hiển thị danh sách.
     * ArrayList chỉ chứa kết quả truy vấn để đưa lên giao diện; nếu bảng rỗng trả về danh sách rỗng.
     */
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_PRODUCTS, null, null, null,
                null, null, COL_ID + " ASC")) {
            while (cursor.moveToNext()) {
                products.add(readProduct(cursor));
            }
        }
        return products;
    }

    /** Lấy một sản phẩm theo id; trả về null nếu không tìm thấy. */
    public Product getProductById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_PRODUCTS, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return readProduct(cursor);
            }
            return null;
        }
    }

    /** Cập nhật số lượng (dùng cho Nhập thêm / Bán). Trả về true nếu cập nhật thành công. */
    public boolean updateQuantity(int id, int quantity) {
        if (quantity < 0) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(COL_QUANTITY, quantity);
        int rows = getWritableDatabase().update(TABLE_PRODUCTS, values,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    /** Xóa sản phẩm theo id. Trả về true nếu xóa được. */
    public boolean deleteProduct(int id) {
        int rows = getWritableDatabase().delete(TABLE_PRODUCTS,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    /** Tổng số sản phẩm trong kho. */
    public int countProducts() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    /** Tổng giá trị kho: SUM(price * quantity). Bảng rỗng trả về 0. */
    public long getTotalValue() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(" + COL_PRICE + " * "
                + COL_QUANTITY + "), 0) FROM " + TABLE_PRODUCTS, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    /** Số sản phẩm sắp hết hàng: COUNT với quantity < 5. */
    public int countLowStock() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS
                        + " WHERE " + COL_QUANTITY + " < ?",
                new String[]{String.valueOf(Product.LOW_STOCK_THRESHOLD)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    /** Chuyển dòng hiện tại của Cursor thành đối tượng Product. */
    private Product readProduct(Cursor cursor) {
        return new Product(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_PRICE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACTIVE)) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
    }
}

# Giải thích luồng hoạt động – Ứng dụng Quản lý kho hàng

Tài liệu này giúp đọc hiểu **luồng chạy** của ứng dụng: mỗi khi người dùng bấm một nút thì code nào chạy, đọc/ghi gì vào SQLite, rồi màn hình nào hiện ra. Nên đọc theo thứ tự từ trên xuống.

Đường dẫn rút gọn dùng trong tài liệu:

- `java/` = `app/src/main/java/com/nguyenduytu/quanlykhohang/`
- `res/` = `app/src/main/res/`

---

## Mục lục

1. [Bức tranh tổng thể](#1-bức-tranh-tổng-thể)
2. [Các file trong project](#2-các-file-trong-project)
3. [Cơ sở dữ liệu SQLite – DatabaseHelper](#3-cơ-sở-dữ-liệu-sqlite--databasehelper)
4. [Lớp Product](#4-lớp-product)
5. [Luồng 1 – Mở ứng dụng và đăng nhập](#5-luồng-1--mở-ứng-dụng-và-đăng-nhập)
6. [Luồng 2 – Hiển thị danh sách và thống kê](#6-luồng-2--hiển-thị-danh-sách-và-thống-kê)
7. [Luồng 3 – Thêm sản phẩm](#7-luồng-3--thêm-sản-phẩm)
8. [Luồng 4 – Xem chi tiết, nhập thêm, bán, xóa](#8-luồng-4--xem-chi-tiết-nhập-thêm-bán-xóa)
9. [Vì sao onResume() làm danh sách luôn đúng](#9-vì-sao-onresume-làm-danh-sách-luôn-đúng)
10. [Đoạn code lặp lại ở mọi màn hình: tràn viền](#10-đoạn-code-lặp-lại-ở-mọi-màn-hình-tràn-viền)
11. [Giao diện XML và tài nguyên](#11-giao-diện-xml-và-tài-nguyên)
12. [Đối chiếu với yêu cầu đề bài](#12-đối-chiếu-với-yêu-cầu-đề-bài)
13. [Câu hỏi thường gặp khi vấn đáp](#13-câu-hỏi-thường-gặp-khi-vấn-đáp)

---

## 1. Bức tranh tổng thể

Ứng dụng có 4 màn hình, mỗi màn hình là một **Activity**. Tất cả cùng đọc/ghi một cơ sở dữ liệu SQLite tên `warehouse.db` thông qua lớp `DatabaseHelper`.

```
                         ┌───────────────────────────┐
                         │   warehouse.db (SQLite)   │
                         │   ├── bảng users          │
                         │   └── bảng products       │
                         └─────────────▲─────────────┘
                                       │ đọc / ghi qua DatabaseHelper
        ┌──────────────────┬───────────┴──────┬───────────────────────┐
        │                  │                  │                       │
┌───────┴────────┐  ┌──────┴───────┐  ┌───────┴────────────┐  ┌───────┴──────────────┐
│ LoginActivity  │  │ MainActivity │  │ AddProductActivity │  │ ProductDetailActivity│
│ (Màn hình 1)   │  │ (Màn hình 2) │  │ (Màn hình 3)       │  │ (Màn hình 4)         │
└───────┬────────┘  └──┬────────┬──┘  └─────────▲──────────┘  └──────────▲───────────┘
        │ đúng TK      │        │ nút Thêm      │                        │
        │ + finish()   │        └───────────────┘                        │
        └─────────────►│  nhấn 1 sản phẩm: Intent kèm product_id         │
                       └─────────────────────────────────────────────────┘
```

Ý tưởng quan trọng nhất của bài này:

| Nguyên tắc | Cách làm trong code |
|---|---|
| Dữ liệu chỉ nằm trong SQLite | Mọi thêm/sửa/xóa đều gọi `DatabaseHelper` → ghi vào file `warehouse.db`. Tắt hẳn app mở lại vẫn còn. |
| `ArrayList` chỉ để hiển thị | `getAllProducts()` trả về `List<Product>` là **bản sao kết quả truy vấn**, đưa cho Adapter vẽ lên màn hình, không phải nơi lưu chính. |
| Truyền **id**, không truyền cả đối tượng | `MainActivity` gửi `product_id` qua Intent; `ProductDetailActivity` tự gọi `getProductById(id)` để lấy dữ liệu mới nhất. |
| Tải lại khi quay về | `MainActivity.onResume()` gọi `loadData()` → luôn truy vấn lại danh sách và thống kê. |

---

## 2. Các file trong project

```
app/src/main/
├── AndroidManifest.xml              # Khai báo 4 Activity, LoginActivity là màn hình mở đầu
├── java/com/nguyenduytu/quanlykhohang/
│   ├── Product.java                 # Lớp dữ liệu: 1 sản phẩm = 1 dòng bảng products
│   ├── DatabaseHelper.java          # SQLiteOpenHelper: tạo bảng, dữ liệu mẫu, các hàm truy vấn
│   ├── LoginActivity.java           # Màn hình 1: Đăng nhập
│   ├── MainActivity.java            # Màn hình 2: Thống kê + danh sách
│   ├── ProductAdapter.java          # Adapter vẽ từng dòng sản phẩm lên RecyclerView
│   ├── AddProductActivity.java      # Màn hình 3: Thêm sản phẩm
│   ├── ProductDetailActivity.java   # Màn hình 4: Chi tiết, nhập thêm, bán, xóa
│   └── AppUtils.java                # Định dạng tiền "450.000 đ" + xử lý tràn viền
└── res/
    ├── layout/                      # activity_*.xml, item_product.xml, item_spinner*.xml
    ├── drawable/                    # Nền ô nhập, nền khối thống kê, nền nhãn, icon
    └── values/                      # strings.xml, colors.xml, themes.xml
app/src/test/.../ProductTest.java    # Unit test cho Product và formatMoney
```

---

## 3. Cơ sở dữ liệu SQLite – DatabaseHelper

File: `java/DatabaseHelper.java`. Lớp này **kế thừa `SQLiteOpenHelper`** – lớp có sẵn của Android giúp tạo và mở file cơ sở dữ liệu.

### 3.1. Khi nào cơ sở dữ liệu được tạo?

```java
public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);   // "warehouse.db", phiên bản 1
}
```

Lệnh `new DatabaseHelper(this)` **chưa** tạo file. File chỉ được tạo ở **lần đầu tiên** có code gọi `getReadableDatabase()` hoặc `getWritableDatabase()`:

```
Lần đầu cài app, bấm Đăng nhập
  └─ checkLogin() gọi getReadableDatabase()
       └─ Android thấy chưa có file warehouse.db
            └─ tạo file rồi gọi onCreate(db)
                 ├─ CREATE TABLE users ...
                 ├─ CREATE TABLE products ...
                 ├─ chèn tài khoản admin / 123456
                 └─ chèn 3 sản phẩm mẫu
```

Từ lần mở app thứ hai trở đi, file đã tồn tại nên `onCreate()` **không chạy lại** – dữ liệu cũ được giữ nguyên. Đó là lý do tắt app mở lại vẫn còn dữ liệu.

`onUpgrade()` chỉ chạy khi tăng `DATABASE_VERSION` (ví dụ lên 2): xóa bảng cũ và tạo lại.

### 3.2. Hai bảng

```sql
CREATE TABLE users (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL
);

CREATE TABLE products (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    price       INTEGER NOT NULL CHECK (price > 0),
    quantity    INTEGER NOT NULL CHECK (quantity >= 0),
    category    TEXT    NOT NULL,
    active      INTEGER NOT NULL,      -- 1: đang kinh doanh, 0: ngừng
    description TEXT                   -- được phép NULL / rỗng
);
```

`CHECK (price > 0)` và `CHECK (quantity >= 0)` là lớp bảo vệ thêm ở mức cơ sở dữ liệu: dù code Java có lỗi thì SQLite cũng không cho lưu giá ≤ 0 hoặc số lượng âm.

### 3.3. Vì sao có hai hàm `insertUser` / `insertProduct`?

```java
public long insertUser(String username, String password) {             // dùng từ bên ngoài
    return insertUser(getWritableDatabase(), username, password);
}

private long insertUser(SQLiteDatabase db, String username, String password) { // dùng trong onCreate
    ...
    return db.insert(TABLE_USERS, null, values);
}
```

Bên trong `onCreate(db)` **không được** gọi `getWritableDatabase()` (Android báo lỗi *getDatabase called recursively* vì CSDL đang được tạo dở). Vì vậy `onCreate` dùng bản `private` nhận thẳng đối tượng `db`, còn bên ngoài dùng bản `public`.

### 3.4. Các hàm truy vấn

| Hàm | Câu SQL tương đương | Dùng ở đâu |
|---|---|---|
| `checkLogin(u, p)` | `SELECT id FROM users WHERE username = ? AND password = ? LIMIT 1` | LoginActivity |
| `insertUser(u, p)` | `INSERT INTO users ...` | onCreate (tài khoản mẫu) |
| `insertProduct(p)` | `INSERT INTO products ...` → trả về id mới | onCreate, AddProductActivity |
| `getAllProducts()` | `SELECT * FROM products ORDER BY id ASC` | MainActivity |
| `getProductById(id)` | `SELECT * FROM products WHERE id = ? LIMIT 1` | ProductDetailActivity |
| `updateQuantity(id, q)` | `UPDATE products SET quantity = ? WHERE id = ?` | Nút Nhập thêm 1 / Bán 1 |
| `deleteProduct(id)` | `DELETE FROM products WHERE id = ?` | Nút Xóa sản phẩm |
| `countProducts()` | `SELECT COUNT(*) FROM products` | Khối thống kê |
| `getTotalValue()` | `SELECT COALESCE(SUM(price * quantity), 0) FROM products` | Khối thống kê |
| `countLowStock()` | `SELECT COUNT(*) FROM products WHERE quantity < ?` (tham số `"5"`) | Khối thống kê |

Ba quy tắc đề bài yêu cầu, đều có trong code:

1. **Truy vấn có tham số** – dùng dấu `?` và mảng `selectionArgs`, không cộng chuỗi giá trị người dùng nhập vào câu SQL (tránh lỗi SQL Injection, ví dụ nhập `' OR '1'='1` vào ô tên đăng nhập).
2. **Đóng Cursor sau khi dùng** – mọi Cursor đều mở trong `try (...)` (try-with-resources) nên tự `close()` kể cả khi có lỗi:
   ```java
   try (Cursor cursor = db.query(...)) {
       return cursor.moveToFirst();
   }   // ← cursor.close() tự động được gọi ở đây
   ```
3. **Xử lý danh sách rỗng** – `getAllProducts()` trả về danh sách rỗng (không phải `null`); `getTotalValue()` dùng `COALESCE(..., 0)` vì `SUM` của bảng rỗng là `NULL`; `getProductById()` trả về `null` nếu không tìm thấy và màn hình chi tiết xử lý trường hợp này.

### 3.5. `readProduct(cursor)` – từ một dòng của bảng thành đối tượng Java

```java
return new Product(
        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
        cursor.getLong(cursor.getColumnIndexOrThrow(COL_PRICE)),
        ...
        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACTIVE)) == 1,   // 1 → true, 0 → false
        ...);
```

SQLite không có kiểu boolean nên cột `active` lưu số `1`/`0`; khi đọc ra thì so sánh `== 1` để đổi thành `true`/`false`.

---

## 4. Lớp Product

File: `java/Product.java`. Mỗi đối tượng `Product` tương ứng **một dòng** của bảng `products`.

| Thuộc tính | Kiểu Java | Cột SQLite |
|---|---|---|
| `id` | `int` | `id` |
| `name` | `String` | `name` |
| `price` | `long` | `price` |
| `quantity` | `int` | `quantity` |
| `category` | `String` | `category` |
| `active` | `boolean` | `active` (1/0) |
| `description` | `String` | `description` |

- Hai constructor: **không có id** (khi tạo sản phẩm mới, id do SQLite tự sinh) và **có id** (khi đọc từ CSDL).
- `isLowStock()` → `quantity < 5` (hằng `LOW_STOCK_THRESHOLD = 5`).
- `getStockValue()` → `price * quantity` (giá trị tồn kho).
- Giá dùng `long` chứ không dùng `int`: `int` tối đa khoảng 2,1 tỷ, trong khi giá trị kho (giá × số lượng) dễ vượt con số này.

---

## 5. Luồng 1 – Mở ứng dụng và đăng nhập

### 5.1. Màn hình nào mở đầu tiên?

Trong `AndroidManifest.xml`, chỉ `LoginActivity` có `intent-filter` `MAIN` + `LAUNCHER`, nên bấm icon app sẽ mở màn hình đăng nhập.

### 5.2. `onCreate()` của LoginActivity

```
onCreate()
 ├─ AppUtils.enableEdgeToEdge(this)        // tràn viền (xem mục 10)
 ├─ setContentView(R.layout.activity_login)
 ├─ dbHelper = new DatabaseHelper(this)
 ├─ findViewById: etUsername, etPassword, tvError, btnLogin
 ├─ btnLogin.setOnClickListener(v -> checkLogin())
 ├─ etPassword: nhấn "Xong" trên bàn phím → checkLogin()
 └─ TextWatcher: người dùng gõ lại → ẩn tvError
```

### 5.3. Bấm "Đăng nhập" → `checkLogin()`

```
checkLogin()
 ├─ username = etUsername (đã trim), password = etPassword
 ├─ Bỏ trống một trong hai?
 │     └─ CÓ → tvError = "Vui lòng nhập tên đăng nhập và mật khẩu", dừng
 ├─ dbHelper.checkLogin(username, password)
 │     └─ SELECT id FROM users WHERE username = ? AND password = ?
 ├─ Có dòng khớp (true)?
 │     ├─ CÓ   → startActivity(Intent → MainActivity)
 │     │         finish()      ← đóng LoginActivity, nút Back không quay lại được
 │     └─ KHÔNG → tvError = "Sai tên đăng nhập hoặc mật khẩu", ở lại màn hình
```

Tài khoản **không** viết cố định trong Activity – nó nằm trong bảng `users` (được chèn ở `onCreate` của DatabaseHelper). Muốn thêm tài khoản chỉ cần gọi `insertUser(...)`.

---

## 6. Luồng 2 – Hiển thị danh sách và thống kê

File: `java/MainActivity.java`, `java/ProductAdapter.java`.

### 6.1. Thứ tự các hàm vòng đời

```
Vừa đăng nhập xong
  └─ MainActivity.onCreate()
       ├─ setContentView, findViewById các TextView thống kê, RecyclerView...
       ├─ adapter = new ProductAdapter(product -> mở chi tiết với product.getId())
       ├─ rvProducts.setLayoutManager(LinearLayoutManager)  // danh sách dọc
       └─ btnAddProduct → addProductLauncher.launch(Intent → AddProductActivity)
  └─ MainActivity.onResume()
       └─ loadData()
```

### 6.2. `loadData()` – đọc toàn bộ từ SQLite

```
loadData()
 ├─ products = dbHelper.getAllProducts()          // SELECT * FROM products
 ├─ adapter.setProducts(products)                 // vẽ lại RecyclerView
 ├─ Danh sách rỗng?
 │     ├─ CÓ   → ẩn RecyclerView, hiện tvEmpty "Chưa có sản phẩm nào..."
 │     └─ KHÔNG → hiện RecyclerView
 ├─ tvTotalProducts = dbHelper.countProducts()     // COUNT(*)
 ├─ tvTotalValue    = formatMoney(dbHelper.getTotalValue())   // SUM(price*quantity)
 ├─ tvLowStock      = dbHelper.countLowStock()     // COUNT(*) WHERE quantity < 5
 │                    (> 0 thì tô vàng cho nổi bật)
 └─ Nếu vừa thêm sản phẩm (pendingScrollToId != -1) → cuộn tới sản phẩm đó
```

Với 3 sản phẩm mẫu: tổng giá trị = 150.000×20 + 450.000×3 + 25.000×40 = **5.350.000 đ**, sắp hết hàng = **1** (Bàn phím cơ, SL 3).

### 6.3. ProductAdapter – vẽ từng dòng

RecyclerView không tự biết vẽ một sản phẩm ra sao, nó hỏi Adapter:

| Hàm | Khi nào được gọi | Làm gì |
|---|---|---|
| `getItemCount()` | Khi cần biết có bao nhiêu dòng | Trả về `products.size()` |
| `onCreateViewHolder()` | Khi cần tạo một khung dòng mới | Inflate `item_product.xml`, bọc trong `ProductViewHolder` |
| `onBindViewHolder(holder, position)` | Khi đổ dữ liệu vào một khung | Gọi `holder.bind(product)` và gắn sự kiện click |

`bind(product)`:

- `tvName` ← tên, `tvPrice` ← "Giá bán: 450.000 đ", `tvQuantity` ← "SL: 3", `tvCategory` ← loại.
- `isLowStock()` → hiện nhãn đỏ **"Sắp hết hàng"** và tô đỏ số lượng.
- `!isActive()` → hiện nhãn xám "Ngừng kinh doanh".

Khi nhấn một dòng, Adapter gọi `listener.onProductClick(product)` – interface này được `MainActivity` cài đặt bằng lambda để mở màn hình chi tiết (mục 8).

---

## 7. Luồng 3 – Thêm sản phẩm

### 7.1. Mở màn hình

```java
private final ActivityResultLauncher<Intent> addProductLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                pendingScrollToId = result.getData()
                        .getIntExtra(AddProductActivity.EXTRA_NEW_PRODUCT_ID, -1);
            }
        });
```

Bấm "+ Thêm sản phẩm" → `addProductLauncher.launch(...)` mở `AddProductActivity`. Dùng **Activity Result API** để khi thêm xong, màn hình thêm gửi lại **id sản phẩm mới** – MainActivity dùng id này để cuộn tới đúng sản phẩm vừa thêm.

### 7.2. `onCreate()` của AddProductActivity

- Spinner loại sản phẩm lấy dữ liệu từ mảng `product_categories` trong `strings.xml` (Điện tử, Thực phẩm, Văn phòng phẩm, Gia dụng, Khác) qua `ArrayAdapter.createFromResource(...)`.
- `MaterialSwitch swActive` có `android:checked="true"` → mặc định **bật** (đang kinh doanh).
- Mũi tên ← trên thanh tiêu đề → `finish()` (hủy, không lưu).

### 7.3. Bấm "Lưu sản phẩm" → `saveProduct()`

```
saveProduct()
 ├─ Đọc: name, priceText, quantityText, category (Spinner), active (Switch), description
 ├─ name rỗng?                   → etName.setError("Vui lòng nhập tên sản phẩm"), dừng
 ├─ priceText rỗng?              → setError("Vui lòng nhập giá bán"), dừng
 ├─ Long.parseLong lỗi?          → setError("Giá bán không hợp lệ"), dừng
 ├─ price <= 0?                  → setError("Giá bán phải lớn hơn 0"), dừng
 ├─ quantityText rỗng?           → setError("Vui lòng nhập số lượng"), dừng
 ├─ Integer.parseInt lỗi?        → setError("Số lượng phải là số nguyên hợp lệ"), dừng
 ├─ quantity < 0?                → setError("Số lượng không được âm"), dừng
 ├─ newId = dbHelper.insertProduct(new Product(...))     // INSERT INTO products
 ├─ newId == -1?                 → Toast "Lưu sản phẩm thất bại", dừng
 ├─ Toast "Đã thêm sản phẩm"
 ├─ setResult(RESULT_OK, Intent có EXTRA_NEW_PRODUCT_ID = newId)
 └─ finish()      ← đóng màn hình, quay về MainActivity
```

Sau `finish()`, Android quay lại MainActivity theo thứ tự:

```
1. callback của addProductLauncher   → ghi nhớ pendingScrollToId = newId
2. MainActivity.onResume()           → loadData() → truy vấn lại → sản phẩm mới xuất hiện,
                                       thống kê cập nhật, cuộn tới sản phẩm mới
```

Ghi chú: ô giá và số lượng dùng `inputType="number"` nên bàn phím chỉ có số; `maxLength` giới hạn 12 và 9 chữ số để không vượt giới hạn của `long`/`int`.

---

## 8. Luồng 4 – Xem chi tiết, nhập thêm, bán, xóa

### 8.1. Mở màn hình – chỉ truyền id

Trong `MainActivity`:

```java
adapter = new ProductAdapter(product -> {
    Intent intent = new Intent(this, ProductDetailActivity.class);
    intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
    startActivity(intent);
});
```

Trong `ProductDetailActivity.onCreate()`:

```java
productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
...
loadProduct();
```

### 8.2. `loadProduct()` – truy vấn lại theo id

```
loadProduct()
 ├─ product = dbHelper.getProductById(productId)   // SELECT * FROM products WHERE id = ?
 ├─ product == null? → Toast "Không tìm thấy sản phẩm", finish()
 ├─ (1) tvName, tvCategory, tvStatus (Đang / Ngừng kinh doanh)
 ├─ (2) tvPrice        = formatMoney(price)                   // "450.000 đ"
 ├─ (3) tvQuantity     = quantity, tvStockValue = formatMoney(price × quantity)
 │       quantity < 5 → hiện nhãn "Sắp hết hàng", số lượng màu đỏ
 └─ (4) tvDescription  = mô tả, rỗng thì hiện "(Không có mô tả)"
```

Vì sao truyền id chứ không truyền cả đối tượng `Product`? Vì cơ sở dữ liệu mới là "nguồn sự thật". Truy vấn lại theo id đảm bảo luôn hiển thị dữ liệu mới nhất, và mỗi lần nhập/bán xong chỉ cần gọi lại `loadProduct()`.

### 8.3. Nút (5) "+ Nhập thêm 1" → `importOne()`

```
importOne()
 ├─ dbHelper.updateQuantity(productId, quantity + 1)   // UPDATE products SET quantity = ? WHERE id = ?
 └─ thành công → loadProduct()   // đọc lại từ CSDL, số lượng / giá trị tồn / nhãn cảnh báo cập nhật
```

### 8.4. Nút (6) "– Bán 1" → `sellOne()`

```
sellOne()
 ├─ quantity == 0? → Toast "Hết hàng", KHÔNG giảm nữa, dừng
 ├─ dbHelper.updateQuantity(productId, quantity - 1)
 └─ thành công → loadProduct()
```

Ví dụ: Bàn phím cơ SL 3 → bấm Bán 1 ba lần → SL 0 → bấm thêm lần nữa chỉ hiện "Hết hàng".

### 8.5. Nút (7) "Quay lại" và mũi tên ←

Cả hai gọi `finish()`. MainActivity hiện lại → `onResume()` → `loadData()` → danh sách và thống kê hiển thị đúng số lượng mới.

### 8.6. Nút (8) "Xóa sản phẩm" → `confirmDelete()` → `deleteProduct()`

```
confirmDelete()
 └─ Hộp thoại "Bạn có chắc muốn xóa "Bàn phím cơ" khỏi kho?"  [Hủy] [Xóa]
       └─ bấm Xóa → deleteProduct()
            ├─ dbHelper.deleteProduct(productId)     // DELETE FROM products WHERE id = ?
            ├─ Toast "Đã xóa sản phẩm"
            └─ finish()  → MainActivity.onResume() → loadData() → sản phẩm biến mất, thống kê giảm
```

Hộp thoại xác nhận là phần làm thêm để tránh xóa nhầm (đề chỉ yêu cầu xóa và quay lại).

---

## 9. Vì sao onResume() làm danh sách luôn đúng

Vòng đời khi đi từ danh sách sang màn hình khác rồi quay lại:

```
MainActivity          ProductDetailActivity
onCreate()
onStart()
onResume()  ← loadData() lần 1
   │ nhấn 1 sản phẩm
onPause()
                      onCreate() → loadProduct()
                      ... bấm Bán 1, Nhập thêm 1 (ghi vào SQLite) ...
                      finish()
onResume()  ← loadData() lần 2 (đọc lại SQLite → số lượng mới)
```

Vì `onResume()` chạy **mỗi lần** MainActivity hiện lại, nên dù bên kia thêm, sửa hay xóa gì thì danh sách và thống kê cũng được truy vấn lại – không cần truyền dữ liệu ngược về bằng Intent. Đây đúng là cách đề bài gợi ý ("ví dụ trong onResume").

---

## 10. Đoạn code lặp lại ở mọi màn hình: tràn viền

Ứng dụng đặt `targetSdk 37`. Từ Android 15, ứng dụng nhắm API 35 trở lên **bắt buộc** vẽ tràn viền (nội dung vẽ cả dưới thanh trạng thái và thanh điều hướng). Vì vậy mỗi Activity có hai dòng:

```java
AppUtils.enableEdgeToEdge(this);                          // trước setContentView
setContentView(R.layout.activity_xxx);
AppUtils.applySystemBarInsets(findViewById(R.id.main));   // sau setContentView
```

- `enableEdgeToEdge` – bật tràn viền, chọn biểu tượng thanh trạng thái màu **sáng** (vì nền phía trên màu xanh đậm).
- `applySystemBarInsets` – đặt padding cho layout gốc đúng bằng chiều cao thanh trạng thái / thanh điều hướng / bàn phím, để nội dung không bị che. Layout gốc có nền `primary_dark`, nên phần padding phía trên trông như một thanh trạng thái màu xanh đậm.

---

## 11. Giao diện XML và tài nguyên

### 11.1. Layout

| File | Nội dung (số trong ngoặc = số thứ tự thành phần trong đề) |
|---|---|
| `activity_login.xml` | Logo tròn "SP", (1) `etUsername`, (2) `etPassword` (`textPassword`), (3) `tvError` (mặc định `gone`), (4) `btnLogin` |
| `activity_main.xml` | Toolbar "Kho hàng của tôi", (1) khối thống kê 3 cột, (2) `RecyclerView rvProducts` + `tvEmpty`, (3) `btnAddProduct` |
| `item_product.xml` | `MaterialCardView`: tên, "SL: x", giá, nhãn loại / "Sắp hết hàng" / "Ngừng kinh doanh" (trong `ChipGroup` để tự xuống dòng) |
| `activity_add_product.xml` | (1) tên, (2) giá (`number`), (3) số lượng (`number`), (4) `Spinner`, (5) `MaterialSwitch` bật sẵn, (6) mô tả (`textMultiLine`), (7) nút Lưu |
| `activity_product_detail.xml` | Thẻ thông tin (1)–(4), hàng nút (5) Nhập thêm 1 + (6) Bán 1, (7) Quay lại (viền), (8) Xóa (đỏ) |
| `item_spinner*.xml` | Giao diện mục đang chọn và mục trong danh sách xổ xuống của Spinner |

### 11.2. `values/`

- `strings.xml` – toàn bộ chữ tiếng Việt, thông báo lỗi, mảng `product_categories`. Có chuỗi định dạng như `SL: %1$d`, `Giá bán: %1$s`.
- `colors.xml` – xanh ngọc `#00796B` (chủ đạo), xanh đậm `#004D40`, đỏ `#D32F2F` (cảnh báo/xóa), cam `#EF6C00` (nút Bán).
- `themes.xml` – theme Material 3 sáng, style dùng chung cho nhãn, ô nhập, nhãn chip, nút.

### 11.3. `drawable/`

`bg_input` (ô nhập viền bo góc), `bg_spinner` (ô nhập + mũi tên ▼), `bg_stats` (khối thống kê), `bg_chip_*` (nền các nhãn), `bg_circle` (logo tròn), icon mũi tên quay lại, icon ứng dụng hình thùng hàng.

---

## 12. Đối chiếu với yêu cầu đề bài

| Yêu cầu | Chỗ đáp ứng |
|---|---|
| Java + XML, 4 màn hình, Intent chuyển màn hình (truyền id) | 4 Activity; `EXTRA_PRODUCT_ID` |
| Lớp `Product` (tên, giá, số lượng, loại, trạng thái, mô tả) | `Product.java` |
| SQLite qua `SQLiteOpenHelper`, tên `warehouse.db`, phiên bản 1 | `DatabaseHelper` |
| Tạo bảng + dữ liệu mẫu trong `onCreate()` | `DatabaseHelper.onCreate()` |
| Đăng nhập kiểm tra bằng bảng `users`, không cố định trong Activity | `checkLogin()` |
| Bỏ trống / sai → báo lỗi; đúng → Intent + `finish()` | `LoginActivity.checkLogin()` |
| Thống kê: tổng SP, tổng giá trị, số SP < 5 | `countProducts()`, `getTotalValue()`, `countLowStock()` |
| Dòng sắp hết hàng: nhãn cảnh báo + số lượng đỏ | `ProductAdapter.bind()` |
| Kiểm tra tên / giá / số lượng, báo bằng `setError` | `AddProductActivity.saveProduct()` |
| Nhập thêm 1, Bán 1 ("Hết hàng" khi = 0), Quay lại, Xóa | `ProductDetailActivity` |
| Đủ 9 phương thức tối thiểu của DatabaseHelper | Mục 3.4 |
| Truy vấn có tham số `?`, đóng Cursor, xử lý rỗng | Mục 3.4 |
| Tải lại sau thêm/sửa/xóa | `MainActivity.onResume()` |
| Không dùng Room, Firebase, API; ArrayList chỉ để hiển thị | Chỉ dùng `android.database.sqlite` |

---

## 13. Câu hỏi thường gặp khi vấn đáp

**Hỏi: Dữ liệu được lưu ở đâu trên máy?**
Đáp: Trong file `/data/data/com.nguyenduytu.quanlykhohang/databases/warehouse.db`. Có thể xem bằng *App Inspection → Database Inspector* trong Android Studio khi app đang chạy.

**Hỏi: Tại sao `onCreate()` của DatabaseHelper không chạy lại mỗi lần mở app?**
Đáp: `SQLiteOpenHelper` chỉ gọi `onCreate()` khi file CSDL chưa tồn tại. Muốn chạy lại (ví dụ để có lại dữ liệu mẫu) thì gỡ app hoặc *Settings → Apps → Storage → Clear data*.

**Hỏi: Đổi cấu trúc bảng thì làm sao?**
Đáp: Tăng `DATABASE_VERSION` lên 2, khi đó `onUpgrade()` chạy (trong bài: xóa bảng cũ và tạo lại).

**Hỏi: Vì sao dùng dấu `?` thay vì cộng chuỗi?**
Đáp: Tránh SQL Injection và lỗi ký tự đặc biệt. Ví dụ nhập tên đăng nhập `' OR '1'='1` – nếu cộng chuỗi thì câu SQL luôn đúng và đăng nhập được mà không cần mật khẩu; dùng `?` thì chuỗi đó chỉ được coi là một giá trị bình thường.

**Hỏi: Không đóng Cursor thì sao?**
Đáp: Rò rỉ bộ nhớ / tài nguyên; Android sẽ cảnh báo *"Cursor finalized without prior close()"*. Trong bài dùng try-with-resources nên Cursor luôn được đóng.

**Hỏi: Vì sao gọi `finish()` sau khi đăng nhập?**
Đáp: Để xóa LoginActivity khỏi ngăn xếp Activity (back stack). Bấm Back ở danh sách sẽ thoát app chứ không quay lại màn hình đăng nhập.

**Hỏi: Vì sao màn hình chi tiết nhận id rồi truy vấn lại, không nhận cả đối tượng Product?**
Đáp: Đề yêu cầu vậy, và cách này luôn lấy dữ liệu mới nhất từ CSDL. Không cần `Serializable`/`Parcelable`.

**Hỏi: Làm sao danh sách cập nhật sau khi bán ở màn hình chi tiết?**
Đáp: Màn hình chi tiết ghi số lượng mới vào SQLite; khi quay lại, `MainActivity.onResume()` gọi `loadData()` truy vấn lại.

**Hỏi: Tổng giá trị kho được tính ở đâu?**
Đáp: Ngay trong SQLite: `SELECT COALESCE(SUM(price * quantity), 0) FROM products`. `COALESCE` để bảng rỗng trả về 0 thay vì `NULL`.

**Hỏi: Giá bán có thể nhập số âm không?**
Đáp: Không. `inputType="number"` không có dấu trừ; code kiểm tra `price <= 0`; và bảng có `CHECK (price > 0)`.

**Hỏi: Định dạng "450.000 đ" làm thế nào?**
Đáp: `AppUtils.formatMoney()` dùng `DecimalFormat("#,##0")` với dấu phân cách hàng nghìn là dấu chấm, rồi nối thêm `" đ"`.

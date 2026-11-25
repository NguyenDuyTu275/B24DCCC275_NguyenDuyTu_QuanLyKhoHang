# Ứng dụng Quản lý kho hàng (Android – Java – SQLite)

Bài thực hành Lập trình Android: ứng dụng giúp chủ cửa hàng nhỏ quản lý sản phẩm trong kho (giá bán, số lượng tồn, cảnh báo sắp hết hàng). Gồm 4 màn hình **Đăng nhập → Danh sách sản phẩm → Thêm sản phẩm / Chi tiết sản phẩm**; toàn bộ dữ liệu lưu trong cơ sở dữ liệu SQLite.

- **Sinh viên:** Nguyễn Duy Tư – **MSSV:** B24DCCC275
- **Đề bài:** [De_bai_Quan_ly_kho_hang.docx](De_bai_Quan_ly_kho_hang.docx)
- **Báo cáo:** [B24DCCC275_NguyenDuyTu_BaoCao.docx](B24DCCC275_NguyenDuyTu_BaoCao.docx)
- **Giải thích luồng code:** [GIAI_THICH_CODE.md](GIAI_THICH_CODE.md)
- **Ảnh chụp chạy thử** (máy ảo Pixel 7, Android 17): thư mục [`screenshots/`](screenshots/)

## Chức năng

| Màn hình | Chức năng chính |
|---|---|
| `LoginActivity` | Đăng nhập, kiểm tra bằng truy vấn bảng `users` (tài khoản mẫu `admin / 123456`); báo lỗi khi bỏ trống hoặc sai; đúng thì sang danh sách và `finish()`. |
| `MainActivity` | Khối thống kê (tổng số sản phẩm, tổng giá trị kho, số sản phẩm sắp hết hàng); danh sách `RecyclerView` (tên, giá, loại, số lượng; SL < 5 có nhãn "Sắp hết hàng" và số lượng màu đỏ); nút Thêm sản phẩm. Tải lại trong `onResume()`. |
| `AddProductActivity` | Nhập tên, giá bán, số lượng, loại (`Spinner`), đang kinh doanh (`Switch`), mô tả nhiều dòng; kiểm tra dữ liệu bằng `setError`; lưu vào SQLite. |
| `ProductDetailActivity` | Nhận `id` qua Intent và truy vấn lại; hiển thị giá, số lượng, giá trị tồn kho, mô tả; Nhập thêm 1, Bán 1 (báo "Hết hàng" khi bằng 0), Quay lại, Xóa sản phẩm (có hỏi xác nhận). |

## Cơ sở dữ liệu

- File `warehouse.db`, phiên bản 1, lớp `DatabaseHelper extends SQLiteOpenHelper`.
- Bảng `users(id, username, password)` và `products(id, name, price, quantity, category, active, description)`.
- `onCreate()` tạo bảng, chèn tài khoản `admin / 123456` và 3 sản phẩm mẫu.
- Phương thức: `checkLogin`, `insertUser`, `insertProduct`, `getAllProducts`, `getProductById`, `updateQuantity`, `deleteProduct`, `getTotalValue`, `countLowStock` (thêm `countProducts`).
- Truy vấn có tham số (`?` + `selectionArgs`), Cursor đóng bằng try-with-resources, xử lý bảng rỗng.
- Không dùng Room, Firebase hay API.

## Công nghệ

- Java, giao diện XML, Android Studio
- `compileSdk`/`targetSdk` 37, `minSdk` 24 (Android 7.0 trở lên)
- AndroidX AppCompat, Material Components, RecyclerView
- Gradle 9.5.0, Android Gradle Plugin 9.3.3

## Cấu trúc mã nguồn

```
app/src/main/
├── java/com/nguyenduytu/quanlykhohang/
│   ├── Product.java                 # Lớp sản phẩm
│   ├── DatabaseHelper.java          # SQLiteOpenHelper: bảng, dữ liệu mẫu, truy vấn
│   ├── LoginActivity.java           # Màn hình 1: Đăng nhập
│   ├── MainActivity.java            # Màn hình 2: Thống kê + danh sách
│   ├── ProductAdapter.java          # Adapter của RecyclerView
│   ├── AddProductActivity.java      # Màn hình 3: Thêm sản phẩm
│   ├── ProductDetailActivity.java   # Màn hình 4: Chi tiết sản phẩm
│   └── AppUtils.java                # Định dạng tiền, xử lý tràn viền
└── res/                             # layout/, drawable/, values/
app/src/test/.../ProductTest.java    # Unit test
```

## Cách chạy

1. Mở Android Studio → **File › Open** → chọn thư mục project này, chờ Gradle Sync xong.
2. Chọn máy ảo hoặc điện thoại (Android 7.0+), nhấn **Run ▶**.
3. Đăng nhập bằng tài khoản `admin` / `123456`.

Chạy unit test: `gradlew.bat testDebugUnitTest`.

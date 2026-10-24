# PP.CAM - Picture Profile Preset Loader (Sony A7 Mark II)

**PP.CAM** là ứng dụng chạy trực tiếp trên máy ảnh Sony Alpha (tối ưu hóa cho **Sony A7 Mark II / ILCE-7M2** trên nền tảng PlayMemories Camera Apps), cho phép:
- Đọc các công thức Picture Profile (Film Simulation) chuẩn hóa từ thẻ nhớ dưới dạng file `.DAT` (64 bytes).
- Lướt xem các cuộn phim trực quan với giao diện **Băng chuyền cuộn film 35mm (Sony 9-Row Grid)**.
- Ghi đè trực tiếp $100\%$ thông số (25 thanh ghi PP + WB Kelvin + WB Shifts + EV Bias) vào **khe PP7** chỉ bằng một nút bấm.
- Tinh chỉnh thông số và lưu thành cuộn phim mới (`CUS000.DAT`, `CUS001.DAT`...) trực tiếp ngay trên thân máy ảnh.

---

## 🎞️ Công thức mặc định: Blue Velvet / Cinestill 50D
Ứng dụng tích hợp sẵn công thức film simulation **Cinestill 50D Blue Velvet** làm mặc định:
- **Black Level:** `0`
- **Gamma:** `Still`
- **Black Gamma:** `Middle`, Level `+7`
- **Knee:** `Manual 75%`, Slope `+4`
- **Color Mode:** `Still`
- **Saturation:** `+10`
- **Color Phase:** `-3`
- **Color Depth:** `R: +4`, `G: +5`, `B: -2`, `C: +3`, `M: +5`, `Y: +2`
- **Detail:** `Level: 0`, `Manual`, `V/H: +2`, `B/W: Type 3`, `Limit: 7`, `Crispening: 7`, `Hi-Light: 4`
- **White Balance:** `Kelvin 7400K`, Color Filter `B4 - M1.5`
- **Target Slot:** `PP7`

---

## 🎮 Bảng điều khiển phím cứng (Sony A7 II)

| Phím bấm | Chức năng | Mô tả chi tiết |
| :--- | :--- | :--- |
| **Vòng xoay (Control Wheel)**<br>*(hoặc Trái / Phải)* | **Lướt cuộn film** | Băng chuyền chuyển động mượt: cuộn giữa sáng nét ($100\%$), 2 cuộn hai bên nhỏ hơn và mờ $50\%$. |
| **Nút Giữa (Center)** | **Gán vào PP7** | Ghi tức thì toàn bộ 25 thông số PP + WB + EV vào khe PP7 và lưu vào Flash NVRAM (`Backup_sync_all`). |
| **C2** | **Yêu thích (Favorite)** | Đánh dấu hoặc bỏ đánh dấu ngôi sao vàng $\bigstar$ trên đầu cuộn film. |
| **C3** | **Reset PP7** | Hiện hộp thoại xác nhận khôi phục PP7, WB (AWB), EV ($0.0$) về mặc định ban đầu. |
| **C4 (Thùng rác)** | **Xoá cuộn film** | Mở bảng hỏi: *"Xoá cuộn film khỏi thẻ nhớ?"* để tránh vô tình bấm nhầm. |
| **Fn** | **Chỉnh sửa (Editor)** | Tinh chỉnh tạm thời các thông số. Cuộn xuống mục cuối cùng để **Lưu công thức** thành file `CUSxxx.DAT` với màu vỏ film Custom (Tím Neon). Thoát Fn không lưu sẽ tự động huỷ thay đổi. |
| **MENU** | **Thoát ứng dụng** | Thoát app về màn hình chụp thông thường. |
| **Nút Chụp (Shutter)** | **Chụp ngay** | Thoát nhanh và kích hoạt chụp ảnh với PP7 vừa nạp. |

---

## 💾 Cấu trúc File Preset `.DAT` (64 Bytes Chuẩn)
Mọi preset lưu trữ trên thẻ nhớ tại thư mục:  
`/mnt/sdcard/PP_DAT/` (ví dụ: `PORTRA.DAT`, `GOLD200.DAT`, `CUS000.DAT`).

- Kích thước: Đúng 64 Bytes cố định.
- Mã định danh: `PPDT` (Picture Profile DaTa).
- Mã kiểm tra: `CRC-16-CCITT` tính trên 62 bytes đầu tiên.

---

## 🛠️ Công cụ hỗ trợ (Tools)

### 1. Tạo file `.DAT` bằng Python (`tools/make_dat.py`):
```bash
python3 tools/make_dat.py
```
Tự động sinh ra 2 file mẫu chuẩn trong thư mục `presets/`:
- `DEFAULT.DAT` (Cinestill 50D Blue Velvet)
- `KODAK200.DAT` (Kodak Gold 200)

### 2. Đọc và kiểm tra thông số file `.DAT` (`tools/parse_dat.py`):
```bash
python3 tools/parse_dat.py presets/DEFAULT.DAT
```

---

## 🚀 Hướng dẫn Cài đặt lên máy ảnh Sony A7 II

1. Tải file **`PPCAM-dev.apk`** từ mục Releases của repository hoặc tự build bằng `./build.sh`.
2. Kết nối máy ảnh Sony A7 II với máy tính qua cáp USB (chọn chế độ **USB Mass Storage** trên máy ảnh).
3. Sử dụng công cụ [Sony-PMCA-RE (pmca-gui)](https://github.com/ma1co/Sony-PMCA-RE/releases) để cài đặt file `.apk` vào máy ảnh.
4. Rút cáp, mở menu máy ảnh $\rightarrow$ **Application List** $\rightarrow$ chọn **PP.CAM** và tận hưởng!

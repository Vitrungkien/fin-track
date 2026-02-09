# Migration Guide: Email to Username Authentication

## Thay đổi

Hệ thống đã được cập nhật để sử dụng **username** thay vì **email** cho đăng nhập.

### Các thay đổi chính:

1. **User Entity**: Thêm trường `username` (bắt buộc, duy nhất)
2. **Authentication**: Đăng nhập bằng username thay vì email
3. **Registration**: Yêu cầu username khi đăng ký (3-20 ký tự)
4. **UI**: Cập nhật form đăng nhập và đăng ký

## Hướng dẫn Migration

### Option 1: Database mới (Khuyến nghị cho development)

Nếu bạn đang phát triển và không cần giữ dữ liệu cũ:

```sql
DROP DATABASE IF EXISTS `fin-track`;
CREATE DATABASE `fin-track`;
```

Sau đó khởi động lại ứng dụng. Hibernate sẽ tự động tạo schema mới với trường `username`.

### Option 2: Giữ dữ liệu hiện có

Nếu bạn cần giữ dữ liệu hiện có:

1. **Chạy migration script**:
   ```bash
   mysql -u fintrack_user -p fin-track < src/main/resources/db/migration/add_username_column.sql
   ```

2. **Cập nhật username cho các user hiện có**:
   - Script sẽ tự động tạo username từ phần trước @ của email
   - Ví dụ: `user@example.com` → username: `user`
   - Bạn nên cập nhật lại username phù hợp sau đó

3. **Khởi động lại ứng dụng**

## Thông tin đăng nhập mặc định

Sau khi migration, user mặc định sẽ là:
- **Username**: `admin`
- **Password**: `kienvt@123`
- **Email**: `kienvt@vt.com`

## Testing

1. Đăng nhập với username `admin` và password `kienvt@123`
2. Tạo user mới với username, email và password
3. Đăng xuất và đăng nhập lại với username mới

## Rollback (Nếu cần)

Nếu muốn quay lại sử dụng email:

```sql
ALTER TABLE users DROP COLUMN username;
```

Sau đó revert lại code về commit trước đó.

## Lưu ý

- Username phải duy nhất trong hệ thống
- Username chỉ chấp nhận 3-20 ký tự
- Email vẫn được lưu và có thể dùng cho các mục đích khác (reset password, notifications, etc.)
- SecurityContext vẫn trả về username (không phải email) sau khi authentication

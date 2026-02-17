# Project Guide: FinTrack

Tài liệu này cung cấp cái nhìn tổng quan về cấu trúc và công nghệ của dự án **FinTrack** (Quản lý chi tiêu).

## 1. Sơ đồ cây thư mục rút gọn
```text
fin-track/
├── .mvn/                   # Cấu hình Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/finance/tracker/
│   │   │   ├── config/             # Cấu hình hệ thống (Security, MVC, v.v.)
│   │   │   ├── controller/         # Xử lý Request (Web & API)
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   ├── entity/             # JPA Entities (Database mapping)
│   │   │   ├── repository/         # Tầng truy vấn dữ liệu (Spring Data JPA)
│   │   │   ├── security/           # JWT, Authentication, Authorization logic
│   │   │   ├── service/            # Tầng nghiệp vụ (Business Logic)
│   │   │   └── FinTrackApplication.java # Entry point của Backend
│   │   └── resources/
│   │       ├── db/migration/       # Các file script SQL migration
│   │       ├── static/             # Assets tĩnh (CSS, JS, Images)
│   │       │   ├── css/            # Stylesheets (dashboard.css)
│   │       │   └── js/             # Script Client-side (auth, dash, v.v.)
│   │       ├── templates/          # Giao diện Thymeleaf (HTML)
│   │       ├── application.yml     # Cấu hình ứng dụng chính
│   │       └── application.properties
├── pom.xml                 # Quản lý dependencies (Maven)
└── docker-compose.yml      # Cấu hình Docker (Database MySQL)
```

## 2. Chức năng các thư mục chính
- **`controller/`**: Chứa các lớp định nghĩa các endpoint. Phân tách rõ ràng giữa Web Controller (trả về View) và potentially REST Controller (trả về JSON).
- **`entity/`**: Định nghĩa cấu trúc bảng trong cơ sở dữ liệu (User, Category, Transaction, Budget).
- **`service/`**: Nơi thực hiện các tính toán, xử lý logic nghiệp vụ trước khi lưu vào DB hoặc trả về Controller.
- **`security/`**: Xử lý bảo mật bằng JWT (JSON Web Token), bao gồm việc tạo, xác thực token và phân quyền người dùng.
- **`templates/`**: Chứa giao diện người dùng sử dụng Thymeleaf, cho phép render dữ liệu năng động từ backend.
- **`static/js/`**: Chứa các module JavaScript xử lý logic tại trình duyệt như gọi API, cập nhật UI mà không cần load lại trang.

## 3. Các 'Entry Point' quan trọng
- **Backend**: `com.finance.tracker.FinTrackApplication.java` (Run as Spring Boot App).
- **Frontend (View chính)**: `dashboard.html` (Trình điều khiển trung tâm sau khi đăng nhập).
- **Auth Flow**: `login.html` và `register.html` là điểm bắt đầu cho người dùng.
- **Cấu hình DB**: `application.yml` (Đang sử dụng MySQL tại cổng 3307).

## 4. Công nghệ & Thư viện chính
- **Cốt lõi**: Java 21, Spring Boot 3.5.10.
- **Cơ sở dữ liệu**: MySQL (Driver: `mysql-connector-j`), Spring Data JPA/Hibernate.
- **Bảo mật**: Spring Security, JJWT (JSON Web Token).
- **Giao diện**: Thymeleaf, Bootstrap 5.
- **Tiện ích**: 
    - **Lombok**: Giảm thiểu boilerplate code (Getter, Setter).
    - **Apache POI**: Hỗ trợ xuất dữ liệu ra file Excel.
    - **Jackson**: Xử lý dữ liệu JSON (đặc biệt là kiểu thời gian Java 8+).
- **Quản lý dự án**: Maven.

---
*Ghi chú: File này được tạo tự động để hỗ trợ nắm bắt dự án nhanh chóng.*

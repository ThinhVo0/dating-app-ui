# DatingApp

## Giới thiệu

**DatingApp** là ứng dụng hẹn hò trên nền tảng Android, cho phép người dùng tạo hồ sơ cá nhân, tìm kiếm, kết nối, trò chuyện và tương tác với những người dùng khác dựa trên sở thích, vị trí và các tiêu chí cá nhân.

## Tính năng chính

- Đăng ký, đăng nhập, quên mật khẩu, đặt lại mật khẩu
- Tạo, cập nhật và quản lý hồ sơ cá nhân
- Tìm kiếm, lọc người dùng theo nhiều tiêu chí (vị trí, sở thích, v.v.)
- Gợi ý kết đôi, hiển thị danh sách phù hợp
- Chat, nhắn tin thời gian thực (WebSocket)
- Thông báo, báo cáo người dùng, quản lý danh sách thích
- Giao diện trực quan, dễ sử dụng

## Công nghệ sử dụng

- **Ngôn ngữ:** Java
- **Nền tảng:** Android SDK (minSdk 26, targetSdk 35)
- **Giao diện:** XML Layout, ViewBinding
- **Thư viện chính:**
  - [Retrofit](https://square.github.io/retrofit/) & OkHttp: Kết nối API, xử lý RESTful
  - [Glide](https://github.com/bumptech/glide): Load và hiển thị ảnh
  - [Google Play Services Location](https://developers.google.com/android/guides/overview): Lấy vị trí người dùng
  - [Lombok](https://projectlombok.org/): Tự động sinh code Java
  - [Flexbox](https://github.com/google/flexbox-layout): Bố cục linh hoạt
  - [Spring WebSocket, STOMP, SockJS](https://spring.io/guides/gs/messaging-stomp-websocket/): Chat thời gian thực
  - [Material Components](https://material.io/develop/android): Giao diện hiện đại

## Cấu trúc thư mục

```
datingapp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/datingapp/
│   │   │   │   ├── activity/         # Các màn hình chính (Login, Register, Main, Chat, Profile, ...)
│   │   │   │   ├── fragment/         # Các fragment chức năng
│   │   │   │   ├── model/            # Định nghĩa model dữ liệu
│   │   │   │   ├── network/          # Kết nối API, WebSocket
│   │   │   │   ├── adapter/          # Adapter cho RecyclerView, ViewPager
│   │   │   │   ├── dto/, enums/, util/
│   │   │   ├── res/layout/           # Giao diện XML
│   │   │   ├── AndroidManifest.xml
│   ├── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
```

## Cài đặt & chạy thử

1. Clone repository về máy:
   ```
   git clone <repo-url>
   ```
2. Mở bằng **Android Studio** (Bản mới nhất khuyến nghị).
3. Sync Gradle, cài đặt các dependencies.
4. Kết nối thiết bị ảo hoặc thật, nhấn **Run** để build và trải nghiệm ứng dụng.

## Quyền ứng dụng

- Truy cập Internet, vị trí, bộ nhớ, thông báo (xem chi tiết trong `AndroidManifest.xml`).

## Đóng góp

Mọi đóng góp, báo lỗi hoặc ý tưởng mới đều được hoan nghênh! Vui lòng tạo issue hoặc pull request. 

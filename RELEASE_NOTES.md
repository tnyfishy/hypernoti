HyperNoti preview for Android 8.0+ / Bản thử nghiệm HyperNoti cho Android 8.0 trở lên.

- English and Vietnamese interface / Giao diện Anh và Việt.
- Google Play services detection, installed-app picker, and persistent configuration checklist.
- Default mode needs no root. Advanced mode supports explicit Shizuku or root requests for Android Doze exemption and background app operations.
- Xiaomi autostart is configured through the HyperOS settings screen; automatic Xiaomi-specific permission changes are not implemented.

Download **HyperNoti-preview.apk** below. This is a debug-signed APK for direct testing, not a production release. Preview build signing keys may differ between builds; updating across different keys requires uninstalling the previous APK, which clears its checklist.

Host validation: APK build and 13 unit/UI tests passed; lint has no errors. QUERY_ALL_PACKAGES has a documented, narrowly scoped lint exemption for this app-management tool. Real HyperOS FCM delivery, Shizuku effects and root-manager consent have not been verified on physical hardware. Accepted commands and checklist state do not prove FCM delivery.

Tải **HyperNoti-preview.apk** ở phần Assets. Đây là APK ký debug để thử nghiệm. Mặc định không cần root; chỉ yêu cầu Shizuku/root khi bạn chọn thao tác nâng cao và xác nhận. Bản này không bảo đảm giữ Google Play services luôn sống hoặc nhận FCM trên mọi ROM. Hãy kiểm tra thông báo thật khi tắt màn hình, sau thời gian chờ và sau reboot theo README.

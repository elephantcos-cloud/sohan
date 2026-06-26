# Sohan — Wireless ADB Bridge

Shizuku-এর মতো একটি Wireless ADB bridge।
Root ছাড়াই Android 11+ এ shell-level permission।

## Features
- Auto-connect — Wireless Debugging চালু থাকলে নিজেই কানেক্ট হয়
- App cache viewer — কোন app-এ কত cache তা দেখায়
- Cache clear — প্রতিটা app-এর cache আলাদা বা সব একসাথে clear
- Force Stop — যেকোনো app force stop করা
- Client SDK — অন্য app Sohan ব্যবহার করতে পারে AIDL-এর মাধ্যমে
- Permission system — Shizuku-এর মতো Allow/Deny dialog
- Foreground Service — background-এ ADB session ধরে রাখে
- Theme — Light / Dark / System

## Build করো

### Termux-এ (প্রথমবার)
```bash
# Gradle wrapper তৈরি করো
gradle wrapper --gradle-version=8.4

# Build
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions (Recommended)
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/elephantcos-cloud/sohan.git
git push -u origin main
# Actions tab → Build Sohan APK → Artifacts থেকে APK নামাও
```

### Signed Release
```bash
git tag v1.0.0
git push origin v1.0.0
# CREATE_KEYSTORE.md দেখো
```

## প্রথমবার ব্যবহার
1. APK install করো
2. Settings → Developer Options → Wireless debugging → চালু করো
3. Sohan খোলো — Setup screen দেখাবে
4. Usage Access → Allow করো
5. Auto-connect হবে → "Allow ADB debugging?" → Allow
6. সব হয়ে গেলে "Go to App" চাপো

## অন্য App থেকে ব্যবহার
`CLIENT_SDK_GUIDE.md` এবং `sohan-client/` module দেখো।

## AIDL Service Action
```
com.shohan.sohan.SERVICE
```

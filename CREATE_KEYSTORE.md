# Keystore তৈরি করার নিয়ম

Signed release APK বানাতে একটা keystore লাগে।
**একবারই তৈরি করতে হবে — এটা হারালে আর আপডেট দেওয়া যাবে না।**

## Termux-এ keystore তৈরি করো

```bash
keytool -genkeypair \
  -v \
  -keystore sohan-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias sohan \
  -storepass তোমার_পাসওয়ার্ড \
  -keypass তোমার_পাসওয়ার্ড \
  -dname "CN=Sohan, OU=Dev, O=Shohan, L=Mymensingh, S=Bangladesh, C=BD"
```

এরপর জিজ্ঞেস করবে — সব তথ্য দাও অথবা Enter চাপো।

## GitHub Secrets-এ যোগ করো

GitHub repository → Settings → Secrets and variables → Actions → New repository secret

### Secret 1: KEYSTORE_BASE64
```bash
# Termux-এ এই command চালাও, output copy করো
base64 sohan-release.jks
```
সেই output টা `KEYSTORE_BASE64` secret-এ paste করো।

### Secret 2: KEYSTORE_PASSWORD
keystore বানানোর সময় যে password দিয়েছিলে।

### Secret 3: KEY_ALIAS
`sohan` (উপরে `-alias sohan` দিয়েছি)

### Secret 4: KEY_PASSWORD
key-এর password (keystore password-এর মতোই)

## Release বানাতে tag push করো

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions → "Signed Release APK" job চলবে →
APK automatically GitHub Releases-এ upload হবে।

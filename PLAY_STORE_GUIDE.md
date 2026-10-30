# R7 Play - Google Play Store par approve karwane ki guide

App ka package: `com.r7studio.r7Playapp`  |  Naam: R7 Play  |  Poora native Kotlin (Jetpack Compose) UI.
Sirf games khelte waqt (game ke andar) ek WebView use hota hai, kyunki aapke games web games hain.

## 1. Build se pehle (zaroori)
Support e-mail `rstudioofficial3@gmail.com` pehle se `Config.kt`, Privacy Policy aur Terms me set hai.
Play Console me bhi wahi e-mail daalna aur us inbox ko check karte rehna.

## 2. Apna permanent signing key (ek baar, bahut zaroori)
Play Store ke liye har update isi key se sign hona chahiye. Termux me:

```
pkg install openjdk-17 -y
keytool -genkeypair -v -keystore r7.jks -alias r7key -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 r7.jks > r7.b64
```
GitHub repo > Settings > Secrets and variables > Actions > New repository secret:
- `KEYSTORE_BASE64` = r7.b64 file ka poora text
- `KEYSTORE_PASSWORD` = keystore ka password
- `KEY_ALIAS` = r7key
- `KEY_PASSWORD` = key ka password

`r7.jks` ko 2 jagah backup rakho. Kho gaya to app update nahi ho payega. Isse GitHub par upload mat karna.

## 3. Build
GitHub > Actions > "Build R7 Play" > Run workflow. Artifacts se:
- `R7Play-signed-aab`  -> Play Console par upload karo
- `R7Play-signed-apk`  -> apne phone me test karo

## 4. Play Console me kya bharna hai
- **Privacy policy URL**: `docs/privacy-policy.html` ko internet par public karo (repo public ho to GitHub Pages: Settings > Pages > Branch main /docs). Private repo me Pages free nahi chalta.
- **App category**: Games (ya Entertainment). **Free** app.
- **Content rating**: questionnaire sach sach bharo (games me violence/ads ho to batao).
- **Target audience**: 13+ ya 18+ chuno. Bachchon (Kids/Families) ko target karoge to bahut zyada sakht rules lagte hain.
- **Data safety form**: app khud koi personal data collect nahi karta (koi account, ads ya analytics nahi). Par aapke games ke andar agar ads/analytics/login hai to wo bhi batana padega. Games ka sach pata karke hi "No data collected" chuno.
- **Ads**: games ke andar ads laga rakhe hain, isliye "Contains ads: Yes" chuno, aur Data safety me advertising ID / device ids ka sach batao.
- **Store listing**: icon `R7Play_PlayStore_icon_512.png`, feature graphic `R7Play_feature_graphic_1024x500.png`, phone screenshots (kam se kam 2, app chalake khud lo), short description (80 akshar) aur full description.
- **App access**: koi login nahi, "All functionality available without restrictions".

## 5. Reject hone se bachne ke liye
- **Games ke rights**: sirf wahi games rakho jo aapke apne hain ya jinki permission/license aapke paas hai. Doosron ke games bina permission ke daaloge to "Intellectual property" policy me app hata di jati hai.
- **Naya personal developer account** ho to Google pehle **closed testing** maangta hai (kuch testers, kuch din). Play Console me jo likha ho wahi follow karo, ye rule badalta rehta hai.
- Game ke links **https** hone chahiye (app me http band hai).
- Description me jhoothe dave mat likho (jaise "#1 game", "official <kisi aur ka brand>").
- App ke naam/icon me kisi aur company ka naam ya logo mat rakho.
- Pehli baar review me 1-7 din (kabhi zyada) lag sakte hain.

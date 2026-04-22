# Beta Recruitment Templates

Plantillas listas para copiar/pegar en cada plataforma. Reemplaza los placeholders entre `{llaves}` antes de postear.

---

## 1. Reddit — r/TestMyApp / r/AndroidAppTesting

**Título:**
```
[F2F] Wey Ya! — Offline call blocker (0 internet permission). Need 12 testers for 14 days closed testing
```

**Cuerpo:**
```
Hi everyone! I'm looking for 12 testers to help me meet Google Play's 14-day closed testing requirement.

**App:** ¡Wey Ya! — a spam call blocker for Android
**Unique angle:** ZERO internet permission. No analytics, no ads, 100% offline. Everything runs locally on your phone.

**What it does:**
- Blocks unknown/spam calls via Android's CallScreeningService
- Two modes: block unknowns only, or block all
- Urgency bypass: if someone calls N times in X minutes, it rings through
- Schedules (e.g. only block 22:00–07:00)
- Whitelist from contacts
- Home screen widget + Quick Tile
- Available in Spanish, English, Portuguese, Hindi, Indonesian

**What I need:**
- Join the Google Group: {LINK_GOOGLE_GROUP}
- Accept testing: {LINK_PLAY_TESTING}
- Install from Play Store and use it at least once a day for 14 days

**F2F (feedback-for-feedback):** drop your app link in comments and I'll test yours back.

Source code: https://github.com/samumirandam/wey-ya (GPL-3.0)
```

---

## 2. Reddit — r/privacy / r/degoogle / r/fossdroid

**Título:**
```
Wey Ya! — Open-source Android call blocker with ZERO internet permission (beta testers needed)
```

**Cuerpo:**
```
Sharing a project I've been building for people tired of "privacy" apps that phone home.

**¡Wey Ya!** is a call blocker that has literally no INTERNET permission in its manifest. It cannot connect to the network even if it wanted to. No analytics, no crash reporting to third parties, no ads, no ID of any kind. All data (whitelist, block log, schedules) stays in a local Room database.

- **Source:** https://github.com/samumirandam/wey-ya
- **License:** GPL-3.0
- **Min SDK:** Android 10 (API 29)

Looking for beta testers to help me publish on Play Store (Google requires 12 testers × 14 days for new apps). If you're interested:

1. Join: {LINK_GOOGLE_GROUP}
2. Opt in: {LINK_PLAY_TESTING}
3. Keep it installed 14 days

Happy to answer questions about the architecture or the zero-network guarantee.
```

---

## 3. Discord / Telegram (grupos tipo "Play Store 14 Days Testing")

```
Hi! 👋 Looking for 12 testers for 14-day closed testing.

📱 App: ¡Wey Ya! — offline call blocker for Android
🔒 Zero internet permission, no analytics, no ads, fully local
🌍 Languages: ES / EN / PT / HI / IN
📦 Min SDK: Android 10

Join Google Group: {LINK_GOOGLE_GROUP}
Opt in to test: {LINK_PLAY_TESTING}

F2F welcome — drop your link and I'll test back. DM me once you've opted in so I can track testers.

Source: github.com/samumirandam/wey-ya
```

---

## 4. X / Twitter

```
Looking for 12 Android beta testers for 14 days 🙏

¡Wey Ya! — an offline call blocker with ZERO internet permission. No analytics, no ads, 100% local.

Opt in: {LINK_PLAY_TESTING}
Source: github.com/samumirandam/wey-ya

#AndroidDev #buildinpublic #PlayStoreTesting
```

---

## 5. LinkedIn / Indie Hackers

```
I'm launching ¡Wey Ya!, an open-source Android call blocker with a strict privacy guarantee: zero internet permission. It cannot phone home, ever.

Google Play now requires 12 testers to run the app for 14 days before a new account can publish to production. If you have 2 minutes:

1. Join the tester group: {LINK_GOOGLE_GROUP}
2. Opt in on Play: {LINK_PLAY_TESTING}
3. Install and keep it on your phone for 14 days

The app is GPL-3.0, built with Jetpack Compose + Room + Hilt. Available in 5 languages.

Source code: https://github.com/samumirandam/wey-ya
```

---

## 6. Versión corta en español (para grupos hispanos de WhatsApp/Telegram)

```
Hola! 👋 Necesito 12 testers para Android durante 14 días.

📱 ¡Wey Ya! — bloqueador de llamadas offline
🔒 0 permisos de internet, 0 analytics, 100% local
💚 GPL-3.0, código abierto

1. Únete al grupo: {LINK_GOOGLE_GROUP}
2. Acepta el test: {LINK_PLAY_TESTING}
3. Deja la app instalada 14 días

Si también buscas testers, comparto la tuya. Mándame DM cuando entres.

Código: github.com/samumirandam/wey-ya
```

---

## Placeholders a reemplazar

- `{LINK_GOOGLE_GROUP}` — URL del Google Group que crees para los testers (ej: `https://groups.google.com/g/wey-ya-beta`)
- `{LINK_PLAY_TESTING}` — URL de opt-in del closed testing de Play Console (ej: `https://play.google.com/apps/testing/com.weyya.app`)

## Checklist antes de postear

- [ ] Google Group creado y configurado como público
- [ ] Closed testing track configurado en Play Console con el Google Group como audiencia
- [ ] APK/AAB subido al track
- [ ] Opt-in link copiado del Play Console
- [ ] `BETA_TESTING.md` actualizado con los links reales
- [ ] Respeta las reglas de cada subreddit (algunos exigen flair `[F2F]` o `[Testing]`)

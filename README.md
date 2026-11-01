# CartFinder ⛳ - Golf Cart GPS & Compass Web App

**CartFinder** is a mobile-optimized Progressive Web Application (PWA) designed to run flawlessly on both **iPhone (iOS Safari)** and **Android (Google Chrome)**, as well as desktop browsers. You can push this repository directly to GitHub and host it for free with **GitHub Pages**!

---

## 🚀 How to get to the application on your phone

### Step 1: Open your web browser
1. On your device, open your default web browser (Safari, Chrome, etc)

### Step 2: Navigate to the page
1. Navigate to https://therealalvin.github.io/CartFinderWeb/

---

## 📱 How to Install on Your Phone (Full Screen App)

### On iPhone (iOS Safari)
1. Open your GitHub Pages link in **Safari**.
2. Tap the **Share** button (the square with an arrow pointing up at the bottom).
3. Scroll down and tap **"Add to Home Screen"**.
4. Tap **Add** in the top right corner.
5. Launch **CartFinder** from your home screen — it will open in fullscreen standalone app mode!
6. *Tip*: Tap the menu (⋮) -> **Calibrate / Enable Compass** to allow iOS motion sensors to rotate the navigation needle in real-time as you turn your body!

### On Android Phone (Google Chrome)
1. Open your GitHub Pages link in **Chrome**.
2. Tap the **Three Dots (⋮)** in the top right corner.
3. Tap **"Install app"** or **"Add to Home screen"**.
4. Confirm installation. The CartFinder icon will appear in your app drawer and home screen with offline caching enabled!

---

## ✨ Features

- 📍 **High-Precision Geolocation**: Uses real-time GPS tracking with accuracy radius indicators and status badge.
- 🧭 **Rotating Compass HUD**: Live compass rose with dynamic rotating needle pointing directly toward your parked cart as you rotate your phone. Supports iOS WebKit compass heading and Android device orientation.
- 🛰️ **Interactive Map & Satellite Golf Course Imagery**: Built with Leaflet & OpenStreetMap, plus a one-tap toggle for high-resolution satellite imagery (ideal for viewing golf fairways, greens, sand traps, and cart paths).
- ⛳ **One-Tap "Park Cart Here"**: Quick-save your current location with custom tags (*Clubhouse, Hole 1, Hole 9 Turn, Range, Cart Barn*) and notes (*e.g. "Cart #48, near oak tree"*).
- 📏 **Customizable Units**: Toggle between **Yards** (golf standard), **Meters**, and **Feet**.
- 🎉 **Proximity Arrival Alerts**: Automatically triggers when you are within 10 meters / 11 yards of your cart, with synthesized musical chime and haptic vibration feedback.
- 🗺️ **Turn-by-Turn Deep Links**: Direct one-tap buttons to open walking directions in **Apple Maps** (on iPhone) or **Google Maps** (on Android).
- 🎮 **Demo / Simulation Mode**: Built-in simulator to test walking, distance countdown, compass needle, and arrival fanfare indoors or on a computer without having to be at a golf course.
- 📴 **Offline Ready (PWA)**: Includes Service Worker (`sw.js`) and Web App Manifest (`manifest.json`) for instant loading even in poor reception areas on the golf course.

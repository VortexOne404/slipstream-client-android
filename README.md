# Slipstream Client for Android

An **Android VPN client** inspired by *Slipstream-style* architectures.
It captures **all system traffic** using Android `VpnService` (TUN),
then forwards packets through a **tun2socks** pipeline into a **Slipstream transport**
(e.g. **SOCKS5**).

---

## Status

![Latest Release](https://img.shields.io/github/v/release/VortexOne88/slipstream-client-android)
![Downloads](https://img.shields.io/github/downloads/VortexOne88/slipstream-client-android/total)
![Stars](https://img.shields.io/github/stars/VortexOne88/slipstream-client-android)
![Forks](https://img.shields.io/github/forks/VortexOne88/slipstream-client-android)

---

## Screenshots

> Place screenshots in: `docs/screenshots/`

<p float="left">
  <img src="docs/screenshots/Screenshot_20260131_023837_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_023903_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_023926_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_023940_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_024504_Slipstream%20Client.png" width="220" />
</p>

---

## Overview

**Slipstream Client** is a lightweight and modular VPN client for Android:

- Captures all device traffic via Android `VpnService` (TUN)
- Uses **tun2socks** to convert IP packets (TCP/UDP/DNS) into user-space streams
- Forwards traffic into a **Slipstream transport layer**
- Supports **full-tunnel** mode (routes `0.0.0.0/0`)

The design goal is a small Android shell (UI + lifecycle) with a transport backend
that can be extended and swapped easily.

---

## Architecture

### Data Path

```text
Android Apps (system traffic)
   |
   v
VpnService TUN (fd)
   |
   v
tun2socks
(IP packets -> streams)
   |
   v
Slipstream Transport Layer
(SOCKS5 / custom tunnel)
   |
   v
Internet
```

### Components

- **Android `VpnService`**
  - Creates the device-wide TUN interface
  - Installs routes + DNS
  - Runs as a foreground service for stability
  - Protects the tunnel socket to avoid routing loops

- **tun2socks core**
  - Reads/writes raw IP packets from the TUN FD
  - Maps TCP/UDP flows into user-space connections
  - Forwards flows into the transport backend

- **Slipstream transport backend**
  - Provides proxied connectivity (default: SOCKS5)
  - Designed to be pluggable (swap transports without rewriting the app)

---

## Features

- ✅ **Full-tunnel** mode (proxy all system traffic)
- ✅ **SOCKS5 backend**
- ✅ Modular design for experimenting with new transports
- ✅ Import/Export configs using URI format: `slipstream://{base64}`

> Note: UDP/IPv6 behavior depends on the tun2socks implementation and the chosen transport.
> DNS handling is recommended to avoid leaks.

---

## Requirements

- Android 8.0+ (recommended)
- Android Studio + NDK (for native tun2socks components)
- A reachable transport endpoint (e.g. a SOCKS5 server)

---

## Setup

Clone with submodules:

```bash
git clone --recurse-submodules https://github.com/VortexOne88/slipstream-client-android.git
cd slipstream-client-android
```

Build:

```bash
./gradlew assembleRelease
```

---

## License

MIT

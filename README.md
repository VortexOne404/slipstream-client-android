
# Slipstream Client for Android

An **Android VPN client** inspired by *Slipstream-style* architectures.
It captures **all system traffic** using Android `VpnService` (TUN),
then forwards packets through a **tun2socks** pipeline into a **Slipstream transport**
(e.g. **SOCKS5**).

---

## Documentation

- [English](README.md)
- [فارسی](README_FA.md)

---

## Update

To improve performance and increase connection speed, the new version replaces the previous dependency:

https://github.com/Mygod/slipstream-rust

with the optimized implementation:

https://github.com/Fox-Fig/slipstream-rust-plus

**Improvements:**
- Higher throughput
- Lower latency
- Better connection stability
- More efficient resource usage

---

## Protocol Flow

```
Client Apps
    ↓
tun2socks
    ↓
Slipstream Client
    ↓
DNS Resolution
    ↓
Slipstream Server
    ↓
Internet Access
```

---

## Status

![Latest Release](https://img.shields.io/github/v/release/VortexOne404/slipstream-client-android)
![Downloads](https://img.shields.io/github/downloads/VortexOne404/slipstream-client-android/total)
![Stars](https://img.shields.io/github/stars/VortexOne404/slipstream-client-android)
![Forks](https://img.shields.io/github/forks/VortexOne404/slipstream-client-android)

---

## Screenshots

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

## Server Setup

To set up a Slipstream server, use:

https://github.com/AliRezaBeigy/slipstream-rust-deploy

**One-command installation:**

```bash
bash <(curl -Ls https://raw.githubusercontent.com/AliRezaBeigy/slipstream-rust-deploy/master/slipstream-rust-deploy.sh)
```

---

## Architecture

### Data Path

```text
Client Apps
   |
   v
VpnService TUN (fd)
   |
   v
tun2socks
(IP packets -> streams)
   |
   v
Slipstream Client Core
   |
   v
DNS Processing
   |
   v
Slipstream Server
   |
   v
Internet
```

---

### Components

#### Android `VpnService`
- Creates the device-wide TUN interface
- Installs routes + DNS
- Runs as a foreground service for stability
- Protects the tunnel socket to avoid routing loops

#### tun2socks core
- Reads/writes raw IP packets from the TUN FD
- Maps TCP/UDP flows into user-space connections
- Forwards flows into the transport backend

#### Slipstream Client Core
- Handles transport logic
- Manages DNS routing
- Connects to Slipstream server

#### Slipstream transport backend
- Default: SOCKS5
- Designed to be pluggable (swap transports without rewriting the app)

---

## Features

- Full-tunnel mode (proxy all system traffic)
- SOCKS5 backend
- Modular design for experimenting with new transports
- Import/Export configs using URI format: `slipstream://{base64}`
- Designed for research and custom transport development

> Note: UDP/IPv6 behavior depends on the tun2socks implementation and the chosen transport.  
> Proper DNS configuration is recommended to avoid leaks.

---

## Requirements

- Android 8.0+ (recommended)
- Android Studio
- Android NDK (for native tun2socks components)
- A reachable transport endpoint (e.g. a Slipstream with SOCKS5 server)

---

## Setup

Clone with submodules:

```bash
git clone --recurse-submodules https://github.com/VortexOne404/slipstream-client-android.git
cd slipstream-client-android
```

Build:

```bash
./gradlew assembleRelease
```

---

## Support

Telegram: https://t.me/VortexOne
Telegram Channel: https://t.me/silk_road_community

---

## License

MIT

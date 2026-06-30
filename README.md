# wayland-robot-java

[![Status](https://img.shields.io/badge/status-planted%20flag-d97706?style=flat-square&logo=github)](.) [![JDK](https://img.shields.io/badge/JDK-11%2B-1f883d?style=flat-square&logo=openjdk)](.) [![License](https://img.shields.io/badge/license-Apache%202.0-0a66c2?style=flat-square&logo=apache)](LICENSE) [![Upstream](https://img.shields.io/badge/upstream-Project%20Wakefield-grey?style=flat-square&logo=openjdk)](https://wiki.openjdk.org/display/wakefield/)

> A drop-in `java.awt.Robot` replacement for Wayland sessions, built on `xdg-desktop-portal` `RemoteDesktop`. Buys back five years on Java + Wayland before OpenJDK Wakefield lands.

## Why this exists

`java.awt.Robot` does not work on pure Wayland sessions. Mouse and keyboard events synthesized through `Robot.mousePress()` or `Robot.keyPress()` are silently dropped — no error, no exception, no event ever reaches the focused window. This is tracked upstream as [JDK-8280983](https://bugs.openjdk.org/browse/JDK-8280983), and it has been the number-one blocker for every Java project that does input synthesis: accessibility tools, headless test rigs, remote-control utilities, screen reader bridges, the entire Sikuli-derived stack.

The official answer is [OpenJDK Project Wakefield](https://wiki.openjdk.org/display/wakefield/), a multi-year effort led by Oracle and Red Hat to bring native Wayland support to the JDK. Wakefield is the right long-term direction. But it will arrive in JDK 25 or 26 GA at the earliest, and won't reach mainstream LTS adoption before 2030. Every Java project running on JDK 11, 17, or 21 today is stuck.

This library is the bridge. It aims to let any Java project on JDK 11 and up call `Robot`-style input synthesis on Wayland today, via `xdg-desktop-portal` `RemoteDesktop` — the same path Wakefield is going to use anyway, exposed at the library layer instead of the JDK layer.

## What this is, and what it is not

This is **not** a Wakefield replacement. Wakefield is a full toolkit overhaul inside the JDK itself; this is a small, focused library that solves one specific problem. When Wakefield ships in your JDK, both implementations should coexist, and switching between them ought to be a one-line change.

The goal is to give the Java ecosystem a working answer right now, so projects can stop documenting Wayland as "unsupported" and start shipping.

## Status

Early planning. No release yet. The repository exists to plant the flag publicly, gather feedback, and start a conversation with anyone who has been blocked on the same problem.

If this matches a need you have — watch the repo, open an issue describing your use case (window manager, target compositor, JDK version, what you are trying to automate), and check back in a few weeks.

## License

Apache 2.0 — see [LICENSE](LICENSE).

🦎

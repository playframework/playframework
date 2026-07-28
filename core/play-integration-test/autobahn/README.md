<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Autobahn WebSocket conformance harness

This opt-in harness runs the [Autobahn WebSocket Testsuite](https://github.com/crossbario/autobahn-testsuite) against a real Play server. It exercises Play's shared WebSocket protocol handling through either the Netty or Pekko HTTP backend.

Docker is required. The official Autobahn image is currently published only for `linux/amd64`; the harness pins that platform explicitly, so ARM hosts also require Docker to have x86-64 emulation configured.

Run the core RFC 6455 cases with:

```shell
core/play-integration-test/autobahn/run.sh netty core
core/play-integration-test/autobahn/run.sh pekko-http core
```

The available profiles are:

| Profile | Included cases |
| --- | --- |
| `core` | Protocol cases, excluding performance and compression (`9.*`, `12.*`, and `13.*`) |
| `full` | Protocol and permessage-deflate cases, excluding performance (`9.*`) |
| `all` | Every Autobahn case, including long-running limits and performance cases |

The harness starts an echo WebSocket on an ephemeral port, runs the pinned `crossbario/autobahn-testsuite:25.10.1` image, checks `index.json`, and then stops the server. It raises the frame and decompression limits and enables both no-context-takeover directions so that Autobahn can exercise the supported compression behavior. `OK`, `NON-STRICT`, and `INFORMATIONAL` protocol results are accepted. Closing behavior must be `OK` or `INFORMATIONAL`; all other results fail the command.

Some RFC 7692 parameters are optional. Netty without its optional JZlib dependency declines all `server_max_window_bits` offers, while Pekko HTTP declines values below `15` because the JDK compression API cannot configure the window size. The corresponding group 13 cases are accepted only when Autobahn reports them as `UNIMPLEMENTED`; all other compression cases must pass.

HTML and JSON reports are written to `target/autobahn/<backend>/<profile>/reports`.

To reproduce selected cases or customize the container, set:

```shell
AUTOBAHN_CASES="6.*,7.*" \
AUTOBAHN_EXCLUDE_CASES="" \
core/play-integration-test/autobahn/run.sh netty core
```

Supported environment variables:

| Variable | Purpose |
| --- | --- |
| `AUTOBAHN_CASES` | Comma-separated case patterns, replacing the profile's default `*` |
| `AUTOBAHN_EXCLUDE_CASES` | Comma-separated exclusions, replacing the profile defaults |
| `AUTOBAHN_REPORT_DIR` | Report directory, replacing `target/autobahn/<backend>/<profile>` |
| `AUTOBAHN_IMAGE` | Docker image, replacing `crossbario/autobahn-testsuite:25.10.1` |
| `AUTOBAHN_DOCKER_PLATFORM` | Docker platform, replacing `linux/amd64` |
| `AUTOBAHN_DOCKER_USER` | UID and GID used in the container; the wrapper defaults to the current user |
| `AUTOBAHN_DOCKER` | Docker-compatible executable |
| `AUTOBAHN_HOST` | Hostname used by the container to reach the Play server |

This suite is intentionally separate from the regular unit and scripted tests: it requires Docker, downloads a large legacy compatibility image, and can take substantially longer depending on the selected cases and CPU emulation.

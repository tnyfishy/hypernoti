# HyperNoti development

Native Android app: Java 17, Android API 35 (min 26), AGP 8.7.3, Gradle 8.9.

Use the existing checkout; each cloud task is isolated. Do not create worktrees unless the user asks.

In the prepared cloud machine, source `/workspace/tooling/env.sh`, then run from `/workspace/hypernoti`:

```sh
./gradlew --no-daemon --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Keep English and Vietnamese resources synchronized. The default workflow requires no Firebase credentials, root, or Shizuku. Advanced changes require explicit user selection and confirmation. Never claim that an installed package, accepted shell command, checklist state, or host test proves FCM delivery. Xiaomi autostart is separate from Android app operations; device-specific behavior must be validated on an actual HyperOS ROM.

Validate package names before privileged commands. Do not add automatic root requests, a blanket security-service disable, arbitrary shell execution, uploads of installed-app lists, or persistent background processes without an explicit product requirement.

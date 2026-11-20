# Compile-only libraries

`build.sh` and `test/run.sh` put every `libs/*.jar` on the compile classpath. Nothing here is packaged in the APK.

## androidx.annotation (required)

The sources use `@NonNull`, `@Nullable`, `@MainThread` and `@WorkerThread` from `androidx.annotation`. They document nullability and
threading for Android Studio and lint. They are not checked by `javac` and cost nothing at run time (R8 drops them).

```
curl -L -o libs/annotation-jvm-1.9.1.jar \
  https://repo1.maven.org/maven2/androidx/annotation/annotation-jvm/1.9.1/annotation-jvm-1.9.1.jar
```

Keep one jar in this folder (delete an older one when you upgrade). Licence: Apache 2.0 (listed in `NOTICE.md`).

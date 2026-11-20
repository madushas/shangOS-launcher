# R8 rules for release and offline builds (build.sh). R8 only SHRINKS and optimises here.
# The aapt2 link step adds the keep rules for every class named in AndroidManifest.xml.

# Readable crash reports: CrashGuard and `adb logcat` show real class and method names.
-dontobfuscate

# androidx.annotation is compile-only (libs/); its annotations are not needed at run time.
-dontwarn androidx.annotation.**

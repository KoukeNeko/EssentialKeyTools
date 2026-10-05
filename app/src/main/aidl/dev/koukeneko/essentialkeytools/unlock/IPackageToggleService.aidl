package dev.koukeneko.essentialkeytools.unlock;

/** Runs in the Shizuku user service process, which has shell privileges. */
interface IPackageToggleService {
    // Transaction code reserved by Shizuku for tearing the user service process down.
    void destroy() = 16777114;

    boolean setPackageEnabled(String packageName, boolean enabled, int userId) = 1;
}

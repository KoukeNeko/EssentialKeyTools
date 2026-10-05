package dev.koukeneko.essentialkeytools.unlock

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

/**
 * Enables and disables consumer packages through Shizuku, for builds where App Info greys out the
 * Disable button. Owns every call into the Shizuku API so the rest of the app only sees
 * [ShizukuStatus] and [setPackageEnabled].
 */
class ShizukuPackageToggler(context: Context) {

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, PackageToggleService::class.java.name)
    )
        .processNameSuffix(SERVICE_PROCESS_SUFFIX)
        .daemon(false)

    private val userId = Process.myUid() / PER_USER_UID_RANGE

    fun status(): ShizukuStatus = when {
        !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.READY
        Shizuku.shouldShowRequestPermissionRationale() -> ShizukuStatus.PERMISSION_DENIED
        else -> ShizukuStatus.PERMISSION_REQUIRED
    }

    fun requestPermission() {
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    /** Calls [onChange] when Shizuku starts, stops, or answers the permission request. */
    fun observeStatus(onChange: () -> Unit): () -> Unit {
        val binderReceived = Shizuku.OnBinderReceivedListener { onChange() }
        val binderDead = Shizuku.OnBinderDeadListener { onChange() }
        val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> onChange() }
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        return {
            Shizuku.removeBinderReceivedListener(binderReceived)
            Shizuku.removeBinderDeadListener(binderDead)
            Shizuku.removeRequestPermissionResultListener(permissionResult)
        }
    }

    /** Returns whether `pm` accepted the change. Binds a fresh service for each call and releases it. */
    suspend fun setPackageEnabled(packageName: String, enabled: Boolean): Boolean {
        val service = CompletableDeferred<IPackageToggleService>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service.complete(IPackageToggleService.Stub.asInterface(binder))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service.completeExceptionally(RemoteException("Shizuku user service disconnected"))
            }
        }
        Shizuku.bindUserService(userServiceArgs, connection)
        return try {
            val toggleService = withTimeout(BIND_TIMEOUT_MS) { service.await() }
            // The service call blocks until `pm` exits, so it must stay off the main thread.
            withContext(Dispatchers.IO) { toggleService.setPackageEnabled(packageName, enabled, userId) }
        } catch (error: RemoteException) {
            Log.w(TAG, "Shizuku could not toggle $packageName", error)
            false
        } catch (error: TimeoutCancellationException) {
            Log.w(TAG, "Shizuku user service did not start for $packageName", error)
            false
        } finally {
            Shizuku.unbindUserService(userServiceArgs, connection, true)
        }
    }

    private companion object {
        const val TAG = "ShizukuPackageToggler"
        const val SERVICE_PROCESS_SUFFIX = "package_toggle"
        const val PERMISSION_REQUEST_CODE = 1
        const val BIND_TIMEOUT_MS = 10_000L

        // Android derives the user id from an app uid by dividing out this per-user range.
        const val PER_USER_UID_RANGE = 100_000
    }
}

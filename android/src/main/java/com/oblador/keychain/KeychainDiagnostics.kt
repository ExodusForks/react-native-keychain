package com.oblador.keychain

import android.os.Build
import android.security.KeyStoreException
import androidx.annotation.RequiresApi
import com.oblador.keychain.cipherStorage.CipherStorage

/**
 * Describes a failed save or read for error reports: exception types, the Keystore's own error and
 * the state of the entry's key. It never includes exception messages, aliases or stored values,
 * which can hold private data, so apps can forward it to their own reports as is.
 */
object KeychainDiagnostics {
  /** Key of the diagnostics in the rejection's `userInfo`. */
  const val USER_INFO_KEY = "keychainDiagnostics"

  const val OPERATION_READ = "read"
  const val OPERATION_SAVE = "save"

  /** Enough to reach the Keystore's error under the library's own wrappers. */
  private const val MAX_CAUSES = 8

  fun describe(
    error: Throwable,
    operation: String,
    storage: CipherStorage?,
    alias: String
  ): Map<String, Any?> {
    val causes = causeChain(error)
    val diagnostics = mutableMapOf<String, Any?>(
      "operation" to operation,
      "causes" to causes.map { it.javaClass.name },
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      causes.firstNotNullOfOrNull { it as? KeyStoreException }?.let {
        diagnostics.putAll(describeKeystoreError(it))
      }
    }

    if (storage != null) {
      val key = storage.describeKey(alias)
      diagnostics["storage"] = storage.getCipherStorageName()
      diagnostics["keyExists"] = key.exists
      diagnostics["keySecurityLevel"] = key.securityLevel
    }

    return diagnostics
  }

  /** The error and its causes, outermost first, stopping at a cycle. */
  private fun causeChain(error: Throwable): List<Throwable> {
    val chain = mutableListOf<Throwable>()
    var current: Throwable? = error
    while (current != null && chain.size < MAX_CAUSES && chain.none { it === current }) {
      chain.add(current)
      current = current.cause
    }
    return chain
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun describeKeystoreError(error: KeyStoreException): Map<String, Any?> =
    mapOf(
      "errorCode" to error.numericErrorCode,
      "errorName" to keystoreErrorName(error.numericErrorCode),
      "isTransient" to error.isTransientFailure,
      "requiresUserAuthentication" to error.requiresUserAuthentication(),
    )

  private fun keystoreErrorName(code: Int): String =
    when (code) {
      KeyStoreException.ERROR_OTHER -> "OTHER"
      KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED -> "USER_AUTHENTICATION_REQUIRED"
      KeyStoreException.ERROR_KEYSTORE_UNINITIALIZED -> "KEYSTORE_UNINITIALIZED"
      KeyStoreException.ERROR_INTERNAL_SYSTEM_ERROR -> "INTERNAL_SYSTEM_ERROR"
      KeyStoreException.ERROR_PERMISSION_DENIED -> "PERMISSION_DENIED"
      KeyStoreException.ERROR_KEY_DOES_NOT_EXIST -> "KEY_DOES_NOT_EXIST"
      KeyStoreException.ERROR_KEY_CORRUPTED -> "KEY_CORRUPTED"
      KeyStoreException.ERROR_ID_ATTESTATION_FAILURE -> "ID_ATTESTATION_FAILURE"
      KeyStoreException.ERROR_ATTESTATION_CHALLENGE_TOO_LARGE -> "ATTESTATION_CHALLENGE_TOO_LARGE"
      KeyStoreException.ERROR_KEYMINT_FAILURE -> "KEYMINT_FAILURE"
      KeyStoreException.ERROR_KEYSTORE_FAILURE -> "KEYSTORE_FAILURE"
      KeyStoreException.ERROR_UNIMPLEMENTED -> "UNIMPLEMENTED"
      KeyStoreException.ERROR_INCORRECT_USAGE -> "INCORRECT_USAGE"
      KeyStoreException.ERROR_KEY_NOT_TEMPORALLY_VALID -> "KEY_NOT_TEMPORALLY_VALID"
      KeyStoreException.ERROR_KEY_OPERATION_EXPIRED -> "KEY_OPERATION_EXPIRED"
      KeyStoreException.ERROR_ATTESTATION_KEYS_UNAVAILABLE -> "ATTESTATION_KEYS_UNAVAILABLE"
      else -> "UNKNOWN"
    }
}

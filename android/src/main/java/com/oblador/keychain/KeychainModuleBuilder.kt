package com.oblador.keychain

import com.facebook.react.bridge.ReactApplicationContext

class KeychainModuleBuilder {
  private var reactContext: ReactApplicationContext? = null
  private var useWarmUp = DEFAULT_USE_WARM_UP
  private var useStrongBox = DEFAULT_USE_STRONG_BOX

  fun withReactContext(reactContext: ReactApplicationContext?): KeychainModuleBuilder {
    this.reactContext = reactContext
    return this
  }

  fun usingWarmUp(): KeychainModuleBuilder {
    useWarmUp = true
    return this
  }

  fun withoutWarmUp(): KeychainModuleBuilder {
    useWarmUp = false
    return this
  }

  /** Create new Keystore keys in the TEE even when the phone has a StrongBox chip. */
  fun withoutStrongBox(): KeychainModuleBuilder {
    useStrongBox = false
    return this
  }

  fun usingStrongBox(): KeychainModuleBuilder {
    useStrongBox = true
    return this
  }

  fun build(): KeychainModule {
    validate()
    return if (useWarmUp) {
      KeychainModule.withWarming(reactContext!!, useStrongBox)
    } else {
      KeychainModule(reactContext!!, useStrongBox)
    }
  }

  private fun validate() {
    if (reactContext == null) {
      throw Error("React Context was not provided")
    }
  }

  companion object {
    const val DEFAULT_USE_WARM_UP = false
    const val DEFAULT_USE_STRONG_BOX = true
  }
}

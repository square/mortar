/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mortar.bundler;

import android.os.Bundle;
import mortar.MortarScope;

/** Implemented by objects that want to persist via the bundle. */
public interface Bundler {
  /**
   * Like {@link mortar.Scoped#onEnterScope}, called synchronously when a bundler
   * is {@link BundleService#register registered} with a {@link BundleService}.
   */
  void onEnterScope(MortarScope scope);

  /**
   * The key that will identify the bundles passed to this instance via {@link #onLoad}
   * and {@link #onSave}.
   */
  String getMortarBundleKey();

  /**
   * Called when this object is {@link BundleService#register registered}, and each time
   * {@link BundleServiceRunner#onCreate} is called (e.g. after a configuration change like
   * rotation, or after the app process is respawned). Callers should assume that the initial
   * call to this method is made asynchronously, but be prepared for a synchronous call.
   *
   * <p>Note that receivers are likely to outlive multiple activity instances, and so receive
   * multiple calls of this method. Implementations should be prepared to ignore saved state if
   * they are already initialized.
   *
   * @param savedInstanceState the state written by the most recent call to {@link #onSave}, or
   * null if that has never happened.
   */
  void onLoad(Bundle savedInstanceState);

  /**
   * Called from the {@link BundleServiceRunner#onSaveInstanceState}, to allow the receiver
   * to save state before the process is killed. Note that receivers are likely to outlive multiple
   * activity instances, and so receive multiple calls of this method. Any state required to revive
   * a new instance of the receiver in a new process should be written out each time, as there is
   * no way to know if the app is about to hibernate.
   *
   * @param outState a bundle to write any state that needs to be restored if the plugin is
   * revived
   */
  void onSave(Bundle outState);

  void onExitScope();
}

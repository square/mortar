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
package mortar;

import android.os.Bundle;

/** Implemented by objects that want to persist via the bundle. */
public interface Bundler extends Scoped {
  /**
   * The key that will identify the bundles passed to this instance via {@link #onLoad}
   * and {@link #onSave}.
   */
  String getMortarBundleKey();

  /**
   * Called when this object is first {@link MortarScope#register registered}, and each time a
   * new {@link android.app.Activity} instance is created (typically after a configuration change
   * like rotation). Note that the receiver may outlive multiple activity instances, and so receive
   * multiple calls to this method. It is also possible to receive several redundant calls
   * before any call to {@link #onSave} (sorry, working on it). Always be idempotent.
   *
   * @param savedInstanceState written by the previous activity instance, or null if this is a
   * fresh creation.
   */
  void onLoad(Bundle savedInstanceState);

  /**
   * Called from the {@link android.app.Activity#onSaveInstanceState onSaveInstanceState} method
   * of the current {@link android.app.Activity}. This is the receiver's sign that the activity
   * is being torn down, and possibly the entire app along with it.
   * <p/>
   * Note that receivers may outlive multiple activity instances, and so receive multiple calls
   * of this method. Any precious state should be written out each time, as there is no way to
   * know if the app is about to hibernate.
   *
   * @param outState a bundle to write any state that needs to be restored if the plugin is
   * revived
   */
  void onSave(Bundle outState);
}

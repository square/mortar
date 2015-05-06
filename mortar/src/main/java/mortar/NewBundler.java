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
import mortar.bundler.BundleService;
import mortar.bundler.BundleServiceRunner;

/** Implemented by objects that want to persist via the bundle. */
public interface NewBundler {
  /**
   * The key that will identify the bundles passed to this instance via {@link #onLoad}
   * and {@link #onSave}.
   */
  String getMortarBundleKey();

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
}

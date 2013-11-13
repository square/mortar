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

public interface MortarActivityScope extends MortarScope {
  /**
   * Registers the given to have its {@link mortar.Scoped#onDestroy()} method called. In addition,
   * if it is an instance of {@link Bundler}, we'll call its {@link Bundler#onLoad} from {@link
   * #onResume()} and {@link #onSaveInstanceState} from {@link #onSaveInstanceState}. If we are
   * active (between calls to {@link #onResume()} and {@link #onSaveInstanceState}), {@link
   * Bundler#onLoad} is called immediately.
   * <p/>
   * Redundant calls are no-ops.
   */
  @Override void register(Scoped s);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onCreate}. Makes note of the {@link Bundle} to be used when {@link
   * #onResume} is called.
   */
  void onCreate(Bundle savedInstanceState);

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onResume()}. Calls the registrants' {@link Bundler#onLoad} methods
   * from last {@link Bundle} passed to {@link #onCreate} or {@link #onSaveInstanceState}.
   */
  void onResume();

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onSaveInstanceState}. Calls the registrants' {@link Bundler#onSave}
   * methods, and makes note of the given {@link Bundle} to be used when {@link #onResume} is next
   * called.
   */
  void onSaveInstanceState(Bundle outState);
}

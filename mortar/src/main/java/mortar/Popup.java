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

import android.content.Context;
import android.os.Parcelable;

/**
 * Implemented by classes that run a popup display for a view, typically a dialog.
 *
 * @see PopupPresenter
 * @param <D> info to display
 */
public interface Popup<D extends Parcelable, R> {
  /**
   * Show the given info. How to handle redundant calls is a decision to be made
   * per implementation. Some classes may throw {@link IllegalStateException}
   * if the popup is already visible. Others may update a visible display to reflect
   * the new info.
   */
  void show(D info, boolean withFlourish, PopupPresenter<D, R> presenter);

  boolean isShowing();

  void dismiss(boolean withFlourish);

  Context getContext();
}

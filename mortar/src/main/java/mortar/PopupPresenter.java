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
import android.os.Parcelable;

/**
 * @param <D> the type of info this dialog displays. T must provide value-based implementations
 * of {@link #hashCode()} and {@link #equals(Object)} in order for debouncing code in {@link #show}
 * to work properly.
 */
public abstract class PopupPresenter<D extends Parcelable, R> extends Presenter<Popup<D, R>> {
  private static String KEY = "popup";
  private static boolean WITH_FLOURISH = true;

  private D whatToShow;

  public D showing() {
    return whatToShow;
  }

  public void show(D info) {
    if (whatToShow == info || whatToShow != null && whatToShow.equals(info)) {
      // It's very likely this is a button bounce
      // http://stackoverflow.com/questions/2886407/dealing-with-rapid-tapping-on-buttons
      return;
    }

    whatToShow = info;
    Popup<D, R> view = getView();
    if (view == null) return;
    view.show(whatToShow, WITH_FLOURISH, this);
  }

  public final void onDismissed(R result) {
    dismiss();
    onPopupResult(result);
  }

  private void dismiss() {
    if (whatToShow != null) {
      whatToShow = null;

      Popup<D, R> popUp = getView();
      if (popUp == null) return;

      if (popUp.isShowing()) popUp.dismiss(WITH_FLOURISH);
    }
  }

  abstract protected void onPopupResult(R result);

  @Override protected MortarScope extractScope(Popup<D, R> view) {
    return Mortar.getScope(view.getContext());
  }

  @Override public void dropView(Popup<D, R> view) {
    Popup<D, R> oldView = getView();
    if (oldView == view && oldView.isShowing()) oldView.dismiss(false);
    super.dropView(view);
  }

  @Override public void onLoad(Bundle savedInstanceState) {
    if (whatToShow == null && savedInstanceState != null) {
      whatToShow = savedInstanceState.getParcelable(KEY);
    }

    if (whatToShow == null) return;

    Popup<D, R> view = getView();
    if (view == null) return;

    if (!view.isShowing()) view.show(whatToShow, !WITH_FLOURISH, this);
  }

  @Override public void onSave(Bundle outState) {
    if (whatToShow != null) outState.putParcelable(KEY, whatToShow);
  }

  @Override public void onDestroy() {
    Popup<D, R> popUp = getView();
    if (popUp != null && popUp.isShowing()) popUp.dismiss(!WITH_FLOURISH);
    super.onDestroy();
  }
}

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
import mortar.bundler.BundleService;

/**
 * @param <D> the type of info this dialog displays. D must provide value-based implementations
 * of {@link #hashCode()} and {@link #equals(Object)} in order for debouncing code in {@link #show}
 * to work properly.
 *
 * When using multiple {@link PopupPresenter}s of the same type in the same view, construct them with
 * {@link #PopupPresenter(String)} to give them a name to distinguish them.
 */
public abstract class PopupPresenter<D extends Parcelable, R> extends Presenter<Popup<D, R>> {
  private static final boolean WITH_FLOURISH = true;

  private D whatToShow;

  // TODO(ray) If we're going to keep the presenters (NO!) the finder should be set via the
  // constructor to fix some of our current testing woes.
  private final BundleService.Finder serviceFinder = new BundleService.Finder();
  private final String whatToShowKey;

  /**
   * @param customStateKey custom key name for saving state, useful when you have multiple instance
   * of the same PopupPresenter class tied to a view.
   */
  protected PopupPresenter(String customStateKey) {
    this.whatToShowKey = getClass().getName() + customStateKey;
  }

  protected PopupPresenter() {
    this("");
  }

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
    if (!hasView()) return;
    getView().show(whatToShow, WITH_FLOURISH, this);
  }

  public void dismiss() {
    if (whatToShow != null) {
      whatToShow = null;
      if (!hasView()) return;
      Popup<D, R> popUp = getView();
      if (popUp.isShowing()) popUp.dismiss(WITH_FLOURISH);
    }
  }

  public final void onDismissed(R result) {
    whatToShow = null;
    onPopupResult(result);
  }

  abstract protected void onPopupResult(R result);

  @Override protected BundleService extractBundleService(Popup<D, R> view) {
    return serviceFinder.get(view.getContext());
  }

  @Override public void dropView(Popup<D, R> view) {
    Popup<D, R> oldView = getView();
    if (oldView == view && oldView.isShowing()) oldView.dismiss(!WITH_FLOURISH);
    super.dropView(view);
  }

  @Override public void onLoad(Bundle savedInstanceState) {
    if (whatToShow == null && savedInstanceState != null) {
      whatToShow = savedInstanceState.getParcelable(whatToShowKey);
    }

    if (whatToShow == null) return;

    if (!hasView()) return;
    Popup<D, R> view = getView();

    if (!view.isShowing()) {
      view.show(whatToShow, !WITH_FLOURISH, this);
    }
  }

  @Override public void onSave(Bundle outState) {
    if (whatToShow != null) {
      outState.putParcelable(whatToShowKey, whatToShow);
    }
  }

  @Override public void onExitScope() {
    Popup<D, R> popUp = getView();
    if (popUp != null && popUp.isShowing()) popUp.dismiss(!WITH_FLOURISH);
  }
}

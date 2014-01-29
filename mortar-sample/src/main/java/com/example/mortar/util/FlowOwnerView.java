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
package com.example.mortar.util;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.example.mortar.R;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;

import static android.view.animation.AnimationUtils.loadAnimation;

/**
 * A parent view that displays subviews within a {@link #getContainer() container view}.
 * <p/>
 * Like all Mortar views, subclasses must call {@link mortar.ViewPresenter#takeView},
 * typically from {@link #onFinishInflate()}. E.g.
 * <code><pre>{@literal@}Override protected void onFinishInflate() {
 *   super.onFinishInflate();
 *   getPresenter().takeView(this);
 * }</pre></code>
 *
 * @param <S> the type of the screens that serve as a {@link Blueprint} for subview. Must
 * be annotated with {@link flow.Layout}, suitable for use with {@link flow.Layouts#createView}.
 */
public abstract class FlowOwnerView<S extends Blueprint> extends FrameLayout {

  private SparseArray<Parcelable> lastRestoredHierarchyState;

  public FlowOwnerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Mortar.inject(context, this);
  }

  public void showScreen(S screen, Flow.Direction direction) {
    MortarScope myScope = Mortar.getScope(getContext());
    MortarScope newChildScope = myScope.requireChild(screen);

    View oldChild = getChildView();
    View newChild;

    if (oldChild != null) {
      MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
      if (oldChildScope.getName().equals(screen.getMortarScopeName())) {
        // If it's already showing, short circuit.
        return;
      }

      oldChildScope.destroy();
    }

    // Create the new child.
    Context childContext = newChildScope.createContext(getContext());
    newChild = Layouts.createView(childContext, screen);

    setAnimation(direction, oldChild, newChild);

    // Out with the old, in with the new.
    ViewGroup container = getContainer();
    if (oldChild != null) container.removeView(oldChild);
    container.addView(newChild);

    // Restore view state.
    if (lastRestoredHierarchyState != null) {
      newChild.restoreHierarchyState(lastRestoredHierarchyState);
      lastRestoredHierarchyState = null;
    }
  }

  protected void setAnimation(Flow.Direction direction, View oldChild, View newChild) {
    if (oldChild == null) return;

    int out = direction == Flow.Direction.FORWARD ? R.anim.slide_out_left : R.anim.slide_out_right;
    int in = direction == Flow.Direction.FORWARD ? R.anim.slide_in_right : R.anim.slide_in_left;

    oldChild.setAnimation(loadAnimation(getContext(), out));
    newChild.setAnimation(loadAnimation(getContext(), in));
  }

  @Override public void restoreHierarchyState(SparseArray<Parcelable> container) {
    // This is called before revived instances are told what child to show, so save the
    // container for later.

    lastRestoredHierarchyState = container;
    super.restoreHierarchyState(container);
  }

  public boolean onBackPressed() {
    return getPresenter().onRetreatSelected();
  }

  public boolean onUpPressed() {
    return getPresenter().onUpSelected();
  }

  /**
   * Return the container used to host child views. Typically this is a {@link
   * android.widget.FrameLayout} under the action bar.
   */
  protected abstract ViewGroup getContainer();

  /**
   * Return the {@link FlowOwner} that manages this view. Remember that subclasses
   * can refine the type returned by this method.
   */
  protected abstract FlowOwner<? extends Blueprint, ?> getPresenter();

  private View getChildView() {
    return getContainer().getChildAt(0);
  }
}
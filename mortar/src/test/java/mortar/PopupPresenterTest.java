/*
 * Copyright 2014 Square Inc.
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
import android.os.Bundle;
import android.os.Parcelable;
import mortar.bundler.BundleServiceRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static mortar.bundler.BundleServiceRunner.getBundleServiceRunner;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

// Robolectric allows us to use Bundles.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PopupPresenterTest {

  static class TestPopupPresenter extends PopupPresenter<Parcelable, String> {
    String result;

    TestPopupPresenter() {
    }

    TestPopupPresenter(String customStateKey) {
      super(customStateKey);
    }

    @Override protected void onPopupResult(String result) {
      this.result = result;
    }
  }

  static final boolean WITH_FLOURISH = true;
  static final boolean WITHOUT_FLOURISH = false;

  @Mock Popup<Parcelable, String> view;
  @Mock Context context;

  MortarScope root;
  MortarScope activityScope;
  TestPopupPresenter presenter;

  @Before public void setUp() {
    initMocks(this);
    when(view.getContext()).thenReturn(context);
    when((context).getSystemService(anyString())).then(returnScopedService());

    newProcess();
    getBundleServiceRunner(activityScope).onCreate(null);
    presenter = new TestPopupPresenter();
  }

  /** Simulate a new proecess by creating brand new scope instances. */
  private void newProcess() {
    root = MortarScope.buildRootScope().build("Root");
    activityScope = root.buildChild()
        .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
        .build("activity");
  }

  private Answer<Object> returnScopedService() {
    return new Answer<Object>() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        return activityScope.getService((String) invocation.getArguments()[0]);
      }
    };
  }

  @Test public void takeViewDoesNotShowView() {
    presenter.takeView(view);
    verify(view, never()).show(any(Parcelable.class), anyBoolean(), any(TestPopupPresenter.class));
  }

  @Test public void showAfterTakeViewShowsView() {
    presenter.takeView(view);
    Parcelable info = mock(Parcelable.class);
    presenter.show(info);
    verify(view).show(same(info), eq(WITH_FLOURISH), same(presenter));
  }

  @Test public void dismissAfterShowDismissesView() {
    presenter.takeView(view);
    presenter.show(mock(Parcelable.class));
    when(view.isShowing()).thenReturn(true);
    presenter.dismiss();
    verify(view).dismiss(eq(WITH_FLOURISH));
  }

  @Test public void dismissWithViewNotShowingDoesNotDismissView() {
    presenter.takeView(view);
    presenter.show(mock(Parcelable.class));
    when(view.isShowing()).thenReturn(false);
    presenter.dismiss();
    verify(view, never()).dismiss(anyBoolean());
  }

  @Test public void dismissWithoutShowDoesNotDismissView() {
    presenter.takeView(view);
    presenter.dismiss();
    verify(view, never()).dismiss(anyBoolean());
  }

  @Test public void showingReturnsInfo() {
    Parcelable info = mock(Parcelable.class);
    presenter.show(info);
    assertThat(presenter.showing()).isSameAs(info);
  }

  @Test public void dismissClearsInfo() {
    presenter.show(mock(Parcelable.class));
    presenter.dismiss();
    assertThat(presenter.showing()).isNull();
  }

  @Test public void showTwiceWithSameInfoDebounces() {
    presenter.takeView(view);
    Parcelable info = mock(Parcelable.class);
    presenter.show(info);
    presenter.show(info);
    verify(view).show(same(info), anyBoolean(), same(presenter));
  }

  @Test public void destroyDismissesWithoutFlourish() {
    presenter.takeView(view);
    when(view.isShowing()).thenReturn(true);
    activityScope.destroy();
    verify(view).dismiss(eq(WITHOUT_FLOURISH));
  }

  @Test public void takeViewRestoresPopup() {
    presenter.takeView(view);
    Parcelable info = mock(Parcelable.class);
    presenter.show(info);

    Bundle state = new Bundle();
    getBundleServiceRunner(activityScope).onSaveInstanceState(state);

    newProcess();
    getBundleServiceRunner(activityScope).onCreate(state);

    presenter = new TestPopupPresenter();
    presenter.takeView(view);
    verify(view).show(same(info), eq(WITHOUT_FLOURISH), same(presenter));
  }

  @Test public void customStateKeyAvoidsStateMixing() {
    String customStateKey1 = "presenter1";
    TestPopupPresenter presenter1 = new TestPopupPresenter(customStateKey1);
    presenter1.takeView(view);
    Bundle info1 = new Bundle();
    info1.putString("key", "data1");
    presenter1.show(info1);

    String customStateKey2 = "presenter2";
    TestPopupPresenter presenter2 = new TestPopupPresenter(customStateKey2);
    presenter2.takeView(view);
    Bundle info2 = new Bundle();
    info2.putString("key", "data2");
    presenter2.show(info2);

    Bundle state = new Bundle();
    getBundleServiceRunner(activityScope).onSaveInstanceState(state);
    newProcess();
    getBundleServiceRunner(activityScope).onCreate(state);

    presenter1 = new TestPopupPresenter(customStateKey1);
    presenter1.takeView(view);
    assertThat(presenter1.showing()).isEqualTo(info1).isNotEqualTo(info2);

    presenter2 = new TestPopupPresenter(customStateKey2);
    presenter2.takeView(view);
    assertThat(presenter2.showing()).isEqualTo(info2).isNotEqualTo(info1);
  }

  @Test public void onDismissedClearsInfo() {
    presenter.show(mock(Parcelable.class));
    presenter.onDismissed("");
    assertThat(presenter.showing()).isNull();
  }

  @Test public void onDismissedDeliversResult() {
    presenter.show(mock(Parcelable.class));
    presenter.onDismissed("result");
    assertThat(presenter.result).isEqualTo("result");
  }
}

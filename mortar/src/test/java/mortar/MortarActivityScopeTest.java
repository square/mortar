// Copyright 2013 Square, Inc.
package mortar;

import android.app.Activity;
import android.os.Bundle;
import dagger.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class MortarActivityScopeTest {

  @Module class ModuleAndBlueprint implements Blueprint {

    @Override public String getMortarScopeName() {
      return "name";
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  class Context extends Activity implements HasMortarScope {
    @Override public MortarScope getMortarScope() {
      throw new UnsupportedOperationException();
    }
  }

  @Mock Scoped scoped;
  @Mock Bundler earlyBundler;
  @Mock Bundler lateBundler;

  @Captor ArgumentCaptor<Bundle> earlyCaptor;
  @Captor ArgumentCaptor<Bundle> lateCaptor;

  private MortarActivityScope activityScope;

  @Before
  public void setUp() {
    initMocks(this);
    when(earlyBundler.getMortarBundleKey()).thenReturn("early");
    when(lateBundler.getMortarBundleKey()).thenReturn("late");

    MortarScope root = Mortar.createRootScope(false, new ModuleAndBlueprint());
    activityScope = Mortar.getActivityScope(root, new ModuleAndBlueprint());
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonNullKeyRequired() {
    activityScope.register(mock(Bundler.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonEmptyKeyRequired() {
    Bundler mock = mock(Bundler.class);
    when(mock.getMortarBundleKey()).thenReturn("");
    activityScope.register(mock);
  }

  @Test
  public void lifeCycle() {
    activityScope.register(scoped);
    activityScope.register(earlyBundler);

    // Load is always called immediately.
    verify(earlyBundler).onLoad(isNull(Bundle.class));

    activityScope.onCreate(null);
    // Load is not called on create
    verify(earlyBundler, times(1)).onLoad(isNull(Bundle.class));

    // Load is called again on resume. (Actually, I think this is a bug, but for now
    // it's expected.)
    activityScope.onResume();
    verify(earlyBundler, times(2)).onLoad(isNull(Bundle.class));

    Bundle out = new Bundle();
    activityScope.onSaveInstanceState(out);
    verify(earlyBundler, times(1)).onSave(earlyCaptor.capture());

    assertThat(out.keySet()).hasSize(1);
    assertThat(earlyCaptor.getValue()).isSameAs(out.getBundle("early"));

    activityScope.onResume();
    verify(earlyBundler, times(3)).onLoad(earlyCaptor.capture());

    assertThat(earlyCaptor.getValue()).isSameAs(out.getBundle("early"));

    // A new registrant shows up and gets the bundle right away.
    activityScope.register(lateBundler);
    verify(lateBundler).onLoad(lateCaptor.capture());
    assertThat(out.keySet()).hasSize(2);
    assertThat(lateCaptor.getValue()).isSameAs(out.getBundle("late"));

    // A new activity instance appears
    Bundle fromNewActivity = new Bundle(out);
    activityScope.onCreate(fromNewActivity);
    activityScope.onResume();
    verify(earlyBundler, times(4)).onLoad(earlyCaptor.capture());
    verify(lateBundler, times(2)).onLoad(lateCaptor.capture());
    assertThat(earlyCaptor.getValue()).isSameAs(fromNewActivity.getBundle("early"));
    assertThat(lateCaptor.getValue()).isSameAs(fromNewActivity.getBundle("late"));

    verifyNoMoreInteractions(scoped);

    activityScope.destroy();
    verify(scoped).onDestroy();
    verify(earlyBundler).onDestroy();
    verify(lateBundler).onDestroy();

    // destroy() is idemptotent

    activityScope.destroy();
    verify(scoped, times(1)).onDestroy();
    verify(earlyBundler, times(1)).onDestroy();
    verify(lateBundler, times(1)).onDestroy();
  }

  @Test
  public void childLifeCycle() {
    MortarScope child = activityScope.requireChild(new ModuleAndBlueprint());

    child.register(scoped);
    child.register(earlyBundler);

    // Load is always called immediately.
    verify(earlyBundler).onLoad(isNull(Bundle.class));

    activityScope.onCreate(null);
    // Load is not called on create
    verify(earlyBundler, times(1)).onLoad(isNull(Bundle.class));

    // Load is not called again on resume. (Actually, I think this is a bug, but for now
    // it's expected.)
    activityScope.onResume();
    verify(earlyBundler, times(2)).onLoad(isNull(Bundle.class));

    Bundle out = new Bundle();
    activityScope.onSaveInstanceState(out);
    verify(earlyBundler, times(1)).onSave(earlyCaptor.capture());

    assertThat(out.keySet()).hasSize(1);
    Bundle firstTopBundle = out.getBundle("name");
    assertThat(firstTopBundle.keySet()).hasSize(1);
    assertThat(earlyCaptor.getValue()).isSameAs(firstTopBundle.getBundle("early"));

    activityScope.onResume();
    verify(earlyBundler, times(3)).onLoad(earlyCaptor.capture());

    assertThat(earlyCaptor.getValue()).isSameAs(firstTopBundle.getBundle("early"));

    // A new registrant shows up and gets the bundle right away.
    child.register(lateBundler);
    verify(lateBundler).onLoad(lateCaptor.capture());
    assertThat(out.keySet()).hasSize(1);
    assertThat(firstTopBundle.keySet()).hasSize(2);
    assertThat(lateCaptor.getValue()).isSameAs(firstTopBundle.getBundle("late"));

    // A new activity instance appears
    Bundle fromNewActivity = new Bundle(out);
    activityScope.onCreate(fromNewActivity);
    activityScope.onResume();
    verify(earlyBundler, times(4)).onLoad(lateCaptor.capture());
    verify(lateBundler, times(2)).onLoad(lateCaptor.capture());

    Bundle nextTopBundle = fromNewActivity.getBundle("name");
    assertThat(earlyCaptor.getValue()).isSameAs(nextTopBundle.getBundle("early"));
    assertThat(lateCaptor.getValue()).isSameAs(nextTopBundle.getBundle("late"));

    verifyNoMoreInteractions(scoped);

    activityScope.destroy();
    verify(scoped).onDestroy();
    verify(earlyBundler).onDestroy();
    verify(lateBundler).onDestroy();

    // recursive destroy() is idemptotent

    child.destroy();
    verify(scoped, times(1)).onDestroy();
    verify(earlyBundler, times(1)).onDestroy();
    verify(lateBundler, times(1)).onDestroy();
  }
}

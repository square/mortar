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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

// Robolectric allows us to use Bundles.
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
    doTest(activityScope);
  }

  @Test
  public void childLifeCycle() {
    doTest((MortarActivityScope) activityScope.requireChild(new ModuleAndBlueprint()));
  }

  @Test
  public void childCreatedWhileActive() {
    activityScope.onCreate(null);
    activityScope.onResume();
    MortarScope child = activityScope.requireChild(new ModuleAndBlueprint());
    child.register(earlyBundler);
    verify(earlyBundler).onLoad(any(Bundle.class));
  }

  private void doTest(MortarActivityScope registerScope) {
    registerScope.register(scoped);
    registerScope.register(earlyBundler);

    // Load is not called before resume.
    verify(earlyBundler, times(0)).onLoad(any(Bundle.class));

    activityScope.onCreate(null);
    // Load is not called on create, because it's before resume. See?
    verify(earlyBundler, times(0)).onLoad(any(Bundle.class));

    // Resuming, now we can load
    activityScope.onResume();
    verify(earlyBundler).onLoad(isNull(Bundle.class));

    // When we save, the bundler gets its own bundle to write to.
    Bundle out = new Bundle();
    activityScope.onSaveInstanceState(out);
    verify(earlyBundler).onSave(earlyCaptor.capture());

    assertThat(out.keySet()).hasSize(1);
    assertThat(earlyCaptor.getValue()).isSameAs(out.getBundle("early"));
    earlyCaptor.getAllValues().clear();

    // Resuming the same activity instance, the out bundle is sent to load()
    activityScope.onResume();
    verify(earlyBundler, times(2)).onLoad(earlyCaptor.capture());
    assertThat(earlyCaptor.getValue()).isSameAs(out.getBundle("early"));
    earlyCaptor.getAllValues().clear();

    // A new registrant shows up and gets the bundle right away.
    registerScope.register(lateBundler);
    verify(lateBundler).onLoad(lateCaptor.capture());
    assertThat(out.keySet()).hasSize(2);
    assertThat(lateCaptor.getValue()).isSameAs(out.getBundle("late"));

    // A new activity instance appears
    activityScope.onSaveInstanceState(out);
    Bundle fromNewActivity = new Bundle(out);

    activityScope.onCreate(fromNewActivity);
    activityScope.onResume();
    verify(earlyBundler, times(3)).onLoad(earlyCaptor.capture());
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
}

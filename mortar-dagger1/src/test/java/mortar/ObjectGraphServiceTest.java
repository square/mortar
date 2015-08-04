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
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import java.lang.annotation.Retention;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Qualifier;
import mortar.dagger1support.ObjectGraphService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static mortar.dagger1support.ObjectGraphService.SERVICE_NAME;
import static mortar.dagger1support.ObjectGraphService.create;
import static mortar.dagger1support.ObjectGraphService.getObjectGraph;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("InnerClassMayBeStatic") public class ObjectGraphServiceTest {

  @Mock Context context;
  @Mock Scoped scoped;

  @Qualifier @Retention(RUNTIME) @interface Apple {
  }

  @Qualifier @Retention(RUNTIME) @interface Bagel {
  }

  @Qualifier @Retention(RUNTIME) @interface Carrot {
  }

  @Qualifier @Retention(RUNTIME) @interface Dogfood {
  }

  @Qualifier @Retention(RUNTIME) @interface Eggplant {
  }

  @Module(injects = HasApple.class) class AppleModule {
    @Provides @Apple String provideApple() {
      return Apple.class.getName();
    }
  }

  @Module(injects = HasBagel.class) class BagelModule {
    @Provides @Bagel String provideBagel() {
      return Bagel.class.getName();
    }
  }

  @Module(injects = HasCarrot.class) class CarrotModule {
    @Provides @Carrot String provideCharlie() {
      return Carrot.class.getName();
    }
  }

  @Module(injects = HasDogfood.class) class DogfoodModule {
    @Provides @Dogfood String provideDogfood() {
      return Dogfood.class.getName();
    }
  }

  @Module(injects = HasEggplant.class) class EggplanModule {
    @Provides @Eggplant String provideEggplant() {
      return Eggplant.class.getName();
    }
  }

  static class HasApple {
    @Inject @Apple String string;
  }

  static class HasBagel {
    @Inject @Bagel String string;
  }

  static class HasCarrot {
    @Inject @Carrot String string;
  }

  static class HasDogfood {
    @Inject @Dogfood String string;
  }

  static class HasEggplant {
    @Inject @Eggplant String string;
  }

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void createMortarScopeUsesModules() {
    MortarScope scope = createRootScope(ObjectGraph.create(new AppleModule(), new BagelModule()));
    ObjectGraph objectGraph = getObjectGraph(scope);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test public void destroyRoot() {
    MortarScope scope = createRootScope(ObjectGraph.create(new AppleModule()));
    scope.register(scoped);
    scope.destroy();
    verify(scoped).onExitScope();
  }

  @Test public void activityScopeName() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    String name = Bagel.class.getName();
    MortarScope activityScope =
        root.buildChild().withService(SERVICE_NAME, create(root, new BagelModule())).build(name);

    assertThat(activityScope.getName()).isEqualTo(name);
    assertThat(root.findChild(name)).isSameAs(activityScope);
    assertThat(root.findChild("herman")).isNull();
  }

  @Test public void getActivityScopeWithOneModule() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    String name = Bagel.class.getName();
    MortarScope activityScope =
        root.buildChild().withService(SERVICE_NAME, create(root, new BagelModule())).build(name);
    ObjectGraph objectGraph = getObjectGraph(activityScope);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test public void getActivityScopeWithMoreModules() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new DogfoodModule(), new EggplanModule()))
        .build("moar");

    ObjectGraph objectGraph = getObjectGraph(activityScope);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test public void destroyActivityScopeDirect() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    String name = Bagel.class.getName();
    MortarScope activityScope =
        root.buildChild().withService(SERVICE_NAME, create(root, new BagelModule())).build(name);

    activityScope.register(scoped);
    activityScope.destroy();
    verify(scoped).onExitScope();
    assertThat(root.findChild(name)).isNull();
  }

  @Test public void destroyActivityScopeRecursive() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");

    activityScope.register(scoped);
    root.destroy();
    verify(scoped).onExitScope();

    try {
      getObjectGraph(activityScope);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // pass
    }
  }

  @Test public void activityChildScopeName() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");

    String childScopeName = Carrot.class.getName();
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build(childScopeName);

    assertThat(child.getName()).isEqualTo(childScopeName);
    assertThat(activityScope.findChild(childScopeName)).isSameAs(child);
    assertThat(activityScope.findChild("herman")).isNull();
  }

  @Test public void requireGrandchildWithOneModule() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build("child");
    MortarScope grandchild = child.buildChild()
        .withService(SERVICE_NAME, create(child, new DogfoodModule()))
        .build("grandchild");

    ObjectGraph objectGraph = getObjectGraph(grandchild);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    try {
      objectGraph.get(HasEggplant.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test public void requireGrandchildWithMoreModules() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build("child");
    MortarScope grandchild = child.buildChild()
        .withService(SERVICE_NAME, create(child, new DogfoodModule(), new EggplanModule()))
        .build("grandchild");

    ObjectGraph objectGraph = getObjectGraph(grandchild);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
    try {
      objectGraph.get(String.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test public void requireGrandchildWithNoModules() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build("child");
    MortarScope grandchild = child.buildChild().build("grandchild");

    ObjectGraph objectGraph = getObjectGraph(grandchild);
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());

    try {
      objectGraph.get(String.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test public void destroyActivityChildScopeDirect() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build("child");

    assertThat(activityScope.findChild("child")).isSameAs(child);
    child.register(scoped);
    child.destroy();
    verify(scoped).onExitScope();
    assertThat(activityScope.findChild("child")).isNull();
  }

  @Test public void destroyActivityChildScopeRecursive() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build("child");

    assertThat(activityScope.findChild("child")).isSameAs(child);
    child.register(scoped);
    root.destroy();
    verify(scoped).onExitScope();

    try {
      getObjectGraph(child);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // pass
    }
  }

  @Test public void activityGrandchildScopeName() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    MortarScope activityScope = root.buildChild()
        .withService(SERVICE_NAME, create(root, new BagelModule()))
        .build("activity");
    MortarScope child = activityScope.buildChild()
        .withService(SERVICE_NAME, create(activityScope, new CarrotModule()))
        .build("child");
    MortarScope grandchild = child.buildChild()
        .withService(SERVICE_NAME, create(child, new DogfoodModule()))
        .build("grandchild");

    assertThat(grandchild.getName()).isEqualTo("grandchild");
    assertThat(child.findChild("grandchild")).isSameAs(grandchild);
    assertThat(child.findChild("herman")).isNull();
  }

  @Test public void handlesRecursiveDestroy() {
    final AtomicInteger i = new AtomicInteger(0);

    final MortarScope scope = createRootScope(ObjectGraph.create(new AppleModule()));
    scope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        i.incrementAndGet();
        scope.destroy();
      }
    });
    scope.destroy();
    assertThat(i.get()).isEqualTo(1);
  }

  @Test public void inject() {
    final MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    when(context.getSystemService(any(String.class))).then(new Answer<Object>() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        return root.getService((String) invocation.getArguments()[0]);
      }
    });
    HasApple apple = new HasApple();
    ObjectGraphService.inject(context, apple);
    assertThat(apple.string).isEqualTo(Apple.class.getName());
  }

  @Test public void getScope() {
    MortarScope root = createRootScope(ObjectGraph.create(new AppleModule()));
    wireScopeToMockContext(root, context);
    assertThat(MortarScope.getScope(context)).isSameAs(root);
  }

  @Test public void canGetNameFromDestroyed() {
    MortarScope scope = createRootScope(ObjectGraph.create(new AppleModule()));
    String name = scope.getName();
    assertThat(name).isNotNull();
    scope.destroy();
    assertThat(scope.getName()).isEqualTo(name);
  }

  @Test public void cannotGetObjectGraphFromDestroyed() {
    MortarScope scope = createRootScope(ObjectGraph.create(new AppleModule()));
    scope.destroy();

    try {
      getObjectGraph(scope);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // pass
    }
  }

  @Test public void cannotGetObjectGraphFromContextOfDestroyed() {
    MortarScope scope = createRootScope(ObjectGraph.create(new AppleModule()));
    Context context = mockContext(scope);
    scope.destroy();

    try {
      getObjectGraph(context);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // pass
    }
  }

  private static MortarScope createRootScope(ObjectGraph objectGraph) {
    return MortarScope.buildRootScope().withService(SERVICE_NAME, objectGraph).build("Root");
  }

  private static Context mockContext(final MortarScope root) {
    Context appContext = mock(Context.class);
    return wireScopeToMockContext(root, appContext);
  }

  private static Context wireScopeToMockContext(final MortarScope root, Context appContext) {
    when(appContext.getSystemService(anyString())).thenAnswer(new Answer<Object>() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        String name = (String) invocation.getArguments()[0];
        return root.hasService(name) ? root.getService(name) : null;
      }
    });
    return appContext;
  }
}

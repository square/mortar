// Copyright 2013 Square, Inc.
package mortar;

import android.app.Activity;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import java.lang.annotation.Retention;
import javax.inject.Inject;
import javax.inject.Qualifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MortarScopeTest {
  static class MyContext extends Activity implements HasMortarScope {
    @Override public MortarScope getMortarScope() {
      throw new UnsupportedOperationException();
    }
  }

  @Mock MyContext context;
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

  @Module(injects = HasApple.class) class Able implements Blueprint {
    @Provides @Apple String provideApple() {
      return Apple.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideApple();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasBagel.class) class Baker implements Blueprint {
    @Provides @Bagel String provideBagel() {
      return Bagel.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideBagel();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasCarrot.class) class Charlie implements Blueprint {
    @Provides @Carrot String provideCharlie() {
      return Carrot.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideCharlie();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasDogfood.class) class Delta implements Blueprint {
    @Provides @Dogfood String provideDogfood() {
      return Dogfood.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideDogfood();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = HasEggplant.class) class Echo implements Blueprint {
    @Provides @Eggplant String provideEggplant() {
      return Eggplant.class.getName();
    }

    @Override public String getMortarScopeName() {
      return provideEggplant();
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = DoImpossible.class) class Impossible implements Blueprint {
    @Override public String getMortarScopeName() {
      return "Impossible";
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  class MoreModules implements Blueprint {

    @Override public String getMortarScopeName() {
      return "Moar";
    }

    @Override public Object getDaggerModule() {
      return asList(new Delta(), new Echo());
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

  static class DoImpossible {
    @SuppressWarnings("UnusedDeclaration") @Inject float floot;
  }

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testRootDoesNotValidate() {
    Mortar.createRootScope(false, new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void testRootDoesValidate() {
    Mortar.createRootScope(true, new Impossible());
  }

  @Test
  public void testRootScopeHasName() {
    MortarScope scope = Mortar.createRootScope(false, new Able());
    assertThat(scope.getName()).isSameAs(MortarScope.ROOT_NAME);
  }

  @Test
  public void testCreateRootScopeUsesModules() {
    MortarScope scope = Mortar.createRootScope(false, new Able(), new Baker());
    ObjectGraph objectGraph = scope.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test
  public void destroyRoot() {
    MortarScope scope = Mortar.createRootScope(false, new Able());
    scope.register(scoped);
    scope.destroy();
    verify(scoped).onDestroy();
  }

  @Test
  public void testActivityDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    Mortar.getActivityScope(root, new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void testActivityDoesValidate() {
    MortarScope root = Mortar.createRootScope(true, new Able());
    Mortar.getActivityScope(root, new Impossible());
  }

  @Test
  public void testActivityScopeName() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());

    String bagel = Bagel.class.getName();
    assertThat(activityScope.getName()).isEqualTo(bagel);
    assertThat(root.findChild(bagel)).isSameAs(activityScope);
    assertThat(root.requireChild(new Baker())).isSameAs(activityScope);
    assertThat(Mortar.getActivityScope(root, new Baker())).isSameAs(activityScope);
    assertThat(root.findChild("herman")).isNull();
  }

  @Test
  public void testGetActivityScopeWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    ObjectGraph objectGraph = activityScope.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test
  public void testGetActivityScopeWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new MoreModules());
    ObjectGraph objectGraph = activityScope.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
    try {
      objectGraph.get(HasCarrot.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test
  public void destroyActivityScopeDirect() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    Baker blueprint = new Baker();
    MortarScope activityScope = Mortar.getActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    activityScope.destroy();
    verify(scoped).onDestroy();
    assertThat(root.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test
  public void destroyActivityScopeRecursive() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    Baker blueprint = new Baker();
    MortarScope activityScope = Mortar.getActivityScope(root, blueprint);
    assertThat(root.findChild(blueprint.getMortarScopeName())).isSameAs(activityScope);
    activityScope.register(scoped);
    root.destroy();
    verify(scoped).onDestroy();
    assertThat(root.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test
  public void testActivityChildDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    activityScope.requireChild(new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void testActivityChildDoesValidate() {
    MortarScope root = Mortar.createRootScope(true, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    activityScope.requireChild(new Impossible());
  }

  @Test
  public void testActivityChildScopeName() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    MortarScope child = activityScope.requireChild(new Charlie());

    String carrot = Carrot.class.getName();
    assertThat(child.getName()).isEqualTo(carrot);
    assertThat(activityScope.findChild(carrot)).isSameAs(child);
    assertThat(activityScope.requireChild(new Charlie())).isSameAs(child);
    assertThat(activityScope.findChild("herman")).isNull();
  }

  @Test
  public void testRequireChildWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarScope child = Mortar.getActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new Delta());
    ObjectGraph objectGraph = grandchild.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    try {
      objectGraph.get(HasEggplant.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }

  @Test
  public void testRequireGrandchildWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarScope child = Mortar.getActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new MoreModules());

    ObjectGraph objectGraph = grandchild.getObjectGraph();
    assertThat(objectGraph.get(HasApple.class).string).isEqualTo(Apple.class.getName());
    assertThat(objectGraph.get(HasBagel.class).string).isEqualTo(Bagel.class.getName());
    assertThat(objectGraph.get(HasCarrot.class).string).isEqualTo(Carrot.class.getName());
    assertThat(objectGraph.get(HasDogfood.class).string).isEqualTo(Dogfood.class.getName());
    assertThat(objectGraph.get(HasEggplant.class).string).isEqualTo(Eggplant.class.getName());
    try {
      objectGraph.get(String.class);
    } catch (IllegalArgumentException e) {
      // pass
      return;
    }
    fail("Expected IllegalArgumentException");
  }


  @Test
  public void destroyActivityChildScopeDirect() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    Charlie blueprint = new Charlie();
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    MortarScope child = activityScope.requireChild(blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    child.destroy();
    verify(scoped).onDestroy();
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test
  public void destroyActivityChildScopeRecursive() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    Charlie blueprint = new Charlie();
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    MortarScope child = activityScope.requireChild(blueprint);
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isSameAs(child);
    child.register(scoped);
    root.destroy();
    verify(scoped).onDestroy();
    assertThat(activityScope.findChild(blueprint.getMortarScopeName())).isNull();
  }

  @Test
  public void testActivityGrandchildDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    activityScope.requireChild(new Delta()).requireChild(new Impossible());
    // ta da!
  }

  @Test(expected = IllegalStateException.class)
  public void testActivityGrandchildDoesValidate() {
    MortarScope root = Mortar.createRootScope(true, new Able());
    MortarActivityScope activityScope = Mortar.getActivityScope(root, new Baker());
    activityScope.requireChild(new Delta()).requireChild(new Impossible());
  }

  @Test
  public void testActivityGrandchildScopeName() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarScope child = Mortar.getActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new Delta());

    String dogfood = Dogfood.class.getName();
    assertThat(grandchild.getName()).isEqualTo(dogfood);
    assertThat(child.findChild(dogfood)).isSameAs(grandchild);
    assertThat(child.requireChild(new Delta())).isSameAs(grandchild);
    assertThat(child.findChild("herman")).isNull();
  }

  @Test
  public void testRequireGranddhildWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarScope child = Mortar.getActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new Delta());
    assertThat(grandchild.getObjectGraph().get(HasApple.class).string).isEqualTo(
        Apple.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasBagel.class).string).isEqualTo(
        Bagel.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasCarrot.class).string).isEqualTo(
        Carrot.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasDogfood.class).string).isEqualTo(
        Dogfood.class.getName());
  }

  @Test
  public void testRequireChildWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarScope child = Mortar.getActivityScope(root, new Baker()).requireChild(new Charlie());
    MortarScope grandchild = child.requireChild(new MoreModules());
    assertThat(grandchild.getObjectGraph().get(HasApple.class).string).isEqualTo(
        Apple.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasBagel.class).string).isEqualTo(
        Bagel.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasCarrot.class).string).isEqualTo(
        Carrot.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasDogfood.class).string).isEqualTo(
        Dogfood.class.getName());
    assertThat(grandchild.getObjectGraph().get(HasEggplant.class).string).isEqualTo(
        Eggplant.class.getName());
  }

  @Test
  public void testInject() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    when(context.getMortarScope()).thenReturn(root);
    HasApple apple = new HasApple();
    Mortar.inject(context, apple);
    assertThat(apple.string).isEqualTo(Apple.class.getName());
  }

  @Test
  public void testGetScope() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    when(context.getMortarScope()).thenReturn(root);
    assertThat(Mortar.getScope(context)).isSameAs(root);
  }
}

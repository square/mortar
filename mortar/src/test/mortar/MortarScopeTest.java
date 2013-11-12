// Copyright 2013 Square, Inc.
package mortar;

import dagger.Module;
import dagger.Provides;
import javax.inject.Inject;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;

public class MortarScopeTest {

  private static final String STRING = "Able";
  private static final int INT = 123;
  private static final double DOUBLE = 123.45;

  @Module(injects = DoMeString.class) class Able implements Blueprint {
    @Provides String provideString() {
      return STRING;
    }

    @Override public String getMortarScopeName() {
      return STRING;
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = DoMeInt.class) class Baker implements Blueprint {
    @Provides int provideInt() {
      return INT;
    }

    @Override public String getMortarScopeName() {
      return "baker";
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Module(injects = DoMeDouble.class) class Charlie implements Blueprint {
    @Provides double provideDouble() {
      return DOUBLE;
    }

    @Override public String getMortarScopeName() {
      return "Charlie";
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
      return asList(new Baker(), new Charlie());
    }
  }

  static class DoMeString {
    @Inject String string;
  }

  static class DoMeInt {
    @Inject int eent;
  }

  static class DoMeDouble {
    @Inject double dibble;
  }

  static class DoImpossible {
    @Inject float floot;
  }

  @Test
  public void testRootScope_hasName() {
    MortarScope scope = Mortar.createRootScope(false, new Able());
    assertThat(scope.getName()).isSameAs(MortarScope.ROOT_NAME);
  }

  @Test
  public void testCreateRootScope_usesModules() {
    MortarScope scope = Mortar.createRootScope(false, new Able(), new Baker());
    assertThat(scope.getObjectGraph().get(DoMeString.class).string).isEqualTo(STRING);
    assertThat(scope.getObjectGraph().get(DoMeInt.class).eent).isEqualTo(INT);
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
  public void testGetActivityScopeWithOneModule() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope junior = Mortar.getActivityScope(root, new MoreModules());
    assertThat(junior.getObjectGraph().get(DoMeString.class).string).isEqualTo(STRING);
    assertThat(junior.getObjectGraph().get(DoMeInt.class).eent).isEqualTo(INT);
  }

  @Test
  public void testGetActivityScopeWithMoreModules() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    MortarActivityScope junior = Mortar.getActivityScope(root, new MoreModules());
    assertThat(junior.getObjectGraph().get(DoMeString.class).string).isEqualTo(STRING);
    assertThat(junior.getObjectGraph().get(DoMeInt.class).eent).isEqualTo(INT);
    assertThat(junior.getObjectGraph().get(DoMeDouble.class).dibble).isEqualTo(DOUBLE);
  }

  @Test
  public void testActivityRootDoesNotValidate() {
    MortarScope root = Mortar.createRootScope(false, new Able());
    Mortar.getActivityScope(root, new Impossible());
    // ta da!
  }

  @Test
  public void testInject() {

  }

  @Test
  public void testGetScope() {

  }
}

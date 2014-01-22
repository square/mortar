package mortar;

import dagger.ObjectGraph;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MortarScopeDevHelperTest {

  class CustomScope implements Blueprint {

    private final String name;

    CustomScope(String name) {
      this.name = name;
    }

    @Override public String getMortarScopeName() {
      return name;
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Mock ObjectGraph graph;

  @Before
  public void setUp() {
    initMocks(this);
    when(graph.plus(any())).thenReturn(graph);
  }

  @Test public void nestedScopeHierarchyToString() {
    MortarScope root = Mortar.createRootScope(false, graph);
    MortarScope elder = root.requireChild(new CustomScope("Elder"));
    elder.requireChild(new CustomScope("ElderElder"));
    elder.requireChild(new CustomScope("ElderCadet"));
    root.requireChild(new CustomScope("Cadet"));

    String hierarchy = MortarScopeDevHelper.scopeHierarchyToString(root);

    /*
        Here is what it should look like:

        Root
        +- Elder
        |  +- ElderElder
        |  \- ElderCadet
        \- Cadet
     */

    assertThat(hierarchy).isEqualTo("" //
        + "Root\n" //
        + "+- Elder\n" //
        + "|  +- ElderElder\n" //
        + "|  \\- ElderCadet\n" //
        + "\\- Cadet\n");
  }
}

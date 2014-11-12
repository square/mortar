package mortar.dagger1support;

import dagger.ObjectGraph;
import java.util.Collection;
import mortar.Blueprint;

public abstract class Dagger1Blueprint implements Blueprint {

  /**
   * Returns the {@literal @}{@link dagger.Module Module} that will define the scope
   * of the new graph by being added to that of its parent. If the returned value
   * is an instance of {@link Collection} its contents will be used as modules.
   * Returns null if this scope needs no modules.
   *
   * @see ObjectGraph#plus(Object...)
   */
  protected abstract Object getDaggerModule();

  @Override public final Object createSubgraph(Object o) {
    ObjectGraph graph = (ObjectGraph) o;
    Object daggerModule = getDaggerModule();
    ObjectGraph newGraph;
    if (daggerModule == null) {
      newGraph = graph.plus();
    } else if (daggerModule instanceof Collection) {
      Collection c = (Collection) daggerModule;
      newGraph = graph.plus(c.toArray(new Object[c.size()]));
    } else {
      newGraph = graph.plus(daggerModule);
    }
    return newGraph;
  }
}

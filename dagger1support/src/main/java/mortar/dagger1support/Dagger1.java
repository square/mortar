package mortar.dagger1support;

import android.app.Activity;
import android.content.Context;
import dagger.ObjectGraph;
import java.util.Collection;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;

/**
 * Provides utility methods for using Mortar with Dagger 1.
 */
public class Dagger1 {

  public static MortarScope createRootScope() {
    return Mortar.createRootScope(ObjectGraph.create());
  }

  public static MortarScope createRootScope(ObjectGraph objectGraph) {
    return Mortar.createRootScope(objectGraph);
  }

  public static ObjectGraph getObjectGraph(Context context) {
    return Mortar.getScope(context).getObjectGraph();
  }

  /**
   * A convenience wrapper for {@link Mortar#getScope} to simplify dynamic injection, typically
   * for {@link Activity} and {@link android.view.View} instances that must be instantiated
   * by Android.
   */
  public static void inject(Context context, Object object) {
    getObjectGraph(context).inject(object);
  }

  public static ObjectGraph createSubgraph(ObjectGraph parentGraph, Object daggerModule) {
    ObjectGraph newGraph;
    if (daggerModule == null) {
      newGraph = parentGraph.plus();
    } else if (daggerModule instanceof Collection) {
      Collection c = (Collection) daggerModule;
      newGraph = parentGraph.plus(c.toArray(new Object[c.size()]));
    } else {
      newGraph = parentGraph.plus(daggerModule);
    }
    return newGraph;
  }

  /**
   * Returns the existing {@link MortarActivityScope} scope for the given {@link Activity}, or
   * uses the {@link Blueprint} to create one if none is found.
   * <p/>
   * It is expected that this method will be called from {@link Activity#onCreate}. Calling
   * it at other times may lead to surprises.
   * <p/>
   * This scope can be destroyed by the {@link MortarScope#destroyChild} method on the
   * given parent.
   */
  public static MortarActivityScope requireActivityScope(MortarScope parentScope,
      Blueprint blueprint) {
    String childName = blueprint.getMortarScopeName();
    MortarActivityScope child = (MortarActivityScope) parentScope.findChild(childName);
    if (child == null) {
      ObjectGraph parentGraph = parentScope.getObjectGraph();
      Object daggerModule = blueprint.getDaggerModule();
      Object childGraph = createSubgraph(parentGraph, daggerModule);
      child = Mortar.createActivityScope(parentScope, childName, childGraph);
    }
    return child;
  }

  /**
   * Returns the existing child whose name matches the given {@link Blueprint}'s
   * {@link Blueprint#getMortarScopeName()} value. If there is none, a new child is created
   * based on {@link Blueprint#getDaggerModule()}. Note that
   * {@link Blueprint#getDaggerModule()} is not called otherwise.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  public static MortarScope requireChild(MortarScope parentScope, Blueprint blueprint) {
    String childName = blueprint.getMortarScopeName();
    MortarScope child = parentScope.findChild(childName);
    if (child == null) {
      ObjectGraph parentGraph = parentScope.getObjectGraph();
      Object daggerModule = blueprint.getDaggerModule();
      Object childGraph = createSubgraph(parentGraph, daggerModule);
      child = parentScope.createChild(childName, childGraph);
    }
    return child;
  }
}

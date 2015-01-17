package mortar.dagger1support;

import android.app.Activity;
import android.content.Context;
import dagger.ObjectGraph;
import java.util.Collection;
import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;

/**
 * Provides utility methods for using Mortar with Dagger 1.
 */
public class ObjectGraphService {
  public static final String SERVICE_NAME = ObjectGraphService.class.getName();

  /**
   * Create a new {@link ObjectGraph} based on the given module. The new graph will extend
   * the graph found in the parent scope (via {@link ObjectGraph#plus}), if there is one.
   */
  public static ObjectGraph create(MortarScope parent, Object... daggerModules) {
    ObjectGraph parentGraph = getObjectGraph(parent);

    return parentGraph == null ? ObjectGraph.create(daggerModules)
        : parentGraph.plus(daggerModules);
  }

  public static ObjectGraph getObjectGraph(Context context) {
    return (ObjectGraph) context.getSystemService(ObjectGraphService.SERVICE_NAME);
  }

  public static ObjectGraph getObjectGraph(MortarScope scope) {
    return scope.getService(ObjectGraphService.SERVICE_NAME);
  }

  /**
   * A convenience wrapper for {@link ObjectGraphService#getObjectGraph} to simplify dynamic
   * injection, typically for {@link Activity} and {@link android.view.View} instances that must be
   * instantiated by Android.
   */
  public static void inject(Context context, Object object) {
    getObjectGraph(context).inject(object);
  }

  private static ObjectGraph createSubgraphBlueprintStyle(ObjectGraph parentGraph,
      Object daggerModule) {
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
   * Returns the existing {@link MortarScope} scope for the given {@link Activity}, or
   * uses the {@link Blueprint} to create one if none is found. The scope will provide
   * {@link mortar.bundler.BundleService} and {@link BundleServiceRunner}.
   * <p/>
   * It is expected that this method will be called from {@link Activity#onCreate}. Calling
   * it at other times may lead to surprises.
   *
   * @see MortarScope.Builder#withService(String, Object)
   * @deprecated This method is provided to ease migration from earlier releases, which
   * coupled Dagger and Activity integration. Instead build new scopes with {@link
   * MortarScope#buildChild(String)}, and bind {@link ObjectGraphService} and
   * {@link BundleServiceRunner} instances to them.
   */
  @Deprecated public static MortarScope requireActivityScope(MortarScope parentScope,
      Blueprint blueprint) {
    String childName = blueprint.getMortarScopeName();
    MortarScope child = parentScope.findChild(childName);
    if (child == null) {
      ObjectGraph parentGraph = parentScope.getService(ObjectGraphService.SERVICE_NAME);
      Object daggerModule = blueprint.getDaggerModule();
      Object childGraph = createSubgraphBlueprintStyle(parentGraph, daggerModule);
      child = parentScope.buildChild(childName)
          .withService(ObjectGraphService.SERVICE_NAME, childGraph)
          .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
          .build();
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
   * @see MortarScope.Builder#withService(String, Object)
   * @deprecated This method is provided to ease migration from earlier releases, which
   * required Dagger integration. Instead build new scopes with {@link
   * MortarScope#buildChild(String)}, and bind {@link ObjectGraphService}  instances to them.
   */
  @Deprecated public static MortarScope requireChild(MortarScope parentScope, Blueprint blueprint) {
    String childName = blueprint.getMortarScopeName();
    MortarScope child = parentScope.findChild(childName);
    if (child == null) {
      ObjectGraph parentGraph = parentScope.getService(ObjectGraphService.SERVICE_NAME);
      Object daggerModule = blueprint.getDaggerModule();
      Object childGraph = createSubgraphBlueprintStyle(parentGraph, daggerModule);
      child = parentScope.buildChild(childName)
          .withService(ObjectGraphService.SERVICE_NAME, childGraph)
          .build();
    }
    return child;
  }
}

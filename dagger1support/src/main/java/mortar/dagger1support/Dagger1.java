package mortar.dagger1support;

import android.app.Activity;
import android.content.Context;
import dagger.ObjectGraph;
import mortar.Mortar;
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
}

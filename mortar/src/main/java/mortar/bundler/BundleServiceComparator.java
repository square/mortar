package mortar.bundler;

import java.util.Comparator;
import mortar.MortarScope;

class BundleServiceComparator implements Comparator<BundleService> {
  @Override public int compare(BundleService left, BundleService right) {
    String[] leftPath = left.scope.getPath().split(MortarScope.DIVIDER);
    String[] rightPath = right.scope.getPath().split(MortarScope.DIVIDER);

    if (leftPath.length != rightPath.length) {
      return leftPath.length < rightPath.length ? -1 : 1;
    }

    int segments = leftPath.length;
    for (int i = 0; i < segments; i++) {
      int result = leftPath[i].compareTo(rightPath[i]);
      if (result != 0) return result;
    }

    return 0;
  }
}

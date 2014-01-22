package mortar;

import java.util.Collection;
import java.util.Collections;

public class MortarScopeDevHelper {

  /** Format the scope hierarchy as a multi line string containing the scope names. */
  public static String scopeHierarchyToString(MortarScope mortarScope) {
    StringBuilder result = new StringBuilder();
    scopeHierarchyToString(result, 0, 0, mortarScope);
    return result.toString();
  }

  private static void scopeHierarchyToString(StringBuilder result, int depth, long lastChildMask,
      MortarScope mortarScope) {

    int lastDepth = depth - 1;
    for (int parentDepth = 0; parentDepth <= lastDepth; parentDepth++) {
      if (parentDepth > 0) {
        result.append(' ');
      }
      boolean lastChild = (lastChildMask & (1 << parentDepth)) != 0;
      if (lastChild) {
        if (parentDepth == lastDepth) {
          result.append('\\');
        } else {
          result.append(' ');
        }
      } else {
        if (parentDepth == lastDepth) {
          result.append('+');
        } else {
          result.append("| ");
        }
      }
    }
    if (depth > 0) {
      result.append("- ");
    }
    result.append(mortarScope.getName());
    result.append('\n');

    Collection<MortarScope> children = getScopeChildren(mortarScope);
    int lastIndex = children.size() - 1;
    int index = 0;
    for (MortarScope childScope : children) {
      if (index == lastIndex) {
        lastChildMask = lastChildMask | (1 << depth);
      }
      scopeHierarchyToString(result, depth + 1, lastChildMask, childScope);
      index++;
    }
  }

  private static Collection<MortarScope> getScopeChildren(MortarScope mortarScope) {
    if (!(mortarScope instanceof RealMortarScope)) {
      return Collections.emptyList();
    }
    RealMortarScope realMortarScope = (RealMortarScope) mortarScope;
    return Collections.<MortarScope>unmodifiableCollection(realMortarScope.children.values());
  }
}

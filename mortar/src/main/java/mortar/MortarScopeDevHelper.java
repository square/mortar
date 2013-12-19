package mortar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MortarScopeDevHelper {

  /** Format the scope hierarchy as a multi line string containing the scope names. */
  public static String scopeHierarchyToString(MortarScope mortarScope) {
    StringBuilder result = new StringBuilder();
    scopeHierarchyToString(result, 0, mortarScope);
    return result.toString();
  }

  private static void scopeHierarchyToString(StringBuilder result, int depth,
      MortarScope mortarScope) {
    if (depth > 1) {
      result.append(String.format("%" + (depth - 1) + "s", " ").replaceAll(" ", "| "));
    }
    if (depth > 0) {
      result.append("|-");
    }
    result.append(mortarScope.getName());
    result.append('\n');

    for (MortarScope childScope : getScopeChildren(mortarScope)) {
      scopeHierarchyToString(result, depth + 1, childScope);
    }
  }

  private static List<MortarScope> getScopeChildren(MortarScope mortarScope) {
    if (!(mortarScope instanceof RealMortarScope)) {
      return Collections.emptyList();
    }

    RealMortarScope realMortarScope = (RealMortarScope) mortarScope;
    return new ArrayList<MortarScope>(realMortarScope.children.values());
  }
}

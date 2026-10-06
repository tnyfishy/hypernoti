package dev.hypernoti;
import java.util.ArrayList;
import java.util.List;
public final class CommandPolicy {
 private CommandPolicy() {}
 public static List<String[]> commands(String pkg, boolean enable) {
  if (pkg == null || !pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) throw new IllegalArgumentException("Invalid package name");
  List<String[]> result = new ArrayList<>();
  result.add(new String[]{"/system/bin/cmd", "deviceidle", "whitelist", (enable ? "+" : "-") + pkg});
  result.add(new String[]{"/system/bin/cmd", "appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", enable ? "allow" : "default"});
  return result;
 }
 public static String rootCommand(String pkg, boolean enable) {
  StringBuilder result = new StringBuilder();
  for (String[] command : commands(pkg, enable)) {
   if (result.length() > 0) result.append(" && ");
   result.append(String.join(" ", command));
  }
  return result.toString();
 }
}

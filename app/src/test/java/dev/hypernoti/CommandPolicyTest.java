package dev.hypernoti;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandPolicyTest {
 @Test public void enableAddsWhitelistAndAllowsBackground() {
  assertArrayEquals(new String[]{"/system/bin/cmd","deviceidle","whitelist","+com.google.android.gms"}, CommandPolicy.commands("com.google.android.gms",true).get(0));
  assertEquals("allow", CommandPolicy.commands("com.google.android.gms",true).get(1)[5]);
 }
 @Test public void undoRemovesWhitelistAndResetsAppOp() {
  assertEquals("-dev.hypernoti", CommandPolicy.commands("dev.hypernoti",false).get(0)[3]);
  assertEquals("default", CommandPolicy.commands("dev.hypernoti",false).get(1)[5]);
 }
 @Test(expected=IllegalArgumentException.class) public void rejectsShellInjection() { CommandPolicy.rootCommand("x.y; reboot",true); }
 @Test(expected=IllegalArgumentException.class) public void rejectsNull() { CommandPolicy.commands(null,true); }
 @Test(expected=IllegalArgumentException.class) public void rejectsPath() { CommandPolicy.commands("../../system",true); }
}

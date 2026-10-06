package dev.hypernoti;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReadinessTest {
 @Test public void absentServicesAreUnavailable() { assertFalse(Readiness.googleAvailable(false, true)); }
 @Test public void disabledServicesAreUnavailable() { assertFalse(Readiness.googleAvailable(true, false)); }
 @Test public void enabledServicesAreAvailable() { assertTrue(Readiness.googleAvailable(true, true)); }
 @Test public void countsOnlyConfirmedSteps() { assertEquals(2, Readiness.completed(new boolean[]{true,false,true,false})); }
 @Test public void emptyChecklistHasNoCompletedSteps() { assertEquals(0, Readiness.completed(new boolean[]{})); }
}

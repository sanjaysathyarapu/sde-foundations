package foundations;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Keeps CI meaningful before the first entry lands: proves the toolchain compiles and runs tests. */
class BuildSanityTest {

    @Test
    void runsOnJava21OrNewer() {
        assertTrue(Runtime.version().feature() >= 21, "examples target Java 21");
    }
}

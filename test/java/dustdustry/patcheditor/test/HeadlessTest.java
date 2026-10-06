package dustdustry.patcheditor.test;

import org.junit.jupiter.api.BeforeAll;

/** Starts the fake game environment before the first test body of every suite. */
abstract class HeadlessTest{

    @BeforeAll
    static void bootGameEnvironment(){
        HeadlessEnv.init();
    }
}

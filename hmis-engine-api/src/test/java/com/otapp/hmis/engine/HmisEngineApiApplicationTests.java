package com.otapp.hmis.engine;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class HmisEngineApiApplicationTests {

    @Test
    void modulesAreWellFormed() {
        ApplicationModules.of(HmisEngineApiApplication.class).verify();
    }
}

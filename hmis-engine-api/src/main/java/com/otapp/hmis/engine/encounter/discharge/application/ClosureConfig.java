package com.otapp.hmis.engine.encounter.discharge.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Wires the closure-workflow configuration ({@link ClosureProperties}). */
@Configuration
@EnableConfigurationProperties(ClosureProperties.class)
public class ClosureConfig {
}

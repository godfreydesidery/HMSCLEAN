package com.otapp.hmis.engine.iam.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hmis.bootstrap")
public record IamBootstrapProperties(String rootUsername, String rootPassword) {
}

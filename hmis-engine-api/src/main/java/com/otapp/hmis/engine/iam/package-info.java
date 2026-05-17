/**
 * Identity and access management module.
 *
 * <p>Owns users, roles, privileges, authentication (login, token refresh), and
 * authorization. Other modules consume IAM only through its published API:
 * the JWT-derived {@code Authentication} and the public DTOs.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Identity & Access",
        allowedDependencies = {"common", "common.*"}
)
package com.otapp.hmis.engine.iam;

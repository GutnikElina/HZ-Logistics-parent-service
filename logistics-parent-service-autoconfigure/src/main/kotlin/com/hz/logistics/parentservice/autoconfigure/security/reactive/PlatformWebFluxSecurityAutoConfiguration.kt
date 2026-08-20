package com.hz.logistics.parentservice.autoconfigure.security.reactive

import com.hz.logistics.parentservice.autoconfigure.PlatformAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

/**
 * Reactive/WebFlux selection boundary for the platform security capability.
 *
 * Web-specific security types remain strings in the condition so an
 * application that did not select WebFlux never links reactive infrastructure.
 * The default reactive chain is added by the security implementation phase.
 */
@AutoConfiguration
@AutoConfigureAfter(PlatformAutoConfiguration::class)
@ConditionalOnClass(
    name = [
        "org.springframework.security.oauth2.jwt.ReactiveJwtDecoder",
        "org.springframework.web.reactive.DispatcherHandler",
    ],
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(
    prefix = "logistics.parent-service.security",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class PlatformWebFluxSecurityAutoConfiguration

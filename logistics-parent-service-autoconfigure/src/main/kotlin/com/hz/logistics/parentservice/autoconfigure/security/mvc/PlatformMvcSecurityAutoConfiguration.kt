package com.hz.logistics.parentservice.autoconfigure.security.mvc

import com.hz.logistics.parentservice.autoconfigure.PlatformAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

/**
 * Servlet/MVC selection boundary for the platform security capability.
 *
 * Web-specific security types remain strings in the condition so an
 * application that did not select MVC never links Servlet infrastructure.
 * The default MVC chain is added by the security implementation phase.
 */
@AutoConfiguration
@AutoConfigureAfter(PlatformAutoConfiguration::class)
@ConditionalOnClass(
    name = [
        "org.springframework.security.oauth2.jwt.JwtDecoder",
        "org.springframework.web.servlet.DispatcherServlet",
    ],
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "logistics.parent-service.security",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class PlatformMvcSecurityAutoConfiguration

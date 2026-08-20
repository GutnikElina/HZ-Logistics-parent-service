package com.hz.logistics.parentservice.autoconfigure

import com.hz.logistics.parentservice.autoconfigure.properties.PlatformProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.Ordered

/**
 * Non-web foundation for the shared platform.
 *
 * This configuration deliberately references no Servlet, MVC, WebFlux, or
 * reactive type. Stack-specific auto-configurations can therefore be selected
 * later by Spring Boot without linking an unselected web model. The capability
 * `enabled` flags live on the independently bound property groups and are
 * consumed by each capability's own conditional configuration.
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(PlatformProperties::class)
class PlatformAutoConfiguration

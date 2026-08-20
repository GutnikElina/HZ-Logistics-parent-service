package com.hz.logistics.parentservice.autoconfigure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/** Minimal Reactive consumer used by the public-starter adoption test. */
@SpringBootApplication(proxyBeanMethods = false)
class WebFluxAdoptionFixtureApplication {

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder = ReactiveJwtDecoder {
        Mono.error(JwtException("A token is not needed for the startup fixture"))
    }
}

/** A representative WebFlux endpoint; the application, not the starter, chose WebFlux. */
@RestController
class WebFluxAdoptionFixtureController {

    @GetMapping("/fixtures/webflux")
    fun response(): Mono<Map<String, String>> = Mono.just(mapOf("stack" to "webflux"))
}

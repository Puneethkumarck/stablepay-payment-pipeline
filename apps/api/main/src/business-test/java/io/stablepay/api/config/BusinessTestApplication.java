package io.stablepay.api.config;

import io.stablepay.api.StablepayApiApplication;
import io.stablepay.api.infrastructure.opensearch.OpenSearchConfig;
import io.stablepay.api.infrastructure.opensearch.OpenSearchTransactionRepository;
import io.stablepay.api.infrastructure.security.SecurityConfig;
import io.stablepay.api.infrastructure.sse.SseTransactionPoller;
import io.stablepay.api.infrastructure.trino.TrinoConfig;
import io.stablepay.api.infrastructure.trino.TrinoCustomerRepository;
import io.stablepay.api.infrastructure.trino.TrinoDlqRepository;
import io.stablepay.api.infrastructure.trino.TrinoFlowRepository;
import io.stablepay.api.infrastructure.trino.TrinoStuckRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableScheduling
@ComponentScan(
    basePackages = "io.stablepay.api",
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
              StablepayApiApplication.class,
              TrinoConfig.class,
              TrinoCustomerRepository.class,
              TrinoDlqRepository.class,
              TrinoFlowRepository.class,
              TrinoStuckRepository.class,
              OpenSearchConfig.class,
              OpenSearchTransactionRepository.class,
              SseTransactionPoller.class,
              SecurityConfig.class
            }))
public class BusinessTestApplication {}

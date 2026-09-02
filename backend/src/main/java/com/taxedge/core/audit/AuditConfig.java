package com.taxedge.core.audit;

import org.springframework.context.annotation.Configuration;

/**
 * Auditing enabled via @EnableJpaAuditing on the main application class.
 * Extend here if a custom AuditorAware is required to record actor id.
 */
@Configuration
public class AuditConfig {
}

package com.zyelectric.ocpp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor websocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);           // Minimum number of threads
        executor.setMaxPoolSize(500);            // Maximum number of threads
        executor.setQueueCapacity(1000);         // Queue size for waiting tasks
        executor.setKeepAliveSeconds(60);        // Keep idle threads alive
        executor.setThreadNamePrefix("ws-");     // Thread name prefix for better tracking
        executor.initialize();
        return executor;
    }
}

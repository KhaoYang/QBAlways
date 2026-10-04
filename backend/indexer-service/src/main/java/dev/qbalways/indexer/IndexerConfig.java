package dev.qbalways.indexer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class IndexerConfig {
    @Bean
    TaskExecutor indexingExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("qbalways-index-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(1);
        return executor;
    }
}

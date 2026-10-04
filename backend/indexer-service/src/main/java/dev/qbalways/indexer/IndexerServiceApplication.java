package dev.qbalways.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IndexerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IndexerServiceApplication.class, args);
    }
}

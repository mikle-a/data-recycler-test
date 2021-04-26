package com.test.datarecycler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties("app")
public class AppProperties {

    private Integer batchSize;
    private Database database;
    private Executor executor;
    private Duration completeJobsRetentionPeriod;

    @Data
    public static class Database {
        private String url;
        private String user;
        private String pass;
    }

    @Data
    public static class Executor {
        private Integer threadsCount;
    }

}

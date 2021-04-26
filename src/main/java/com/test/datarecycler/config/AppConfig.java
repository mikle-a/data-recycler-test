package com.test.datarecycler.config;

import com.test.datarecycler.db.Database;
import com.test.datarecycler.db.PostgresDatabase;
import com.test.datarecycler.exec.LocalRecycleJobExecutor;
import com.test.datarecycler.exec.RecycleJobExecutor;
import com.test.datarecycler.repository.InMemoryJobRepository;
import com.test.datarecycler.repository.RecycleJobRepository;
import com.test.datarecycler.service.RecycleJobService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AppConfig {

    @Bean
    public RecycleJobService recycleJobService(RecycleJobRepository jobRepository,
                                               RecycleJobExecutor jobExecutor,
                                               AppProperties properties) {
        return new RecycleJobService(jobRepository, jobExecutor, properties.getBatchSize());
    }

    @Bean
    public RecycleJobExecutor recycleJobExecutor(Database database,
                                                 RecycleJobRepository jobRepository,
                                                 ExecutorService jobsExecutorsService) {
        return new LocalRecycleJobExecutor(database, jobRepository, jobsExecutorsService);
    }

    @Bean
    public RecycleJobRepository recycleJobRepository(ScheduledExecutorService scheduler, AppProperties appProperties) {
        return new InMemoryJobRepository(scheduler, appProperties.getCompleteJobsRetentionPeriod());
    }

    @Bean
    public ScheduledExecutorService scheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }


    @Bean
    public ExecutorService jobsExecutorsService(AppProperties appProperties) {
        return Executors.newFixedThreadPool(appProperties.getExecutor().getThreadsCount());
    }

    @Bean
    public Database database(DataSource dataSource) {
        return new PostgresDatabase(dataSource);
    }

    @Bean
    public DataSource dataSource(AppProperties properties) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getDatabase().getUrl());
        config.setUsername(properties.getDatabase().getUser());
        config.setPassword(properties.getDatabase().getPass());
        config.setAutoCommit(false);
        return new HikariDataSource(config);
    }

}

package edu.stanford.protege.github.cloneservice;

import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepositoryCommandHandler;
import edu.stanford.protege.github.cloneservice.service.ProjectHistoryStorer;
import edu.stanford.protege.github.cloneservice.utils.OntologyHistoryAnalyzer;
import edu.stanford.protege.webprotege.ipc.EventDispatcher;
import edu.stanford.protege.webprotege.ipc.WebProtegeIpcApplication;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Import(WebProtegeIpcApplication.class)
public class ApplicationBeansConfiguration {

    @Bean
    CreateProjectHistoryFromGitHubRepositoryCommandHandler createProjectHistoryFromGitHubRepositoryCommandHandler(
            OntologyHistoryAnalyzer ontologyHistoryAnalyzer,
            ProjectHistoryStorer projectHistoryStorer,
            EventDispatcher eventDispatcher,
            @Qualifier("projectHistoryImportExecutor") Executor projectHistoryImportExecutor) {
        return new CreateProjectHistoryFromGitHubRepositoryCommandHandler(
                ontologyHistoryAnalyzer, projectHistoryStorer, eventDispatcher, projectHistoryImportExecutor);
    }

    @Bean(name = "projectHistoryImportExecutor")
    Executor projectHistoryImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("project-history-import-");
        executor.initialize();
        return executor;
    }
}

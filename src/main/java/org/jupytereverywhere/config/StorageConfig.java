package org.jupytereverywhere.config;

import org.jupytereverywhere.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.ApplicationContext;

@Configuration
public class StorageConfig {

    @Value("${storage.type:file}")
    private String storageType;

    @Autowired
    private ApplicationContext context;

    @Bean
    @Primary
    public StorageService storageService() {
        if ("s3".equalsIgnoreCase(storageType)) {
            return context.getBean("s3StorageService", StorageService.class);
        }
        return context.getBean("fileStorageService", StorageService.class);
    }
}

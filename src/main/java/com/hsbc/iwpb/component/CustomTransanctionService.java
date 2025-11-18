package com.hsbc.iwpb.component;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class CustomTransanctionService {
    
	@Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setIsolationLevel(TransactionTemplate.ISOLATION_REPEATABLE_READ);
		template.setTimeout(10); // Set timeout to 10 seconds
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		return template;
    }
}
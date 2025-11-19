package com.hsbc.iwpb.component;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomTransanctionServiceTest {
    @Test
    void testTransactionTemplateBeanProperties() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        CustomTransanctionService service = new CustomTransanctionService();
        TransactionTemplate template = service.transactionTemplate(manager);
        assertEquals(TransactionTemplate.ISOLATION_REPEATABLE_READ, template.getIsolationLevel());
        assertEquals(10, template.getTimeout());
        assertEquals(TransactionTemplate.PROPAGATION_REQUIRED, template.getPropagationBehavior());
    }
}

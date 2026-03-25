package com.example.warehouse.integration;

import com.example.warehouse.entity.CommunicationChannel;
import com.example.warehouse.entity.CommunicationLogEntity;
import com.example.warehouse.entity.CommunicationStatus;
import com.example.warehouse.entity.EntityAuditLogEntity;
import com.example.warehouse.repository.CommunicationLogRepository;
import com.example.warehouse.repository.EntityAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warehouse-comm-audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class CommunicationAndAuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Autowired
    private EntityAuditLogRepository entityAuditLogRepository;

    @BeforeEach
    void setUp() {
        communicationLogRepository.deleteAll();
        entityAuditLogRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void communicationsListSupportsDocumentChannelStatusFilters() throws Exception {
        communicationLogRepository.save(log("SALES_ORDER", CommunicationStatus.SENT));
        communicationLogRepository.save(log("PURCHASE_ORDER", CommunicationStatus.FAILED));

        mockMvc.perform(get("/api/communications")
                        .param("documentType", "sales_order")
                        .param("channel", "email")
                        .param("status", "sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documentType").value("SALES_ORDER"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void communicationsRejectInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/communications")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid communication status"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void auditLogSupportsEntityFilters() throws Exception {
        entityAuditLogRepository.save(audit("PRODUCT", "CREATE"));
        entityAuditLogRepository.save(audit("SALES_ORDER", "STATUS_CHANGE"));

        mockMvc.perform(get("/api/audit-log")
                        .param("entityType", "sales_order")
                        .param("action", "status_change"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].entityType").value("SALES_ORDER"))
                .andExpect(jsonPath("$[0].action").value("STATUS_CHANGE"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void auditLogEndpointIsManagerOnly() throws Exception {
        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isForbidden());
    }

    private CommunicationLogEntity log(String documentType, CommunicationStatus status) {
        CommunicationLogEntity entity = new CommunicationLogEntity();
        entity.setDocumentType(documentType);
        entity.setDocumentId(UUID.randomUUID());
        entity.setRecipient("ops@example.com");
        entity.setChannel(CommunicationChannel.EMAIL);
        entity.setStatus(status);
        entity.setSenderUsername("tester");
        entity.setDetails("details");
        return entity;
    }

    private EntityAuditLogEntity audit(String entityType, String action) {
        EntityAuditLogEntity entity = new EntityAuditLogEntity();
        entity.setEntityType(entityType);
        entity.setEntityId(UUID.randomUUID());
        entity.setAction(action);
        entity.setPerformedByUsername("tester");
        entity.setNewValue("{}");
        return entity;
    }
}

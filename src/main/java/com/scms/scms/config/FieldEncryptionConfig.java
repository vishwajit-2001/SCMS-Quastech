package com.scms.scms.config;

import com.scms.scms.security.FieldEncryptionSupport;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FieldEncryptionConfig {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionConfig.class);
    private static final String DEFAULT_SECRET = "SCMS_DEV_ONLY_CHANGE_THIS_SECRET_BEFORE_PRODUCTION";

    @Value("${app.security.field-encryption-secret:" + DEFAULT_SECRET + "}")
    private String fieldEncryptionSecret;

    @PostConstruct
    public void init() {
        FieldEncryptionSupport.configure(fieldEncryptionSecret);
        if (DEFAULT_SECRET.equals(fieldEncryptionSecret)) {
            log.warn("Using default field encryption secret. Set app.security.field-encryption-secret or FIELD_ENCRYPTION_SECRET for production.");
        }
    }
}

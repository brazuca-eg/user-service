package com.beamcard.user.config;

import com.beamcard.user.auth.service.EmailSender;
import com.beamcard.user.email.LogEmailSender;
import com.beamcard.user.email.ResendEmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    @Bean
    @ConditionalOnProperty(name = "beamcard.email.provider", havingValue = "resend")
    public EmailSender resendEmailSender(EmailProperties properties) {
        EmailProperties.Resend resend = properties.resend();
        return new ResendEmailSender(resend.apiKey(), resend.baseUrl(), properties.from());
    }

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender logEmailSender() {
        return new LogEmailSender();
    }
}

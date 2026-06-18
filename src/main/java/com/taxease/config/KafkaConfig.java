package com.taxease.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${taxease.kafka.topics.tax-events:tax-events}")
    private String taxEventsTopic;

    @Value("${taxease.kafka.topics.document-events:document-events}")
    private String documentEventsTopic;

    @Value("${taxease.kafka.topics.insight-events:insight-events}")
    private String insightEventsTopic;

    @Bean
    public NewTopic taxEventsTopic() {
        return TopicBuilder.name(taxEventsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic documentEventsTopic() {
        return TopicBuilder.name(documentEventsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic insightEventsTopic() {
        return TopicBuilder.name(insightEventsTopic).partitions(1).replicas(1).build();
    }
}

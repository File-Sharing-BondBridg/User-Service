package com.file_sharing.user_service.config;

import io.nats.client.Nats;
import io.nats.client.Options;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfig {

    private final NatsProperties natsProperties;

    public NatsConfig(NatsProperties natsProperties) {
        this.natsProperties = natsProperties;
    }

    @Bean
    public io.nats.client.Connection natsConnection() throws Exception {
        Options.Builder builder = new Options.Builder();

        if (natsProperties.getServers() != null && !natsProperties.getServers().isEmpty()) {
            builder.servers(natsProperties.getServers().toArray(new String[0]));
        } else {
            builder.server(Options.DEFAULT_URL); // nats://localhost:4222
        }

        return Nats.connect(builder.build());
    }
}
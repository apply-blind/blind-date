package kr.gravy.blind.configuration.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.kafka")
public record KafkaProperties(
        @NotBlank String bootstrapServers,
        @Valid Consumer consumer,
        @Valid Topic topic
) {
    public record Consumer(@NotBlank String groupId) {
    }


    public record Topic(@Positive int partitions, @Positive int replicas) {
    }
}

package app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lift")
@Data
public class LiftsConfiguration {

    private int secondsPerFloor;

}
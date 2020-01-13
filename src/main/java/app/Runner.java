package app;

import app.config.DefaultConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(DefaultConfiguration.class)
public class Runner {
    public static void main(String[] args) {
        SpringApplication.run(Runner.class);
    }
}

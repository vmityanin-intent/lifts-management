package app.config;

import app.service.LiftEngineService;
import app.service.LiftRequestsDispatchingService;
import app.service.impl.DefaultLiftEngineService;
import app.service.impl.DefaultLiftRequestsDispatchingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultConfiguration {

    @Bean
    public LiftEngineService getLiftEngineService(LiftsConfiguration liftsConfiguration) {
        return new DefaultLiftEngineService(liftsConfiguration);
    }

    @Bean
    public LiftRequestsDispatchingService getLiftRequestsDispatchingService(LiftEngineService liftEngineService) {
        return new DefaultLiftRequestsDispatchingService(liftEngineService);
    }
}

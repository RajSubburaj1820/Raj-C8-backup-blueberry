package io.camunda.blueberry.platform.rule;

import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.platform.rule.RuleActuatorEndpoint.CheckType;
import io.camunda.blueberry.platform.rule.RuleActuatorEndpoint.EndpointCheck;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * One endpoint rule per component: Operate, Tasklist and Zeebe.
 */
@Configuration
public class RuleActuatorEndpointConfig {

    @Bean
    public Rule ruleOperateEndpoint(BlueberryConfig blueberryConfig, RestTemplate restTemplate) {
        return new RuleActuatorEndpoint("Operate", blueberryConfig::getOperateActuatorUrl,
                List.of(new EndpointCheck("backups", CheckType.ACCESS)),
                restTemplate);
    }

    @Bean
    public Rule ruleTasklistEndpoint(BlueberryConfig blueberryConfig, RestTemplate restTemplate) {
        return new RuleActuatorEndpoint("Tasklist", blueberryConfig::getTasklistActuatorUrl,
                List.of(new EndpointCheck("backups", CheckType.ACCESS)),
                restTemplate);
    }

    /**
     * Zeebe: backups (list and start a backup) and exporting (pause and resume the exporters during a backup).
     * Exporting is checked in the /actuator list: calling /actuator/exporting/pause would pause the exporters.
     */
    @Bean
    public Rule ruleZeebeEndpoint(BlueberryConfig blueberryConfig, RestTemplate restTemplate) {
        return new RuleActuatorEndpoint("Zeebe", blueberryConfig::getZeebeActuatorUrl,
                List.of(new EndpointCheck("backups", CheckType.ACCESS),
                        new EndpointCheck("exporting", CheckType.LINK)),
                restTemplate);
    }
}

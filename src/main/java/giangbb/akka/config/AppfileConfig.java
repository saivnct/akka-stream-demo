package giangbb.akka.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@PropertySource(value = {
        "classpath:application-${spring.profiles.active:default}.properties"
})
@Component
@Qualifier("appfileConfig")
public class AppfileConfig {
    @Value("${log4j.configuration}")
    public String log4jConfiguration;
}

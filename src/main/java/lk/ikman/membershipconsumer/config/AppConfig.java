package lk.ikman.membershipconsumer.config;

import lk.ikman.membershipconsumer.helper.RESTCallHelper;
import lk.ikman.membershipconsumer.helper.RabbitMQMessageHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public RESTCallHelper restCallHelper() {
        return new RESTCallHelper();
    }

    @Bean
    public RabbitMQMessageHelper rabbitMQMessageHelper() {
        return new RabbitMQMessageHelper();
    }

}

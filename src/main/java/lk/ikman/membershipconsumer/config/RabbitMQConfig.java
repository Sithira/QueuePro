package lk.ikman.membershipconsumer.config;

import lk.ikman.membershipconsumer.services.RabbitMQConsumer;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtual_host;


    @Value("${membership-consumer.queue}")
    private String queue;

    @Value("${membership-consumer.exchange}")
    private String exchange;

    @Value("${membership-consumer.routing_key}")
    private String routingKey;

    @Bean
    Queue queue() {
        return new Queue(queue, true, false, false);
    }

    @Bean
    CustomExchange exchange() {

        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");

        return new CustomExchange(exchange, "x-delayed-message", true, false, args);
    }

    @Bean
    Binding binding(Queue queue, CustomExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
    }

    @Bean
    public RabbitMQConsumer rabbitMQConsumer() {
        return new RabbitMQConsumer();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ConnectionFactory connectionFactory() {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtual_host);

        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitAmqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setExchange(exchange);
        rabbitTemplate.setRoutingKey(routingKey);
        rabbitTemplate.setDefaultReceiveQueue(queue);

        //rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleMessageListenerContainer membershipQueueConsumer() {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(queue);
        container.setMessageListener(rabbitMQConsumer());

        return container;
    }
}

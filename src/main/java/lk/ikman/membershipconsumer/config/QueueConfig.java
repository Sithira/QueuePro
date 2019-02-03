package lk.ikman.membershipconsumer.config;

import lk.ikman.membershipconsumer.consumers.APIQueueConsumer;
import lk.ikman.membershipconsumer.consumers.SuccessQueueConsumer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
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
public class QueueConfig {

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


    @Value("${membership-consumer.api_queue}")
    private String api_queue;

    @Value("${membership-consumer.success_queue}")
    private String success_queue;

    @Value("${membership-consumer.exchange}")
    private String exchange;

    @Value("${membership-consumer.routing_key}")
    private String routingKey;

    @Value("${membership-consumer.success_routing_key}")
    private String success_routing_key;

    @Bean
    Queue apiQueue() {
        return new Queue(api_queue, true, false, false);
    }

    @Bean
    Queue successQueue() {
        return new Queue(success_queue, true, false, false);
    }

    @Bean
    CustomExchange exchange() {

        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");

        return new CustomExchange(exchange, "x-delayed-message", true, false, args);
    }

    @Bean
    Binding apiBinding(Queue apiQueue, CustomExchange exchange) {
        return BindingBuilder.bind(apiQueue).to(exchange).with(routingKey).noargs();
    }

    @Bean
    Binding successBinding(Queue successQueue, CustomExchange exchange) {
        return BindingBuilder.bind(successQueue).to(exchange).with(success_routing_key).noargs();
    }

    @Bean
    public APIQueueConsumer apiQueueConsumer() {
        return new APIQueueConsumer();
    }

    @Bean
    public SuccessQueueConsumer successQueueConsumer() {
        return new SuccessQueueConsumer();
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
    public RabbitTemplate apiRabbitMQTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = setTemplate(connectionFactory, exchange);
        template.setDefaultReceiveQueue(api_queue);
        template.setRoutingKey(routingKey);
        return template;
    }

    @Bean
    public RabbitTemplate successRabbitMQTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = setTemplate(connectionFactory, exchange);
        template.setDefaultReceiveQueue(success_queue);
        template.setRoutingKey(success_routing_key);
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer apiQueueMessageContainer() {
        SimpleMessageListenerContainer container = buildMessageContainer(api_queue);
        container.setMessageListener(apiQueueConsumer());
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer successQueueMessageContainer() {
        SimpleMessageListenerContainer container = buildMessageContainer(success_queue);
        container.setMessageListener(successQueueConsumer());
        return container;
    }

    /**
     * Build RabbitTemplate with provided params
     *
     * @param factory  Connection for the listener to listen
     * @param exchange Queue exchange
     * @return RabbitTemplate
     */
    private RabbitTemplate setTemplate(ConnectionFactory factory, String exchange) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(factory);

        rabbitTemplate.setExchange(exchange);

        //rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Build SimpleMessageListenerContainer for the apiQueue provided.
     *
     * @param queue Queue name
     * @return SimpleMessageListenerContainer
     */
    private SimpleMessageListenerContainer buildMessageContainer(String queue) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(queue);

        return container;
    }
}

package lk.ikman.membershipconsumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class TestMessagePublisher {

    private static final String QUEUE_NAME = "membership_api_requests_queue";
    private static final String BROKER_URL = "amqp://sithira:sithira@localhost:5672";

    private ConnectionFactory factory = new ConnectionFactory();

    public void sendMessage(String text) throws Exception {
        factory.setUri(BROKER_URL);
        factory.setVirtualHost("/");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.basicPublish("membership_exchange", "membership_key_default", null, text.getBytes());

    }

}

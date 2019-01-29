package lk.ikman.membershipconsumer.services;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.rabbitmq.client.Channel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.charset.StandardCharsets;

public class RabbitMQConsumer implements ChannelAwareMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);

    private static final String X_RETRIES_HEADER = "x-retries";

    @Autowired
    @Qualifier(value = "rabbitAmqpTemplate")
    private RabbitTemplate rabbitTemplate;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        toLogs(message);

        final String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        final JSONObject jsonObject = new JSONObject(payload);

        final HttpResponse<JsonNode> response = makeHttpCall(jsonObject);

        final long DELIVERY_TAG = message.getMessageProperties().getDeliveryTag();

        if (response != null) {

            if (response.getStatus() == 200 && response.getBody() == null) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
                return;
            }

            if (response.getStatus() == 200 || response.getStatus() == 201) {
                channel.basicAck(DELIVERY_TAG, true);
                return;
            }

            int retryCount;

            if (message.getMessageProperties().getHeaders().containsKey(X_RETRIES_HEADER)) {

                retryCount = (int) message.getMessageProperties().getHeaders().get(X_RETRIES_HEADER);

                if (retryCount > 3) {

                    System.out.println("Retry Exceeded....");

                    channel.basicNack(DELIVERY_TAG, true, false);

                } else {

                    message.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, retryCount + 1);

                    message.getMessageProperties().getHeaders().put("x-delay", (2000 * 60));

                    rabbitTemplate.send(attemptToRepair(message));
                }

            } else {

                message.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, 1);

                message.getMessageProperties().getHeaders().put("x-delay", (2000 * 60));

                rabbitTemplate.send(attemptToRepair(message));
            }
        }

    }

    private HttpResponse<JsonNode> makeHttpCall(JSONObject request) {

        final String URL_TO_REST = request.get("url").toString();

        try {

            return Unirest
                    .post(URL_TO_REST)
                    .body(request)
                    .asJson();

        } catch (UnirestException e) {

            e.printStackTrace();

            return null;
        } finally {
            // todo: some logs
        }
    }

    private void toLogs(Message message) {

        final MessageProperties messageProperties = message.getMessageProperties();

        final String body = new String(message.getBody(), StandardCharsets.UTF_8);

        LOGGER.debug("*********** AMQP Message **********");
        LOGGER.debug(" Id          : " + messageProperties.getMessageId());
        LOGGER.debug(" CorrelId    : " + messageProperties.getCorrelationId());
        LOGGER.debug(" Timestamp   : " + messageProperties.getTimestamp());
        LOGGER.debug(" Service     : " + messageProperties.getHeaders().get("service"));
        LOGGER.debug(" Content-Type: " + messageProperties.getContentType());
        LOGGER.debug(" Encoding    : " + messageProperties.getContentEncoding());
        LOGGER.debug(" Message     : " + body);
        LOGGER.debug("*************** End ***************");

    }

    private Message attemptToRepair(Message failedMessage) {
        String messageBody = new String(failedMessage.getBody());

        return MessageBuilder.withBody(messageBody.getBytes()).copyHeaders(failedMessage.getMessageProperties().getHeaders()).build();
    }
}

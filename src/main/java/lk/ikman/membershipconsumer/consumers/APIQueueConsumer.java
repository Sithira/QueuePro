package lk.ikman.membershipconsumer.consumers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.rabbitmq.client.Channel;
import lk.ikman.membershipconsumer.helper.RESTCallHelper;
import lk.ikman.membershipconsumer.helper.RabbitMQMessageHelper;
import lk.ikman.membershipconsumer.models.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;

public class APIQueueConsumer extends BaseConsumer implements ChannelAwareMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIQueueConsumer.class);

    @Autowired
    private RESTCallHelper restCallHelper;

    @Autowired
    private RabbitMQMessageHelper rabbitMQMessageHelper;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        rabbitMQMessageHelper.toLogs(message, false, LOGGER);

        final Request request = rabbitMQMessageHelper.toRequest(new String(message.getBody()));

        final HttpResponse<JsonNode> response = restCallHelper.makeHttpCall(request);

        final long DELIVERY_TAG = message.getMessageProperties().getDeliveryTag();

        if (response != null) {

            if (isSuccess(message, response, LOGGER)) {
                LOGGER.info("Message with delivery TAG : " + DELIVERY_TAG + " is successfully acknowledged");
                return;
            }

            handleFailed(message, channel, true, LOGGER);
        }

    }
}

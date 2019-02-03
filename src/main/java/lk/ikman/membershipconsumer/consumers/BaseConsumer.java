package lk.ikman.membershipconsumer.consumers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.rabbitmq.client.Channel;
import lk.ikman.membershipconsumer.helper.RESTCallHelper;
import lk.ikman.membershipconsumer.helper.RabbitMQMessageHelper;
import lk.ikman.membershipconsumer.models.Request;
import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

class BaseConsumer {

    protected static final String X_RETRIES_HEADER = "x-retries";
    @Autowired
    @Qualifier(value = "apiRabbitMQTemplate")
    private RabbitTemplate rabbitTemplate;
    @Autowired
    @Qualifier(value = "successRabbitMQTemplate")
    private RabbitTemplate rabbitTemplateSuccess;
    @Autowired
    private RESTCallHelper restCallHelper;
    @Autowired
    private RabbitMQMessageHelper rabbitMQMessageHelper;

    protected boolean isSuccess(Message message, HttpResponse<JsonNode> response, Logger LOGGER) {
        if (response.getStatus() == 200 && response.getBody() != null) {
            //channel.basicAck(DELIVERY_TAG, false);

            successConsume(message, response, LOGGER);
            return true;
        }

        if ((response.getStatus() == 200 || response.getStatus() == 201) && response.getBody() != null) {
            //channel.basicAck(DELIVERY_TAG, false);

            successConsume(message, response, LOGGER);
            return true;
        }

        return false;
    }

    protected void handleFailed(Message message, Channel channel, boolean requeue, Logger LOGGER)
            throws IOException {

        long DELIVERY_TAG = message.getMessageProperties().getDeliveryTag();

        int retryCount;

        if (message.getMessageProperties().getHeaders().containsKey(X_RETRIES_HEADER)) {

            retryCount = (int) message.getMessageProperties().getHeaders().get(X_RETRIES_HEADER);

            if (retryCount > 3) {

                LOGGER.debug("Retrying exceeded (count exceeded)");

                channel.basicNack(DELIVERY_TAG, true, false);

            } else {

                message.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, retryCount + 1);

                requeueOrNack(message, channel, requeue, DELIVERY_TAG);
            }

        } else {

            message.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, 1);

            requeueOrNack(message, channel, true, DELIVERY_TAG);
        }

    }

    /**
     * Requeue or Nacks based on the condition
     *
     * @param message      Message
     * @param channel      Channel
     * @param requeue      condition to requeue or not requeue
     * @param DELIVERY_TAG Delivery TAG
     * @throws IOException if fails to do an IO Operation on MQ
     */
    private void requeueOrNack(Message message, Channel channel, boolean requeue, long DELIVERY_TAG) throws IOException {
        message.getMessageProperties().getHeaders().put("x-delay", (2000 * 60));

        if (requeue) {
            rabbitTemplate.send(rabbitMQMessageHelper.rebuildMessage(message));
        } else {
            channel.basicNack(DELIVERY_TAG, true, false);
        }
    }


    protected HttpResponse<JsonNode> getResponse(Request request) throws IOException {

        return restCallHelper.makeHttpCall(request);

    }

    /**
     * When an API call is successfully made, append the response and push to success queue.
     *
     * @param message  Message from queue
     * @param response Response from API
     */
    private void successConsume(Message message, HttpResponse<JsonNode> response, Logger LOGGER) {
        final Message msg = rabbitMQMessageHelper
                .rebuildMessageAsNew(rabbitMQMessageHelper.addResponseToMessageBody(message, response.getBody()));

        rabbitTemplateSuccess.send(msg);

        rabbitMQMessageHelper.toLogs(msg, true, LOGGER);
    }

}

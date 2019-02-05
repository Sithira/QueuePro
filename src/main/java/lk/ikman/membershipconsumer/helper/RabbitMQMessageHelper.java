package lk.ikman.membershipconsumer.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.JsonNode;
import lk.ikman.membershipconsumer.models.Request;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitMQMessageHelper {

    /**
     * Add the message to the Log files.
     *
     * @param message     Message
     * @param hasResponse has sent HTTP Requests
     * @param LOGGER for logging purposes
     */
    public void toLogs(Message message, boolean hasResponse, Logger LOGGER) {

        final MessageProperties messageProperties = message.getMessageProperties();

        final String body = new String(message.getBody(), StandardCharsets.UTF_8);

        LOGGER.debug("*********** AMQP Message **********");
        LOGGER.debug(" Id          : " + messageProperties.getMessageId());
        LOGGER.debug(" CorrelId    : " + messageProperties.getCorrelationId());
        LOGGER.debug(" Timestamp   : " + messageProperties.getTimestamp());
        LOGGER.debug(" Service     : " + messageProperties.getHeaders().get("service"));
        LOGGER.debug(" Content-Type: " + messageProperties.getContentType());

        // if has response, add the response to logs too.
        if (hasResponse) {
            JSONObject response = new JSONObject(new String(message.getBody(), StandardCharsets.UTF_8));

            LOGGER.debug(" Response: " + response.get("response"));
        }

        LOGGER.debug(" Encoding    : " + messageProperties.getContentEncoding());
        LOGGER.debug(" Message     : " + body);
        LOGGER.debug("*************** End ***************");

    }


    /**
     * Rebuild an already existing message.
     *
     * @param failedMessage Message
     * @return Message
     */
    public Message rebuildMessage(Message failedMessage) {
        String messageBody = new String(failedMessage.getBody());

        return MessageBuilder.withBody(messageBody.getBytes()).copyHeaders(failedMessage.getMessageProperties().getHeaders()).build();
    }

    /**
     * This will recreate a already existing message as a new message
     * to requeue and set the content type as JSON.
     * <p>
     * most likely it(message) has failed in the first attempt.
     *
     * @param successMessage Message RabbitMQ
     * @return Message
     */
    public Message rebuildMessageAsNew(Message successMessage) {

        return MessageBuilder
                .withBody(successMessage.getBody())
                .setHeader("content_type", "application/json")
                .build();
    }

    /**
     * When a message is provided, as well as a jsonNode (a response)
     * it will add the response to the message body.
     * <p>
     * This message will be used only to push a message to a success queue.
     * </p>
     *
     * @param message  Message RabbitMQ
     * @param response JsonNode as the response
     * @return Message
     */
    public Message addResponseToMessageBody(Message message, JsonNode response) {
        JSONObject request = new JSONObject(new String(message.getBody(), StandardCharsets.UTF_8));

        request.put("response", response.toString());

        return MessageBuilder
                .withBody(request.toString().getBytes())
                .build();
    }

    /**
     * Convert the request into a POJO.
     *
     * @param message String message
     * @return Request
     * @throws IOException if an error in the data string.
     */
    public Request toRequest(String message) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        return mapper.readValue(message, Request.class);
    }

}

package lk.ikman.membershipconsumer.services;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.rabbitmq.client.Channel;
import org.json.JSONException;
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
    @Qualifier(value = "defaultRabbitMQTemplate")
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier(value = "successRabbitMQTemplate")
    private RabbitTemplate rabbitTemplateSuccess;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        toLogs(message, false);

        final String payload = new String(message.getBody());

        final HttpResponse<JsonNode> response = makeHttpCall(payload);

        final long DELIVERY_TAG = message.getMessageProperties().getDeliveryTag();

        if (response != null) {

            if (response.getStatus() == 200 && response.getBody() != null) {
                //channel.basicAck(DELIVERY_TAG, false);

                final Message msg = rebuildMessageAsNew(addResponseToMessageBody(message, response.getBody()));

                rabbitTemplateSuccess.send(msg);

                toLogs(msg, true);

                return;
            }

            if ((response.getStatus() == 200 || response.getStatus() == 201) && response.getBody() != null) {
                //channel.basicAck(DELIVERY_TAG, false);

                final Message msg = rebuildMessageAsNew(addResponseToMessageBody(message, response.getBody()));

                rabbitTemplateSuccess.send(msg);

                toLogs(msg, true);

                return;
            }

            int retryCount;

            if (message.getMessageProperties().getHeaders().containsKey(X_RETRIES_HEADER)) {

                retryCount = (int) message.getMessageProperties().getHeaders().get(X_RETRIES_HEADER);

                if (retryCount > 3) {

                    LOGGER.debug("Retrying exceeded (count exceeded)");

                    channel.basicNack(DELIVERY_TAG, true, false);

                } else {

                    message.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, retryCount + 1);

                    message.getMessageProperties().getHeaders().put("x-delay", (2000 * 60));

                    rabbitTemplate.send(rebuildMessage(message));
                }

            } else {

                message.getMessageProperties().getHeaders().put(X_RETRIES_HEADER, 1);

                message.getMessageProperties().getHeaders().put("x-delay", (2000 * 60));

                rabbitTemplate.send(rebuildMessage(message));
            }
        }

    }

    /**
     * Make the HTTP Call for the decoded URL from the payload.
     *
     * @param payload String
     * @return HttpResponse<JsonNode>
     */
    private HttpResponse<JsonNode> makeHttpCall(String payload) {

        try {
            // convert the string to json
            final JSONObject request = new JSONObject(payload);

            // get the callable url from the object
            final String URL_TO_REST = request.get("url").toString();

            final JSONObject request_body = request.getJSONObject("body");

            final String method = request.getString("method");

            String authentication = null;

            // check and set the authentication from message payload.
            if (request.has("authentication")) {
                authentication = request.getString("authentication");
            }

            try {

                HttpRequestWithBody unirest;

                // find the method that we need to make the rest call for.
                switch (method.toLowerCase()) {

                    case "patch":
                        unirest = Unirest.patch(URL_TO_REST);
                        break;

                    case "put":
                        unirest = Unirest.put(URL_TO_REST);
                        break;

                    default:
                    case "post":
                        unirest = Unirest.post(URL_TO_REST);
                        break;

                }

                // set the auth key for the requests
                unirest = setBasicAuthentication(authentication, unirest);

                // fire the REST call
                unirest.body(request_body).asJson();

            } catch (UnirestException e) {

                LOGGER.debug(" Http call exception cause: " + e.getCause().toString());

                return null;
            }
        } catch (JSONException exp) {
            exp.printStackTrace();
        }

        return null;
    }

    /**
     * Set authentication to the request if the payload has set an authentication key.
     *
     * @param authentication Authentication token
     * @param unirest        Unirest Instance
     * @return HttpRequestWithBody
     */
    private HttpRequestWithBody setBasicAuthentication(String authentication, HttpRequestWithBody unirest) {

        if (authentication != null) {
            String[] auth = authentication.split(":");

            String username = auth[0];

            String password = auth[1];

            unirest.basicAuth(username, password);
        }

        return unirest;
    }

    /**
     * Add the message to the Log files.
     *
     * @param message     Message
     * @param hasResponse has sent HTTP Requests
     */
    private void toLogs(Message message, boolean hasResponse) {

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
    private Message rebuildMessage(Message failedMessage) {
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
    private Message rebuildMessageAsNew(Message successMessage) {

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
    private Message addResponseToMessageBody(Message message, JsonNode response) {
        JSONObject request = new JSONObject(new String(message.getBody(), StandardCharsets.UTF_8));

        request.put("response", response.toString());

        return MessageBuilder
                .withBody(request.toString().getBytes())
                .build();
    }
}

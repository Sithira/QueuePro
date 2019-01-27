package lk.ikman.membershipconsumer.services;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

public class RabbitMQConsumer implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @Override
    public void onMessage(Message message) {

        toLogs(message);

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            JSONObject jsonObject = new JSONObject(payload);

            HttpResponse<JsonNode> response = makeHttpCall(jsonObject);

            if (response != null)
            {
                if (response.getStatus() == 200 && response.getBody() == null) {

                }
            }

            System.out.println(payload);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

    }

    private HttpResponse<JsonNode> makeHttpCall(JSONObject request) {
        try {

            return Unirest
                    .post("http://localhost:1025/test")
                    .body(request)
                    .asJson();

        } catch (UnirestException e) {

            e.printStackTrace();

            // todo: do some logs

            return null;
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
}

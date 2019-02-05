package lk.ikman.membershipconsumer.helper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import lk.ikman.membershipconsumer.models.Request;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RESTCallHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RESTCallHelper.class);

    /**
     * Make the HTTP Call for the decoded URL from the payload.
     *
     * @param request Request
     * @return JSON Response as a string
     */
    public HttpResponse<JsonNode> makeHttpCall(Request request) {
        try {

            String authentication = null;

            // check and set the authentication from message payload.
            if (request.getAuthentication() != null) {
                authentication = request.getAuthentication();
            }

            try {

                HttpRequestWithBody unirest;

                // find the method that we need to make the rest call for.
                switch (request.getMethod().toLowerCase()) {

                    case "patch":
                        unirest = Unirest.patch(request.getUrl());
                        break;

                    case "put":
                        unirest = Unirest.put(request.getUrl());
                        break;

                    default:
                    case "post":
                        unirest = Unirest.post(request.getUrl());
                        break;

                }

                // set the auth key for the requests
                unirest = setBasicAuthentication(authentication, unirest);

                // fire the REST call
                unirest.body(request.getBody().toString());

                return unirest.asJson();

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
}

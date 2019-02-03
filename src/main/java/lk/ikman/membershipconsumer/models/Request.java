package lk.ikman.membershipconsumer.models;

import com.fasterxml.jackson.databind.JsonNode;

public class Request {

    private String url;

    private JsonNode body;

    private String method;

    private JsonNode response;

    private String authentication;

    private Request callback_data;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JsonNode getBody() {
        return body;
    }

    public void setBody(JsonNode body) {
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }


    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }

    public Request getCallback_data() {
        return callback_data;
    }

    public void setCallback_data(Request callback_data) {
        this.callback_data = callback_data;
    }
}

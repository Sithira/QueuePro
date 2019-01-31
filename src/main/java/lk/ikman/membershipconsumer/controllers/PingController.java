package lk.ikman.membershipconsumer.controllers;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @RequestMapping(method = RequestMethod.GET, value = "/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok().body("pong");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/test")
    public ResponseEntity test(@RequestBody String body) {

        JSONObject fakeResponse = new JSONObject(body);

        if (fakeResponse.has("body")) {
            System.out.println("ACKE'D FROM CONTROLLER");
        }

        fakeResponse.put("status", "OK");

        return ResponseEntity.ok(fakeResponse.toString());
    }

}

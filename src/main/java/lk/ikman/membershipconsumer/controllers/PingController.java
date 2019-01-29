package lk.ikman.membershipconsumer.controllers;

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

        System.out.println("ACKED FROM CONTROLLER");

        return ResponseEntity.ok(body);
    }

}

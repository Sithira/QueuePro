### S-QUEUE Pro

QueuePro basically helps you to call all the API calls to external services through this and retries if fails.

#### How QueuePro works ?

QueuePro basically acts like a broker between your application and external webservices. when a request a come to the
default queue it will call the web services with the data provided in the payload. if the response we get is not a 200 
or an empty body, QueuePro will retry the request in 2 minutes. This process will happen for maxium 5 times for period 
of 10 mins.

When a external call receives a success response, QueuePro will immediately send the response and payload combined to the 
success queue, so the consumers of that queue will be able to consume them. QueuePro will also will listen for the success 
queues calls, thus handling error responses as same as the default queue.

QueuePro will give up retrying after 5th time and Negatively acknowledges to the respective queue. 

#### Payload structure
For QueuePro to work, you need a basic JSON payload structure as mentioned below.
```json
{  
   "url":"http://example.com/test",
   "body":{  
      "some_key":"some-value",
      "some_other_key":true,
      "authentication": "username:password"
   },
   "method": "POST",
   "callback_data": {
        "url": "http://example.com/test-url",
        "body":{  
          "action": "done"
        },
        "method": "POST"
   }
}
```

#### Authenticated resources
QueuePro supports basic authentication for now.
```json
{
"authentication":"username:password"
}
```
As mentioned above you must have the authentication field filled with username and password separated by a ":", QueuePro
will handle the rest for you.

#### How to add a new queue to the QueuePro

All the configurations for the QP are stored in application.yml.
These are the default configs for the QP beside default Spring configs.
```yaml
membership-consumer:
  api_queue: "membership_api_requests_queue"
  success_queue: "membership_api_requests_success_queue"
  exchange: "membership_exchange"
  routing_key: "membership_key_default"
  success_routing_key: "membership_key_success"
  x_delay: 2000
```

#### Creating a new consumer
Creating a consumer is not as hard as it sounds. below are the steps to create you own queue consumer.

* Create a new JAVA class under the membershipconsumer.consumers package
* Extend it from BaseConsumer and implement it from `ChannelAwareMessageListener` (you can implement any message listener)
* You can use `isSuccess()` method to check the successes of the external API call and `handleFailed()` method to handle 
the failed fails. These methods are already implemented, but if you want to use your own logic you are free to override 
them. 

```java
class MyNewConsumer extends BaseConsumer implements ChannelAwareMessageListener {
    
        @Autowired
        private RESTCallHelper restCallHelper;
    
        @Autowired
        private RabbitMQMessageHelper rabbitMQMessageHelper;
        
        if (isSuccess()) {
            // your success logic
        } else {
            handleFailed();
        }
    
}
```


#### About BaseConsumer

BaseConsumer is the parent class that handles all the queue logic and letting us to separate queue logic.
You can use `isSuccess()` method to check the successes of the external API call and `handleFailed()` method to handle 
the failed fails. These methods are already implemented, but if you want to use your own logic you are free to override 
them. 

#### Testing
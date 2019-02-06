package lk.ikman.membershipconsumer;

import lk.ikman.membershipconsumer.config.AppConfig;
import lk.ikman.membershipconsumer.config.QueueConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = {AppConfig.class, QueueConfig.class})
@EnableConfigurationProperties
public class MembershipConsumerApplicationTests {

    @Test
    public void publishesMessage() throws Exception {

        TestMessagePublisher testMessagePublisher = new TestMessagePublisher();

        testMessagePublisher.sendMessage("{\n" +
                "   \"url\":\"http://example.com/test\",\n" +
                "   \"body\":{  \n" +
                "      \"some_key\":\"some-value\",\n" +
                "      \"some_other_key\":true,\n" +
                "      \"authentication\": \"username:password\"\n" +
                "   },\n" +
                "   \"method\": \"POST\",\n" +
                "   \"callback_data\": {\n" +
                "        \"url\": \"http://example.com/test-url\",\n" +
                "        \"body\":{  \n" +
                "          \"action\": \"done\"\n" +
                "        },\n" +
                "        \"method\": \"POST\"\n" +
                "   }\n" +
                "}");

    }

}


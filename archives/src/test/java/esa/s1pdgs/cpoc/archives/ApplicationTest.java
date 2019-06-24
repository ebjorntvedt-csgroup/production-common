package esa.s1pdgs.cpoc.archives;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@Ignore
public class ApplicationTest {

    /**
     * Embedded Kafka
     */
    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true,
            "t-pdgs-l0-slices-nrt", 
            "t-pdgs-l0-segments");

    @Test
    public void applicationContextTest() {
        Application.main(new String[] {});
    }
}

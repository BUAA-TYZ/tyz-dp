import com.example.DpAppApplication;
import com.example.rocketmq.message.SeckillVoucherMessage;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "rocketmq.name-server=localhost:9876",
        "rocketmq.producer.group=tyz-producer",
})
public class RocketMqTest {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Test
    public void testSend() {
        SeckillVoucherMessage seckillVoucherMessage = new SeckillVoucherMessage();
        seckillVoucherMessage.setUserId(1L);
        seckillVoucherMessage.setVoucherId(1L);
        seckillVoucherMessage.setBeforeQty(100);
        seckillVoucherMessage.setChangeQty(1);
        seckillVoucherMessage.setAfterQty(99);
        rocketMQTemplate.convertAndSend("test-topic", seckillVoucherMessage);
    }
}

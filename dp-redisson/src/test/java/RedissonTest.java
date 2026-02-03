import com.example.config.RedissonAutoConfiguration;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = RedissonAutoConfiguration.class,
        properties = {
                "spring.data.redis.host=localhost",
                "spring.data.redis.port=6379",
                "spring.data.redis.database=0",
                "spring.data.redis.password=123456"
        })
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    public void testBloomFilter() {
        String bfKey = "test-bloom-filter";

        RBloomFilter<Object> bf = redissonClient.getBloomFilter(bfKey);
        boolean delete = bf.delete();
        log.info("bloom filter delete success {}", delete);

        // 预估：比如你系统里最多 1 千张券
        long expectedInsertions = 1_000L;
        // 误判率：万分之一/千分之一 看你业务容忍度
        double falsePositiveRate = 1e-4;

        // tryInit：仅当底层结构不存在时初始化；已存在就不会破坏
        bf.tryInit(expectedInsertions, falsePositiveRate);

        for (long i = 1; i <= 300; i++) {
            bf.add(i);
        }
        assertTrue(bf.contains(42L));
        assertFalse(bf.contains(623L));
    }
}

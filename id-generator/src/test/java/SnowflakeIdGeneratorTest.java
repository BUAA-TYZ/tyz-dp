import com.example.config.IdGeneratorAutoConfig;
import com.example.impl.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = IdGeneratorAutoConfig.class,
properties = {
        "id.snowflake.data-center-id=1",
        "id.snowflake.worker-id=2"
})
class SnowflakeIdGeneratorTest {

    // 你在生成器里用的 epochMillis（保持一致即可）
    private static final long EPOCH = 1704038400000L;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Test
    void shouldGenerateUniqueAndIncreasingInSingleThread() {
        long prev = snowflakeIdGenerator.nextId();

        for (int i = 0; i < 10_000; i++) {
            long id = snowflakeIdGenerator.nextId();
            System.out.println(prev + " " + id);
            assertTrue(id > prev, "ID should be strictly increasing in single thread");
            prev = id;
        }
    }

    @Test
    void shouldGenerateNoDuplicateUnderConcurrency() throws Exception {
        int threads = 8;
        int perThread = 10_000; // 总 8w 个 ID，够测且不太慢
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        // 用 ConcurrentHashMap 做并发去重
        Set<Long> ids = ConcurrentHashMap.newKeySet(threads * perThread);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        ids.add(snowflakeIdGenerator.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Interrupted");
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Timeout waiting workers");
        pool.shutdownNow();

        assertEquals(threads * perThread, ids.size(), "Should not have duplicates");
    }

    @Test
    void shouldEncodeWorkerAndDatacenterBitsCorrectly() {
        // 选一个好验的值
        long datacenterId = 3;
        long workerId = 17;
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(datacenterId, workerId, EPOCH);

        long id = gen.nextId();

        // 按经典 41-5-5-12 布局解码（与你生成器的 shift 一致）
        long seq = id & ((1L << 12) - 1);                // 低 12 位
        long decodedWorker = (id >> 12) & 31L;           // 接着 5 位
        long decodedDc = (id >> (12 + 5)) & 31L;         // 再 5 位

        assertTrue(seq >= 0 && seq <= 4095);
        assertEquals(workerId, decodedWorker);
        assertEquals(datacenterId, decodedDc);
    }
}
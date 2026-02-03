package com.example.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.entity.Shop;
import com.example.impl.SnowflakeIdGenerator;
import com.example.mapper.ShopMapper;
import com.example.service.IShopService;
import com.example.utils.RedisConstants;
import com.example.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RBloomFilter<Long> shopBloomFilter;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result<Long> saveShop(Shop shop) {
        long id = snowflakeIdGenerator.nextId();
        shop.setId(id);
        save(shop);
        // 放入布隆过滤器
        shopBloomFilter.add(id);
        return Result.ok(id);
    }

    @Override
    public Result<Shop> queryById(Long id) {
        // 1. 检查是否在布隆过滤器中
        if (!shopBloomFilter.contains(id)) {
            log.info("查询商铺 布隆过滤器判断不存在 商铺id : {}", id);
            return Result.fail("店铺不存在");
        }

        // 2. 如果在则查询缓存
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        // 3. 缓存存在则返回
        if (StrUtil.isNotBlank(shopJson)) {
            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
        }

        // 4. 不存在则拿分布式锁去数据库进行查询
        Shop shop;
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_SHOP_KEY + id);
        try {
            lock.lock();

            shopJson = stringRedisTemplate.opsForValue().get(shopKey);
            // 5. 缓存此时可能被别的线程填充好
            if (StrUtil.isNotBlank(shopJson)) {
                return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
            }

            // 6. 否则为唯一的查询线程
            shop = getById(id);
            if (shop == null) {
                return Result.fail("店铺不存在");
            }
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Result update(Shop shop) {
        // 采取先更新数据库，再删除缓存的方式
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        //TODO 先改成 x 和 y 都是空
        x = null;
        y = null;
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        return null;
    }
}

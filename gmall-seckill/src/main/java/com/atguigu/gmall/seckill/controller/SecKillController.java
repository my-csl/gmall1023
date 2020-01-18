package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    /***
     * 先到先得式秒杀
     * @return
     */
    @RequestMapping("secKill")
    @ResponseBody
    public String secKill() {

        Jedis jedis = redisUtil.getJedis();


        RSemaphore semaphore = redissonClient.getSemaphore("106");
        boolean b = semaphore.tryAcquire();

        if (b) {
            int stock = Integer.parseInt(jedis.get("106"));
            System.out.println("当前剩余数量为" + stock + "，某用户抢购成功，当前抢购人数为：" + (1000 - stock));
        } else {
            int stock = Integer.parseInt(jedis.get("106"));
            System.out.println("当前剩余数量为" + stock + "，某用户抢购失败");
        }

        jedis.close();
        return "1";
    }

    /***
     * 随机拼运气式秒杀
     * @return
     */
    @RequestMapping("kill")
    @ResponseBody
    public String kill() {

        String memberId = "1";

        Jedis jedis = redisUtil.getJedis();

        //开启商品监控
        jedis.watch("106");
        int stock = Integer.parseInt(jedis.get("106"));

        if (stock > 0) {
            //开启事务
            Transaction multi = jedis.multi();
            multi.incrBy("106", -1);
            List<Object> exec = multi.exec();
            if (exec != null && exec.size() > 0) {
                System.out.println("当前剩余数量为" + stock + "，某用户抢购成功，当前抢购人数为：" + (1000 - stock));
            } else {
                System.out.println("当前剩余数量为" + stock + "，某用户抢购失败");
            }


        }

        jedis.close();

        return "1";
    }
}

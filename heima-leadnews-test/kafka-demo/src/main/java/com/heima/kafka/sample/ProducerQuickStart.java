package com.heima.kafka.sample;

import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProducerQuickStart {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //1.kafka的配置信息

        Properties properties = new Properties();
        //kafka的连接地址
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.200.130:9092");

        //消息key的序列化器
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //消息value的序列化器
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        // 生产者消息确认机制
        properties.put(ProducerConfig.ACKS_CONFIG, "1");

        // 重试次数
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);

        // 压缩类型
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        //2.生产者对象
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        //3.发送消息
        /**
         * 第一个参数 topic
         * 第二个参数 消息的key
         * 第三个参数 消息的value
         */
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>("topic-first", "topic-first", "hi,kafka");
        // 同步发送消息
//        RecordMetadata recordMetadata = producer.send(producerRecord).get();
//        System.out.println(recordMetadata.offset());

        // 异步发送消息
        producer.send(producerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if (e != null) {
                    System.out.println("记录异常消息到日志表中");
                }

                System.out.println(recordMetadata.offset());
            }
        });

        //4.关闭消息通道，必须关闭，否则消息发送不成功
        producer.close();
    }
}

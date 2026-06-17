package com.example.infra.idempotency;

/**
 * 幂等结果序列化接口。
 *
 * 默认实现建议用 Jackson ObjectMapper。
 * 如果项目用其他序列化框架（Gson、Protobuf），实现此接口即可替换。
 *
 * 使用方式：
 *   @Configuration
 *   public class IdempotencyConfig {
 *       @Bean
 *       public IdempotencyResultSerializer idempotencyResultSerializer(ObjectMapper mapper) {
 *           return new JacksonIdempotencyResultSerializer(mapper);
 *       }
 *   }
 */
public interface IdempotencyResultSerializer {

    /** 将结果对象序列化为 JSON 字符串 */
    String serialize(Object result);

    /** 从 JSON 字符串反序列化为指定类型 */
    <T> T deserialize(String json, Class<T> type);
}

package com.heima.wemedia;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.heima.apis.article.fallback") // 扫描熔断降级的包
public class InitConfig {
}

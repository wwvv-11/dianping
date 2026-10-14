package com.dianping.config;

import com.dianping.Interceptor.LoginInterceptor;
import com.dianping.Interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 刷新token拦截器，拦截所有请求（order=0，先执行）
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);

        // 登录拦截器，排除不需要登录的路径（order=1，后执行）
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",        // 发送验证码
                        "/user/login",       // 登录
                        "/blog/hot",         // 热门探店
                        "/shop/**",          // 店铺查询
                        "/shop-type/**",     // 店铺类型
                        "/upload/**",        // 文件上传
                        "/voucher/**"        // 优惠券查询
                )
                .order(1);
    }
}

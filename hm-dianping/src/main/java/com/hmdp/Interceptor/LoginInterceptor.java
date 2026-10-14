package com.hmdp.Interceptor;

import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * 登录拦截器
 * 判断ThreadLocal中是否有用户，没有则拦截
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //判断是否需要拦截（threadlocal中是否有用户
        if(UserHolder.getUser()==null){
            response.setStatus(401);
            return false;
        }


        return true;
    }
}

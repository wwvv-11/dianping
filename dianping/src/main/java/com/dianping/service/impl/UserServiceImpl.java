package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IUserService;
import com.dianping.utils.RegexUtils;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.dianping.utils.SystemConstants;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号

        //不符合  返回错误信息
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }



        //符合  生成验证码
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute("code",code);

        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.info("发送验证码成功，code={}", code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        String phone=loginForm.getPhone();
        //校验手机号
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式错误");
        }

        //校验验证码   TODO 从redis中获取验证码并校验
       Object cacheCode= stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code=loginForm.getCode();

        if(cacheCode==null || !cacheCode.equals(code)){
            //不一致  报错
            return Result.fail("验证码错误");
        }




        //一致  根据手机号查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();


        //判断用户是否存在
        if (user == null) {
            //不存在  创建新用户并保存
            user=createUserWithPhone(loginForm.getPhone());
        }
        //存在  保存用户信息到redis
        //随机生成token  作为登录令牌
        //将user转为hash存储
        //存储
        String token = UUID.randomUUID().toString().replace("-", "");

        UserDTO userDTO=new UserDTO();
        BeanUtils.copyProperties(user,userDTO);

        Map<String,Object> userMap= BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));

        String tokenKey=LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);

        //设置token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //获取当前登录用户
        Long userId= UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        //拼接key
        String key=USER_SIGN_KEY+userId+keySuffix;
        //获取今日是本月的第几天
        int dayOfMonth=now.getDayOfMonth();
        //写入redis  setbit key offset  1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取本月截至今天为止所有的签到记录
        Long userId=UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        //拼接key
        String key=USER_SIGN_KEY+userId+keySuffix;
        //获取今日是本月的第几天
        int dayOfMonth=now.getDayOfMonth();
        //获取本月截至今天位置所有的签到记录  返回的是十进制数字
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );

        if(result==null||result.isEmpty()){
            return Result.ok(0);
        }
        Long num=result.get(0);
        if(num==null||num==0){
            return Result.ok(0);
        }

        //循环遍历
        int count=0;
       while (true){
           //让这个数字与1  做与运算 得到数字的数字最后一个bit位
           //判断这个bit位是否为0
            if((num&1)==0){
                //若为0 则未签到
                break;
            }else {
                //不为0 已签到  计数器+1
                count++;
            }
           //把数字右移一位  抛弃最后一位
           num>>>=1;
       }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));


        save(user);
        return user;
    }



}

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.CommentDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import com.hmdp.vo.commentsVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Resource
    private IUserService userService;
    @Override
    public Result saveComment(CommentDTO commentDTO) {
        //content非空校验
        if(StrUtil.isBlank(commentDTO.getContent())){
            return Result.fail("当前评论为空");
        }
        Long userId= UserHolder.getUser().getId();
        BlogComments blogComments=new BlogComments();
        blogComments.setUserId(userId);
        blogComments.setBlogId(commentDTO.getBlogId());
        blogComments.setParentId(commentDTO.getParentId()==null?0L: commentDTO.getParentId());
        blogComments.setAnswerId(commentDTO.getAnswerId()==null?0L: commentDTO.getAnswerId());
        blogComments.setContent(commentDTO.getContent());
        save(blogComments);

        return Result.ok();
    }

    @Override
    public List<commentsVO> queryByBlogId(Long blogId) {
        // 1. 查该 blog  所有评论
        List<BlogComments> all=query().eq("blog_id",blogId)
                .orderByDesc("create_time")
                .list();
        // 2. 收集所有相关 userId (评论者 + 被回复评论的作者)
        Set<Long> userIds = all.stream().map((a) -> {
            return a.getUserId();
        }).collect(Collectors.toSet());

        // 3. 批量查 user → Map<Long, UserDTO>

        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        // 4. 把评论按 parentId 分组 (一级 + 各一级下的回复)

        //一级评论 ： parentId=0
        List<BlogComments> tops = all.stream()
                .filter((a) -> {
                    return a.getParentId() == 0L;
                }).collect(Collectors.toList());

        //子回复
        Map<Long, List<BlogComments>> reply = all.stream()
                .filter((a) -> {
                    return a.getParentId() != 0L;
                }).collect(Collectors.groupingBy(BlogComments::getParentId));

        Map<Long, BlogComments> commentMap = all.stream()
                .collect(Collectors.toMap(BlogComments::getId, c -> c));

        // 5. 遍历组装 CommentVO, 塞 replies
        List<commentsVO> result = new ArrayList<>();

        for (BlogComments top : tops) {
            commentsVO vo = toCommentVO(top, userMap, commentMap);
            List<commentsVO> childVOs = new ArrayList<>();
            for (BlogComments child : reply.getOrDefault(top.getId(), new ArrayList<>())) {
                childVOs.add(toCommentVO(child, userMap, commentMap));
            }
            vo.setReplies(childVOs);
            result.add(vo);
        }

        // 6. return
        return result;
    }

    private commentsVO toCommentVO(BlogComments c, Map<Long, User> userMap, Map<Long, BlogComments> commentMap) {
        commentsVO vo = BeanUtil.copyProperties(c, commentsVO.class);
        User user = userMap.get(c.getUserId());
        if (user != null) {
            vo.setNickName(user.getNickName());
            vo.setIcon(user.getIcon());
        }
        Long answerId = c.getAnswerId();
        if (answerId != null && answerId != 0L) {
            BlogComments answered = commentMap.get(answerId);
            if (answered != null) {
                User answerUser = userMap.get(answered.getUserId());
                if (answerUser != null) {
                    vo.setAnswerNickName(answerUser.getNickName());
                }
            }
        }
        return vo;
    }
}

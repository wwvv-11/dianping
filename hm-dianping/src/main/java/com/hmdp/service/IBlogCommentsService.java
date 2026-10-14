package com.hmdp.service;

import com.hmdp.dto.CommentDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.vo.commentsVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService extends IService<BlogComments> {

    Result saveComment(CommentDTO commentDTO);

    List<commentsVO> queryByBlogId(Long blogId);
}

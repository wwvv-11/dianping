package com.dianping.service;

import com.dianping.dto.CommentDTO;
import com.dianping.dto.Result;
import com.dianping.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.vo.commentsVO;

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

package com.hmdp.controller;


import com.hmdp.dto.CommentDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {
    @Resource
    private IBlogCommentsService blogCommentsService;

    @PostMapping
    public Result saveComment(@RequestBody CommentDTO commentDTO){
        return blogCommentsService.saveComment(commentDTO);

    }

    @GetMapping("/of/blog/{blogId}")
    public Result queryByBlogId(@PathVariable Long blogId){

        return Result.ok(blogCommentsService.queryByBlogId(blogId));
    }
}

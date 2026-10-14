package com.hmdp.dto;


import lombok.Data;

@Data
public class CommentDTO {
    private Long blogId;
     private String content;
     private Long parentId;
     private Long answerId;
}

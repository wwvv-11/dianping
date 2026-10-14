package com.dianping.dto;


import lombok.Data;

@Data
public class CommentDTO {
    private Long blogId;
     private String content;
     private Long parentId;
     private Long answerId;
}

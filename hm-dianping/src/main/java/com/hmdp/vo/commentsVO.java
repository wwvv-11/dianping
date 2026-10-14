package com.hmdp.vo;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class commentsVO {

    private Long id;
    private Long userId;
    private String nickName;
    private String icon;

    private String content;
    private Integer liked;
    private LocalDateTime createTime;
    private Long parentId;
    private Long answerId;
    private String answerNickName;
    private List<commentsVO> replies;
}

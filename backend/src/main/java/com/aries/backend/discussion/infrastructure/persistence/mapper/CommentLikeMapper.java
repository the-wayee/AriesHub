package com.aries.backend.discussion.infrastructure.persistence.mapper;

import com.aries.backend.discussion.infrastructure.persistence.po.CommentLikePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞表 Mapper。
 *
 * <p>取消点赞是物理删除，用 {@code delete(wrapper)} 即可，因此这里不需要任何手写语句；
 * 幂等插入与计数自增放在 {@link CommentMapper}，因为它们分别作用于 comment_likes 与 comments。
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLikePO> {}

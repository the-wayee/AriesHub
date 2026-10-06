package com.aries.backend.identity.infrastructure.persistence.po;

import com.aries.backend.shared.infrastructure.persistence.po.BasePO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** users 表的持久化对象，仅供基础设施层使用。 */
@Getter
@Setter
@TableName("users")
public class UserPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String bio;
    private String avatarFileId;
    private String role;
    private String status;
    private Boolean emailVerified;
    private OffsetDateTime lastLoginAt;
}

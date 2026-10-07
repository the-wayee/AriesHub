package com.aries.backend.identity.application.event;

/** 注册成功事实，不携带邮件、密码或会话。 */
public record MemberJoined(long userId) {}

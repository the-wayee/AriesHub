package com.aries.backend.inspiration.application.port;

import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;

/** 定时获取一条哲学文案，无有效内容返回 null，连接失败可抛出异常。 */
public interface PhilosophyQuoteProvider {
    PhilosophyQuoteView fetch();
}

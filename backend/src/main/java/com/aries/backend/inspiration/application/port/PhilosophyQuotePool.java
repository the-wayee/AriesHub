package com.aries.backend.inspiration.application.port;

import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;

import java.util.List;

/** 不可变的已缓存快照，未预热时为空，读取绝不触发外部请求。 */
public interface PhilosophyQuotePool {
    List<PhilosophyQuoteView> snapshot();
}

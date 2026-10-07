package com.aries.backend.inspiration.application.service;

import com.aries.backend.inspiration.application.port.PhilosophyQuotePool;
import com.aries.backend.inspiration.application.view.PhilosophyQuoteView;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 只从内存快照随机取句，用户访问量不改变上游请求量。 */
@Service
public class PhilosophyQuoteService {
    private final PhilosophyQuotePool pool;

    public PhilosophyQuoteService(PhilosophyQuotePool pool) {
        this.pool = pool;
    }

    public PhilosophyQuoteView random() {
        List<PhilosophyQuoteView> quotes = pool.snapshot();
        return quotes.isEmpty()
                ? null
                : quotes.get(ThreadLocalRandom.current().nextInt(quotes.size()));
    }
}

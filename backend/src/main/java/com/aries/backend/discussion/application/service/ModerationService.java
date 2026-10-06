package com.aries.backend.discussion.application.service;

import com.aries.backend.discussion.application.port.ModerationReadPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 跨线程只读审核队列；隐藏和锁帖仍由讨论领域原有用例执行。 */
@Service
@RequiredArgsConstructor
public class ModerationService {
    private final ModerationReadPort reads;

    public record Page(List<ModerationReadPort.Comment> items, int page, int size, long total) {}

    @Transactional(readOnly = true)
    public Page list(String status, int page, int size) {
        return new Page(
                reads.list(status, size, (page - 1) * size), page, size, reads.count(status));
    }
}

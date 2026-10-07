package com.aries.backend.inspiration.application.view;

/** 入场文案只读模型，正文来自一言，source 保留作者与出处。 */
public record PhilosophyQuoteView(String text, String source) {}

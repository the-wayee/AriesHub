"use client";

import { useSyncExternalStore } from "react";

export type LocalTopic = {
  id: string;
  title: string;
  body: string;
  tag: string;
  replies: string[];
};

type CommunityState = {
  saved: string[];
  liked: string[];
  history: Record<string, number>;
  topics: LocalTopic[];
};

const initial: CommunityState = {
  saved: [],
  liked: [],
  history: {},
  topics: [],
};
const storageKey = "arieshub-community-preview-v1";
const listeners = new Set<() => void>();
let snapshot = initial;
let loaded = false;

function read(): CommunityState {
  try {
    const value = JSON.parse(localStorage.getItem(storageKey) || "null");
    if (!value || typeof value !== "object") return initial;
    const strings = (items: unknown) =>
      Array.isArray(items)
        ? items.filter((item): item is string => typeof item === "string")
        : [];
    return {
      saved: strings(value.saved),
      liked: strings(value.liked),
      history: Object.entries(value.history || {}).reduce<
        Record<string, number>
      >((result, [slug, chapter]) => {
        if (typeof chapter === "number" && chapter >= 0) result[slug] = chapter;
        return result;
      }, {}),
      topics: Array.isArray(value.topics) ? value.topics : [],
    };
  } catch {
    return initial;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getSnapshot() {
  if (!loaded) {
    snapshot = read();
    loaded = true;
  }
  return snapshot;
}

function update(next: CommunityState) {
  snapshot = next;
  try {
    localStorage.setItem(storageKey, JSON.stringify(snapshot));
  } catch {
    // The preview stays usable for this session when storage is unavailable.
  }
  listeners.forEach((listener) => listener());
}

export function useCommunityState() {
  const state = useSyncExternalStore(subscribe, getSnapshot, () => initial);
  return {
    state,
    toggle(field: "saved" | "liked", slug: string) {
      update({
        ...state,
        [field]: state[field].includes(slug)
          ? state[field].filter((item) => item !== slug)
          : [...state[field], slug],
      });
    },
    remember(slug: string, chapter: number) {
      update({ ...state, history: { ...state.history, [slug]: chapter } });
    },
    addTopic(topic: LocalTopic) {
      update({ ...state, topics: [topic, ...state.topics] });
    },
    reply(topic: LocalTopic, body: string) {
      const current =
        state.topics.find((item) => item.id === topic.id) || topic;
      const next = { ...current, replies: [...current.replies, body] };
      update({
        ...state,
        topics: [next, ...state.topics.filter((item) => item.id !== topic.id)],
      });
    },
  };
}

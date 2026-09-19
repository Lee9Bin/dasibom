"use client";
import { useSyncExternalStore } from "react";
const key = "dasibom:saved";
function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  window.addEventListener("dasibom:saved", callback);
  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener("dasibom:saved", callback);
  };
}
function snapshot() {
  try { return localStorage.getItem(key) ?? "[]"; } catch { return "[]"; }
}
export function useSavedRegions() {
  const raw = useSyncExternalStore(subscribe, snapshot, () => "[]");
  let codes: string[] = [];
  try { const parsed: unknown = JSON.parse(raw); if (Array.isArray(parsed)) codes = parsed.filter((v): v is string => typeof v === "string" && /^\d{5}$/.test(v)); } catch { /* Ignore malformed local storage. */ }
  function toggle(code: string) {
    const next = codes.includes(code) ? codes.filter(c => c !== code) : [...new Set([...codes, code])];
    localStorage.setItem(key, JSON.stringify(next));
    window.dispatchEvent(new Event("dasibom:saved"));
  }
  return { codes, toggle };
}

"use client";
import {useState} from "react";
import {PhotoImage} from "./photo";
import type {Photo} from "@/lib/types";
export function PhotoGallery({photos}:{photos:Photo[]}) {
  const [count,setCount]=useState(12);
  if(!photos.length)return <p className="empty-inline">사진을 준비하고 있어요.</p>;
  return <><div className="photo-grid">{photos.slice(0,count).map(p=><figure key={p.id}>
    <a className="gallery-image" href={p.url} target="_blank" rel="noopener noreferrer" aria-label={`${p.title} 원본 사진 새 탭에서 보기`}><PhotoImage src={p.url} alt={p.title} sizes="(max-width:700px) 50vw, 25vw"/></a>
    <figcaption><strong>{p.title}</strong><span>© {p.photographer||p.source}</span><span>{p.location}</span><span>{/^\d{6}$/.test(p.month)?`${p.month.slice(0,4)}.${p.month.slice(4)} 촬영`:"촬영월 미제공"} · 포토코리아</span></figcaption>
  </figure>)}</div>{count<photos.length&&<button className="button" onClick={()=>setCount(c=>c+12)}>사진 더 보기 ({Math.min(count,photos.length)}/{photos.length})</button>}</>;
}

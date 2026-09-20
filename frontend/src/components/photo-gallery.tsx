"use client";
import {useEffect,useRef,useState} from "react";
import {PhotoImage} from "./photo";
import type {Photo} from "@/lib/types";
import type {PhotoPage} from "@/lib/api";
export function PhotoGallery({regionCode,initial}:{regionCode:string;initial:PhotoPage|null}) {
  const [photos,setPhotos]=useState<Photo[]>(initial?.items??[]),[page,setPage]=useState(initial?.page??1),[more,setMore]=useState(initial?.hasMore??false),[loading,setLoading]=useState(false),[error,setError]=useState(false);const sentinel=useRef<HTMLDivElement>(null);
  useEffect(()=>{const target=sentinel.current;if(!target||!more)return;const observer=new IntersectionObserver(async entries=>{if(!entries[0]?.isIntersecting||loading)return;setLoading(true);setError(false);try{const response=await fetch(`/api/v1/regions/${regionCode}/photos?page=${page+1}&size=12`);if(!response.ok)throw Error();const next=await response.json() as PhotoPage;setPhotos(current=>{const ids=new Set(current.map(p=>p.id));return [...current,...next.items.filter(p=>!ids.has(p.id))]});setPage(next.page);setMore(next.hasMore);}catch{setError(true);setMore(false);}finally{setLoading(false);}}, {rootMargin:"500px"});observer.observe(target);return()=>observer.disconnect();},[regionCode,page,more,loading]);
  if(!photos.length&&!more)return <p className="empty-inline">사진을 준비하고 있어요.</p>;
  return <><div className="photo-grid">{photos.map(p=><figure key={p.id}>
    <a className="gallery-image" href={p.url} target="_blank" rel="noopener noreferrer" aria-label={`${p.title} 원본 사진 새 탭에서 보기`}><PhotoImage src={p.url} alt={p.title} sizes="(max-width:700px) 50vw, 25vw"/></a>
    <figcaption>{p.photoType==="AWARD"&&<em className="award-label">관광사진 수상작{p.award?` · ${p.award}`:""}</em>}<strong>{p.title}</strong><span>{p.source} · {p.photographer||"촬영자 정보 없음"}</span><span>{p.location}</span><span>{/^\d{6}$/.test(p.month)?`${p.month.slice(0,4)}.${p.month.slice(4)} 촬영`:"촬영월 미제공"}</span></figcaption>
  </figure>)}</div><div ref={sentinel} className="gallery-sentinel" role="status">{loading?"다음 사진을 불러오는 중…":more?"아래로 스크롤하면 사진을 더 불러옵니다.":error?"사진을 더 불러오지 못했습니다.":`${photos.length}장의 사진을 모두 봤어요.`}</div></>;
}

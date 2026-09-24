"use client";
import {useCallback,useEffect,useRef,useState} from "react";
import {PhotoImage} from "./photo";
import {photoMonth} from "@/lib/photo-utils";
import type {Photo} from "@/lib/types";
import type {PhotoPage} from "@/lib/api";
export function PhotoGallery({regionCode,initial}:{regionCode:string;initial:PhotoPage|null}) {
 const [photos,setPhotos]=useState<Photo[]>(initial?.items??[]),[page,setPage]=useState(initial?.page??0),[more,setMore]=useState(initial?.hasMore??true),[loading,setLoading]=useState(false),[error,setError]=useState(!initial);
 const sentinel=useRef<HTMLDivElement>(null),busy=useRef(false);
 const load=useCallback(async()=>{if(busy.current)return;busy.current=true;setLoading(true);setError(false);
  try{const response=await fetch(`/api/v1/regions/${regionCode}/photos?page=${page+1}&size=12`,{signal:AbortSignal.timeout(30000)});if(!response.ok)throw Error();const next=await response.json() as PhotoPage;
   setPhotos(current=>{const ids=new Set(current.map(p=>p.placeKey??p.url));return [...current,...next.items.filter(p=>{const key=p.placeKey??p.url;if(ids.has(key))return false;ids.add(key);return true;})]});setPage(next.page);setMore(next.hasMore);
  }catch{setError(true);}finally{busy.current=false;setLoading(false);}
 },[regionCode,page]);
 useEffect(()=>{const target=sentinel.current;if(!target||!more||error)return;const observer=new IntersectionObserver(entries=>{if(entries[0]?.isIntersecting)void load();},{rootMargin:"300px"});observer.observe(target);return()=>observer.disconnect();},[more,error,load]);
 return <>{!photos.length&&!more&&!error&&<p className="empty-inline">이 지역에서 제공되는 대표 사진이 아직 없습니다.</p>}<div className="photo-grid">{photos.map(p=><figure key={p.id}>
 <a className="gallery-image" href={p.url} target="_blank" rel="noopener noreferrer" aria-label={`${p.title} 원본 사진 새 탭에서 보기`}><PhotoImage key={p.url} src={p.url} alt={p.title} sizes="(max-width:700px) 50vw, 25vw"/></a>
 <figcaption>{p.photoType==="AWARD"&&<em className="award-label">관광사진 수상작{p.award?` · ${p.award}`:""}</em>}<strong>{p.placeName??p.title}</strong><span>{p.source} · {p.photographer||"촬영자 정보 없음"}</span><span>{p.location}</span><span>{photoMonth(p.month)}</span></figcaption>
 </figure>)}</div><div ref={sentinel} className="gallery-sentinel" role="status">{loading?"다른 장소의 사진을 불러오는 중…":error?<><p>사진을 불러오지 못했어요.</p><button className="button" onClick={()=>void load()}>다시 불러오기</button></>:more?"아래로 내리면 다른 장소의 사진을 더 볼 수 있어요.":`장소별 대표 사진 ${photos.length}장`}</div></>;
}

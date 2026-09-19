"use client";
import { useState,useMemo } from "react";
import { useRouter,useSearchParams } from "next/navigation";
import dynamic from "next/dynamic";
import { Search, SlidersHorizontal, Map, LayoutGrid, X } from "lucide-react";
import { themes,type Region } from "@/lib/types";
import { RegionCard } from "./region-card";
const RegionMap=dynamic(()=>import("./region-map"),{ssr:false,loading:()=> <div className="map-loading">지도를 불러오고 있어요…</div>});
export function Explorer({regions}:{regions:Region[]}){
 const router=useRouter(),params=useSearchParams();const q=params.get("q")??"",theme=params.get("theme")??"",area=params.get("area")??"";
 const [view,setView]=useState<"grid"|"map">("grid");const [search,setSearch]=useState(q);
 const [limit,setLimit]=useState(12);
 const visible=useMemo(()=>regions.filter(r=>(!q||r.name.includes(q)||r.areaName.includes(q))&&(!theme||r.theme===theme)&&(!area||r.areaCode===area)),[regions,q,theme,area]);
 const shown=visible.slice(0,limit);
 function update(key:string,value:string){const next=new URLSearchParams(params);if(value)next.set(key,value);else next.delete(key);router.replace(`/explore?${next}`,{scroll:false});}
 const areas=Array.from(new globalThis.Map(regions.map(r=>[r.areaCode,r.areaName])).entries());
 return <><div className="explore-controls"><form onSubmit={e=>{e.preventDefault();update("q",search);}} className="search-box"><Search size={18}/><input aria-label="지역 검색" value={search} onChange={e=>setSearch(e.target.value)} placeholder="어느 동네가 궁금하세요?"/><button type="submit">검색</button></form><label className="area-filter"><SlidersHorizontal size={16}/><select aria-label="시도 선택" value={area} onChange={e=>update("area",e.target.value)}><option value="">모든 시도</option>{areas.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label></div><div className="theme-tabs" aria-label="여행 테마">{themes.map(t=><button key={t.id} onClick={()=>update("theme",t.id)} aria-pressed={theme===t.id} className={theme===t.id?"selected":""}>{t.name}</button>)}</div><div className="results-heading"><p><strong>{visible.length}개</strong> 지역 <span> · 공식 시군구 코드 기준</span></p><div className="view-toggle"><button aria-label="목록으로 보기" aria-pressed={view==="grid"} onClick={()=>setView("grid")}><LayoutGrid size={18}/></button><button aria-label="지도로 보기" aria-pressed={view==="map"} onClick={()=>setView("map")}><Map size={18}/></button></div></div>{visible.length===0?<div className="empty-state"><Search size={30}/><h2>아직 발견하지 못한 동네예요</h2><p>다른 지역명이나 테마로 찾아보세요.</p><button className="button" onClick={()=>{setSearch("");router.replace("/explore");}}><X size={16}/> 필터 초기화</button></div>:<>{view==="map"&&<><RegionMap regions={visible}/><p className="muted small">현재 지도에는 중심 좌표를 검증한 에디터 추천 지역만 표시합니다. 전국 지역은 아래 목록과 검색에서 확인할 수 있어요.</p></>}<div className="card-grid">{shown.map((r,i)=><RegionCard key={r.code} region={r} index={i}/>)}</div>{shown.length<visible.length&&<button className="button more-regions" onClick={()=>setLimit(v=>v+24)}>지역 더 보기 ({shown.length}/{visible.length})</button>}</>}</>;
}

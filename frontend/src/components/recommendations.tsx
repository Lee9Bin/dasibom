"use client";
import {useState} from "react";
import {ArrowUpRight,MapPin} from "lucide-react";
import {PhotoImage} from "./photo";
import {safeTourImage,type Recommendations as RecommendationData} from "@/lib/types";
const tabs=[{id:"ATTRACTION" as const,label:"관광지 50선"},{id:"FOOD" as const,label:"맛집 50선"},{id:"STAY" as const,label:"숙박 50선"}];
export function Recommendations({data,regionName}:{data:RecommendationData|null;regionName:string}){
 const [selected,setSelected]=useState<(typeof tabs)[number]["id"]>("ATTRACTION");const category=data?.categories[selected];
 return <><div className="category-tabs">{tabs.map(t=><button key={t.id} aria-pressed={selected===t.id} onClick={()=>setSelected(t.id)}>{t.label}<small>{data?.categories[t.id].available??0}</small></button>)}</div>
 {category?.items.length?<div className="place-grid recommendation-grid">{category.items.map((p,index)=><article className="place-card" key={`${selected}-${p.contentid}`}><div className="place-image"><PhotoImage src={safeTourImage(p.firstimage)} alt={p.title}/><span className="place-rank">{String(index+1).padStart(2,"0")}</span></div><div><h3>{p.title}</h3><p><MapPin size={13}/>{p.addr1||regionName}</p><a href={`https://map.naver.com/p/search/${encodeURIComponent(p.title+" "+regionName)}`} target="_blank" rel="noopener noreferrer">지도에서 보기 <ArrowUpRight size={14}/></a><small>출처: ⓒ한국관광공사 · {p.cpyrhtDivCd||"이용조건 확인 필요"}</small></div></article>)}</div>:<p className="empty-inline">이 카테고리의 실시간 관광정보가 없습니다.</p>}
 {category&&category.available>category.shown&&<p className="source-note">공공 API 검색 결과 {category.available.toLocaleString()}곳 중 조회수 기준 상위 {category.shown}곳을 보여드립니다.</p>}</>;
}

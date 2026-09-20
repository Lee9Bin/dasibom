import Link from "next/link";
import { notFound } from "next/navigation";
import type {Metadata} from "next";
import { ArrowLeft,ArrowUpRight,MapPin,CalendarDays,Info } from "lucide-react";
import { getRegion,getPhotos,getCrowd,getMetrics,getRecommendations } from "@/lib/api";
import {themeName} from "@/lib/types";
import {PhotoImage} from "@/components/photo";
import {Reactions} from "@/components/reactions";
import {PhotoGallery} from "@/components/photo-gallery";
import {RadarChart} from "@/components/radar-chart";
import {Recommendations} from "@/components/recommendations";
export async function generateMetadata({params}:{params:Promise<{code:string}>}):Promise<Metadata>{const r=await getRegion((await params).code);return {title:r?`${r.name}, ${r.tagline}`:"지역 여행",description:r?.tagline,openGraph:{images:r?.heroPhoto?[{url:r.heroPhoto.url,alt:r.heroPhoto.title}]:[]}};}
export default async function RegionPage({params}:{params:Promise<{code:string}>}){
 const {code}=await params;if(!/^\d{5}$/.test(code))notFound();const regionBase=await getRegion(code,false);if(!regionBase)notFound();
 const [photos,recommendations,crowd,metrics]=await Promise.all([getPhotos(code),getRecommendations(code),getCrowd(code),getMetrics(code)]);
 const region={...regionBase,heroPhoto:photos?.items[0]??null};
 const today=new Intl.DateTimeFormat("en-CA",{timeZone:"Asia/Seoul",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date()).replaceAll("-","");
 const forecasts=(crowd?.items??[]).filter(d=>/^\d{8}$/.test(d.baseYmd)&&d.baseYmd>=today&&d.cnctrRate!==""&&Number.isFinite(Number(d.cnctrRate))).sort((a,b)=>a.baseYmd.localeCompare(b.baseYmd)).slice(0,30);
 const quietDays=[...forecasts].sort((a,b)=>Number(a.cnctrRate)-Number(b.cnctrRate)).slice(0,3);
 const values=forecasts.map(d=>Number(d.cnctrRate));const min=values.length?Math.min(...values):0,max=values.length?Math.max(...values):1;
 const policy=region.populationStatus==="DECLINING"?"인구감소지역":region.populationStatus==="INTEREST"?"인구감소 관심지역":null;
 return <main id="main" className="detail-main"><div className="wrap detail-top"><Link className="back-link" href="/explore"><ArrowLeft size={16}/> 지역 둘러보기</Link><span className="eyebrow">{themeName(region.theme)}</span></div><section className="detail-hero wrap"><div className="detail-title"><p className="eyebrow"><MapPin size={14}/>{region.areaName}</p><h1>{region.name}</h1><p>{region.tagline}</p><div className="policy-badges">{policy&&<span>{policy}</span>}{region.halfPrice&&<span>반값여행 참여지역</span>}</div><Reactions code={code} name={region.name} tagline={region.tagline} imageUrl={region.heroPhoto?.url}/></div><div className="detail-photo"><PhotoImage src={region.heroPhoto?.url} alt={region.heroPhoto?.title??region.name} priority/><div className="photo-caption">{region.heroPhoto?.photoType==="AWARD"&&<b>한국관광공사 관광사진 수상작</b>}{region.heroPhoto?.title}<span>{region.heroPhoto?.source} · {region.heroPhoto?.photographer||"촬영자 정보 없음"}</span></div></div></section>
 <nav className="section-nav wrap" aria-label="지역 상세 섹션"><a href="#metrics">숨은 매력</a><a href="#places">여기도 좋아요</a><a href="#photos">사진 산책</a><a href="#quiet">한적한 날짜</a></nav>
 <section id="metrics" className="wrap detail-section"><div className="section-heading"><div><p className="eyebrow">17 TOURISM SIGNALS</p><h2>이 동네의 숨은 매력.</h2></div><span className="muted small">관광 서비스 12개 + 문화자원 5개 지표</span></div><RadarChart data={metrics}/></section>
 <section id="places" className="wrap detail-section"><div className="section-heading"><div><p className="eyebrow">YOU MAY ALSO LIKE</p><h2>여기도 좋아요.</h2></div><span className="muted small">출처: ⓒ한국관광공사 · 실시간 조회</span></div><Recommendations data={recommendations} regionName={region.name}/></section>
 <section id="photos" className="wrap detail-section"><div className="section-heading"><div><p className="eyebrow">A WALK THROUGH PHOTOGRAPHS</p><h2>한 장씩, 동네 산책.</h2></div><span className="muted small">아래로 내리면 다음 사진을 자동으로 불러와요.</span></div><PhotoGallery regionCode={code} initial={photos}/><p className="source-note">출처: ⓒ한국관광콘텐츠랩 · 이용조건은 <a href="https://korean.visitkorea.or.kr/photogallery/main.do" target="_blank" rel="noopener noreferrer">원문에서 확인</a>해 주세요.</p></section>
 {quietDays.length>0&&<div className="wrap notice"><strong>여유로운 여행 추천일</strong><p>{quietDays.map(d=>`${d.baseYmd.slice(4,6)}월 ${d.baseYmd.slice(6)}일 (${Number(d.cnctrRate).toFixed(1)})`).join(" · ")}</p>{crowd?.fetchedAt&&<small>실시간 조회: {new Date(crowd.fetchedAt).toLocaleString("ko-KR",{timeZone:"Asia/Seoul"})}</small>}</div>}
 <section id="quiet" className="wrap detail-section quiet-section"><div><p className="eyebrow"><CalendarDays size={15}/> 30-DAY FORECAST</p><h2>지금 한적해요.</h2><p>{region.anchorPlace?`${region.anchorPlace}의`:"이 지역에서 지원되는 관광지의"} 향후 30일 집중률 예측이에요.<br/>색이 옅고 숫자가 낮은 날일수록 여유롭습니다.</p><span className="source-note">출처: ⓒ한국관광공사 · 상대적 예측값이며 실제 방문객 수가 아닙니다.<br/>당일 현장 상황과 다를 수 있어요.</span></div><div>{forecasts.length?<><div className="calendar-grid">{forecasts.map(d=>{const level=Math.min(4,Math.floor((Number(d.cnctrRate)-min)/Math.max(1,max-min)*5));return <div key={`${d.baseYmd}-${d.tAtsNm}`} className={`calendar-cell heat-${level}`} title={`${d.tAtsNm}: ${d.cnctrRate}`}><span>{d.baseYmd.slice(4,6)}.{d.baseYmd.slice(6)}</span><strong>{Number(d.cnctrRate).toFixed(1)}</strong></div>})}</div><p className="muted small">옅은 색일수록 한적 · 진한 색일수록 집중 · 출처: ⓒ한국관광공사</p></>:<div className="empty-inline"><CalendarDays size={28}/><p>이 지역은 집중률 API 지원 결과가 없거나 연결이 원활하지 않습니다.<br/>사진과 주변 장소를 먼저 둘러보세요.</p></div>}</div></section>
 <section id="data" className="wrap data-note"><Info size={23}/><div><h3>여행의 근거도, 투명하게.</h3><p>지역·사진·관광지·17개 매력 지표·집중률을 화면 요청 때 한국관광공사 OpenAPI에서 조회합니다. 공공 API 응답을 서비스 DB에 저장하지 않으며, 원천 데이터가 비어 있으면 임의 점수를 만들지 않습니다.</p><Link href="/methodology">출처와 선정 방식 살펴보기 <ArrowUpRight size={15}/></Link></div></section></main>;
}

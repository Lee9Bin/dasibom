import { Suspense } from "react";
import type {Metadata} from "next";
import { getRegions,getMapRegions } from "@/lib/api";
import { Explorer } from "@/components/explorer";
export const metadata:Metadata={title:"지역 둘러보기"};
export default async function ExplorePage(){const [regions,mapData]=await Promise.all([getRegions(),getMapRegions()]);return <main id="main" className="wrap page-main"><p className="eyebrow">FIND YOUR OWN PLACE</p><h1 className="page-title">다음 여행의 한 페이지.</h1><p className="page-description">마음이 가는 풍경, 나와 닮은 동네를 찾아보세요.</p>{regions?<Suspense fallback={<p>지역을 불러오고 있어요…</p>}><Explorer regions={regions} mapRegions={mapData?.items??regions} visitorDataAsOf={mapData?.visitorDataAsOf}/></Suspense>:<div className="empty-state"><h2>지역을 불러오지 못했어요.</h2><p>데이터 연결을 확인하고 잠시 후 다시 방문해 주세요.</p><a className="button" href="/explore">다시 불러오기</a></div>}</main>;}

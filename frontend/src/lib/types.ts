import { z } from "zod";
export const photoSchema = z.object({ id:z.string(), url:z.string().url(), title:z.string(), photographer:z.string(), location:z.string(), month:z.string(), copyrightType:z.string(), source:z.string(),photoType:z.string().optional(),award:z.string().optional(),placeName:z.string().optional(),placeKey:z.string().optional() });
export const regionSchema = z.object({ code:z.string(), name:z.string(), areaName:z.string(), areaCode:z.string(), tagline:z.string(), theme:z.string(), latitude:z.number().nullable(), longitude:z.number().nullable(), anchorPlace:z.string().nullable(), populationStatus:z.string(),halfPrice:z.boolean(),heroPhoto:photoSchema.nullable(), dataStatus:z.string(), hiddenScore:z.number().nullable(), attractionScore:z.number().nullable(), likes:z.number(), source:z.string(),selectionReason:z.string().optional(),visitorDataAsOf:z.string().nullable().optional(),candidateCount:z.number().optional() });
export type Region = z.infer<typeof regionSchema>;
export type Photo = z.infer<typeof photoSchema>;
export type Place = { contentid:string; title:string; addr1:string; firstimage?:string; cpyrhtDivCd?:string; mapx?:string; mapy?:string };
export type Crowd = { baseYmd:string; cnctrRate:string; tAtsNm:string };
export type Metric={code:string;name:string;theme:string;value:number};
export type RadarMetric={theme:string;value:number|null};
export type MetricsResponse={status:string;baseYm:string|null;items:Metric[];radar:RadarMetric[];attractionScore:number|null;message:string|null;source:string};
export type RecommendationCategory={items:Place[];available:number;shown:number};
export type Recommendations={categories:Record<"ATTRACTION"|"FOOD"|"STAY",RecommendationCategory>;fetchedAt:string;source:string};
export const themes = [{id:"",name:"모든 여행"},{id:"NATURE",name:"자연과 야외"},{id:"HEALING",name:"쉼과 힐링"},{id:"CULTURE",name:"문화와 예술"},{id:"FOOD",name:"미식 여행"},{id:"STAY",name:"머무는 여행"},{id:"ACTIVITY",name:"체험과 활동"}];
export function themeName(id:string) { return themes.find(t=>t.id===id)?.name ?? "지역 여행"; }
export function safeTourImage(url?:string) {
  if(!url) return null;
  try { const u=new URL(url);return u.hostname === "tong.visitkorea.or.kr" && ["https:","http:"].includes(u.protocol) ? url.replace(/^http:/,"https:") : null; } catch { return null; }
}

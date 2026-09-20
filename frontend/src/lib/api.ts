import { regionSchema, type Region, type Photo, type Place, type Crowd,type MetricsResponse,type Recommendations } from "./types";
const backend = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";
export async function api<T>(path:string):Promise<T|null> {
  try { const res=await fetch(`${backend}/api/v1${path}`,{cache:"no-store",signal:AbortSignal.timeout(30000)});if(!res.ok)return null;return await res.json() as T; }
  catch { return null; }
}
export async function getRegions(query=""):Promise<Region[]|null>{
  const data=await api<{items:unknown[]}>(`/regions?size=300&${query}`);if(!data)return null;
  const parsed=regionSchema.array().safeParse(data.items);return parsed.success?parsed.data:null;
}
export async function getFeaturedRegions():Promise<Region[]|null> { const data=await api<{items:unknown[]}>("/regions?featured=true&withHero=true&size=4");if(!data)return null;const parsed=regionSchema.array().safeParse(data.items);return parsed.success?parsed.data:null; }
export async function getRegion(code:string,withHero=true) { if(!/^\d{5}$/.test(code))return null; const data=await api<unknown>(`/regions/${code}?withHero=${withHero}`);const parsed=regionSchema.safeParse(data);return parsed.success?parsed.data:null; }
export type PhotoPage={items:Photo[];page:number;size:number;total:number;hasMore:boolean;fetchedAt:string|null};
type LiveData<T>={items:T[];fetchedAt:string|null;stale:boolean};
export const getPhotos=(code:string,page=1,size=12)=>api<PhotoPage>(`/regions/${code}/photos?page=${page}&size=${size}`);
export const getPlaces=(code:string)=>api<LiveData<Place>>(`/regions/${code}/places`);
export const getCrowd=(code:string)=>api<LiveData<Crowd>>(`/regions/${code}/crowding`);
export const getMetrics=(code:string)=>api<MetricsResponse>(`/regions/${code}/metrics`);
export const getRecommendations=(code:string)=>api<Recommendations>(`/regions/${code}/recommendations`);
export const getMapRegions=()=>api<{items:Region[];visitorDataAsOf:string}>("/map/regions");

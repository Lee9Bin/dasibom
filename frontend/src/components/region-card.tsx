"use client";
import Link from "next/link";
import {useEffect,useState} from "react";
import { ArrowUpRight } from "lucide-react";
import type { Region } from "@/lib/types";
import { themeName } from "@/lib/types";
import { PhotoImage } from "./photo";
export function RegionCard({region,index=0}:{region:Region;index?:number}){
 const [current,setCurrent]=useState(region);
 useEffect(()=>{if(region.heroPhoto)return;const controller=new AbortController();fetch(`/api/v1/regions/${region.code}`,{signal:controller.signal}).then(r=>r.ok?r.json():null).then(data=>{if(data?.code===region.code)setCurrent(data);}).catch(()=>{});return()=>controller.abort();},[region]);
 return <Link href={`/regions/${current.code}`} className="region-card"><div className="card-image"><PhotoImage src={current.heroPhoto?.url} alt={current.heroPhoto?.title??current.name} sizes="(max-width:700px) 90vw, 33vw"/><span className="card-tag">{themeName(current.theme)}</span><span className="card-number">{String(index+1).padStart(2,"0")}</span></div><div className="card-body"><div><span className="eyebrow">{current.areaName}</span><h3>{current.name}<ArrowUpRight size={23}/></h3><p>{current.tagline}</p></div></div>{current.heroPhoto&&<span className="card-credit">{current.heroPhoto.source} · {current.heroPhoto.photographer||"촬영자 정보 없음"}</span>}</Link>;
}

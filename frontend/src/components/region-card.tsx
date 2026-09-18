import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import type { Region } from "@/lib/types";
import { themeName } from "@/lib/types";
import { PhotoImage } from "./photo";
export function RegionCard({region,index=0}:{region:Region;index?:number}){return <Link href={`/regions/${region.code}`} className="region-card"><div className="card-image"><PhotoImage src={region.heroPhoto?.url} alt={region.heroPhoto?.title??region.name} sizes="(max-width:700px) 90vw, 33vw"/><span className="card-tag">{themeName(region.theme)}</span><span className="card-number">{String(index+1).padStart(2,"0")}</span></div><div className="card-body"><div><span className="eyebrow">{region.areaName}</span><h3>{region.name.replace(/[시군]$/,"")}<ArrowUpRight size={23}/></h3><p>{region.tagline}</p></div></div>{region.heroPhoto&&<span className="card-credit">사진: {region.heroPhoto.photographer||region.heroPhoto.source}</span>}</Link>;}

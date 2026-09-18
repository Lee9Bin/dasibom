"use client";
import {useEffect,useState} from "react";
import Link from "next/link";
import {Bookmark} from "lucide-react";
import type {Region} from "@/lib/types";
import {RegionCard} from "./region-card";
export function SavedList({regions}:{regions:Region[]}){const[codes,setCodes]=useState<string[]|null>(null);useEffect(()=>{try{const v=JSON.parse(localStorage.getItem("dasibom:saved")??"[]");setCodes(Array.isArray(v)?v:[]);}catch{setCodes([]);}},[]);if(codes===null)return <p>저장한 여행을 불러오고 있어요…</p>;const items=regions.filter(r=>codes.includes(r.code));return items.length?<div className="card-grid">{items.map((r,i)=><RegionCard key={r.code} region={r} index={i}/>)}</div>:<div className="empty-state"><Bookmark size={36} strokeWidth={1.2}/><h2>다음 여행을 이곳에 모아두세요.</h2><p>마음에 드는 지역에서 ‘여행 저장’을 눌러주세요.<br/>저장한 여행은 지금 이 브라우저에 보관됩니다.</p><Link href="/explore" className="button">새로운 동네 만나기 ↗</Link></div>;}

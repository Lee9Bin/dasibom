"use client";
import Image from "next/image";
import { useState } from "react";
import { Mountain } from "lucide-react";
export function PhotoImage({src,alt,priority=false,sizes="(max-width: 700px) 100vw, 50vw"}:{src?:string|null;alt:string;priority?:boolean;sizes?:string}){const[failed,setFailed]=useState(false);if(!src||failed)return <div className="image-placeholder" role="img" aria-label={`${alt} 사진 준비 중`}><Mountain size={50} strokeWidth={1}/><span>풍경을 준비하고 있어요</span></div>;return <Image src={src} alt={alt} fill priority={priority} sizes={sizes} onError={()=>setFailed(true)} className="cover-image"/>;}

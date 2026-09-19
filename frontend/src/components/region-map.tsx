"use client";
import {useEffect,useRef,useState} from "react";
import * as maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import type {Region} from "@/lib/types";
export default function RegionMap({regions}:{regions:Region[]}){
 const el=useRef<HTMLDivElement>(null);const [failed,setFailed]=useState(false);
 useEffect(()=>{if(!el.current)return;let map:maplibregl.Map|undefined;
 try{map=new maplibregl.Map({container:el.current,center:[127.8,36.2],zoom:6,style:{version:8,sources:{osm:{type:"raster",tiles:["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],tileSize:256,attribution:'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'}},layers:[{id:"base",type:"raster",source:"osm",paint:{"raster-saturation":-0.7}}]}});
 map.addControl(new maplibregl.NavigationControl(),"top-right");map.on("error",()=>setFailed(true));
 const bounds=new maplibregl.LngLatBounds();regions.forEach(r=>{const link=document.createElement("a");link.className="map-pin";link.href=`/regions/${r.code}`;link.textContent=r.name;link.setAttribute("aria-label",`${r.name} 상세 보기`);new maplibregl.Marker({element:link}).setLngLat([r.longitude,r.latitude]).addTo(map!);bounds.extend([r.longitude,r.latitude]);});if(regions.length>1)map.fitBounds(bounds,{padding:70,maxZoom:8,duration:0});
 }catch{queueMicrotask(()=>setFailed(true));}return()=>map?.remove();},[regions]);
 return <div className="map-shell"><div ref={el} className="region-map" aria-label="지역 위치 지도"/>{failed&&<p className="map-warning" role="status">지도를 표시하지 못했습니다. 아래 지역 목록을 이용해 주세요.</p>}</div>;
}

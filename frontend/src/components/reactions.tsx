"use client";
import { Heart,Bookmark,Share2,Check } from "lucide-react";
import {useEffect,useState} from "react";
import {useQuery,useMutation,useQueryClient} from "@tanstack/react-query";
export function Reactions({code,name}:{code:string;name:string}){
 const [saved,setSaved]=useState(false),[message,setMessage]=useState("");const client=useQueryClient();
 useEffect(()=>{try{setSaved(JSON.parse(localStorage.getItem("dasibom:saved")??"[]").includes(code));}catch{setSaved(false);}},[code]);
 const {data}=useQuery<{liked:boolean;count:number}>({queryKey:["like",code],queryFn:async()=>{const r=await fetch(`/api/v1/regions/${code}/like`);if(!r.ok)throw Error();return r.json();}});
 const mutation=useMutation({mutationFn:async()=>{const r=await fetch(`/api/v1/regions/${code}/like`,{method:data?.liked?"DELETE":"PUT"});if(!r.ok)throw Error();return r.json();},onSuccess:value=>{client.setQueryData(["like",code],value);setMessage("");},onError:()=>setMessage("좋아요를 저장하지 못했어요. 다시 시도해 주세요.")});
 function save(){try{const raw=JSON.parse(localStorage.getItem("dasibom:saved")??"[]");const list:string[]=Array.isArray(raw)?raw:[];const next=saved?list.filter(c=>c!==code):[...new Set([...list,code])];localStorage.setItem("dasibom:saved",JSON.stringify(next));setSaved(!saved);setMessage(saved?"저장을 취소했어요.":"이 브라우저에 여행을 저장했어요.");}catch{setMessage("브라우저 저장 공간을 사용할 수 없어요.");}}
 async function share(){try{if(navigator.share){await navigator.share({title:`다시봄, ${name}`,url:location.href});}else{await navigator.clipboard.writeText(location.href);setMessage("여행 링크를 복사했어요.");}}catch(e){if(!(e instanceof DOMException&&e.name==="AbortError"))setMessage("공유하지 못했어요. 주소창의 링크를 복사해 주세요.");}}
 return <div className="reactions"><button disabled={mutation.isPending||!data} aria-pressed={data?.liked??false} onClick={()=>mutation.mutate()}><Heart size={18} fill={data?.liked?"currentColor":"none"}/>{data?.count??0}</button><button aria-pressed={saved} onClick={save}>{saved?<Check size={18}/>:<Bookmark size={18}/>} {saved?"저장됨":"여행 저장"}</button><button onClick={share}><Share2 size={18}/> 공유</button><span role="status" className="action-message">{message}</span></div>;
}

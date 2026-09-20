import { NextRequest, NextResponse } from "next/server";
const allowed=/^(home\/today|regions(?:\/\d{5}(?:\/(photos|places|crowding|metrics|recommendations|like))?)?|map\/regions|filters|data-status)$/;
async function proxy(request:NextRequest,{params}:{params:Promise<{path:string[]}>}){
 const path=(await params).path.join("/");if(!allowed.test(path))return NextResponse.json({detail:"Not found"},{status:404});
 if(request.method!=="GET"&&!/^regions\/\d{5}\/like$/.test(path))return NextResponse.json({detail:"Method not allowed"},{status:405});
 const headers=new Headers();const cookie=request.headers.get("cookie");if(cookie)headers.set("Cookie",cookie);
 if(request.method!=="GET"){
   const origin=request.headers.get("origin");const expected=process.env.NEXT_PUBLIC_SITE_URL??"http://localhost:3000";
   if(origin!==expected)return NextResponse.json({detail:"허용되지 않은 요청입니다."},{status:403});
   headers.set("Origin",origin);
 }
 try{
  const upstream=await fetch(`${process.env.BACKEND_URL??"http://127.0.0.1:8080"}/api/v1/${path}${request.nextUrl.search}`,{method:request.method,headers,cache:"no-store",signal:AbortSignal.timeout(30000)});
  const out=new NextResponse(await upstream.text(),{status:upstream.status,headers:{"Content-Type":upstream.headers.get("content-type")??"application/json","Cache-Control":"no-store"}});
  upstream.headers.getSetCookie().forEach(cookie=>out.headers.append("Set-Cookie",cookie));return out;
 }catch{return NextResponse.json({detail:"서버에 연결하지 못했습니다."},{status:503});}
}
export const GET=proxy;export const PUT=proxy;export const DELETE=proxy;

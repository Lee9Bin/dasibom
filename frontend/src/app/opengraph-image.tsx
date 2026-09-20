import {ImageResponse} from "next/og";
export const alt="다시봄, 한국의 숨은 동네 여행";
export const size={width:1200,height:630};
export const contentType="image/png";
export default function OpenGraphImage(){return new ImageResponse(<div style={{width:"100%",height:"100%",display:"flex",flexDirection:"column",justifyContent:"space-between",padding:"72px",background:"#f6f4ed",color:"#213d30",fontFamily:"serif"}}><div style={{display:"flex",fontSize:24,letterSpacing:5}}>DASIBOM · KOREA</div><div style={{display:"flex",flexDirection:"column"}}><div style={{fontSize:82,fontWeight:600}}>익숙한 한국,</div><div style={{fontSize:82,color:"#5e6c5a"}}>낯선 설렘을 만나다.</div></div><div style={{display:"flex",fontSize:24,color:"#657569"}}>출처: ⓒ한국관광공사 · ⓒ한국관광콘텐츠랩</div></div>,size);}

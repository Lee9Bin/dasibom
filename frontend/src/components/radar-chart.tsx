import type {MetricsResponse} from "@/lib/types";

const labels:Record<string,string>={NATURE:"자연",HEALING:"힐링",FOOD:"미식",CULTURE:"문화",STAY:"숙박",ACTIVITY:"체험"};
export function RadarChart({data}:{data:MetricsResponse|null}){
 if(!data||data.status!=="LIVE"||!data.items.length)return <div className="metric-empty"><strong>{data?.status==="UNAVAILABLE"?"아직 제공되지 않은 관광 수요 지표예요":"관광 수요 지표를 불러오지 못했어요"}</strong><p>{data?.message??"공공 API 연결이 원활하지 않습니다."}</p><small>값을 임의로 만들거나 과거 파일을 저장해 대신 표시하지 않습니다.</small></div>;
 const values=data.radar.map(v=>Math.max(0,Math.min(100,v.value??0)));const point=(i:number,value:number)=>{const a=-Math.PI/2+i*Math.PI*2/values.length,r=112*value/100;return `${150+Math.cos(a)*r},${150+Math.sin(a)*r}`};
 const complete=data.radar.every(v=>v.value!==null);
 return <div className="radar-wrap">{complete?<svg className="radar" viewBox="0 0 300 300" role="img" aria-label="관광 자원 수요 6개 테마 레이더 차트">
  {[25,50,75,100].map(level=><polygon key={level} points={values.map((_,i)=>point(i,level)).join(" ")} className="radar-ring"/>)}
  {values.map((_,i)=><line key={i} x1="150" y1="150" x2={point(i,100).split(",")[0]} y2={point(i,100).split(",")[1]} className="radar-axis"/>)}
  <polygon points={values.map((v,i)=>point(i,v)).join(" ")} className="radar-value"/>
  {data.radar.map((v,i)=>{const [x,y]=point(i,124).split(",").map(Number);return <text key={v.theme} x={x} y={y} textAnchor="middle" dominantBaseline="middle">{labels[v.theme]??v.theme}</text>})}
 </svg>:<p className="muted">일부 테마의 지표가 없어 확인된 개별 수치만 표시합니다.</p>}<div><p className="metric-score"><span>관광 수요 지표 평균</span><strong>{data.attractionScore?.toFixed(1)}</strong><small>/ 100 · 기준월 {data.baseYm}</small></p><p className="source-note">관심·소비·검색 수요를 나타내며 여행지의 품질 점수가 아닙니다. {data.items.length}/17개 지표 제공.</p><div className="metric-list">{data.items.map(m=><div key={m.code}><span>{m.name}</span><progress max="100" value={m.value}/><strong>{m.value.toFixed(1)}</strong></div>)}</div></div></div>;
}

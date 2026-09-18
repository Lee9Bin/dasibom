import {getRegions} from "@/lib/api";
import {SavedList} from "@/components/saved-list";
export const metadata={title:"저장한 여행"};
export default async function Saved(){const regions=await getRegions();return <main id="main" className="wrap page-main"><p className="eyebrow">PLACES TO REMEMBER</p><h1 className="page-title">언젠가의 여행을 담아.</h1><p className="page-description">마음이 먼저 다녀온 동네, 천천히 꺼내보세요.</p>{regions?<SavedList regions={regions}/>:<div className="empty-inline">여행 정보를 불러오지 못했어요. 저장 목록은 브라우저에 보관되어 있습니다.</div>}</main>;}

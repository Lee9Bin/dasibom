import Link from "next/link";
export default function NotFound(){return <main id="main" className="wrap empty-state"><p className="eyebrow">A SMALL DETOUR</p><h1>잠깐, 다른 길로 왔네요.</h1><p>이 페이지를 찾을 수 없거나 여행 정보에 연결하지 못했어요.</p><Link href="/explore" className="button">지역 둘러보기</Link></main>;}

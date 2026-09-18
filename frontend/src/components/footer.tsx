import Link from "next/link";
export function Footer(){return <footer className="site-footer"><div><Link className="footer-brand" href="/">다시봄.</Link><p>익숙한 한국에서, 새로운 여행을 발견합니다.</p></div><div className="footer-note"><p>관광 데이터 · 사진 제공 © 한국관광공사</p><Link href="/methodology">데이터 출처와 이용 안내 ↗</Link><span>© {new Date().getFullYear()} DASIBOM. Made for slow discoveries.</span></div></footer>;}

"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ArrowUpRight, Bookmark, Sprout } from "lucide-react";
export function Header(){const path=usePathname();return <header className="site-header"><Link href="/" className="brand" aria-label="다시봄 홈"><Sprout size={27} strokeWidth={1.7}/><span>다시봄<span className="brand-dot">.</span></span><small>한국을 새롭게</small></Link><nav aria-label="주요 메뉴"><Link className={path==="/"?"active":""} href="/">오늘의 발견</Link><Link className={path==="/explore"?"active":""} href="/explore">지역 둘러보기</Link><Link className={path==="/methodology"?"active":""} href="/methodology">다시봄 이야기</Link></nav><Link className="saved-link" href="/saved"><Bookmark size={17}/><span>저장한 여행</span><ArrowUpRight size={15}/></Link></header>;}

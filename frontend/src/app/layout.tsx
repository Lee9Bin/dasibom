import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "@/components/providers";
import { Header } from "@/components/header";
import { Footer } from "@/components/footer";
export const metadata:Metadata={metadataBase:new URL(process.env.NEXT_PUBLIC_SITE_URL??"http://localhost:3000"),title:{default:"다시봄, 한국 — 조금 다른 여행의 시작",template:"%s | 다시봄, 한국"},description:"아직 만나지 못한 한국의 풍경. 사진과 관광 데이터로 발견하는 조금 더 느린 지역 여행.",openGraph:{locale:"ko_KR",type:"website",siteName:"다시봄, 한국"}};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="ko"><body><Providers><a className="skip-link" href="#main">본문으로 바로가기</a><Header/>{children}<Footer/></Providers></body></html>;}

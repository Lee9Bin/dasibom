"use client";
export default function ErrorPage({reset}:{reset:()=>void}){return <main id="main" className="wrap empty-state"><h1>잠시 쉬어가는 중이에요.</h1><p>페이지를 불러오는 데 문제가 생겼어요.</p><button className="button" onClick={reset}>다시 시도하기</button></main>;}

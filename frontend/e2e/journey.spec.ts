import {test,expect} from "@playwright/test";
test("홈에서 지역 검색, 상세, 저장, 공유 흐름",async({page})=>{
 await page.goto("/");await expect(page.getByRole("heading",{name:/익숙한 한국/})).toBeVisible();
 await page.getByRole("link",{name:"지역 둘러보기",exact:true}).click();
 await page.getByRole("textbox",{name:"지역 검색"}).fill("원주");await page.getByRole("button",{name:"검색",exact:true}).click();
 await expect(page.locator(".region-card")).toHaveCount(1);await page.locator(".region-card").click();
 await expect(page.getByRole("heading",{name:"원주시",exact:true})).toBeVisible();
 await page.getByRole("button",{name:"여행 저장",exact:true}).click();await expect(page.getByRole("button",{name:"저장됨",exact:true})).toBeVisible();
 await page.getByRole("link",{name:"저장한 여행"}).click();await expect(page.locator(".region-card")).toHaveCount(1);
});
test("레이아웃이 화면을 넘치지 않고 빈 검색을 설명한다",async({page})=>{
 await page.goto("/explore?q=없는지역");await expect(page.getByRole("heading",{name:"아직 발견하지 못한 동네예요"})).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
 await page.getByRole("button",{name:"필터 초기화"}).click();await expect(page.locator(".region-card")).toHaveCount(8);
});
test("관광 사진과 날짜 정보, 지도 탐색이 동작한다",async({page})=>{
 await page.goto("/regions/51130");
 await expect(page.locator(".detail-photo img")).toBeVisible();
 await expect(page.locator(".place-card").first()).toBeVisible();
 const photos=page.locator(".photo-grid figure");
 await expect(photos).toHaveCount(12);
 await page.getByRole("button",{name:/사진 더 보기/}).click();
 await expect(photos).toHaveCount(24);
 await expect(page.locator(".calendar-cell").first()).toBeVisible();
 await page.goto("/explore");
 await page.getByRole("button",{name:"지도로 보기"}).click();
 await expect(page.locator(".map-pin")).toHaveCount(8);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
test("공개 API 상태와 관리자 차단, 좋아요 멱등성",async({request,baseURL})=>{
 expect((await request.get("/api/health")).ok()).toBe(true);
 expect((await request.post("/api/v1/admin/sync/catalog")).status()).toBe(404);
 const path="/api/v1/regions/51130/like";
 const origin=process.env.PLAYWRIGHT_ORIGIN??baseURL!;
 try {
  const first=await request.put(path,{headers:{Origin:origin}});
  expect(first.ok()).toBe(true);
  const liked=await first.json();expect(liked.liked).toBe(true);
  const second=await request.put(path,{headers:{Origin:origin}});
  expect((await second.json()).count).toBe(liked.count);
  expect((await request.put(path,{headers:{Origin:"https://invalid.example"}})).status()).toBe(403);
 } finally {
  const removed=await request.delete(path,{headers:{Origin:origin}});
  expect(removed.ok()).toBe(true);
  expect((await removed.json()).liked).toBe(false);
 }
});

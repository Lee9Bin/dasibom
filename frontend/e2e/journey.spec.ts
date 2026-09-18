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

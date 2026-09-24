export function photoMonth(value:string){
 const match=/^(\d{4})(\d{2})/.exec(value);
 if(!match)return /^\d{4}$/.test(value)?`${value}년 촬영`:"촬영 시기 미제공";
 const month=Number(match[2]);return month>=1&&month<=12?`${match[1]}.${match[2]} 촬영`:`${match[1]}년 촬영 · 월 미제공`;
}

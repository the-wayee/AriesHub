// 原创内容流程图，使用浏览器排版导出 PNG；无外部素材与网络请求。
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const { chromium } = require(path.join(root, 'frontend/node_modules/@playwright/test'));
(async () => {
 const rows=JSON.parse(fs.readFileSync(path.join(root, 'backend/scripts/community-content.json'),'utf8'));
 const browser=await chromium.launch({...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ? {executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH} : {}),headless:true});
 const page=await browser.newPage({viewport:{width:1200,height:720},deviceScaleFactor:1});
 for(let i=0;i<rows.length;i++) {
  const r=rows[i];
  await page.setContent(`<style>*{box-sizing:border-box}body{margin:0;background:#f1f1ed;color:#222;font-family:'Microsoft YaHei',sans-serif;padding:68px}header{display:flex;justify-content:space-between;font-size:17px;border-bottom:1px solid #aaa;padding-bottom:23px}h1{font-size:38px;font-weight:500;line-height:1.55;max-width:960px;margin:49px 0}section{display:flex;gap:19px;margin-top:65px}article{width:25%;position:relative;padding:25px 21px;border:1px solid #bbb;background:#fcfcfa;height:144px}b{display:block;font-size:13px;color:#888;margin-bottom:22px}strong{font-size:25px;font-weight:400}article:not(:last-child):after{content:'→';position:absolute;right:-16px;top:63px;font-size:22px}footer{margin-top:70px;font-size:15px;letter-spacing:3px;color:#777}</style><header><span>ARIESHUB / 实践笔记</span><span>${r.theme} · 0${i+1}</span></header><h1>${r.title}</h1><section>${r.diagram.map((s,n)=>`<article><b>STEP 0${n+1}</b><strong>${s}</strong></article>`).join('')}</section><footer>小范围开始 · 保留证据 · 失败可恢复</footer>`);
  await page.screenshot({path:path.join(root, `backend/scripts/community-assets/${String(i+1).padStart(2,'0')}.png`)});
 }
 await browser.close();
})();

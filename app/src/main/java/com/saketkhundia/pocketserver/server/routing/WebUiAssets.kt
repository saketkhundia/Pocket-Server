package com.saketkhundia.pocketserver.server.routing

// Embedded browser client — premium 2026 web-app feel (Linear/Vercel-like).
// Dark-first with light-mode support, sidebar on desktop, bottom nav on mobile.
// API contracts are unchanged (status, auth, files, media, download, upload,
// folder, rename, delete). No secrets are embedded; auth uses HttpOnly cookie.
object WebUiAssets {
    fun indexHtml(serverName: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
<meta name="color-scheme" content="dark light"/>
<title>${serverName.escapeHtml()} — Pocket Server</title>
<style>
:root{
  --bg:#0a0a0b;--surface:#111214;--elev:#18191c;--border:#212226;--border-soft:#1a1b1f;
  --text:#f4f5f7;--text2:#9ca3af;--text3:#6b7280;
  --accent:#6ea8fe;--accent-ink:#060a14;--accent-soft:rgba(110,168,254,.12);
  --ok:#4ade80;--warn:#fbbf24;--err:#f87171;
  --radius:12px;--radius-lg:16px;
  --font:ui-sans-serif,system-ui,-apple-system,"Segoe UI",Roboto,Inter,Arial,sans-serif;
  --mono:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;
  --shadow:0 12px 40px rgba(0,0,0,.45);
}
@media (prefers-color-scheme:light){
  :root{--bg:#fafaf9;--surface:#fff;--elev:#f4f4f5;--border:#e8e8e7;--border-soft:#efefee;
    --text:#111214;--text2:#5b5f68;--text3:#9ca3af;--accent:#2f6bff;--accent-ink:#fff;
    --accent-soft:rgba(47,107,255,.1);--ok:#15803d;--warn:#b45309;--err:#dc2626;
    --shadow:0 12px 32px rgba(0,0,0,.12)}
}
*{box-sizing:border-box}
[hidden]{display:none!important}
html{-webkit-text-size-adjust:100%}
body{margin:0;font-family:var(--font);background:var(--bg);color:var(--text);min-height:100vh;font-size:15px;line-height:1.5}
button{font:inherit;color:inherit;background:none;border:0;cursor:pointer}
input,select{font:inherit;color:var(--text)}
a{color:var(--accent);text-decoration:none}
:focus-visible{outline:2px solid var(--accent);outline-offset:2px;border-radius:6px}
.mono{font-family:var(--mono);font-size:.86em}
.muted{color:var(--text2)}.tiny{font-size:12px}
/* ── App frame ── */
.app{display:flex;min-height:100vh}
.sidebar{width:248px;flex-shrink:0;border-right:1px solid var(--border-soft);padding:20px 14px;position:sticky;top:0;height:100vh;display:flex;flex-direction:column;gap:4px;background:var(--bg)}
.brand{display:flex;align-items:center;gap:10px;padding:6px 10px 18px;font-weight:600;font-size:15px}
.brand .mark{width:28px;height:28px;border-radius:9px;background:var(--text);color:var(--bg);display:grid;place-items:center;font-size:13px;font-weight:700;flex-shrink:0}
.nav{display:flex;flex-direction:column;gap:2px}
.nav button{display:flex;align-items:center;gap:11px;padding:8px 10px;border-radius:10px;color:var(--text2);font-size:14px;text-align:left;width:100%;transition:background .12s,color .12s}
.nav button:hover{background:var(--elev);color:var(--text)}
.nav button[aria-current="page"]{background:var(--elev);color:var(--text);font-weight:500}
.nav button .nbadge{margin-left:auto;font-size:11px;color:var(--text3)}
.sidefoot{margin-top:auto;padding:10px;font-size:12px;color:var(--text3)}
.conn{display:flex;align-items:center;gap:8px;padding:8px 10px;border-radius:10px;width:100%;text-align:left;font-size:13px;color:var(--text2)}
.conn:hover{background:var(--elev)}
.dot{width:8px;height:8px;border-radius:50%;background:var(--ok);flex-shrink:0}
.dot.pulse{animation:pulse 2s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}
.main{flex:1;min-width:0;display:flex;flex-direction:column}
/* ── Header ── */
.topbar{position:sticky;top:0;z-index:20;display:flex;align-items:center;gap:12px;padding:14px 28px;border-bottom:1px solid var(--border-soft);background:color-mix(in srgb,var(--bg) 88%,transparent);backdrop-filter:blur(12px)}
.topbar h1{font-size:17px;font-weight:600;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.crumbs{display:flex;align-items:center;gap:4px;min-width:0;flex-wrap:nowrap;overflow-x:auto;scrollbar-width:none}
.crumbs::-webkit-scrollbar{display:none}
.crumbs button{padding:5px 9px;border-radius:8px;color:var(--text2);font-size:13px;white-space:nowrap}
.crumbs button:hover{background:var(--elev);color:var(--text)}
.crumbs button.cur{color:var(--text);font-weight:500}
.crumbs .sep{color:var(--text3);font-size:12px}
.search{margin-left:auto;display:flex;align-items:center;gap:8px;background:var(--elev);border:1px solid transparent;border-radius:10px;padding:7px 12px;min-width:200px;max-width:320px;flex-shrink:1}
.search:focus-within{border-color:var(--accent)}
.search input{background:none;border:0;outline:0;width:100%;font-size:14px}
.search input::placeholder{color:var(--text3)}
.iconbtn{display:grid;place-items:center;width:34px;height:34px;border-radius:9px;color:var(--text2);flex-shrink:0}
.iconbtn:hover{background:var(--elev);color:var(--text)}
.iconbtn[aria-pressed="true"]{background:var(--accent-soft);color:var(--accent)}
/* ── Content ── */
.content{padding:22px 28px 120px;max-width:1100px;width:100%;margin:0 auto}
.toolbar{display:flex;align-items:center;gap:10px;margin-bottom:6px;flex-wrap:wrap}
.toolbar .spacer{flex:1}
.btn{display:inline-flex;align-items:center;gap:7px;padding:8px 15px;border-radius:10px;font-size:14px;font-weight:500;transition:filter .12s,background .12s}
.btn-primary{background:var(--accent);color:var(--accent-ink)}
.btn-primary:hover{filter:brightness(1.08)}
.btn-quiet{background:var(--elev);color:var(--text)}
.btn-quiet:hover{filter:brightness(1.12)}
.btn-danger-soft{background:none;color:var(--err)}
select.sortsel{background:var(--elev);border:0;border-radius:10px;padding:8px 10px;font-size:13px;color:var(--text2)}
.count{font-size:12px;color:var(--text3);margin:14px 2px 0}
/* ── File list ── */
.list{margin-top:8px;border-top:1px solid var(--border-soft)}
.list.zone-enter,.grid.zone-enter,.gallery.zone-enter,.state.zone-enter{animation:zoneIn .16s ease-out}
@keyframes zoneIn{from{opacity:.4;transform:translateY(4px)}to{opacity:1;transform:none}}
.frow{display:flex;align-items:center;gap:13px;padding:11px 8px;border-bottom:1px solid var(--border-soft);border-radius:8px;cursor:pointer;min-height:56px;content-visibility:auto;contain-intrinsic-size:auto 56px}
.frow:hover{background:var(--elev)}
.frow.selected{background:var(--accent-soft)}
.frow .meta{min-width:0;flex:1}
.frow .nm{font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.frow .mt{font-size:12px;color:var(--text3);margin-top:1px}
.frow .r{color:var(--text3);font-size:12px;white-space:nowrap}
.fcheck{width:20px;height:20px;accent-color:var(--accent);flex-shrink:0}
.fbadge{width:38px;height:38px;border-radius:10px;display:grid;place-items:center;font-family:var(--mono);font-size:9px;font-weight:700;letter-spacing:.4px;flex-shrink:0;background:var(--elev);color:var(--text2)}
.fbadge.folder{background:var(--accent-soft);color:var(--accent)}
.fbadge.img{color:#a78bfa}.fbadge.vid{color:#f472b6}.fbadge.aud{color:var(--ok)}.fbadge.pdf{color:var(--err)}.fbadge.arc{color:var(--warn)}
/* grid */
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:12px;margin-top:14px}
.gcard{border:1px solid var(--border-soft);border-radius:var(--radius);padding:16px 12px;text-align:center;cursor:pointer;transition:border-color .12s,transform .12s,background .12s;background:var(--surface);content-visibility:auto;contain-intrinsic-size:auto 120px}
.gcard:hover{border-color:var(--accent);transform:translateY(-1px)}
.gcard.selected{border-color:var(--accent);background:var(--accent-soft)}
.gcard .fbadge{width:44px;height:44px;margin:0 auto 10px;font-size:10px}
.gcard .nm{font-size:13px;font-weight:500;word-break:break-word;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.gcard .mt{font-size:11px;color:var(--text3);margin-top:4px}
/* gallery */
.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px;margin-top:14px}
.gallery figure{margin:0;position:relative;border-radius:var(--radius);overflow:hidden;background:var(--elev);aspect-ratio:1;cursor:pointer;content-visibility:auto;contain-intrinsic-size:auto 150px}
.gallery img{width:100%;height:100%;object-fit:cover;display:block;transition:transform .2s}
.gallery figure:hover img{transform:scale(1.03)}
/* selection bar */
.selbar{position:fixed;left:50%;transform:translateX(-50%) translateY(20px);bottom:92px;z-index:40;display:none;align-items:center;gap:14px;background:var(--elev);border:1px solid var(--border);border-radius:14px;padding:10px 12px 10px 18px;box-shadow:var(--shadow);opacity:0;transition:opacity .15s,transform .15s}
.selbar.show{display:flex;opacity:1;transform:translateX(-50%) translateY(0)}
.selbar .n{font-size:13px;font-weight:600}
/* skeleton */
.sk{border-top:1px solid var(--border-soft);margin-top:8px}
.skrow{display:flex;gap:13px;align-items:center;padding:12px 8px;border-bottom:1px solid var(--border-soft)}
.skbox{border-radius:8px;background:var(--elev);animation:sk 1.1s infinite alternate}
@keyframes sk{from{opacity:.4}to{opacity:.75}}
/* empty / error */
.state{padding:64px 20px;text-align:center}
.state h3{margin:0 0 6px;font-size:16px;font-weight:600}
.state p{margin:0 0 18px;color:var(--text2);font-size:14px}
/* drop overlay */
#dropOverlay{position:fixed;inset:0;z-index:60;display:none;place-items:center;background:color-mix(in srgb,var(--accent) 10%,transparent);backdrop-filter:blur(2px)}
#dropOverlay.show{display:grid}
#dropOverlay .box{border:2px dashed var(--accent);border-radius:20px;padding:48px 64px;font-size:17px;font-weight:600;background:var(--bg)}
/* upload drawer */
#upDrawer{position:fixed;right:20px;bottom:86px;width:min(360px,calc(100vw - 40px));z-index:55;background:var(--elev);border:1px solid var(--border);border-radius:var(--radius-lg);box-shadow:var(--shadow);display:none;overflow:hidden;transform-origin:bottom right}
#upDrawer.show{display:block;animation:drawerIn .2s ease-out}
@keyframes drawerIn{from{opacity:0;transform:translateY(12px) scale(.98)}to{opacity:1;transform:none}}
#upDrawer header{display:flex;align-items:center;padding:12px 16px;border-bottom:1px solid var(--border-soft);font-weight:600;font-size:14px}
#upList{max-height:300px;overflow:auto;padding:6px 0}
.upitem{padding:9px 16px;font-size:13px}
.upitem .t{display:flex;justify-content:space-between;gap:8px;margin-bottom:5px}
.upitem .t span:last-child{color:var(--text2);font-size:12px}
.bar{height:4px;border-radius:4px;background:var(--border);overflow:hidden}
.bar i{display:block;height:100%;width:100%;background:var(--accent);border-radius:4px;transform:scaleX(0);transform-origin:left;transition:transform .15s ease-out;will-change:transform}
.upitem.done .bar i{background:var(--ok)}
.upitem.err .bar i{background:var(--err)}
/* lightbox */
#viewer{position:fixed;inset:0;z-index:70;display:none;background:rgba(0,0,0,.88);place-items:center}
#viewer.show{display:grid;animation:viewerIn .18s ease-out}
@keyframes viewerIn{from{opacity:0}to{opacity:1}}
#viewer.show .stage{animation:stageIn .2s ease-out}
@keyframes stageIn{from{opacity:0;transform:scale(.97)}to{opacity:1;transform:none}}
#viewer .stage{max-width:92vw;max-height:86vh;display:grid;place-items:center}
#viewer img,#viewer video,#viewer audio{max-width:92vw;max-height:80vh;border-radius:12px}
#viewer audio{width:min(480px,90vw)}
#viewer .vbar{position:absolute;left:0;right:0;bottom:0;display:flex;align-items:center;gap:12px;padding:16px 22px;color:#fff;font-size:13px;background:linear-gradient(transparent,rgba(0,0,0,.7))}
#viewer .vbar button{color:#fff;padding:8px;border-radius:8px}
#viewer .vbar button:hover{background:rgba(255,255,255,.15)}
#viewer .vbar .nm{flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
/* modal */
#modalWrap{position:fixed;inset:0;z-index:65;display:none;place-items:center;background:rgba(0,0,0,.5);padding:20px}
#modalWrap.show{display:grid;animation:viewerIn .15s ease-out}
#modal{background:var(--surface);border:1px solid var(--border);border-radius:var(--radius-lg);box-shadow:var(--shadow);width:min(420px,100%);padding:22px}
#modalWrap.show #modal{animation:modalIn .18s ease-out}
@keyframes modalIn{from{opacity:0;transform:scale(.96) translateY(6px)}to{opacity:1;transform:none}}
#modal h3{margin:0 0 4px;font-size:16px}
#modal .sub{font-size:13px;color:var(--text2);margin-bottom:14px}
#modal input{width:100%;background:var(--elev);border:1px solid var(--border);border-radius:10px;padding:10px 12px;font-size:14px;outline:none}
#modal input:focus{border-color:var(--accent)}
#modal .actions{display:flex;justify-content:flex-end;gap:8px;margin-top:18px}
.kv{display:grid;grid-template-columns:110px 1fr;gap:7px 12px;font-size:13px;margin-top:6px}
.kv dt{color:var(--text2)}.kv dd{margin:0;word-break:break-word}
/* context menu */
#ctxmenu{position:fixed;z-index:66;display:none;min-width:190px;background:var(--elev);border:1px solid var(--border);border-radius:12px;box-shadow:var(--shadow);padding:5px;overflow:hidden;transform-origin:top left}
#ctxmenu.show{display:block;animation:ctxIn .15s ease-out}
@keyframes ctxIn{from{opacity:0;transform:scale(.96)}to{opacity:1;transform:none}}
#ctxmenu button{display:flex;width:100%;text-align:left;padding:9px 12px;border-radius:8px;font-size:14px;gap:10px;align-items:center}
#ctxmenu button:hover{background:var(--accent-soft)}
#ctxmenu button.danger{color:var(--err)}
/* toasts */
#toasts{position:fixed;bottom:92px;left:50%;transform:translateX(-50%);z-index:80;display:flex;flex-direction:column;gap:8px;align-items:center;pointer-events:none}
.toast{background:var(--elev);border:1px solid var(--border);color:var(--text);font-size:13px;padding:10px 18px;border-radius:100px;box-shadow:var(--shadow);animation:tin .18s ease-out}
@keyframes tin{from{opacity:0;transform:translateY(8px)}to{opacity:1}}
/* login */
.loginwrap{min-height:100vh;display:grid;place-items:center;padding:24px}
.logincard{width:min(380px,100%);text-align:center}
.logincard .mark{width:52px;height:52px;border-radius:16px;background:var(--text);color:var(--bg);display:grid;place-items:center;font-weight:700;font-size:20px;margin:0 auto 20px}
.logincard h2{margin:0 0 4px;font-size:22px}
.logincard p{margin:0 0 24px;color:var(--text2);font-size:14px}
.logincard input{width:100%;background:var(--elev);border:1px solid var(--border);border-radius:12px;padding:12px 14px;font-size:15px;margin-bottom:12px;outline:none}
.logincard input:focus{border-color:var(--accent)}
.logincard .btn-primary{width:100%;justify-content:center;padding:12px}
#loginErr{color:var(--err);font-size:13px;min-height:20px;margin-top:10px}
/* bottom nav (mobile) */
.bottomnav{display:none;position:fixed;left:0;right:0;bottom:0;z-index:30;background:color-mix(in srgb,var(--bg) 92%,transparent);backdrop-filter:blur(14px);border-top:1px solid var(--border-soft);padding:6px 8px calc(8px + env(safe-area-inset-bottom))}
.bottomnav nav{display:flex}
.bottomnav button{flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;padding:7px 0;font-size:11px;color:var(--text3);border-radius:10px}
.bottomnav button[aria-current="page"]{color:var(--accent)}
/* responsive */
@media(max-width:1023px){
  .sidebar{display:none}
  .bottomnav{display:block}
  .topbar{padding:12px 16px}
  .content{padding:16px 16px 140px}
  .search{min-width:0}
  .search input::placeholder{font-size:0}
}
@media(min-width:1024px){.topbar .only-mobile{display:none}}
@media(max-width:767px){
  .grid{grid-template-columns:repeat(auto-fill,minmax(104px,1fr))}
  .gallery{grid-template-columns:repeat(3,1fr);gap:6px}
  .topbar h1{font-size:15px}
  .btn .lbl{display:none}
  .btn{padding:8px 11px}
  .frow .r{display:none}
  #upDrawer{right:12px;bottom:150px}
  #toasts{bottom:150px}
  .selbar{bottom:150px;max-width:calc(100vw - 24px)}
}
@media (prefers-reduced-motion:reduce){
  *,*::before,*::after{animation:none!important;transition:none!important}
}
</style>
</head>
<body>
<div id="loginView" class="loginwrap" hidden>
  <div class="logincard">
    <div class="mark" aria-hidden="true">PS</div>
    <h2>Welcome back</h2>
    <p id="loginSub">Sign in to continue</p>
    <form id="loginForm" autocomplete="on">
      <input id="u" name="username" placeholder="Username" autocomplete="username" autocapitalize="off" autocorrect="off" spellcheck="false" aria-label="Username" required/>
      <input id="p" name="password" type="password" placeholder="Password" autocomplete="current-password" autocapitalize="off" autocorrect="off" spellcheck="false" aria-label="Password" required/>
      <button class="btn btn-primary" type="submit" id="loginBtn">Sign in</button>
    </form>
    <div id="loginErr" role="alert"></div>
    <button class="btn btn-quiet" id="recheckBtn" type="button" style="margin-top:6px">I turned auth off — check again</button>
  </div>
</div>
<div id="app" class="app" hidden>
  <aside class="sidebar" aria-label="Primary">
    <div class="brand"><span class="mark">PS</span><span id="brandName">Pocket Server</span></div>
    <nav class="nav" id="sideNav">
      <button data-view="home"><span aria-hidden="true">⌂</span>Home</button>
      <button data-view="files"><span aria-hidden="true">▤</span>Files</button>
      <button data-view="photos"><span aria-hidden="true">◉</span>Photos</button>
      <button data-view="media"><span aria-hidden="true">▶</span>Media</button>
    </nav>
    <div class="sidefoot">
      <button class="conn" id="connBtn" aria-label="Connection details"><span class="dot pulse" id="connDot"></span><span id="connLabel">Connected</span></button>
    </div>
  </aside>
  <div class="main">
    <div class="topbar">
      <h1 id="crumbTitle">Home</h1>
      <div class="crumbs" id="crumbs" aria-label="Breadcrumb"></div>
      <div class="search" role="search">
        <span aria-hidden="true" class="muted">⌕</span>
        <input id="search" placeholder="Search files…" aria-label="Search files" autocomplete="off"/>
      </div>
      <button class="iconbtn" id="viewToggle" aria-label="Toggle grid or list view" aria-pressed="false" title="Grid / list">▦</button>
      <button class="iconbtn" id="logoutBtn" aria-label="Sign out" title="Sign out" hidden>⎋</button>
    </div>
    <div class="content">
      <div class="toolbar">
        <button class="btn btn-primary" id="upBtn"><span aria-hidden="true">＋</span><span class="lbl">Upload</span></button>
        <button class="btn btn-quiet" id="mkdirBtn"><span aria-hidden="true">＋</span><span class="lbl">New folder</span></button>
        <span class="spacer"></span>
        <select class="sortsel" id="sort" aria-label="Sort files">
          <option value="name">Name ↑</option><option value="name_desc">Name ↓</option>
          <option value="size">Smallest</option><option value="size_desc">Largest</option>
          <option value="date">Oldest</option><option value="date_desc">Newest</option>
        </select>
        <input type="file" id="filePick" multiple hidden/>
      </div>
      <div id="listZone"></div>
      <div class="count" id="count"></div>
    </div>
  </div>
</div>
<div class="bottomnav" id="bottomnav" hidden>
  <nav>
    <button data-view="home">⌂<span>Home</span></button>
    <button data-view="files">▤<span>Files</span></button>
    <button data-view="photos">◉<span>Photos</span></button>
    <button data-view="media">▶<span>Media</span></button>
  </nav>
</div>
<div id="dropOverlay" aria-hidden="true"><div class="box">Drop files to upload</div></div>
<div id="upDrawer" role="status" aria-label="Uploads"><header><span id="upTitle">Uploads</span><span style="flex:1"></span><button class="iconbtn" id="upClose" aria-label="Hide uploads">✕</button></header><div id="upList"></div></div>
<div id="viewer" role="dialog" aria-modal="true" aria-label="Preview"><div class="stage" id="vStage"></div><div class="vbar"><button id="vPrev" aria-label="Previous">‹</button><button id="vNext" aria-label="Next">›</button><span class="nm" id="vName"></span><button id="vDl" aria-label="Download">⤓</button><button id="vClose" aria-label="Close">✕</button></div></div>
<div id="modalWrap"><div id="modal" role="dialog" aria-modal="true"></div></div>
<div id="ctxmenu" role="menu"></div>
<div id="toasts" aria-live="polite"></div>
<script>
"use strict";
let cur="",view="files",sort="name",grid=false,authed=false,authNeeded=true,serverName="Pocket Server";
let files=[],selected=new Set(),searchTimer=null,viewerList=[],viewerIdx=0;
const $=id=>document.getElementById(id);
function esc(s){return String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]))}
function toast(m){let t=document.createElement("div");t.className="toast";t.textContent=m;$("toasts").appendChild(t);setTimeout(()=>{t.style.opacity="0";setTimeout(()=>t.remove(),250)},2600)}
async function api(p,o={}){o.credentials="same-origin";o.headers=o.headers||{};if(o.body&&typeof o.body==="object"&&!(o.body instanceof FormData)){o.headers["Content-Type"]="application/json";o.body=JSON.stringify(o.body)}let r=await fetch(p,o);if(r.status===401){showLogin();throw new Error("unauthorized")}if(r.status===429){toast("Too many attempts — try again later");throw new Error("rate-limited")}return r}
/* ── boot / auth ── */
async function fetchStatus(){let r=await fetch("api/status",{credentials:"same-origin"});let j=await r.json();authNeeded=!!j.authRequired;authed=!!j.authenticated;serverName=j.name||"Pocket Server";document.title=serverName+" — Pocket Server";$("brandName").textContent=serverName;return j}
async function boot(){try{await fetchStatus()}catch(e){authNeeded=false;authed=true}if(!authNeeded||authed){showApp()}else{showLogin()}}
// Re-check auth state whenever the page regains focus: if auth was turned
// off in the app while this tab was open, enter directly instead of
// showing a stale login form.
document.addEventListener("visibilitychange",()=>{if(!document.hidden)refreshAuthState()});
window.addEventListener("focus",()=>refreshAuthState());
async function refreshAuthState(){try{await fetchStatus();if(!authNeeded||authed){if($("loginView").hidden===false)showApp();else{$("logoutBtn").hidden=!authNeeded}}else{showLogin()}}catch(_){}}
function showLogin(){$("app").hidden=true;$("bottomnav").hidden=true;$("loginView").hidden=false;$("loginSub").textContent="Sign in to "+serverName}
function showApp(){$("loginView").hidden=true;$("app").hidden=false;$("bottomnav").hidden=false;$("logoutBtn").hidden=!authNeeded;setView(view||"files")}
$("loginForm").addEventListener("submit",async e=>{e.preventDefault();let b=$("loginBtn");b.disabled=true;b.textContent="Signing in…";try{let r=await fetch("api/auth/login",{method:"POST",headers:{"Content-Type":"application/json"},credentials:"same-origin",body:JSON.stringify({username:$("u").value.trim(),password:$("p").value})});if(r.ok){authed=true;$("u").value="";$("p").value="";$("loginErr").textContent="";try{await fetchStatus()}catch(_){}showApp()}else if(r.status===429){let wait=r.headers.get("Retry-After");$("loginErr").textContent="Too many attempts. Please wait "+(wait?Math.ceil(wait/60)+" minute(s)":"a few minutes")+" and try again."}else{$("loginErr").textContent="Couldn't sign you in. Check the username and password (default is admin / admin)."}}catch(_){$("loginErr").textContent="Connection lost. Check the network and retry."}b.disabled=false;b.textContent="Sign in"});
$("logoutBtn").onclick=async()=>{try{await fetch("api/auth/logout",{method:"POST",credentials:"same-origin"})}catch(_){}location.reload()};
$("recheckBtn").onclick=async()=>{let b=$("recheckBtn");b.disabled=true;try{await fetchStatus();if(!authNeeded||authed){showApp()}else{$("loginErr").textContent="Authentication is still required by the server."}}catch(_){$("loginErr").textContent="Connection lost. Check the network and retry."}b.disabled=false};
/* ── navigation ── */
document.querySelectorAll("[data-view]").forEach(b=>b.onclick=()=>setView(b.dataset.view));
function setView(v){view=v;cur="";selected.clear();document.querySelectorAll("[data-view]").forEach(b=>{if(b.dataset.view===v)b.setAttribute("aria-current","page");else b.removeAttribute("aria-current")});$("crumbTitle").textContent={home:"Home",files:"Files",photos:"Photos",media:"Media"}[v]||"Files";load()}
/* ── crumbs / search / sort / view ── */
function crumbs(){let c=$("crumbs");c.innerHTML="";if(view!=="files"&&view!=="home"){return}let mk=(t,p,isCur)=>{let b=document.createElement("button");b.textContent=t;if(isCur)b.className="cur";b.onclick=()=>{cur=p;selected.clear();load()};c.appendChild(b)};mk(view==="home"?"Shared":"Files","",!cur);if(!cur)return;let parts=cur.split("/"),acc="";parts.forEach((s,i)=>{acc=acc?acc+"/"+s:s;let sp=document.createElement("span");sp.className="sep";sp.textContent="/";c.appendChild(sp);mk(s,acc,i===parts.length-1)})}
$("search").addEventListener("input",()=>{clearTimeout(searchTimer);searchTimer=setTimeout(load,220)});
$("sort").addEventListener("change",e=>{sort=e.target.value;load()});
$("viewToggle").onclick=()=>{grid=!grid;$("viewToggle").setAttribute("aria-pressed",grid);load()};
async function load(){crumbs();if(view==="photos"){await loadGallery("image")}else if(view==="media"){await loadGallery("video")}else{await loadFiles()}}
/* ── file explorer ── */
function kindOf(n,isDir){if(isDir)return["folder",""];let e=n.split(".").pop().toLowerCase();if(["jpg","jpeg","png","webp","gif","heic"].includes(e))return["img",e.toUpperCase()];if(["mp4","mkv","webm","mov"].includes(e))return["vid",e.toUpperCase()];if(["mp3","wav","flac","ogg"].includes(e))return["aud",e.toUpperCase()];if(e==="pdf")return["pdf","PDF"];if(["zip","rar","7z","apk"].includes(e))return["arc",e.toUpperCase()];return["",e?e.slice(0,4).toUpperCase():"···"]}
function fmt(b){if(b==null)return"—";if(b<1024)return b+" B";let u=["KB","MB","GB","TB"],v=b/1024,i=0;while(v>=1024&&i<3){v/=1024;i++}return v.toFixed(1)+" "+u[i]}
function fmtDate(t){if(!t)return"—";let d=new Date(t),now=new Date();if(d.toDateString()===now.toDateString())return"Today";let y=new Date(now-864e5);if(d.toDateString()===y.toDateString())return"Yesterday";return d.toLocaleDateString()}
function skeleton(){let z=$("listZone");z.innerHTML='<div class="sk" aria-hidden="true">'+Array(6).fill('<div class="skrow"><div class="skbox" style="width:38px;height:38px"></div><div style="flex:1"><div class="skbox" style="height:12px;width:45%;margin-bottom:6px"></div><div class="skbox" style="height:10px;width:25%"></div></div></div>').join("")+"</div>"}
function zoneEnter(){try{let z=$("listZone"),f=z.firstElementChild;if(!f||f.classList.contains("sk"))return;f.classList.remove("zone-enter");void f.offsetWidth;f.classList.add("zone-enter")}catch(_){}}
async function loadFiles(){let q=$("search").value.trim();skeleton();try{let url="api/files?path="+encodeURIComponent(cur);if(q)url+="&search="+encodeURIComponent(q);url+="&sort="+encodeURIComponent(sort);let r=await api(url);let j=await r.json();files=j.files||[];renderFiles()}catch(e){if(e.message!=="unauthorized")errorState("Couldn't load this folder.","Your device may have lost its connection.")}}
function errorState(t,s){$("listZone").innerHTML='<div class="state"><h3>'+esc(t)+'</h3><p>'+esc(s)+'</p><button class="btn btn-quiet" onclick="load()">Try again</button></div>';$("count").textContent=""}
function renderFiles(){let z=$("listZone");selected.forEach(p=>{if(!files.some(f=>f.path===p))selected.delete(p)});updateSelbar();
if(!files.length){let q=$("search").value.trim();z.innerHTML='<div class="state"><h3>'+(q?"No results":"No files here")+'</h3><p>'+(q?"Nothing matches your search.":"This folder is empty. Upload files to get started.")+'</p>'+(q?"":'<button class="btn btn-quiet" id="emptyUp">Upload files</button>')+'</div>';let b=$("emptyUp");if(b)b.onclick=()=>$("filePick").click();$("count").textContent="";zoneEnter();return}
if(grid){z.innerHTML='<div class="grid">'+files.map((f,i)=>{let k=kindOf(f.name,f.isDir);return '<div class="gcard'+(selected.has(f.path)?" selected":"")+'" data-i="'+i+'" tabindex="0" role="button" aria-label="'+esc(f.name)+'"><div class="fbadge '+(f.isDir?"folder":k[0])+'">'+(f.isDir?folderSvg():esc(k[1]))+'</div><div class="nm">'+esc(f.name)+'</div><div class="mt">'+(f.isDir?"Folder":fmt(f.size)+" · "+fmtDate(f.lastModified))+'</div></div>'}).join("")+"</div>"}else{z.innerHTML='<div class="list" role="list">'+files.map((f,i)=>{let k=kindOf(f.name,f.isDir);return '<div class="frow'+(selected.has(f.path)?" selected":"")+'" data-i="'+i+'" tabindex="0" role="listitem"><input type="checkbox" class="fcheck" data-c="'+i+'" '+(selected.has(f.path)?"checked":"")+' aria-label="Select '+esc(f.name)+'"/><div class="fbadge '+(f.isDir?"folder":k[0])+'">'+(f.isDir?folderSvg():esc(k[1]))+'</div><div class="meta"><div class="nm">'+esc(f.name)+'</div><div class="mt">'+(f.isDir?"Folder":fmt(f.size)+" · "+fmtDate(f.lastModified))+'</div></div><div class="r">'+(f.isDir?"":fmt(f.size))+'</div></div>'}).join("")+"</div>"}
$("count").textContent=files.length+" item"+(files.length===1?"":"s")+(cur?"":" · shared folders");zoneEnter()}
function folderSvg(){return '<svg width="18" height="18" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path d="M2 5.5A1.5 1.5 0 0 1 3.5 4h4l1.5 2h7A1.5 1.5 0 0 1 17.5 7.5v7a1.5 1.5 0 0 1-1.5 1.5H3.5A1.5 1.5 0 0 1 2 14.5v-9Z"/></svg>'}
/* ── row events: delegated, attached ONCE ── */
// Per-row listeners cost 2–5 addEventListener × N on EVERY render — a 1k-file
// folder paid thousands of installs per refresh. Delegation keeps render cost
// at string-building only; behavior is identical (open / select / menu /
// long-press details, gallery viewer).
function entryFromEvent(e){let el=e.target&&e.target.closest?e.target.closest("[data-i]"):null;if(!el||!$("listZone").contains(el))return null;let i=+el.dataset.i;if(view==="photos"||view==="media"){let g=viewerList[i];return g?{i:i,f:g,gallery:true}:null}let f=files[i];return f?{i:i,f:f,gallery:false}:null}
let pressTimer=null;
$("listZone").addEventListener("click",e=>{let cb=e.target&&e.target.closest?e.target.closest("[data-c]"):null;if(cb){e.stopPropagation();let f=files[+cb.dataset.c];if(!f)return;if(cb.checked)selected.add(f.path);else selected.delete(f.path);renderFiles();return}let r=entryFromEvent(e);if(!r)return;if(r.gallery){viewerIdx=r.i;showViewer()}else openEntry(r.f)});
$("listZone").addEventListener("keydown",e=>{if(e.key!=="Enter")return;let r=entryFromEvent(e);if(!r)return;if(r.gallery){viewerIdx=r.i;showViewer()}else openEntry(r.f)});
$("listZone").addEventListener("contextmenu",e=>{let r=entryFromEvent(e);if(r&&!r.gallery){e.preventDefault();ctxMenu(e.clientX,e.clientY,r.f)}});
$("listZone").addEventListener("touchstart",e=>{let r=entryFromEvent(e);if(!r||r.gallery)return;if(pressTimer)clearTimeout(pressTimer);let f=r.f;pressTimer=setTimeout(()=>detailsSheet(f),550)},{passive:true});
$("listZone").addEventListener("touchend",()=>{if(pressTimer){clearTimeout(pressTimer);pressTimer=null}});
function openEntry(f){if(f.isDir){cur=f.path;$("search").value="";selected.clear();load()}else{let e=f.name.split(".").pop().toLowerCase();if(["jpg","jpeg","png","webp","gif"].includes(e)||["mp4","webm","mov","mp3","wav","ogg"].includes(e)){viewerList=files.filter(x=>!x.isDir);viewerIdx=Math.max(0,viewerList.findIndex(x=>x.path===f.path));showViewer()}else{window.open("api/download?path="+encodeURIComponent(f.path),"_blank","noopener")}}}
function updateSelbar(){let b=$("selbar");if(!b){b=document.createElement("div");b.id="selbar";b.className="selbar";document.body.appendChild(b)}if(!selected.size){b.className="selbar";return}b.innerHTML='<span class="n">'+selected.size+' selected</span><button class="btn btn-quiet" id="selDl">Download</button><button class="btn btn-quiet" id="selDel" style="color:var(--err)">Delete</button><button class="iconbtn" id="selX" aria-label="Clear selection">✕</button>';b.className="selbar show";$("selDl").onclick=()=>{selected.forEach(p=>window.open("api/download?path="+encodeURIComponent(p),"_blank","noopener"));toast(selected.size+" download"+(selected.size>1?"s":"")+" started")};$("selDel").onclick=()=>confirmModal("Delete "+selected.size+" items?","This can't be undone.",async()=>{let n=0;for(let p of [...selected]){try{let r=await api("api/file?path="+encodeURIComponent(p),{method:"DELETE"});if(r.ok){n++;selected.delete(p)}}catch(_){}}toast(n+" deleted");load()});$("selX").onclick=()=>{selected.clear();renderFiles()}}
/* ── photos & media ── */
async function loadGallery(kind){skeleton();try{let r=await api("api/media?type="+kind+"&limit=300");let j=await r.json();let items=j.files||[];viewerList=items;let z=$("listZone");
if(!items.length){z.innerHTML='<div class="state"><h3>'+(kind==="image"?"No photos":"No media")+'</h3><p>'+(kind==="image"?"Shared images will appear here.":"Shared video and audio will appear here.")+'</p></div>';$("count").textContent="";zoneEnter();return}
if(kind==="image"){z.innerHTML='<div class="gallery">'+items.map((f,i)=>'<figure data-i="'+i+'" tabindex="0" role="button" aria-label="'+esc(f.name)+'"><img loading="lazy" decoding="async" src="api/download?path='+encodeURIComponent(f.path)+'" alt="'+esc(f.name)+'"/></figure>').join("")+"</div>"}else{z.innerHTML='<div class="list">'+items.map((f,i)=>{let k=kindOf(f.name,false);return '<div class="frow" data-i="'+i+'" tabindex="0"><div class="fbadge '+k[0]+'">'+esc(k[1]||"▶")+'</div><div class="meta"><div class="nm">'+esc(f.name)+'</div><div class="mt">'+esc(f.path)+' · '+fmt(f.size)+'</div></div></div>'}).join("")+"</div>"}
$("count").textContent=items.length+" "+(kind==="image"?"photos":"tracks");zoneEnter()}catch(e){if(e.message!=="unauthorized")errorState("Couldn't load media.","Check the connection and retry.")}}
/* ── viewer (lightbox) ── */
function showViewer(){let f=viewerList[viewerIdx];if(!f)return;let v=$("viewer"),st=$("vStage");$("vName").textContent=f.name;let url="api/download?path="+encodeURIComponent(f.path);let e=f.name.split(".").pop().toLowerCase();st.innerHTML="";if(["jpg","jpeg","png","webp","gif"].includes(e)){let im=document.createElement("img");im.src=url;im.alt=f.name;st.appendChild(im)}else if(["mp4","webm","mov"].includes(e)){let vd=document.createElement("video");vd.src=url;vd.controls=true;vd.autoplay=true;st.appendChild(vd)}else{let ad=document.createElement("audio");ad.src=url;ad.controls=true;ad.autoplay=true;st.appendChild(ad)}$("vPrev").style.visibility=viewerList.length>1?"visible":"hidden";$("vNext").style.visibility=viewerList.length>1?"visible":"hidden";v.classList.add("show");document.body.style.overflow="hidden"}
function hideViewer(){$("viewer").classList.remove("show");$("vStage").innerHTML="";document.body.style.overflow=""}
$("vClose").onclick=hideViewer;$("viewer").addEventListener("click",e=>{if(e.target.id==="viewer")hideViewer()});
$("vPrev").onclick=e=>{e.stopPropagation();viewerIdx=(viewerIdx-1+viewerList.length)%viewerList.length;showViewer()};
$("vNext").onclick=e=>{e.stopPropagation();viewerIdx=(viewerIdx+1)%viewerList.length;showViewer()};
$("vDl").onclick=()=>{let f=viewerList[viewerIdx];if(f)window.open("api/download?path="+encodeURIComponent(f.path),"_blank","noopener")};
document.addEventListener("keydown",e=>{if(!$("viewer").classList.contains("show"))return;if(e.key==="Escape")hideViewer();if(e.key==="ArrowLeft")$("vPrev").click();if(e.key==="ArrowRight")$("vNext").click()});
/* ── context menu / details ── */
function hideCtx(){$("ctxmenu").classList.remove("show")}
document.addEventListener("click",hideCtx);
function ctxMenu(x,y,f){let m=$("ctxmenu");m.innerHTML="";let items=f.isDir?[["Open",()=>openEntry(f)]]:[["Open",()=>openEntry(f)],["Download",()=>window.open("api/download?path="+encodeURIComponent(f.path),"_blank","noopener")]];items.push(["Details",()=>detailsSheet(f)],["Rename",()=>renameModal(f)],["Delete",()=>deleteModal(f),true]);items.forEach(([label,fn,danger])=>{let b=document.createElement("button");if(danger)b.className="danger";b.textContent=label;b.setAttribute("role","menuitem");b.onclick=()=>{hideCtx();fn()};m.appendChild(b)});m.classList.add("show");let r=m.getBoundingClientRect();m.style.left=Math.min(x,innerWidth-r.width-8)+"px";m.style.top=Math.min(y,innerHeight-r.height-8)+"px"}
function detailsSheet(f){let size=f.isDir?"—":fmt(f.size);let mod=f.lastModified?new Date(f.lastModified).toLocaleString():"—";openModal({title:esc(f.name),sub:(f.isDir?"Folder":kindLabel(f.name)+" · "+fmt(f.size)),body:'<dl class="kv"><dt>Location</dt><dd class="mono">'+esc(f.path)+'</dd><dt>Modified</dt><dd>'+esc(mod)+'</dd></dl>',actions:f.isDir?[{label:"Open",primary:true,fn:()=>openEntry(f)},{label:"Rename",fn:()=>renameModal(f)},{label:"Delete",danger:true,fn:()=>deleteModal(f)}]:[{label:"Download",primary:true,fn:()=>window.open("api/download?path="+encodeURIComponent(f.path),"_blank","noopener")},{label:"Rename",fn:()=>renameModal(f)},{label:"Delete",danger:true,fn:()=>deleteModal(f)}]})}
function kindLabel(n){let e=n.split(".").pop().toLowerCase();if(["jpg","jpeg","png","webp","gif","heic"].includes(e))return"Image";if(["mp4","mkv","webm","mov"].includes(e))return"Video";if(["mp3","wav","flac","ogg"].includes(e))return"Audio";if(e==="pdf")return"PDF";return"File"}
/* ── modal ── */
function openModal(o){let w=$("modalWrap"),m=$("modal");m.innerHTML="<h3>"+o.title+"</h3>"+(o.sub?'<div class="sub">'+o.sub+"</div>":"")+(o.input?'<input id="mInput" value="'+esc(o.input)+'"/>':"")+(o.body||"");let bar=document.createElement("div");bar.className="actions";(o.actions||[{label:"Close",primary:true}]).forEach(a=>{let b=document.createElement("button");b.className="btn "+(a.primary?"btn-primary":"btn-quiet");if(a.danger)b.style.color="var(--err)";b.textContent=a.label;b.onclick=()=>{closeModal();if(a.fn)a.fn()};bar.appendChild(b)});m.appendChild(bar);w.classList.add("show");let inp=$("mInput");if(inp){inp.focus();inp.select()}}
function closeModal(){$("modalWrap").classList.remove("show")}
$("modalWrap").addEventListener("click",e=>{if(e.target.id==="modalWrap")closeModal()});
document.addEventListener("keydown",e=>{if(e.key==="Escape"){closeModal();hideCtx()}});
function renameModal(f){openModal({title:"Rename",sub:esc(f.name),input:f.name,actions:[{label:"Cancel"},{label:"Rename",primary:true,fn:async()=>{let v=$("mInput").value.trim();if(!v||v===f.name)return;try{let r=await api("api/file",{method:"PUT",body:{path:f.path,newName:v}});if(r.ok){toast("Renamed");load()}else toast("Rename failed")}catch(_){toast("Rename failed")}}}]})}
function deleteModal(f){confirmModal("Delete “"+f.name+"”?",f.isDir?"The folder and everything in it will be removed.":"This file will be permanently removed.",async()=>{try{let r=await api("api/file?path="+encodeURIComponent(f.path),{method:"DELETE"});if(r.ok){toast("Deleted");selected.delete(f.path);load()}else toast("Delete failed")}catch(_){toast("Delete failed")}})}
function confirmModal(t,s,fn){openModal({title:esc(t),sub:esc(s),actions:[{label:"Cancel"},{label:"Delete",primary:true,fn}]})}
/* ── folder create ── */
$("mkdirBtn").onclick=()=>{if(view!=="files"&&view!=="home"){toast("Open Files to create a folder");return}openModal({title:"New folder",sub:cur?"In "+cur:"At the top level of a shared folder — open one first."+(cur?"":""),input:"",actions:[{label:"Cancel"},{label:"Create",primary:true,fn:async()=>{let v=$("mInput").value.trim();if(!v)return;try{let r=await api("api/folder",{method:"POST",body:{path:cur,name:v}});if(r.status===201){toast("Folder created");load()}else if(r.status===409)toast("A folder with that name exists");else toast("Couldn't create folder")}catch(_){toast("Couldn't create folder")}}}]})};
/* ── upload: picker, drop overlay, drawer with progress ── */
$("upBtn").onclick=()=>$("filePick").click();$("filePick").onchange=()=>{uploadFiles($("filePick").files);$("filePick").value=""};
let dragDepth=0;
window.addEventListener("dragenter",e=>{if(![...e.dataTransfer.types].includes("Files"))return;e.preventDefault();dragDepth++;$("dropOverlay").classList.add("show")});
window.addEventListener("dragleave",e=>{e.preventDefault();if(--dragDepth<=0){dragDepth=0;$("dropOverlay").classList.remove("show")}});
window.addEventListener("dragover",e=>e.preventDefault());
window.addEventListener("drop",e=>{e.preventDefault();dragDepth=0;$("dropOverlay").classList.remove("show");if(e.dataTransfer.files.length)uploadFiles(e.dataTransfer.files)});
function uploadFiles(list){if(!list.length)return;if(view!=="files"&&view!=="home"){toast("Open Files to choose an upload folder")}if(!cur){toast("Open a shared folder first, then upload");return}$("upDrawer").classList.add("show");$("upTitle").textContent="Uploading to "+(cur.split("/").pop()||"files");[...list].forEach(f=>uploadOne(f))}
function uploadOne(f){let id="u"+Math.random().toString(36).slice(2);let row=document.createElement("div");row.className="upitem";row.id=id;row.innerHTML='<div class="t"><span>'+esc(f.name)+'</span><span class="st">Waiting…</span></div><div class="bar"><i style="transform:scaleX(0)"></i></div>';$("upList").prepend(row);let xhr=new XMLHttpRequest();xhr.open("POST","api/upload?path="+encodeURIComponent(cur));xhr.upload.onprogress=e=>{if(e.lengthComputable){let p=Math.round(e.loaded/e.total*100);row.querySelector("i").style.transform="scaleX("+(p/100)+")";row.querySelector(".st").textContent=fmt(e.loaded)+" / "+fmt(e.total)}};xhr.onload=()=>{if(xhr.status===201){row.classList.add("done");row.querySelector("i").style.transform="scaleX(1)";row.querySelector(".st").textContent="Done ✓";toast("Uploaded "+f.name);load()}else{row.classList.add("err");row.querySelector(".st").textContent="Failed ("+xhr.status+")"}};xhr.onerror=()=>{row.classList.add("err");row.querySelector(".st").textContent="Interrupted"};let fd=new FormData();fd.append("files",f,f.name);xhr.send(fd);row.dataset.xhr="1";row._xhr=xhr}
$("upClose").onclick=()=>$("upDrawer").classList.remove("show");
/* ── connection info ── */
$("connBtn").onclick=async()=>{let info={name:serverName};try{let r=await fetch("api/status",{credentials:"same-origin"});info=await r.json()}catch(_){}openModal({title:"Connection",sub:"Pocket Server",body:'<dl class="kv"><dt>Server</dt><dd>'+esc(info.name||serverName)+'</dd><dt>Address</dt><dd class="mono">'+esc(location.host)+'</dd><dt>Protocol</dt><dd>HTTP · local network</dd><dt>Status</dt><dd>● Connected</dd></dl>',actions:[{label:"Close",primary:true}]})};
boot();
</script>
</body>
</html>
    """.trimIndent()

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;")
}

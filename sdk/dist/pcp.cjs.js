"use strict";const x={apiUrl:"",wsUrl:"",tenantId:null,apiKey:null,currency:"USD",locale:"en",theme:{primaryColor:"#6366f1",primaryTextColor:"#ffffff",fontFamily:"inherit",borderRadius:"8px"},mockMode:!1};let p={...x},$=!1;function v(e={}){const t=typeof window<"u"&&window.PCPConfig||{};return p={...x,...t,...e,theme:{...x.theme,...t.theme||{},...e.theme||{}}},p.apiUrl||(p.mockMode=!0),I(p.theme),$=!0,p}function u(){return $||v(),p}function I(e){if(typeof document>"u")return;const t=document.documentElement;t.style.setProperty("--pcp-primary-color",e.primaryColor),t.style.setProperty("--pcp-primary-text-color",e.primaryTextColor),t.style.setProperty("--pcp-font-family",e.fontFamily),t.style.setProperty("--pcp-border-radius",e.borderRadius)}function G(){return u().mockMode===!0}async function V(){var t,n,a;const e=u();if(!e.tenantId||e.mockMode)return e;try{const r=await fetch(`${e.apiUrl.replace(/\/$/,"")}/api/v1/tenants/${e.tenantId}/config`);if(!r.ok)return e;const o=await r.json(),i=(o==null?void 0:o.data)||{},c=typeof window<"u"&&((t=window.PCPConfig)==null?void 0:t.theme)||{};p={...p,currency:((n=window.PCPConfig)==null?void 0:n.currency)||i.currency||p.currency,locale:((a=window.PCPConfig)==null?void 0:a.locale)||i.locale||p.locale,features:i.features||{},theme:{...p.theme,...i.theme||{},...c}},I(p.theme)}catch{}return p}const b="pcp_token",h="pcp_refresh",d={getToken(){return D(b)},setToken(e){P(b,e)},getRefresh(){return D(h)},setRefresh(e){P(h,e)},clear(){A(b),A(h)},isAuthenticated(){return!!this.getToken()},getRoles(){const e=this.getToken();if(!e)return[];try{const t=JSON.parse(atob(e.split(".")[1])),n=t.roles||t.authorities||[];return Array.isArray(n)?n.map(a=>String(a).toLowerCase()):[]}catch{return[]}},getUserId(){const e=this.getToken();if(!e)return null;try{const t=JSON.parse(atob(e.split(".")[1]));return t.sub||t.userId||null}catch{return null}},headers(){const e=this.getToken();return e?{Authorization:`Bearer ${e}`}:{}}};function D(e){try{return localStorage.getItem(e)}catch{return null}}function P(e,t){try{localStorage.setItem(e,t)}catch{}}function A(e){try{localStorage.removeItem(e)}catch{}}function K(e,t={}){return e.includes("/auctions")&&!e.includes("/questions")?Promise.resolve(W(e)):e.includes("/properties/search")||e.includes("/properties?")?Promise.resolve(X()):Promise.resolve({success:!0,data:null})}function W(e){const t=["Oakwood Residences","The Commercial Quarter","Riverside Business Park","Harbour View Apartments","The Merchant Centre","Estate at Millfield"],n=["London","New York","Sydney","Cape Town","Dubai","Singapore"],a=["OPEN","SCHEDULED","OPEN","EXTENDED","SCHEDULED","CLOSED"],r=t.map((c,s)=>({id:`mock-lot-${s+1}`,title:c,propertyId:`mock-prop-${s+1}`,status:a[s],auctionType:["ENGLISH","DUTCH","ENGLISH","SEALED_BID","ENGLISH","ENGLISH"][s],startingPrice:[45e4,82e4,31e4,59e4,12e5,275e3][s],currentBidAmount:a[s]==="OPEN"||a[s]==="EXTENDED"?[475e3,0,34e4,615e3,0,0][s]:null,winningAmount:a[s]==="CLOSED"?29e4:null,currency:"USD",uniqueBidders:[4,0,2,7,0,0][s],totalBids:[11,0,5,19,0,3][s],startsAt:new Date(Date.now()+(s-2)*864e5).toISOString(),scheduledEndsAt:new Date(Date.now()+(s%2===0?1:-1)*36e5+65e3).toISOString(),location:{city:n[s]}}));let o=r;const i=e.match(/status=([^&]+)/);if(i){const c=decodeURIComponent(i[1]).split(",");o=r.filter(s=>c.includes(s.status))}return{success:!0,data:{content:o,totalElements:o.length,totalPages:1,page:0,size:12}}}function X(){return{success:!0,data:{content:[1,2,3,4,5,6].map(e=>({id:`mock-${e}`,title:["Oakwood Residences","The Commercial Quarter","Riverside Business Park","Harbour View Apartments","The Merchant Centre","Estate at Millfield"][e-1],location:{city:["London","New York","Sydney","Cape Town","Dubai","Singapore"][e-1]},pricing:{currentDynamicRate:[85e4,12e5,475e3,62e4,98e4,32e4][e-1],currency:"USD"},bedrooms:[2,3,4,1,3,2][e-1],maxGuests:[4,6,8,2,6,4][e-1],averageRating:[4.9,4.7,4.8,4.6,4.5,4.4][e-1],totalReviews:[42,28,61,15,19,8][e-1],imageUrls:[],status:"ACTIVE"})),totalElements:6,page:0,size:6}}}let m=null;async function S(e,t={}){const n=u();if(G())return K(e,t);const a=n.apiUrl.replace(/\/$/,"")+e,r=await fetch(a,{...t,headers:J(n,t)});if(r.status===401){if(await Z(n))return S(e,t);d.clear(),Q()}const o=await r.json().catch(()=>({}));if(!r.ok){const i=new Error(o.message||`API error ${r.status}`);throw i.status=r.status,i.data=o,i}return o}function J(e,t){const n={"Content-Type":"application/json",...e.tenantId?{"X-Tenant-Id":e.tenantId}:{},...d.headers(),...t.headers||{}};return!d.getToken()&&e.apiKey&&(n["X-Api-Key"]=e.apiKey),n}async function Z(e){const t=d.getRefresh();return t?(m||(m=fetch(e.apiUrl.replace(/\/$/,"")+"/api/v1/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:t})}).then(n=>n.json()).then(n=>{var a;return n.success&&((a=n.data)!=null&&a.accessToken)?(d.setToken(n.data.accessToken),d.setRefresh(n.data.refreshToken),!0):!1}).catch(()=>!1).finally(()=>{m=null})),m):!1}function Q(){typeof document>"u"||document.dispatchEvent(new CustomEvent("pcp:auth-expired"))}let l=null;function ee(){return l&&document.body.contains(l)||(l=document.createElement("div"),l.id="pcp-toast-container",l.setAttribute("aria-live","polite"),l.style.cssText=["position:fixed","bottom:20px","right:20px","z-index:99999","display:flex","flex-direction:column","gap:8px","font-family:var(--pcp-font-family, inherit)"].join(";"),document.body.appendChild(l)),l}function N(e,t="default",n=4e3){if(typeof document>"u")return;const a=ee(),r=document.createElement("div");r.setAttribute("role","status"),r.textContent=e;const o={default:{bg:"#1f2937",fg:"#ffffff"},success:{bg:"#15803d",fg:"#ffffff"},error:{bg:"#b91c1c",fg:"#ffffff"},warning:{bg:"#92400e",fg:"#ffffff"}}[t]||{bg:"#1f2937",fg:"#ffffff"};r.style.cssText=[`background:${o.bg}`,`color:${o.fg}`,"padding:10px 16px","border-radius:var(--pcp-border-radius, 8px)","font-size:14px","box-shadow:0 4px 16px rgba(0,0,0,.2)","max-width:320px","animation:pcp-toast-in .2s ease"].join(";"),a.appendChild(r),setTimeout(()=>r.remove(),n)}function te(e){return String(e||"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}const g=new Map,w=new Map;function R(e,t){if(typeof(t==null?void 0:t.mount)!="function")throw new Error(`PCP widget "${e}" must export a mount(el, props, ctx) function`);g.set(e,t)}function ne(e,t,n={},a){const r=g.get(e);if(!r)return console.error(`[PCP] Unknown widget "${e}". Registered widgets: ${[...g.keys()].join(", ")||"(none loaded)"}`),null;const o=typeof t=="string"?document.querySelector(t):t;if(!o)return console.error(`[PCP] mount target not found: ${t}`),null;E(o);const i=r.mount(o,n,a),c=o;return w.set(c,{name:e,cleanup:i}),o.dataset.pcpWidget=e,{unmount:()=>E(o)}}function ae(e){const t=typeof e=="string"?document.querySelector(e):e;t&&E(t)}function E(e){const t=w.get(e);if(t){if(typeof t.cleanup=="function")try{t.cleanup()}catch(n){console.warn(`[PCP] Cleanup error for widget "${t.name}":`,n)}w.delete(e),delete e.dataset.pcpWidget,e.innerHTML=""}}function re(){return[...g.keys()]}const U={USD:{symbol:"$",name:"US Dollar"},EUR:{symbol:"€",name:"Euro"},GBP:{symbol:"£",name:"British Pound"},JPY:{symbol:"¥",name:"Japanese Yen"},AUD:{symbol:"A$",name:"Australian Dollar"},CAD:{symbol:"C$",name:"Canadian Dollar"},CHF:{symbol:"Fr",name:"Swiss Franc"},CNY:{symbol:"¥",name:"Chinese Yuan"},INR:{symbol:"₹",name:"Indian Rupee"},ZAR:{symbol:"R",name:"South African Rand"},AED:{symbol:"د.إ",name:"UAE Dirham"}},z={en:"en-US",fr:"fr-FR",es:"es-ES",de:"de-DE",pt:"pt-BR",ar:"ar-SA",zh:"zh-CN"};function H(e){var t;return((t=U[e])==null?void 0:t.symbol)||e}function oe(e){const{currency:t,locale:n}=u(),a=z[n]||"en-US";try{return new Intl.NumberFormat(a,{style:"currency",currency:t,maximumFractionDigits:0}).format(Number(e)||0)}catch{return H(t)+Number(e||0).toLocaleString()}}function ie(e,t={}){if(!e)return"";const{locale:n}=u(),a=z[n]||"en-US",r=t.timezone||Intl.DateTimeFormat().resolvedOptions().timeZone;try{return new Intl.DateTimeFormat(a,{timeZone:r,day:"numeric",month:"short",hour:t.withTime?"2-digit":void 0,minute:t.withTime?"2-digit":void 0,year:t.withYear!==!1?"numeric":void 0}).format(new Date(e))}catch{return new Date(e).toLocaleString()}}const M=Object.freeze(Object.defineProperty({__proto__:null,CURRENCIES:U,currencySymbol:H,fmt:oe,fmtDate:ie},Symbol.toStringTag,{value:"Module"}));v();function se(){return{api:S,auth:d,i18n:M,toast:N,escapeHtml:te,config:u()}}const O={init(e){return v(e),V()},config:u,mount(e,t,n){return ne(e,t,n,se())},unmount:ae,registerWidget:R,listWidgets:re,api:S,auth:d,i18n:M,toast:N,version:"2.0.0"};typeof window<"u"&&(window.PCP=O,document.dispatchEvent(new CustomEvent("pcp:ready")));const _=new Set;function ce(e,t){if(_.has(e)||typeof document<"u"&&document.getElementById(e)){_.add(e);return}if(typeof document>"u")return;const n=document.createElement("style");n.id=e,n.textContent=t,document.head.appendChild(n),_.add(e)}const pe=`
.pcp-auction-listing {
  font-family: var(--pcp-font-family, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
  box-sizing: border-box;
}
.pcp-auction-listing *, .pcp-auction-listing *::before, .pcp-auction-listing *::after {
  box-sizing: inherit;
}

/* Tabs */
.pcp-al__tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1.5px solid #e5e7eb; }
.pcp-al__tab {
  padding: 8px 16px; border: none; background: none; cursor: pointer;
  font-size: .9375rem; font-weight: 600; color: #6b7280;
  border-bottom: 2.5px solid transparent; margin-bottom: -1.5px;
}
.pcp-al__tab--active { color: var(--pcp-primary-color, #6366f1); border-bottom-color: var(--pcp-primary-color, #6366f1); }

/* Grid */
.pcp-al__grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.pcp-al__empty, .pcp-al__error { color: #6b7280; padding: 40px 0; text-align: center; grid-column: 1/-1; }

/* Card */
.pcp-al__card {
  border: 1.5px solid #e5e7eb; border-radius: var(--pcp-border-radius, 8px);
  overflow: hidden; background: #fff; display: flex; flex-direction: column;
  transition: box-shadow .15s, border-color .15s;
}
.pcp-al__card:hover { border-color: var(--pcp-primary-color, #6366f1); box-shadow: 0 4px 20px rgba(0,0,0,.08); }
.pcp-al__card--closed { opacity: .75; }
.pcp-al__img-link { display: block; text-decoration: none; }
.pcp-al__img-wrap { position: relative; aspect-ratio: 3/2; background: #f3f4f6; overflow: hidden; }
.pcp-al__img-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.pcp-al__img-placeholder { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 2rem; }
.pcp-al__type-badge {
  position: absolute; top: 10px; left: 10px; font-size: .75rem; font-weight: 700;
  padding: 3px 9px; border-radius: 20px; background: rgba(255,255,255,.92); color: #111;
}
.pcp-al__live-badge {
  position: absolute; top: 10px; right: 10px; font-size: .75rem; font-weight: 700;
  padding: 3px 9px; border-radius: 20px; background: #dcfce7; color: #15803d;
  display: flex; align-items: center; gap: 4px;
}
.pcp-al__live-dot { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; animation: pcp-pulse 1.5s infinite; }
@keyframes pcp-pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
@keyframes pcp-toast-in { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:none} }

.pcp-al__body { padding: 14px 16px; flex: 1; display: flex; flex-direction: column; gap: 6px; }
.pcp-al__location { font-size: .8125rem; color: #6b7280; margin: 0; }
.pcp-al__title { font-size: 1rem; font-weight: 700; margin: 0; line-height: 1.3; }
.pcp-al__title a { color: inherit; text-decoration: none; }

/* Countdown */
.pcp-al__countdown { display: flex; align-items: center; gap: 3px; font-variant-numeric: tabular-nums; margin: 4px 0; }
.pcp-al__cd-seg { display: flex; flex-direction: column; align-items: center; line-height: 1; }
.pcp-al__cd-seg b { font-size: .875rem; font-weight: 800; }
.pcp-al__cd-seg small { font-size: .5625rem; color: #9ca3af; text-transform: uppercase; }
.pcp-al__countdown i { font-style: normal; color: #9ca3af; margin: 0 1px; }
.pcp-al__countdown--urgent .pcp-al__cd-seg b { color: #ef4444; }
.pcp-al__timing { font-size: .8125rem; color: #6b7280; margin: 4px 0; }

.pcp-al__price-row { display: flex; align-items: flex-end; justify-content: space-between; margin-top: auto; padding-top: 8px; }
.pcp-al__price-label { font-size: .6875rem; color: #9ca3af; text-transform: uppercase; letter-spacing: .4px; display: block; }
.pcp-al__price { font-size: 1.125rem; font-weight: 800; color: var(--pcp-primary-color, #6366f1); margin: 2px 0 0; }
.pcp-al__bids { text-align: right; font-size: .8125rem; color: #6b7280; }
.pcp-al__bids span { display: block; font-weight: 700; font-size: .9375rem; color: #111; }

/* Buttons */
.pcp-btn {
  display: inline-block; padding: 9px 16px; border-radius: var(--pcp-border-radius, 8px);
  font-size: .875rem; font-weight: 700; text-decoration: none; text-align: center;
  margin-top: 10px; border: 1.5px solid transparent; cursor: pointer;
}
.pcp-btn--primary { background: var(--pcp-primary-color, #6366f1); color: var(--pcp-primary-text-color, #fff); }
.pcp-btn--ghost { background: none; border-color: #d1d5db; color: #374151; }
.pcp-btn:disabled { opacity: .4; cursor: not-allowed; }

/* Pagination */
.pcp-al__pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 24px; font-size: .875rem; }

/* Skeleton */
.pcp-skel { background: linear-gradient(90deg,#eee 25%,#f5f5f5 37%,#eee 63%); background-size: 400% 100%; animation: pcp-shimmer 1.4s ease infinite; border-radius: 4px; }
.pcp-skel--img { aspect-ratio: 3/2; }
.pcp-skel--line { height: 14px; margin: 10px 16px 0; }
.pcp-skel--line.short { width: 60%; }
@keyframes pcp-shimmer { 0%{background-position:100% 50%} 100%{background-position:0 50%} }
`,le={ENGLISH:"📈",DUTCH:"📉",REVERSE:"🔄",SEALED_BID:"🔒"},de={ENGLISH:"English",DUTCH:"Dutch",REVERSE:"Reverse",SEALED_BID:"Sealed"},ue={mount(e,t,n){ce("pcp-auction-listing-styles",pe);const a={statuses:t.statuses||["OPEN","EXTENDED"],type:t.type||null,page:0,totalPages:0,timers:[],roomUrlTemplate:t.roomUrl||"#lot={lotId}"};return e.className=(e.className?e.className+" ":"")+"pcp-auction-listing",e.innerHTML=fe(),me(e,a,n),C(e,a,n),()=>{a.timers.forEach(r=>clearInterval(r))}}};function fe(){return`
    <div class="pcp-al__tabs" role="tablist" aria-label="Auction status">
      <button class="pcp-al__tab pcp-al__tab--active" data-statuses="OPEN,EXTENDED" role="tab" aria-selected="true">Live</button>
      <button class="pcp-al__tab" data-statuses="SCHEDULED" role="tab" aria-selected="false">Upcoming</button>
      <button class="pcp-al__tab" data-statuses="CLOSED,SETTLED,NO_RESERVE" role="tab" aria-selected="false">Closed</button>
    </div>
    <div class="pcp-al__grid" aria-live="polite"></div>
    <div class="pcp-al__pagination"></div>
  `}function me(e,t,n){e.querySelectorAll(".pcp-al__tab").forEach(a=>{a.addEventListener("click",()=>{e.querySelectorAll(".pcp-al__tab").forEach(r=>{r.classList.remove("pcp-al__tab--active"),r.setAttribute("aria-selected","false")}),a.classList.add("pcp-al__tab--active"),a.setAttribute("aria-selected","true"),t.statuses=a.dataset.statuses.split(","),t.page=0,C(e,t,n)})})}async function C(e,t,n){var r,o;const a=e.querySelector(".pcp-al__grid");a.innerHTML=ge(),t.timers.forEach(i=>clearInterval(i)),t.timers=[];try{const i=new URLSearchParams({status:t.statuses.join(","),page:t.page,size:12,...t.type?{type:t.type}:{}}),c=await n.api(`/api/v1/auctions?${i}`),s=((r=c==null?void 0:c.data)==null?void 0:r.content)||[];if(t.totalPages=((o=c==null?void 0:c.data)==null?void 0:o.totalPages)||1,!s.length){a.innerHTML='<p class="pcp-al__empty">No auctions found for this filter.</p>',L(e,t,n);return}a.innerHTML=s.map(f=>ye(f,n,t)).join(""),be(a,t),L(e,t,n)}catch{a.innerHTML='<p class="pcp-al__error">Could not load auctions. Please try again.</p>'}}function ge(){return Array.from({length:6}).map(()=>`
      <div class="pcp-al__card pcp-al__card--skeleton" aria-hidden="true">
        <div class="pcp-skel pcp-skel--img"></div>
        <div class="pcp-skel pcp-skel--line"></div>
        <div class="pcp-skel pcp-skel--line short"></div>
      </div>`).join("")}function ye(e,t,n){var k;const{escapeHtml:a,i18n:r}=t,o=e.status==="OPEN"||e.status==="EXTENDED",i=["CLOSED","SETTLED","NO_RESERVE","CANCELLED"].includes(e.status),c=o&&e.currentBidAmount?e.currentBidAmount:e.startingPrice,s=i&&e.winningAmount?e.winningAmount:c,f=o?"Current bid":i?"Winning bid":"Starting",y=n.roomUrlTemplate.replace("{lotId}",e.id),F=o?'<span class="pcp-al__live-badge"><span class="pcp-al__live-dot"></span>Live</span>':"",B=o&&e.scheduledEndsAt?`<div class="pcp-al__countdown" data-ends-at="${e.scheduledEndsAt}" role="timer" aria-label="Time remaining">
           ${["d","h","m","s"].map((Y,T)=>`
             <span class="pcp-al__cd-seg"><b data-part="${Y}">--</b><small>${["d","h","m","s"][T]}</small></span>${T<3?"<i>:</i>":""}`).join("")}
         </div>`:e.startsAt?`<p class="pcp-al__timing">Starts ${r.fmtDate(e.startsAt,{withTime:!0})}</p>`:"",j=o?"Bid now":i?"View results":"View lot",q=o?"pcp-btn--primary":"pcp-btn--ghost";return`
    <article class="pcp-al__card${i?" pcp-al__card--closed":""}" data-lot-id="${a(e.id)}">
      <a href="${y}" class="pcp-al__img-link" tabindex="-1" aria-hidden="true">
        <div class="pcp-al__img-wrap">
          ${e.firstImageUrl?`<img src="${a(e.firstImageUrl)}" alt="${a(e.title)}" loading="lazy">`:'<div class="pcp-al__img-placeholder">🏠</div>'}
          <span class="pcp-al__type-badge">${le[e.auctionType]||"🏷"} ${de[e.auctionType]||e.auctionType}</span>
          ${F}
        </div>
      </a>
      <div class="pcp-al__body">
        <p class="pcp-al__location">📍 ${a(((k=e.location)==null?void 0:k.city)||e.propertyCity||"")}</p>
        <h3 class="pcp-al__title"><a href="${y}">${a(e.title)}</a></h3>
        ${B}
        <div class="pcp-al__price-row">
          <div>
            <span class="pcp-al__price-label">${f}</span>
            <p class="pcp-al__price">${r.fmt(s||0)}</p>
          </div>
          <div class="pcp-al__bids">
            <span>${e.totalBids||0}</span><small>bids</small>
          </div>
        </div>
        <a href="${y}" class="pcp-btn ${q}">${j}</a>
      </div>
    </article>`}function be(e,t){e.querySelectorAll(".pcp-al__countdown[data-ends-at]").forEach(n=>{const a=new Date(n.dataset.endsAt).getTime();function r(){const o=a-Date.now(),i=(c,s)=>{const f=n.querySelector(`[data-part="${c}"]`);f&&(f.textContent=String(Math.max(0,s)).padStart(2,"0"))};if(o<=0){["d","h","m","s"].forEach(c=>i(c,0)),n.classList.add("pcp-al__countdown--expired");return}i("d",Math.floor(o/864e5)),i("h",Math.floor(o%864e5/36e5)),i("m",Math.floor(o%36e5/6e4)),i("s",Math.floor(o%6e4/1e3)),n.classList.toggle("pcp-al__countdown--urgent",o<3e5)}r(),t.timers.push(setInterval(r,1e3))})}function L(e,t,n){const a=e.querySelector(".pcp-al__pagination");if(t.totalPages<=1){a.innerHTML="";return}a.innerHTML=`
    <button class="pcp-btn pcp-btn--ghost" ${t.page===0?"disabled":""} data-dir="-1">← Prev</button>
    <span>Page ${t.page+1} of ${t.totalPages}</span>
    <button class="pcp-btn pcp-btn--ghost" ${t.page>=t.totalPages-1?"disabled":""} data-dir="1">Next →</button>
  `,a.querySelectorAll("button[data-dir]").forEach(r=>{r.addEventListener("click",()=>{t.page+=parseInt(r.dataset.dir,10),C(e,t,n)})})}R("auction-listing",ue);module.exports=O;
//# sourceMappingURL=pcp.cjs.js.map

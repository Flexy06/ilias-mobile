package de.flexy06.ilias

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

/**
 * Re-skins ILIAS pages so they look like part of the app: Material You colors from the
 * current (dynamic) color scheme, system font, rounded cards, tonal buttons, M3 tabs.
 */
object PageStyle {

    data class Theme(val vars: String, val dark: Boolean)

    fun theme(c: ColorScheme, dark: Boolean): Theme {
        fun Color.css(): String = String.format("#%06X", toArgb() and 0xFFFFFF)
        val vars = buildString {
            append(":root{")
            append("color-scheme:${if (dark) "dark" else "light"};")
            append("--md-bg:${c.surface.css()};")
            append("--md-on-bg:${c.onSurface.css()};")
            append("--md-on-var:${c.onSurfaceVariant.css()};")
            append("--md-s-low:${c.surfaceContainerLow.css()};")
            append("--md-s:${c.surfaceContainer.css()};")
            append("--md-s-high:${c.surfaceContainerHigh.css()};")
            append("--md-s-highest:${c.surfaceContainerHighest.css()};")
            append("--md-primary:${c.primary.css()};")
            append("--md-on-primary:${c.onPrimary.css()};")
            append("--md-pc:${c.primaryContainer.css()};")
            append("--md-on-pc:${c.onPrimaryContainer.css()};")
            append("--md-sc:${c.secondaryContainer.css()};")
            append("--md-on-sc:${c.onSecondaryContainer.css()};")
            append("--md-tc:${c.tertiaryContainer.css()};")
            append("--md-on-tc:${c.onTertiaryContainer.css()};")
            append("--md-ec:${c.errorContainer.css()};")
            append("--md-on-ec:${c.onErrorContainer.css()};")
            append("--md-outline:${c.outline.css()};")
            append("--md-outline-var:${c.outlineVariant.css()};")
            append("--md-icon-filter:${if (dark) "invert(1) hue-rotate(180deg) brightness(.92)" else "none"};")
            append("}")
        }
        return Theme(vars, dark)
    }

    /** Hides ILIAS chrome that the native app bar / bottom bar replace. */
    private const val COMPACT_CSS = """
.il-mainbar,.il-mainbar-slates,.il-mainbar-remove-tool,.il-mainbar-tools-button{display:none!important}
.il-layout-page>header,header.il-header,.il-layout-page .header-inner,.il-maincontrols-metabar{display:none!important}
.il_HeaderInc h1,.il_HeaderInc .media-left,#headerimage,.ilHeadTitle,.il-page-content-header h1{display:none!important}
.il-layout-page footer,.il-footer{display:none!important}
.il-maincontrols-mainbar,nav.il-maincontrols-mainbar,.il-maincontrols-footer,.il-mainbar-entries{display:none!important}
.il-layout-page,.il-layout-page-content{padding-top:0!important;margin-top:0!important;padding-bottom:0!important}
"""

    private const val THEME_CSS = """
html,body,.il-layout-page,.il-layout-page-content,main,#il_center_col,#ilContentContainer,.il-standard-page,
.ilTabsContentOuter,.il-layout-page-content>div,#mainspacekeeper,.ilContainerStandardPage{background:var(--md-bg)!important;color:var(--md-on-bg)!important}
body,body :not(.glyphicon):not([class*="glyphicon-"]):not(.glyph):not(i):not(.fa):not([class*="icon-font"]){
  font-family:system-ui,-apple-system,Roboto,"Segoe UI",sans-serif!important;letter-spacing:normal!important}
body{-webkit-tap-highlight-color:transparent;-webkit-text-size-adjust:100%}
::-webkit-scrollbar{display:none}
a{color:var(--md-primary)!important;text-decoration:none!important}
h1,h2,h3,h4,h5,.ilHeader,.panel-title{color:var(--md-on-bg)!important;font-weight:600!important;text-shadow:none!important}
hr{border-color:var(--md-outline-var)!important}
p,li,span,div,label,td,th,dt,dd{border-color:var(--md-outline-var)}

/* cards */
.panel,.il-panel-listing-std-container,.ilContainerBlock,.il-block,.ilBlock,.il-item-group,.card,.il-card,
.ilTableOuter,.il-table-presentation,.ilFormOuter,.il-standard-form,.il-section-panel,.ilNewsBlock{
  background:var(--md-s-low)!important;border:0!important;border-radius:24px!important;box-shadow:none!important;overflow:hidden}
.panel-heading,.ilContainerBlockHeader,.ilBlockHeader,.panel-footer,.ilTableHeader,.il-table-presentation-header{
  background:transparent!important;border:0!important;color:var(--md-on-bg)!important}
.panel-body,.panel>div{background:transparent!important}
.panel,.ilContainerBlock,.il-block{margin-bottom:12px!important}
.panel .panel,.il-item-group .il-item-group,.panel .il-item-group{background:transparent!important;border-radius:0!important}

/* group headers (Magazin, Dozent, Datum ...) */
.il-item-group>h3,.il-item-group-title,.ilContainerItemsContainer>h3,.il-item-group>header{
  background:transparent!important;color:var(--md-primary)!important;font-size:14px!important;font-weight:600!important;
  border:0!important;padding:16px 20px 4px!important;margin:0!important}

/* list items */
.il-item,.il-std-item,.ilContainerListItemOuter,.il-item-group-items>li,.il_ContainerListItem{
  background:transparent!important;border:0!important;box-shadow:none!important}
.il-item-group-items>li+li,.ilContainerListItemOuter+.ilContainerListItemOuter{border-top:1px solid var(--md-outline-var)!important}
.il-item-title,.il-item-title a,a.il_ContainerItemTitle,.il_ContainerItemTitle a,.il-std-item .il-item-title a{
  color:var(--md-on-bg)!important;font-weight:500!important;font-size:16px!important}
.il-item-title a::after,a.il_ContainerItemTitle::after,.il_ContainerItemTitle a::after{content:none!important}
.il-item-description,.il_Description,.il-item-property-name,.il_ItemProperty,.ilListItemSection,.il_ItemProperties,.small,small,.text-muted,.help-block{color:var(--md-on-var)!important}
.il-item-property-value{color:var(--md-on-bg)!important}
.il-item:active,.ilContainerListItemOuter:active{background:var(--md-s-high)!important}
img[src*="icon_"]:not([src*="/data/"]),img.ilListItemIcon:not([src*="/data/"]),.il-item .icon img:not([src*="/data/"]){filter:var(--md-icon-filter)}

/* buttons */
.btn{border-radius:20px!important;box-shadow:none!important;text-shadow:none!important;background-image:none!important;
  font-weight:500!important;min-height:40px;padding:8px 18px!important;transition:background-color .15s}
.btn-default,.btn-ctrl,.btn-secondary,.btn-tag,.btn-bulky{background:var(--md-s-highest)!important;color:var(--md-on-bg)!important;border:0!important}
.btn-primary,.btn-default.engaged,.btn.engaged,.btn[aria-pressed="true"],.btn.active,.btn-ctrl.engaged,.btn-default.active{
  background:var(--md-primary)!important;color:var(--md-on-primary)!important;border:0!important}
.btn-link{background:transparent!important;color:var(--md-primary)!important;border:0!important;min-height:0;padding:4px 8px!important}
.btn:active{filter:brightness(1.12)}
.btn-group,.il-viewcontrol-section,.il-viewcontrol-mode,.il-viewcontrol-pagination,.il-viewcontrol-sortation,
.btn-toolbar,.ilToolbar,.il-toolbar{background:transparent!important;border:0!important;box-shadow:none!important;padding:0!important}
.btn-group,.il-viewcontrol-section,.il-viewcontrol-mode{display:inline-flex!important;flex-wrap:wrap;gap:6px;align-items:center}
.btn-group>.btn,.il-viewcontrol-mode>.btn,.il-viewcontrol-section>.btn{border-radius:20px!important;margin:0!important;float:none!important}
.ilToolbar,.il-toolbar{display:flex!important;flex-wrap:wrap;gap:8px;margin:8px 0 12px!important}
.ilToolbarSeparator,.ilToolbar .separator{display:none!important}
.ia-iconbtn,.ia-iconbtn.btn{background:transparent!important;color:var(--md-on-var)!important;width:40px!important;height:40px!important;
  min-height:40px!important;padding:0!important;border-radius:50%!important;display:inline-flex!important;align-items:center;justify-content:center}
.ia-iconbtn .caret{display:none!important}
.ia-iconbtn::before{content:"";width:4px;height:4px;border-radius:50%;background:currentColor;
  box-shadow:0 -7px 0 currentColor,0 7px 0 currentColor}
.caret{border-top-color:currentColor!important}

/* tabs */
#ilTab,#ilSubTab,ul.nav-tabs,.ilTabsContentOuter>ul{display:flex!important;flex-wrap:nowrap!important;overflow-x:auto!important;
  background:transparent!important;border:0!important;border-bottom:1px solid var(--md-outline-var)!important;margin:0 0 12px!important;padding:0!important;scrollbar-width:none}
ul.nav-tabs>li,#ilTab>li{float:none!important;flex:0 0 auto;margin:0!important;background:transparent!important;border:0!important}
ul.nav-tabs>li>a,#ilTab>li>a,ul.nav-tabs>li>.btn,ul.nav-tabs .dropdown-toggle{background:transparent!important;border:0!important;border-radius:0!important;
  color:var(--md-on-var)!important;padding:14px 16px!important;font-weight:500!important;margin:0!important;min-height:0}
ul.nav-tabs>li.active>a,#ilTab>li.active>a,ul.nav-tabs>li[aria-selected="true"]>a{color:var(--md-primary)!important;
  box-shadow:inset 0 -3px 0 var(--md-primary)!important}
#ilSubTab,ul.nav-pills,.ilSubTabs{border:0!important;gap:8px;padding:4px 0!important}
ul.nav-pills>li>a,#ilSubTab>li>a{border-radius:8px!important;border:1px solid var(--md-outline-var)!important;padding:6px 14px!important;color:var(--md-on-var)!important;box-shadow:none!important}
ul.nav-pills>li.active>a,#ilSubTab>li.active>a{background:var(--md-sc)!important;color:var(--md-on-sc)!important;border-color:transparent!important}

/* menus, popovers, modals */
.dropdown-menu,.popover,.il-popover,.webui-popover,.modal-content{background:var(--md-s-high)!important;color:var(--md-on-bg)!important;
  border:0!important;border-radius:16px!important;box-shadow:0 6px 24px rgba(0,0,0,.28)!important;padding:8px 0}
.dropdown-menu>li>a,.dropdown-menu>li>.btn,.dropdown-menu .btn-link,.dropdown-menu button{color:var(--md-on-bg)!important;
  padding:12px 20px!important;background:transparent!important;border-radius:0!important;width:100%;text-align:left}
.dropdown-menu>li>a:active,.dropdown-menu>li>button:active{background:var(--md-s-highest)!important}
.modal-header,.modal-footer{border:0!important}

/* forms */
input:not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit]),select,textarea,.form-control{
  background:var(--md-s-highest)!important;color:var(--md-on-bg)!important;border:0!important;border-bottom:1px solid var(--md-outline)!important;
  border-radius:8px 8px 0 0!important;box-shadow:none!important;min-height:44px}
input:focus,select:focus,textarea:focus,.form-control:focus{border-bottom:2px solid var(--md-primary)!important;outline:0!important}
input[type=checkbox],input[type=radio]{accent-color:var(--md-primary)}
input[type=submit],input[type=button]{border-radius:20px!important;background:var(--md-primary)!important;color:var(--md-on-primary)!important;border:0!important;padding:8px 18px!important}

/* tables */
table,.table,.table>thead>tr>th,.table>tbody>tr>td,th,td,tr{background:transparent!important;color:inherit!important;border-color:var(--md-outline-var)!important}
.table-striped>tbody>tr:nth-of-type(odd){background:var(--md-s)!important}

/* alerts */
.alert,.ilInfoMessage,.ilSuccessMessage,.ilFailureMessage,.ilQuestionMessage{border:0!important;border-radius:16px!important;box-shadow:none!important}
.alert-info,.ilInfoMessage{background:var(--md-sc)!important;color:var(--md-on-sc)!important}
.alert-success,.ilSuccessMessage{background:var(--md-tc)!important;color:var(--md-on-tc)!important}
.alert-danger,.alert-warning,.ilFailureMessage{background:var(--md-ec)!important;color:var(--md-on-ec)!important}

/* pagination, badges */
.pagination>li>a,.pagination>li>span{background:transparent!important;border:0!important;color:var(--md-on-var)!important;border-radius:20px!important}
.pagination>.active>a,.pagination>.active>span{background:var(--md-sc)!important;color:var(--md-on-sc)!important}
.badge,.label{border-radius:10px!important}
.ilCalendarDayHeader,.ilCalHeader,.calhead,.il-calendar-header{background:var(--md-s)!important;color:var(--md-on-bg)!important}

/* spacing */
.il-layout-page-content{padding:4px 12px 16px!important}
.container,.container-fluid{padding-left:0!important;padding-right:0!important}
"""

    /** JS that installs/updates the style element; safe to run at document start and repeatedly. */
    fun installScript(theme: Theme?, compact: Boolean): String {
        val css = buildString {
            if (theme != null) {
                append(theme.vars)
                append(THEME_CSS)
            }
            if (compact) append(COMPACT_CSS)
        }
        val dark = theme?.dark == true
        return """
(function(){
  var css=${JSONObject.quote(css)};
  window.__iaCfg={dark:$dark,themed:${theme != null},compact:$compact};
  function put(){
    var root=document.head||document.documentElement; if(!root) return;
    var s=document.getElementById('ilias-app-style');
    if(!s){s=document.createElement('style');s.id='ilias-app-style';}
    if(s.textContent!==css) s.textContent=css;
    if(s.parentNode!==root||s!==root.lastElementChild) root.appendChild(s);
  }
  put();
  if(window.__iaRun){ if(document.body) window.__iaRun(document.body); return; }

  function rgb(c){
    var m=/rgba?\(([^)]+)\)/.exec(c||''); if(!m) return null;
    var p=m[1].split(',').map(parseFloat);
    if(p.length>3&&p[3]<0.15) return null;
    return p;
  }
  var SKIP=/^(IMG|VIDEO|CANVAS|IFRAME|SVG|PATH|G|USE|INPUT|SELECT|TEXTAREA|OPTION|SCRIPT|STYLE|BR|HR)$/;
  function fixEl(e){
    var cfg=window.__iaCfg;
    if(SKIP.test(e.tagName)||e.closest('svg')) return;
    var cs=getComputedStyle(e);
    if(cfg.compact && cs.position==='fixed'){
      var r=e.getBoundingClientRect();
      if(r.height>0 && r.height<170 && r.bottom>=innerHeight-2 && r.width>=innerWidth*0.8 &&
         !e.closest('.modal,[role=dialog],.il-toast-wrapper,.il-toast,.alert')) e.style.setProperty('display','none','important');
    }
    if(!cfg.themed||!cfg.dark) return;
    if(e.closest('.btn,.badge,.label,.ilLPStatusIcon,[class*="lp-"],[class*="progress"],.il-chart,.ilc_Page .ilc_text_block_Code')) return;
    var bg=rgb(cs.backgroundColor), light=false;
    if(bg){
      if(bg[0]>205&&bg[1]>205&&bg[2]>205){ e.style.setProperty('background-color','transparent','important'); }
      else if((bg[0]*.299+bg[1]*.587+bg[2]*.114)>150) light=true;
    }
    if(!light){
      var fg=rgb(cs.color);
      if(fg && (fg[0]*.299+fg[1]*.587+fg[2]*.114)<95) e.style.setProperty('color','var(--md-on-var)','important');
    }
    if(parseFloat(cs.borderTopWidth)>0||parseFloat(cs.borderBottomWidth)>0){
      var bc=rgb(cs.borderTopColor)||rgb(cs.borderBottomColor);
      if(bc && bc[0]>190&&bc[1]>190&&bc[2]>190) e.style.setProperty('border-color','var(--md-outline-var)','important');
    }
  }
  function run(root){
    if(!root||root.nodeType!==1) return;
    try{
      fixEl(root);
      var all=root.getElementsByTagName('*');
      for(var i=0;i<all.length;i++) fixEl(all[i]);
    }catch(e){}
  }
  window.__iaRun=run;

  function mark(){
    document.querySelectorAll('.dropdown-toggle').forEach(function(b){
      if(b.classList.contains('ia-iconbtn')||b.closest('.nav-tabs')) return;
      if(b.textContent.trim()==='' && !b.querySelector(':not(.caret)')) b.classList.add('ia-iconbtn');
    });
  }
  function info(){
    try{
      var h=document.querySelector('.il_HeaderInc h1, h1.ilHeader, .il-page-content-header h1, #il_center_col h1, main h1, h1');
      var title=h?h.textContent.trim():'';
      if(h && window.__iaCfg.compact && !h.closest('.ilc_Page,.il-panel-listing-std-container,.panel')){
        h.style.setProperty('display','none','important');
        var icon=document.getElementById('headerimage'); if(icon) icon.style.setProperty('display','none','important');
      }
      var crumbs=[].slice.call(document.querySelectorAll('.breadcrumb a, .breadcrumbs a, .il-header-locator a, nav[aria-label*="Locator" i] a'))
        .map(function(a){return a.textContent.trim();}).filter(function(t){return t && t!==title;});
      var sub=crumbs.length?crumbs[crumbs.length-1]:'';
      if(window.IliasApp) IliasApp.onPageInfo(title, sub);
    }catch(e){}
  }
  function all(){ put(); mark(); info(); run(document.body); }

  var pending=[], timer=0;
  function flush(){ timer=0; var list=pending; pending=[]; mark(); list.forEach(run); }
  document.addEventListener('DOMContentLoaded',function(){
    all();
    try{
      new MutationObserver(function(ms){
        ms.forEach(function(m){ m.addedNodes.forEach(function(n){ if(n.nodeType===1) pending.push(n); }); });
        if(pending.length && !timer) timer=setTimeout(flush,120);
      }).observe(document.body,{childList:true,subtree:true});
    }catch(e){}
  });
  window.addEventListener('load',function(){ run(document.body); });
  document.addEventListener('scroll',function(e){
    var t=(e.target===document)?document.scrollingElement:e.target;
    if(t&&window.IliasApp)IliasApp.onScroll(t.scrollTop<=0);
  },true);
  if(document.readyState!=='loading') all();
})();
""".trimIndent()
    }
}

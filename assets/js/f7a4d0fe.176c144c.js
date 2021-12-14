"use strict";(self.webpackChunk=self.webpackChunk||[]).push([[1078],{3905:(e,t,n)=>{n.r(t),n.d(t,{MDXContext:()=>s,MDXProvider:()=>m,mdx:()=>f,useMDXComponents:()=>d,withMDXComponents:()=>l});var r=n(67294);function o(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function a(){return a=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var n=arguments[t];for(var r in n)Object.prototype.hasOwnProperty.call(n,r)&&(e[r]=n[r])}return e},a.apply(this,arguments)}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function p(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){o(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function c(e,t){if(null==e)return{};var n,r,o=function(e,t){if(null==e)return{};var n,r,o={},a=Object.keys(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||(o[n]=e[n]);return o}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(o[n]=e[n])}return o}var s=r.createContext({}),l=function(e){return function(t){var n=d(t.components);return r.createElement(e,a({},t,{components:n}))}},d=function(e){var t=r.useContext(s),n=t;return e&&(n="function"==typeof e?e(t):p(p({},t),e)),n},m=function(e){var t=d(e.components);return r.createElement(s.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},h=r.forwardRef((function(e,t){var n=e.components,o=e.mdxType,a=e.originalType,i=e.parentName,s=c(e,["components","mdxType","originalType","parentName"]),l=d(n),m=o,h=l["".concat(i,".").concat(m)]||l[m]||u[m]||a;return n?r.createElement(h,p(p({ref:t},s),{},{components:n})):r.createElement(h,p({ref:t},s))}));function f(e,t){var n=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var a=n.length,i=new Array(a);i[0]=h;var p={};for(var c in t)hasOwnProperty.call(t,c)&&(p[c]=t[c]);p.originalType=e,p.mdxType="string"==typeof e?e:o,i[1]=p;for(var s=2;s<a;s++)i[s]=n[s];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}h.displayName="MDXCreateElement"},13919:(e,t,n)=>{function r(e){return!0===/^(\w*:|\/\/)/.test(e)}function o(e){return void 0!==e&&!r(e)}n.d(t,{b:()=>r,Z:()=>o})},44996:(e,t,n)=>{n.r(t),n.d(t,{useBaseUrlUtils:()=>a,default:()=>i});var r=n(52263),o=n(13919);function a(){var e=(0,r.default)().siteConfig,t=(e=void 0===e?{}:e).baseUrl,n=void 0===t?"/":t,a=e.url;return{withBaseUrl:function(e,t){return function(e,t,n,r){var a=void 0===r?{}:r,i=a.forcePrependBaseUrl,p=void 0!==i&&i,c=a.absolute,s=void 0!==c&&c;if(!n)return n;if(n.startsWith("#"))return n;if((0,o.b)(n))return n;if(p)return t+n;var l=n.startsWith(t)?n:t+n.replace(/^\//,"");return s?e+l:l}(a,n,e,t)}}}function i(e,t){return void 0===t&&(t={}),(0,a().withBaseUrl)(e,t)}},90554:(e,t,n)=>{n.r(t),n.d(t,{frontMatter:()=>c,contentTitle:()=>s,metadata:()=>l,toc:()=>d,default:()=>u});var r=n(87462),o=n(63366),a=(n(67294),n(3905)),i=n(44996),p=["components"],c={id:"treeprops",title:"TreeProps"},s=void 0,l={unversionedId:"mainconcepts/coordinate-state-actions/treeprops",id:"mainconcepts/coordinate-state-actions/treeprops",isDocsHomePage:!1,title:"TreeProps",description:"This page was moved from the old website without any change and might be updated",source:"@site/../docs/mainconcepts/coordinate-state-actions/treeprops.mdx",sourceDirName:"mainconcepts/coordinate-state-actions",slug:"/mainconcepts/coordinate-state-actions/treeprops",permalink:"/docs/mainconcepts/coordinate-state-actions/treeprops",editUrl:"https://github.com/facebook/litho/edit/master/website/../docs/mainconcepts/coordinate-state-actions/treeprops.mdx",tags:[],version:"current",frontMatter:{id:"treeprops",title:"TreeProps"},sidebar:"mainSidebar",previous:{title:"Triggering events with Handles",permalink:"/docs/mainconcepts/coordinate-state-actions/trigger-events"},next:{title:"ComponentTree",permalink:"/docs/mainconcepts/coordinate-state-actions/componenttree"}},d=[{value:"Declaring a TreeProp",id:"declaring-a-treeprop",children:[],level:2},{value:"Using a TreeProp",id:"using-a-treeprop",children:[],level:2},{value:"TreeProps and Sections",id:"treeprops-and-sections",children:[],level:2}],m={toc:d};function u(e){var t=e.components,n=(0,o.Z)(e,p);return(0,a.mdx)("wrapper",(0,r.Z)({},m,n,{components:t,mdxType:"MDXLayout"}),(0,a.mdx)("div",{className:"admonition admonition-caution alert alert--warning"},(0,a.mdx)("div",{parentName:"div",className:"admonition-heading"},(0,a.mdx)("h5",{parentName:"div"},(0,a.mdx)("span",{parentName:"h5",className:"admonition-icon"},(0,a.mdx)("svg",{parentName:"span",xmlns:"http://www.w3.org/2000/svg",width:"16",height:"16",viewBox:"0 0 16 16"},(0,a.mdx)("path",{parentName:"svg",fillRule:"evenodd",d:"M8.893 1.5c-.183-.31-.52-.5-.887-.5s-.703.19-.886.5L.138 13.499a.98.98 0 0 0 0 1.001c.193.31.53.501.886.501h13.964c.367 0 .704-.19.877-.5a1.03 1.03 0 0 0 .01-1.002L8.893 1.5zm.133 11.497H6.987v-2.003h2.039v2.003zm0-3.004H6.987V5.987h2.039v4.006z"}))),"Content will be updated")),(0,a.mdx)("div",{parentName:"div",className:"admonition-content"},(0,a.mdx)("p",{parentName:"div"},"This page was moved from the old website without any change and might be updated"))),(0,a.mdx)("p",null,"A TreeProp is a special type of ",(0,a.mdx)("a",{parentName:"p",href:"/docs/mainconcepts/passing-data-to-components/props"},"prop"),", which is transparently passed\nfrom a parent component to its children. It provides a convenient way to share\ncontextual data or utilities in a tree without having to explicitly pass ",(0,a.mdx)("inlineCode",{parentName:"p"},"@Prop"),"\nto every component in your hierarchy."),(0,a.mdx)("p",null,"A good candidate, for example, is a prefetcher which fetches network images\nahead of render time. The prefetcher is widely used since images are common. The\nprefetcher implementation might be something we define for any Component that\nneeds to use it without having to pass it as ",(0,a.mdx)("inlineCode",{parentName:"p"},"@Prop")," in the entire tree."),(0,a.mdx)("h2",{id:"declaring-a-treeprop"},"Declaring a TreeProp"),(0,a.mdx)("p",null,"Each TreeProp is declared and created from a method annotated with ",(0,a.mdx)("inlineCode",{parentName:"p"},"@OnCreateTreeProp"),"."),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},"@LayoutSpec\npublic class ParentComponentSpec {\n  @OnCreateTreeProp\n  static Prefetcher onCreatePrefetcher(\n      ComponentContext c,\n      @Prop Prefetcher prefetcher) {\n\n    return prefetcher;\n  }\n\n  @OnCreateLayout\n  static Component onCreateLayout(\n      ComponentContext c,\n      @Prop Uri imageUri) {\n\n    return ChildComponent.create(c)\n        .imageUri(imageUri)\n        .build();\n  }\n}\n")),(0,a.mdx)("p",null,"You can only declare one TreeProp for any one given type. If a child of ParentComponent also defines a TreeProp of type Prefetcher, it will override the value of that TreeProp for all its children (but not for itself)."),(0,a.mdx)("h2",{id:"using-a-treeprop"},"Using a TreeProp"),(0,a.mdx)("p",null,"The child component can access the TreeProp value through a param annotated with TreeProp that has the same type as that which was declared in the parents @OnCreateTreeProp method."),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},"@LayoutSpec\nclass ChildComponentSpec {\n  @OnCreateLayout\n  static Component onCreateLayout(\n      ComponentContext c,\n      @TreeProp Prefetcher prefetcher,\n      @Prop Uri imageUri) {\n    if (prefetcher != null) {\n      prefetcher.prefetch(imageUri);\n    }\n    // ...\n  }\n}\n")),(0,a.mdx)("div",{className:"admonition admonition-caution alert alert--warning"},(0,a.mdx)("div",{parentName:"div",className:"admonition-heading"},(0,a.mdx)("h5",{parentName:"div"},(0,a.mdx)("span",{parentName:"h5",className:"admonition-icon"},(0,a.mdx)("svg",{parentName:"span",xmlns:"http://www.w3.org/2000/svg",width:"16",height:"16",viewBox:"0 0 16 16"},(0,a.mdx)("path",{parentName:"svg",fillRule:"evenodd",d:"M8.893 1.5c-.183-.31-.52-.5-.887-.5s-.703.19-.886.5L.138 13.499a.98.98 0 0 0 0 1.001c.193.31.53.501.886.501h13.964c.367 0 .704-.19.877-.5a1.03 1.03 0 0 0 .01-1.002L8.893 1.5zm.133 11.497H6.987v-2.003h2.039v2.003zm0-3.004H6.987V5.987h2.039v4.006z"}))),"IMPORTANT")),(0,a.mdx)("div",{parentName:"div",className:"admonition-content"},(0,a.mdx)("p",{parentName:"div"},"Once created, the TreeProp value will be passed down to all children, but will not be accessible from the component that created this TreeProp."))),(0,a.mdx)("p",null,"If you want to access a TreeProp from the component that created this TreeProp, you can transform it into ",(0,a.mdx)("a",{parentName:"p",href:"/docs/mainconcepts/coordinate-state-actions/state-overview"},(0,a.mdx)("inlineCode",{parentName:"a"},"@State"))," value like this:"),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},"@LayoutSpec\npublic class ParentComponentSpec {\n\n  @OnCreateInitialState\n  static void createInitialState(\n      ComponentContext c,\n      StateValue<ImportantHelper> helper) {\n\n    helper.set(new ImportantHelper());\n  }\n\n  @OnCreateTreeProp\n  static ImportantHelper onCreateHelper(\n      ComponentContext c,\n      @State ImportantHelper helper) {\n\n    return helper;\n  }\n")),(0,a.mdx)("p",null,"And now ",(0,a.mdx)("inlineCode",{parentName:"p"},"ImportantHelper")," instance is accessible as ",(0,a.mdx)("inlineCode",{parentName:"p"},"@State")," as usual:"),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},"@OnCreateLayout\nstatic Component onCreateLayout(\n    ComponentContext c,\n    @State ImportantHelper helper) {\n\n    //...\n}\n")),(0,a.mdx)("h2",{id:"treeprops-and-sections"},"TreeProps and Sections"),(0,a.mdx)("p",null,"TreeProps can be used in both Components and Sections and even shared and\nmodified between them. Let's consider the example of a logging datastructure we\npass down from the root component to capture information about the hierarchy."),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},'public class LogContext {\n  public final String s;\n\n  public LogContext(String s) {\n    this.s = s;\n  }\n\n  public static LogContext append(@Nullable LogContext t, String s) {\n    if (t == null) {\n      return new LogContext(s);\n    }\n    return new LogContext(t.s + ":" + s);\n  }\n\n  public String toString() {\n    return s;\n  }\n}\n\n')),(0,a.mdx)("p",null,"Immutable TreeProps are easier to reason about, so try to follow that design\npattern whenever possible."),(0,a.mdx)("p",null,"We now create a component hierarchy that looks like this:"),(0,a.mdx)("img",{src:(0,i.default)("/images/treeprop-sections.png")}),(0,a.mdx)("p",null,"We start by setting up the RootComponent and the RecyclerComponent sitting\ninside:"),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},'@LayoutSpec\npublic class RootComponentSpec {\n  @OnCreateLayout\n  static Component onCreateLayout(ComponentContext c) {\n    return Column.create(c)\n        .child(LeafComponent.create(c))\n        .child(\n            RecyclerCollectionComponent.create(c)\n                .section(TopGroupSection.create(new SectionContext(c)).build())\n                .flexGrow(1f)\n                .build())\n        .build();\n  }\n\n  @OnCreateTreeProp\n  static LogContext onCreateTestTreeProp(ComponentContext c) {\n    return new LogContext("root");\n  }\n}\n')),(0,a.mdx)("p",null,'The TopGroupSection takes in the root TreeProp and adds its "top" tag to it.'),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},'@GroupSectionSpec\npublic class TopGroupSectionSpec {\n\n  @OnCreateChildren\n  protected static Children onCreateChildren(SectionContext c) {\n    return Children.create()\n        .child(\n            BottomGroupSection.create(c).build()\n        )\n        .child(\n            SingleComponentSection.create(c).component(LeafComponent.create(c))\n        )\n        .build();\n  }\n\n  @OnCreateTreeProp\n  static LogContext onCreateTestTreeProp(SectionContext c, @TreeProp LogContext t) {\n    return LogContext.append(t, "top");\n  }\n}\n')),(0,a.mdx)("p",null,"We're omitting the bottom part here for brevity, but you can find it in the\nrepository under ",(0,a.mdx)("a",{parentName:"p",href:"https://github.com/facebook/litho/tree/master/litho-instrumentation-tests/src/main/java/com/facebook/litho/sections/treeprops"},"instrumentation-tests"),"."),(0,a.mdx)("p",null,"The leaf node simply renders the TreeProp as text in our example case here, but\nwould normally perform some sort of logging based on the context."),(0,a.mdx)("pre",null,(0,a.mdx)("code",{parentName:"pre",className:"language-java"},'@LayoutSpec\npublic class LeafComponentSpec {\n  @OnCreateLayout\n  static Component onCreateLayout(ComponentContext c, @TreeProp LogContext treeProp) {\n    return Text.create(c)\n        .text(LogContext.append(treeProp, "leaf").toString())\n        .textSizeDip(24)\n        .build();\n  }\n}\n')),(0,a.mdx)("p",null,"The result on screen will be three rows of text that read"),(0,a.mdx)("ul",null,(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("inlineCode",{parentName:"li"},'"root:leaf"')),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("inlineCode",{parentName:"li"},'"root:top:leaf"')),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("inlineCode",{parentName:"li"},'"root:top:bottom:leaf"'))),(0,a.mdx)("p",null,"This illustrates how TreeProps propagate through both component and section\ntrees and can be used to selectively share information with their children."))}u.isMDXComponent=!0}}]);
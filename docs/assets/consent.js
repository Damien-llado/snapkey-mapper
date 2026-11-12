/* Bandeau de consentement et chargement de Microsoft Clarity (RGPD / CNIL).
   Librairie : vanilla-cookieconsent 3.1.0 (orestbida, licence MIT), fichiers de dist/ du paquet npm
   https://registry.npmjs.org/vanilla-cookieconsent/-/vanilla-cookieconsent-3.1.0.tgz (sha1 0f430f4788e9a09e22e795088889b0679a88a263),
   copiés tels quels dans assets/vendor/ (aucun CDN) :
     cookieconsent.umd.js  sha256 bc6e2f2e6381fc025b27a6b0afb5a0e1fc689c6f9fc40a78708adb2054ef4575
     cookieconsent.css     sha256 ca046b8b1b1094107205988e7096a687b241c8ef5f3fefe5e543ed28d26646c1
   Clarity : le script n'est ajouté à la page qu'après acceptation. API de consentement documentée par Microsoft :
   https://learn.microsoft.com/clarity/setup-and-installation/clarity-consent-api-v2 (consentv2, et consent=false pour effacer les cookies).
   Charge par assets/site.js, après la librairie. */
(function () {
  var CLARITY_ID = 'z0nfap9rs6';
  var TEXTS = {
  "en": {
    "consentModal": {
      "title": "Audience measurement",
      "description": "This site measures its audience with Microsoft Clarity (clicks, scrolling, recordings of visits). Clarity only loads, and only sets cookies, if you accept. The app itself collects nothing.",
      "acceptAllBtn": "Accept",
      "acceptNecessaryBtn": "Refuse"
    }
  },
  "fr": {
    "consentModal": {
      "title": "Mesure d’audience",
      "description": "Ce site mesure son audience avec Microsoft Clarity (clics, défilement, enregistrements de visites). Clarity ne se charge et ne dépose ses cookies que si vous acceptez. L’appli, elle, ne collecte rien.",
      "acceptAllBtn": "Accepter",
      "acceptNecessaryBtn": "Refuser"
    }
  },
  "es": {
    "consentModal": {
      "title": "Medición de audiencia",
      "description": "Este sitio mide su audiencia con Microsoft Clarity (clics, desplazamiento, grabaciones de visitas). Clarity solo se carga y deja cookies si aceptas. La app no recoge nada.",
      "acceptAllBtn": "Aceptar",
      "acceptNecessaryBtn": "Rechazar"
    }
  },
  "de": {
    "consentModal": {
      "title": "Reichweitenmessung",
      "description": "Diese Website misst ihre Reichweite mit Microsoft Clarity (Klicks, Scrollen, Aufzeichnungen von Besuchen). Clarity wird nur geladen und setzt nur dann Cookies, wenn du zustimmst. Die App selbst sammelt nichts.",
      "acceptAllBtn": "Akzeptieren",
      "acceptNecessaryBtn": "Ablehnen"
    }
  },
  "nl": {
    "consentModal": {
      "title": "Bezoekersmeting",
      "description": "Deze site meet haar bezoek met Microsoft Clarity (klikken, scrollen, opnames van bezoeken). Clarity wordt alleen geladen en plaatst alleen cookies als je akkoord gaat. De app zelf verzamelt niets.",
      "acceptAllBtn": "Accepteren",
      "acceptNecessaryBtn": "Weigeren"
    }
  },
  "pt": {
    "consentModal": {
      "title": "Medição de audiência",
      "description": "Este site mede sua audiência com o Microsoft Clarity (cliques, rolagem, gravações de visitas). O Clarity só é carregado e só grava cookies se você aceitar. O app em si não coleta nada.",
      "acceptAllBtn": "Aceitar",
      "acceptNecessaryBtn": "Recusar"
    }
  },
  "hi": {
    "consentModal": {
      "title": "विज़िटर की गिनती",
      "description": "यह साइट अपने विज़िटर मापने के लिए Microsoft Clarity इस्तेमाल करती है (क्लिक, स्क्रॉल, विज़िट की रिकॉर्डिंग)। Clarity तभी लोड होता है और कुकी रखता है जब आप स्वीकार करें। ऐप खुद कुछ भी इकट्ठा नहीं करता।",
      "acceptAllBtn": "स्वीकार करें",
      "acceptNecessaryBtn": "अस्वीकार करें"
    }
  },
  "zh": {
    "consentModal": {
      "title": "访问量统计",
      "description": "本网站使用 Microsoft Clarity 统计访问情况（点击、滚动、访问录制）。只有在你同意后，Clarity 才会加载并设置 Cookie。应用本身不收集任何数据。",
      "acceptAllBtn": "接受",
      "acceptNecessaryBtn": "拒绝"
    }
  },
  "id": {
    "consentModal": {
      "title": "Pengukuran pengunjung",
      "description": "Situs ini mengukur pengunjungnya dengan Microsoft Clarity (klik, gulir, rekaman kunjungan). Clarity hanya dimuat dan hanya memasang cookie jika Anda menyetujui. Aplikasinya sendiri tidak mengumpulkan apa pun.",
      "acceptAllBtn": "Terima",
      "acceptNecessaryBtn": "Tolak"
    }
  },
  "it": {
    "consentModal": {
      "title": "Misurazione del pubblico",
      "description": "Questo sito misura il proprio pubblico con Microsoft Clarity (clic, scorrimento, registrazioni delle visite). Clarity si carica e usa i cookie solo se accetti. L'app in sé non raccoglie nulla.",
      "acceptAllBtn": "Accetta",
      "acceptNecessaryBtn": "Rifiuta"
    }
  },
  "ja": {
    "consentModal": {
      "title": "アクセス解析",
      "description": "このサイトは Microsoft Clarity でアクセスを計測しています（クリック、スクロール、訪問の記録）。Clarity は同意した場合にのみ読み込まれ、Cookie を設定します。アプリ自体は何も収集しません。",
      "acceptAllBtn": "同意する",
      "acceptNecessaryBtn": "拒否する"
    }
  },
  "vi": {
    "consentModal": {
      "title": "Đo lường lượt truy cập",
      "description": "Trang này đo lượt truy cập bằng Microsoft Clarity (lượt nhấp, cuộn trang, bản ghi lượt truy cập). Clarity chỉ được tải và chỉ đặt cookie khi bạn đồng ý. Bản thân ứng dụng không thu thập gì.",
      "acceptAllBtn": "Đồng ý",
      "acceptNecessaryBtn": "Từ chối"
    }
  },
  "th": {
    "consentModal": {
      "title": "การวัดผู้เข้าชม",
      "description": "เว็บไซต์นี้วัดผู้เข้าชมด้วย Microsoft Clarity (การคลิก การเลื่อน การบันทึกการเข้าชม) Clarity จะโหลดและตั้งคุกกี้ก็ต่อเมื่อคุณยอมรับเท่านั้น ตัวแอปเองไม่เก็บข้อมูลใดๆ",
      "acceptAllBtn": "ยอมรับ",
      "acceptNecessaryBtn": "ปฏิเสธ"
    }
  }
};

  var lang = (document.documentElement.lang || 'en').slice(0, 2).toLowerCase();
  if (!TEXTS[lang]) lang = 'en';
  var dark = matchMedia('(prefers-color-scheme: dark)');
  function theme() { document.documentElement.classList.toggle('cc--darkmode', dark.matches); }
  theme(); dark.addEventListener('change', theme);

  function loadClarity() {
    if (!window.clarity) {
      (function (c, l, a, r, i, t, y) {
        c[a] = c[a] || function () { (c[a].q = c[a].q || []).push(arguments); };
        t = l.createElement(r); t.async = 1; t.src = 'https://www.clarity.ms/tag/' + i;
        y = l.getElementsByTagName(r)[0]; y.parentNode.insertBefore(t, y);
      })(window, document, 'clarity', 'script', CLARITY_ID);
    }
    window.clarity('consentv2', { ad_Storage: 'granted', analytics_Storage: 'granted' });
  }
  function applyChoice() {
    if (CookieConsent.acceptedCategory('analytics')) loadClarity();
    else if (window.clarity) window.clarity('consent', false); // efface les cookies de Clarity et arrête le suivi
  }

  function start() {
    CookieConsent.run({
      cookie: { name: 'cc_cookie', expiresAfterDays: 182, sameSite: 'Lax' },
      guiOptions: { consentModal: { layout: 'box inline', position: 'bottom center', equalWeightButtons: true, flipButtons: false } },
      categories: {
        necessary: { enabled: true, readOnly: true },
        analytics: { autoClear: { cookies: [{ name: /^(_clck|_clsk|CLID|ANONCHK|SM|MR|MUID|SRM_B)$/ }] } }
      },
      onConsent: applyChoice,
      onChange: applyChoice,
      language: { default: lang, translations: TEXTS }
    });
  }

  // Le lien « Gérer les cookies » du pied de page rouvre le bandeau
  document.addEventListener('click', function (e) {
    if (e.target.closest('[data-cc-reopen]')) CookieConsent.show(true);
  });

  var css = document.createElement('link');
  css.rel = 'stylesheet'; css.href = '/assets/vendor/cookieconsent.css'; css.onload = start;
  document.head.appendChild(css);
})();

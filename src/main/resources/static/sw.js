// Service worker mínimo: deixa o Pulso instalável e abre a casca mesmo sem rede. A API nunca é guardada em cache.
const CASCA = 'pulso-casca-v1';
const ARQUIVOS = ['/', '/css/app.css', '/js/app.js', '/img/pulso.svg'];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CASCA).then((c) => c.addAll(ARQUIVOS)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CASCA).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.pathname.startsWith('/api/') || url.pathname === '/health') return;
  e.respondWith(fetch(e.request).then((r) => {
    const copia = r.clone();
    caches.open(CASCA).then((c) => c.put(e.request, copia)).catch(() => {});
    return r;
  }).catch(() => caches.match(e.request).then((r) => r || caches.match('/'))));
});

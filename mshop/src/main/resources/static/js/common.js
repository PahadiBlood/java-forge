const $ = (sel, root = document) => root.querySelector(sel);

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const money = (n) => '₹' + Number(n).toLocaleString('en-IN');

const stars = (r) => {
  const full = Math.max(0, Math.min(5, Math.round(r)));
  return '★'.repeat(full) + '☆'.repeat(5 - full);
};

async function getJSON(url) {
  const res = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`${res.status} from ${url}`);
  const text = await res.text();
  if (!text) throw new Error(`Empty response from ${url}`);
  return JSON.parse(text);
}

function showError(el, msg) {
  el.innerHTML = `<div class="notice">${esc(msg)}</div>`;
}

function productCard(p) {
  return `<a class="card fade" href="/product/${encodeURIComponent(p.id)}">
    <img src="${esc(p.imageUrl || '/images/placeholder.svg')}" alt="${esc(p.name)}" loading="lazy">
    <div class="card-body">
      <span class="tag">${esc(p.category)}</span>
      <h3>${esc(p.name)}</h3>
      <div class="price">${money(p.price)}</div>
    </div>
  </a>`;
}

function renderPager(el, page, totalPages, onChange) {
  el.innerHTML = '';
  if (!totalPages || totalPages <= 1) return;
  const add = (label, target, opts = {}) => {
    const b = document.createElement('button');
    b.textContent = label;
    b.disabled = !!opts.disabled;
    if (opts.active) b.classList.add('active');
    b.addEventListener('click', () => onChange(target));
    el.appendChild(b);
  };
  add('‹ Prev', page - 1, { disabled: page <= 0 });
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const end = Math.min(totalPages, start + 5);
  for (let i = start; i < end; i++) add(String(i + 1), i, { active: i === page });
  add('Next ›', page + 1, { disabled: page >= totalPages - 1 });
}

async function loadNav() {
  const nav = $('#nav');
  if (!nav) return;
  try {
    const cats = await getJSON('/api/categories');
    nav.innerHTML = cats
      .map((c) => `<a href="/category/${encodeURIComponent(c)}">${esc(c)}</a>`)
      .join('');
  } catch (e) { /* nav stays empty until the endpoint is implemented */ }
}
document.addEventListener('DOMContentLoaded', loadNav);

document.addEventListener('DOMContentLoaded', () => {
  const input = $('#site-search');
  const suggestions = $('#search-suggestions');
  if (!input || !suggestions) return;

  let debounce;
  let controller;
  const closeSuggestions = () => { suggestions.hidden = true; };
  input.addEventListener('input', () => {
    clearTimeout(debounce);
    const query = input.value.trim();
    if (query.length < 2) {
      closeSuggestions();
      return;
    }

    debounce = setTimeout(async () => {
      controller?.abort();
      controller = new AbortController();
      suggestions.innerHTML = '<div class="suggestion-empty">Searching…</div>';
      suggestions.hidden = false;
      try {
        const params = new URLSearchParams({ search: query, page: '0', size: '6', sort: 'relevance' });
        const response = await fetch(`/api/products/search?${params}`, {
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        });
        if (!response.ok) throw new Error('Search unavailable');
        const data = response.status === 204 ? { items: [] } : await response.json();
        const items = Array.isArray(data) ? data : (data.items || []);
        if (input.value.trim() !== query) return;
        suggestions.innerHTML = items.length
          ? items.slice(0, 6).map((product) => `
              <a class="suggestion" role="option" href="/product/${encodeURIComponent(product.id)}">
                <img src="${esc(product.imageUrl || '/images/placeholder.svg')}" alt="" loading="lazy">
                <span class="suggestion-copy"><strong>${esc(product.name)}</strong><span>${esc(product.category)} · ${money(product.price)}</span></span>
              </a>`).join('')
          : '<div class="suggestion-empty">No matching products yet.</div>';
        suggestions.hidden = false;
      } catch (error) {
        if (error.name !== 'AbortError') {
          suggestions.innerHTML = '<div class="suggestion-empty">Search is temporarily unavailable.</div>';
          suggestions.hidden = false;
        }
      }
    }, 250);
  });

  input.addEventListener('focus', () => {
    if (input.value.trim().length >= 2 && suggestions.childElementCount) suggestions.hidden = false;
  });
  document.addEventListener('click', (event) => {
    if (!event.target.closest('.header-search')) closeSuggestions();
  });
  input.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') closeSuggestions();
  });
});

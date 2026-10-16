(async function () {
  const root = $('#sections');
  root.innerHTML = '<div class="skeleton"></div><div class="skeleton"></div>';
  try {
    const sections = await getJSON('/api/home/sections?size=10');
    if (!sections.length) return showError(root, 'No products yet.');
    root.innerHTML = sections.map((s) => `
      <section class="block">
        <div class="block-head">
          <h2>${esc(s.category)}</h2>
          <a href="/category/${encodeURIComponent(s.category)}">View all${s.totalItems ? ` (${s.totalItems})` : ''} →</a>
        </div>
        <div class="row">${s.items.map(productCard).join('')}</div>
      </section>`).join('');
  } catch (e) {
    showError(root, 'Could not load home sections. Implement GET /api/home/sections. (' + e.message + ')');
  }
})();

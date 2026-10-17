(function () {
  const SIZE = 12;
  const name = decodeURIComponent(location.pathname.split('/').filter(Boolean)[1] || '');
  const grid = $('#grid'), pager = $('#pager');
  $('#title').textContent = name;
  document.title = name + ' · mShop';

  const pageFromUrl = () =>
    Math.max(0, (parseInt(new URLSearchParams(location.search).get('page'), 10) || 1) - 1);

  async function load(page, push) {
    grid.innerHTML = '<div class="skeleton" style="grid-column:1/-1"></div>';
    try {
      const url = `/api/products?category=${encodeURIComponent(name)}&page=${page}&size=${SIZE}`;
      const data = await getJSON(url);
      grid.innerHTML = data.items.map(productCard).join('');
      $('#count').textContent = `${data.totalItems} products`;
      const s = data.summary || {};
      $('#summary').innerHTML = s.count
        ? `<div class="big">${Number(s.average).toFixed(1)}</div>
           <div><div class="stars">${stars(s.average)}</div><div class="muted">${s.count} reviews</div></div>`
        : '<div class="muted">No reviews yet.</div>';
      renderPager(pager, data.page, data.totalPages, (p) => load(p, true));
      if (push) {
        history.pushState({ page }, '', `?page=${page + 1}`);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } catch (e) {
      pager.innerHTML = '';
      $('#summary').innerHTML = '';
      showError(grid, 'Could not load products. Implement GET /api/products. (' + e.message + ')');
    }
  }

  window.addEventListener('popstate', () => load(pageFromUrl(), false));
  load(pageFromUrl(), false);
})();

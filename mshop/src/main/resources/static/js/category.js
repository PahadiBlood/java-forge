(function () {
  const SIZE = 15;
  const name = decodeURIComponent(location.pathname.split('/').filter(Boolean)[1] || '');
  const grid = $('#grid'), pager = $('#pager'), sort = $('#sort-products');
  let products = [];
  $('#title').textContent = name;
  document.title = name + ' · mShop';

  const pageFromUrl = () =>
    Math.max(0, (parseInt(new URLSearchParams(location.search).get('page'), 10) || 1) - 1);

  function sortProducts(items) {
    const sorted = [...items];
    if (sort.value === 'name-asc') sorted.sort((a, b) => a.name.localeCompare(b.name));
    if (sort.value === 'price-asc') sorted.sort((a, b) => Number(a.price) - Number(b.price));
    if (sort.value === 'price-desc') sorted.sort((a, b) => Number(b.price) - Number(a.price));
    return sorted;
  }

  function render(page, push) {
    const totalPages = Math.ceil(products.length / SIZE);
    const pageProducts = sortProducts(products).slice(page * SIZE, (page + 1) * SIZE);
    grid.innerHTML = pageProducts.length ? pageProducts.map(productCard).join('')
      : '<div class="search-empty">No products found in this category.</div>';
    $('#count').textContent = `${products.length} products`;
    renderPager(pager, page, totalPages, (target) => render(target, true));
    if (push) {
      history.pushState({ page }, '', `?page=${page + 1}`);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  async function load(page, push) {
    grid.innerHTML = '<div class="skeleton" style="grid-column:1/-1"></div>';
    try {
      const url = `/api/products?category=${encodeURIComponent(name)}&page=0&size=500`;
      const data = await getJSON(url);
      products = data.items || [];
      const s = data.summary || {};
      $('#summary').innerHTML = s.count
        ? `<div class="big">${Number(s.average).toFixed(1)}</div>
           <div><div class="stars">${stars(s.average)}</div><div class="muted">${s.count} reviews</div></div>`
        : '<div class="muted">No reviews yet.</div>';
      render(page, push);
    } catch (e) {
      pager.innerHTML = '';
      $('#summary').innerHTML = '';
      showError(grid, 'Could not load products. Implement GET /api/products. (' + e.message + ')');
    }
  }

  window.addEventListener('popstate', () => load(pageFromUrl(), false));
  sort.addEventListener('change', () => render(0, true));
  load(pageFromUrl(), false);
})();

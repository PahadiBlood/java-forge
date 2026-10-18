(function () {
  const SIZE = 15;
  const params = new URLSearchParams(location.search);
  const query = params.get('q')?.trim() || '';
  const grid = $('#search-grid');
  const pager = $('#search-pager');
  const sort = $('#sort-search');
  const headerInput = $('#site-search');
  let page = Math.max(0, (Number.parseInt(params.get('page'), 10) || 1) - 1);

  if (headerInput) headerInput.value = query;
  $('#search-caption').textContent = query ? `Results for “${query}”` : 'Enter a product name in the search box to get started.';
  if (params.has('sort')) sort.value = params.get('sort');

  async function load(nextPage, updateUrl) {
    if (!query) {
      grid.innerHTML = '<div class="search-empty">Start with a product name, like “headphones” or “running shoes”.</div>';
      $('#search-count').textContent = '';
      pager.innerHTML = '';
      return;
    }

    grid.innerHTML = '<div class="skeleton" style="grid-column:1/-1"></div>';
    try {
      const searchParams = new URLSearchParams({ search: query, page: String(nextPage), size: String(SIZE), sort: sort.value });
      const response = await fetch(`/api/products/search?${searchParams}`, {
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) throw new Error(`${response.status} from search API`);
      const data = response.status === 204 ? { items: [], totalItems: 0, totalPages: 0 } : await response.json();
      const items = Array.isArray(data) ? data : (data.items || []);
      page = data.page ?? nextPage;
      grid.innerHTML = items.length ? items.map(productCard).join('')
        : '<div class="search-empty">No products match this search yet.</div>';
      $('#search-count').textContent = `${data.totalItems ?? items.length} products`;
      renderPager(pager, page, data.totalPages ?? (items.length ? 1 : 0), (target) => load(target, true));
      if (updateUrl) {
        const next = new URLSearchParams({ q: query, page: String(page + 1), sort: sort.value });
        history.pushState({ page }, '', `/search?${next}`);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } catch (error) {
      pager.innerHTML = '';
      $('#search-count').textContent = '';
      showError(grid, 'Could not load search results. Please try again.');
    }
  }

  sort.addEventListener('change', () => load(0, true));
  window.addEventListener('popstate', () => {
    const current = new URLSearchParams(location.search);
    page = Math.max(0, (Number.parseInt(current.get('page'), 10) || 1) - 1);
    sort.value = current.get('sort') || 'relevance';
    load(page, false);
  });
  load(page, false);
})();

(function () {
  const SIZE = 5;
  const id = decodeURIComponent(location.pathname.split('/').filter(Boolean)[1] || '');
  const box = $('#product'), summary = $('#summary'), list = $('#reviews'), pager = $('#pager');
  const reviewSort = $('#sort-reviews');

  async function loadProduct() {
    try {
      const p = await getJSON('/api/products/' + encodeURIComponent(id));
      document.title = p.name + ' · mShop';
      box.innerHTML = `
        <img src="${esc(p.imageUrl || '/images/placeholder.svg')}" alt="${esc(p.name)}">
        <div>
          <a class="tag" href="/category/${encodeURIComponent(p.category)}">${esc(p.category)}</a>
          <h1>${esc(p.name)}</h1>
          <div class="price">${money(p.price)}</div>
          <p>${esc(p.description)}</p>
        </div>`;
      box.classList.add('fade');
    } catch (e) {
      showError(box, 'Could not load product. Implement GET /api/products/{id}. (' + e.message + ')');
    }
  }

  async function loadReviews(page, scroll) {
    try {
      const params = new URLSearchParams({ page: String(page), size: String(SIZE), sort: reviewSort.value });
      const data = await getJSON(`/api/products/${encodeURIComponent(id)}/reviews?${params}`);
      const s = data.summary || {};
      summary.innerHTML = s.count
        ? `<div class="big">${Number(s.average).toFixed(1)}</div>
           <div><div class="stars">${stars(s.average)}</div><div class="muted">${s.count} reviews</div></div>`
        : '<div class="muted">No reviews yet.</div>';
      list.innerHTML = data.items.map((r) => `
        <article class="review fade">
          <div class="review-top">
            <div class="avatar">${esc((r.author || '?')[0])}</div>
            <strong>${esc(r.author)}</strong>
            <span class="stars">${stars(r.rating)}</span>
            <span class="muted">${new Date(r.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
          </div>
          <p>${esc(r.comment)}</p>
        </article>`).join('');
      if (!data.items.length) list.innerHTML = '<div class="notice">No reviews yet.</div>';
      renderPager(pager, data.page, data.totalPages, (target) => loadReviews(target, true));
      if (scroll) $('.reviews').scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) {
      pager.innerHTML = '';
      showError(list, 'Could not load reviews. Implement GET /api/products/{id}/reviews. (' + e.message + ')');
    }
  }

  loadProduct();
  reviewSort.addEventListener('change', () => loadReviews(0, false));
  loadReviews(0, false);
})();

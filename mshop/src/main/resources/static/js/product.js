(function () {
  const SIZE = 5;
  const id = decodeURIComponent(location.pathname.split('/').filter(Boolean)[1] || '');
  const box = $('#product'), summary = $('#summary'), list = $('#reviews'), pager = $('#pager');
  const reviewSort = $('#sort-reviews');
  let reviews = [];

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

  function renderReviews(page, scroll) {
    const sorted = [...reviews];
    if (reviewSort.value === 'newest') sorted.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    if (reviewSort.value === 'oldest') sorted.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    if (reviewSort.value === 'rating-desc') sorted.sort((a, b) => Number(b.rating) - Number(a.rating));
    if (reviewSort.value === 'rating-asc') sorted.sort((a, b) => Number(a.rating) - Number(b.rating));
    const pageReviews = sorted.slice(page * SIZE, (page + 1) * SIZE);
    list.innerHTML = pageReviews.map((r) => `
        <article class="review fade">
          <div class="review-top">
            <div class="avatar">${esc((r.author || '?')[0])}</div>
            <strong>${esc(r.author)}</strong>
            <span class="stars">${stars(r.rating)}</span>
            <span class="muted">${new Date(r.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
          </div>
          <p>${esc(r.comment)}</p>
        </article>`).join('');
    if (!pageReviews.length) list.innerHTML = '<div class="notice">No reviews yet.</div>';
    renderPager(pager, page, Math.ceil(reviews.length / SIZE), (target) => renderReviews(target, true));
    if (scroll) $('.reviews').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function loadReviews(page, scroll) {
    try {
      const data = await getJSON(`/api/products/${encodeURIComponent(id)}/reviews?page=0&size=500`);
      const s = data.summary || {};
      summary.innerHTML = s.count
        ? `<div class="big">${Number(s.average).toFixed(1)}</div>
           <div><div class="stars">${stars(s.average)}</div><div class="muted">${s.count} reviews</div></div>`
        : '<div class="muted">No reviews yet.</div>';
      reviews = data.items || [];
      renderReviews(page, scroll);
    } catch (e) {
      pager.innerHTML = '';
      showError(list, 'Could not load reviews. Implement GET /api/products/{id}/reviews. (' + e.message + ')');
    }
  }

  loadProduct();
  reviewSort.addEventListener('change', () => renderReviews(0, false));
  loadReviews(0, false);
})();

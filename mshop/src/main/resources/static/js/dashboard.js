(function () {
  const range = $('#dashboard-range');
  const search = $('#order-search');
  const status = $('#order-status');
  const sort = $('#order-sort');
  const pageSize = 8;
  let page = 0;
  let searchTimer;
  let orderRequest;

  const money = (amount) => `₹${Number(amount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;

  function metricCard(label, value, detail, tone) {
    const card = document.createElement('article');
    card.className = `metric-card ${tone}`;
    [
      ['span', 'metric-label', label],
      ['strong', 'metric-value', value],
      ['span', 'metric-detail', detail],
    ].forEach(([tag, className, text]) => {
      const element = document.createElement(tag);
      element.className = className;
      element.textContent = text;
      card.appendChild(element);
    });
    return card;
  }

  function addBars(container, rows, labelOf, valueOf, displayOf, emptyMessage, colors) {
    container.replaceChildren();
    if (!rows?.length) {
      container.textContent = emptyMessage;
      return;
    }
    const max = Math.max(...rows.map(valueOf));
    rows.forEach((item, index) => {
      const row = document.createElement('div');
      row.className = 'dashboard-bar-row';
      const heading = document.createElement('div');
      heading.className = 'dashboard-bar-heading';
      const name = document.createElement('span');
      name.textContent = labelOf(item);
      const amount = document.createElement('strong');
      amount.textContent = displayOf(item);
      heading.append(name, amount);
      const track = document.createElement('div');
      track.className = 'dashboard-bar-track';
      track.setAttribute('role', 'img');
      track.setAttribute('aria-label', `${labelOf(item)}: ${displayOf(item)}`);
      const fill = document.createElement('span');
      fill.className = 'dashboard-bar-fill';
      fill.style.width = `${max ? Math.max(4, (valueOf(item) / max) * 100) : 0}%`;
      fill.style.setProperty('--bar-color', colors[index % colors.length]);
      track.appendChild(fill);
      row.append(heading, track);
      container.appendChild(row);
    });
  }

  async function requestJson(url, signal) {
    const response = await fetch(url, { headers: { Accept: 'application/json' }, signal });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  }

  function renderSummary(summary) {
    $('#dashboard-metrics').replaceChildren(
      metricCard('Net sales', money(summary.netSales), 'Excludes cancelled and returned orders', 'metric-green'),
      metricCard('Orders', Number(summary.totalOrders).toLocaleString('en-IN'), `Last ${summary.days} days`, 'metric-blue'),
      metricCard('To fulfill', Number(summary.awaitingFulfillment).toLocaleString('en-IN'), 'Processing orders', 'metric-orange'),
      metricCard('Average order', money(summary.averageOrderValue), `${summary.fulfilledOrders} fulfilled orders`, 'metric-purple'),
    );
    addBars($('#dashboard-status'), summary.ordersByStatus, (item) => item.status,
      (item) => item.orderCount, (item) => `${item.orderCount} orders`, 'No order status data.',
      ['#536dfe', '#13a89e', '#f2a93b', '#8c67d8']);
    addBars($('#dashboard-revenue'), summary.revenueByCategory, (item) => item.category,
      (item) => Number(item.revenue), (item) => money(item.revenue), 'No category revenue data.',
      ['#13a89e', '#536dfe', '#f2a93b', '#8c67d8']);
    addBars($('#dashboard-customers'), summary.topCustomers, (item) => item.customerName,
      (item) => Number(item.totalSpend), (item) => `${money(item.totalSpend)} · ${item.orderCount} orders`,
      'No customer summary data.', ['#8c67d8', '#536dfe', '#13a89e']);
  }

  function renderOrders(result) {
    const body = $('#dashboard-orders');
    body.replaceChildren();
    if (!result.items.length) {
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 6;
      cell.className = 'order-empty';
      cell.textContent = 'No orders match these filters.';
      row.appendChild(cell);
      body.appendChild(row);
    }
    result.items.forEach((order) => {
      const row = document.createElement('tr');
      const values = [
        order.id,
        order.customerName,
        `${order.items.length} line items`,
        order.status,
        new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }),
        money(order.total),
      ];
      values.forEach((value, index) => {
        const cell = document.createElement('td');
        cell.textContent = value;
        if (index === 3) cell.className = `status-cell status-${order.status.toLowerCase()}`;
        if (index === 5) cell.classList.add('dashboard-total');
        row.appendChild(cell);
      });
      body.appendChild(row);
    });

    $('#dashboard-order-count').textContent = `${result.totalItems} orders`;
    $('#order-results').textContent = result.totalPages ? `Page ${result.page + 1} of ${result.totalPages}` : 'No results';
    const pager = $('#order-pagination');
    pager.replaceChildren();
    [
      ['Previous', result.page - 1, result.page <= 0],
      ['Next', result.page + 1, result.page >= result.totalPages - 1],
    ].forEach(([label, targetPage, disabled]) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = label;
      button.disabled = disabled;
      button.addEventListener('click', () => {
        page = targetPage;
        loadOrders();
      });
      pager.appendChild(button);
    });
  }

  function showLoadError(selector, message) {
    const element = $(selector);
    element.replaceChildren();
    element.textContent = message;
  }

  function showOrdersError(message) {
    const body = $('#dashboard-orders');
    body.replaceChildren();
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 6;
    cell.className = 'order-empty';
    cell.textContent = message;
    row.appendChild(cell);
    body.appendChild(row);
  }

  async function loadSummary() {
    try {
      const params = new URLSearchParams({ days: range.value });
      const summary = await requestJson(`/api/admin/dashboard/summary?${params}`);
      renderSummary(summary);
    } catch (error) {
      showLoadError('#dashboard-metrics', `Dashboard summary API is not ready (${error.message}). Implement the controller contract to load JSON data.`);
      ['#dashboard-status', '#dashboard-revenue', '#dashboard-customers'].forEach((selector) => {
        showLoadError(selector, 'Waiting for the dashboard summary API.');
      });
    }
  }

  async function loadOrders() {
    orderRequest?.abort();
    orderRequest = new AbortController();
    const params = new URLSearchParams({
      days: range.value,
      page: String(page),
      size: String(pageSize),
      sort: sort.value,
    });
    if (status.value) params.set('status', status.value);
    if (search.value.trim()) params.set('search', search.value.trim());
    try {
      const result = await requestJson(`/api/admin/orders?${params}`, orderRequest.signal);
      renderOrders(result);
    } catch (error) {
      if (error.name !== 'AbortError') {
        showOrdersError(`Orders API is not ready (${error.message}). Implement the controller contract to load orders.json.`);
        $('#dashboard-order-count').textContent = '';
        $('#order-results').textContent = '';
        $('#order-pagination').replaceChildren();
      }
    }
  }

  range.addEventListener('change', () => {
    page = 0;
    loadSummary();
    loadOrders();
  });
  status.addEventListener('change', () => {
    page = 0;
    loadOrders();
  });
  sort.addEventListener('change', () => {
    page = 0;
    loadOrders();
  });
  search.addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      page = 0;
      loadOrders();
    }, 250);
  });

  loadSummary();
  loadOrders();
})();

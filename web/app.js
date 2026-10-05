const state = {
  route: 'overview',
  query: '',
  employees: [],
  inventory: [],
  sales: [],
  summary: {},
  intelligence: []
};

const view = document.querySelector('#view');
const dialog = document.querySelector('#record-dialog');
const recordForm = document.querySelector('#record-form');
const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const routeTitles = {
  overview: 'Overview',
  employees: 'Employees',
  inventory: 'Inventory',
  sales: 'Sales',
  reports: 'Reports'
};

async function api(path, options = {}) {
  const response = await fetch(path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'The request could not be completed.');
  return result;
}

async function refreshData(showMessage = false) {
  try {
    const [summary, employees, inventory, sales, intelligence] = await Promise.all([
      api('/api/dashboard'),
      api('/api/employees'),
      api('/api/inventory'),
      api('/api/sales'),
      api('/api/intelligence')
    ]);
    state.summary = summary;
    state.employees = employees;
    state.inventory = inventory;
    state.sales = sales;
    state.intelligence = intelligence && Array.isArray(intelligence.actionCenter) ? intelligence.actionCenter : [];
    document.querySelector('#alert-dot').hidden = summary.lowStockCount === 0;
    render();
    if (showMessage) toast('Workspace data refreshed.');
  } catch (error) {
    view.innerHTML = `<div class="empty-state"><strong>Could not load workspace data</strong><span>${escapeHtml(error.message)} Check that the app is running from the project root.</span></div>`;
    toast(error.message, true);
  }
}

function render() {
  const title = routeTitles[state.route] || 'Overview';
  document.querySelector('#section-title').textContent = title;
  document.querySelectorAll('.nav-item').forEach(button => {
    button.classList.toggle('is-active', button.dataset.route === state.route);
    button.setAttribute('aria-current', button.dataset.route === state.route ? 'page' : 'false');
  });
  const renderers = {
    overview: renderOverview,
    employees: renderEmployees,
    inventory: renderInventory,
    sales: renderSales,
    reports: renderReports
  };
  view.innerHTML = renderers[state.route]();
}

function renderOverview() {
  const summary = state.summary;
  const productRevenue = filtered(state.sales, ['product', 'id', 'amount']).reduce((groups, sale) => {
    const name = sale.product || 'Unassigned';
    groups[name] = (groups[name] || 0) + Number(sale.amount || 0);
    return groups;
  }, {});
  const maxRevenue = Math.max(0, ...Object.values(productRevenue));
  const chartRows = Object.entries(productRevenue)
    .sort((first, second) => second[1] - first[1])
    .slice(0, 5)
    .map(([name, amount]) => `<div class="bar-row"><span class="bar-label" title="${escapeHtml(name)}">${escapeHtml(name)}</span><span class="bar-track"><span class="bar-fill" style="width:${maxRevenue ? Math.max(3, amount / maxRevenue * 100) : 0}%"></span></span><strong class="bar-amount">${formatMoney(amount)}</strong></div>`)
    .join('');
  const lowStock = state.inventory
    .filter(item => Number(item.quantity) <= 5 && matchesQuery(item, ['name', 'id', 'quantity']))
    .sort((first, second) => Number(first.quantity) - Number(second.quantity))
    .slice(0, 4);
  const latestSales = filtered(state.sales, ['product', 'id', 'amount']).slice().reverse().slice(0, 5);
  const lowStockCount = Number(summary.lowStockCount || 0);
  const insights = buildIntelligenceInsights();

  return `
    <section class="page-heading">
      <div><span class="eyebrow">OPERATIONS / ${formatDate(new Date())}</span><h1>Good ${greeting()}, Chirag.</h1><p>Your business at a glance. Everything is up to date.</p></div>
      <div class="heading-actions"><button class="button button-quiet" type="button" data-action="open-form" data-kind="employee"><svg><use href="#i-plus"></use></svg>Add employee</button><button class="button button-primary" type="button" data-action="open-form" data-kind="sale"><svg><use href="#i-plus"></use></svg>Record sale</button></div>
    </section>
    <section class="metric-grid" aria-label="Business summary">
      ${metricCard('users', 'Employees', summary.employeeCount || 0, 'On your team', 'green')}
      ${metricCard('box', 'Products', summary.productCount || 0, `${summary.stockUnits || 0} units in stock`, 'blue')}
      ${metricCard('receipt', 'Sales recorded', summary.salesCount || 0, 'Across all entries', 'amber')}
      ${metricCard('chart', 'Recorded revenue', formatMoney(summary.revenue || 0), `${lowStockCount} ${lowStockCount === 1 ? 'item needs' : 'items need'} attention`, 'coral')}
    </section>
    <section class="smart-insights" aria-label="Actionable intelligence">
      <article class="panel insight-panel">
        <div class="panel-heading"><div><h2>Today’s actions</h2><p>Priority-driven recommendations from current data</p></div><span class="chart-legend"><i></i>Live insight</span></div>
        <div class="insight-list">
          ${insights.map(item => `
            <button class="insight-item ${item.level}" type="button" data-route="${item.route}">
              <span class="insight-meta">
                <span class="priority-tag ${item.level}">${item.priority}</span>
                <span class="insight-source">${escapeHtml(item.source)}</span>
              </span>
              <strong>${escapeHtml(item.title)}</strong>
              <p>${escapeHtml(item.reason)}</p>
              <span class="insight-action">${escapeHtml(item.action)}</span>
            </button>
          `).join('')}
        </div>
      </article>
    </section>
    <section class="dashboard-grid">
      <article class="panel">
        <div class="panel-heading"><div><h2>Revenue by product</h2><p>Based on recorded sales</p></div><span class="chart-legend"><i></i>Revenue</span></div>
        <div class="revenue-chart">${chartRows || '<div class="chart-empty">Record a sale to see product revenue here.</div>'}</div>
      </article>
      <article class="panel">
        <div class="panel-heading"><div><h2>Stock watch</h2><p>${lowStockCount} ${lowStockCount === 1 ? 'product' : 'products'} at five units or below</p></div><button class="text-button" type="button" data-route="inventory">View inventory <svg><use href="#i-arrow"></use></svg></button></div>
        <div class="stock-list">${lowStock.length ? lowStock.map(item => `<div class="stock-row"><div class="stock-name">${escapeHtml(item.name)}<small class="stock-id">Item ${escapeHtml(item.id)}</small></div><span class="stock-quantity ${Number(item.quantity) <= 2 ? 'critical' : ''}">${Number(item.quantity) === 0 ? 'Out of stock' : `${Number(item.quantity)} left`}</span></div>`).join('') : '<div class="empty-inline">All products are above the low-stock threshold.</div>'}</div>
      </article>
    </section>
    <article class="panel table-panel">
      <div class="panel-heading"><div><h2>Recent sales</h2><p>Latest entries in your sales ledger</p></div><button class="text-button" type="button" data-route="sales">View all sales <svg><use href="#i-arrow"></use></svg></button></div>
      ${salesTable(latestSales, false)}
    </article>`;
}

function renderEmployees() {
  const employees = filtered(state.employees, ['name', 'id', 'salary']);
  return `
    ${pageHeading('People / DIRECTORY', 'Employees', 'Manage your team and monthly salary records.', 'employee', 'Add employee')}
    <article class="panel table-panel">
      <div class="panel-heading"><div><h2>Team directory</h2><p>Employee records saved in this workspace</p></div><button class="icon-button" type="button" data-action="refresh" title="Refresh data" aria-label="Refresh data"><svg><use href="#i-refresh"></use></svg></button></div>
      <div class="table-toolbar"><span class="record-count">${employees.length} ${employees.length === 1 ? 'person' : 'people'}</span></div>
      <div class="table-wrap"><table><thead><tr><th>EMPLOYEE</th><th>EMPLOYEE ID</th><th>MONTHLY SALARY</th><th class="table-actions"><span class="sr-only">Actions</span></th></tr></thead><tbody>${employees.length ? employees.map(employee => `<tr><td><span class="person-cell"><span class="person-avatar">${escapeHtml(initials(employee.name))}</span><span><strong>${escapeHtml(employee.name)}</strong><small>Team member</small></span></span></td><td>#${escapeHtml(employee.id)}</td><td class="amount-cell">${formatMoney(employee.salary)}</td><td class="table-actions">${deleteButton('employees', employee.id, employee.name)}</td></tr>`).join('') : emptyRow(4, 'No employees match this search.')}</tbody></table></div>
    </article>`;
}

function renderInventory() {
  const items = filtered(state.inventory, ['name', 'id', 'quantity']);
  return `
    ${pageHeading('OPERATIONS / STOCK', 'Inventory', 'Keep an eye on product availability and reorder points.', 'inventory', 'Add product')}
    <section class="metric-grid" aria-label="Inventory summary">
      ${metricCard('box', 'Product types', state.summary.productCount || 0, 'Listed in inventory', 'green')}
      ${metricCard('grid', 'Units on hand', state.summary.stockUnits || 0, 'Across all products', 'blue')}
      ${metricCard('bell', 'Low stock', state.summary.lowStockCount || 0, 'Five units or below', 'coral')}
      ${metricCard('check', 'In stock', Math.max(0, (state.summary.productCount || 0) - (state.summary.lowStockCount || 0)), 'Above reorder point', 'amber')}
    </section>
    <article class="panel table-panel">
      <div class="panel-heading"><div><h2>Product inventory</h2><p>Quantity and stock status by item</p></div><button class="icon-button" type="button" data-action="refresh" title="Refresh data" aria-label="Refresh data"><svg><use href="#i-refresh"></use></svg></button></div>
      <div class="table-toolbar"><span class="record-count">${items.length} ${items.length === 1 ? 'product' : 'products'}</span></div>
      <div class="table-wrap"><table><thead><tr><th>PRODUCT</th><th>REFERENCE</th><th>QUANTITY</th><th>STATUS</th><th class="table-actions"><span class="sr-only">Actions</span></th></tr></thead><tbody>${items.length ? items.map(item => `<tr><td class="primary-cell"><strong>${escapeHtml(item.name)}</strong><small>Inventory item</small></td><td>#${escapeHtml(item.id)}</td><td class="amount-cell">${Number(item.quantity)}</td><td>${stockStatus(Number(item.quantity))}</td><td class="table-actions">${deleteButton('inventory', item.id, item.name)}</td></tr>`).join('') : emptyRow(5, 'No products match this search.')}</tbody></table></div>
    </article>`;
}

function renderSales() {
  const sales = filtered(state.sales, ['product', 'id', 'amount']);
  return `
    ${pageHeading('OPERATIONS / LEDGER', 'Sales', 'Record transactions and review revenue by product.', 'sale', 'Record sale')}
    <section class="metric-grid" aria-label="Sales summary">
      ${metricCard('receipt', 'Transactions', state.summary.salesCount || 0, 'Recorded entries', 'green')}
      ${metricCard('chart', 'Total revenue', formatMoney(state.summary.revenue || 0), 'Across all records', 'blue')}
      ${metricCard('box', 'Products sold', new Set(state.sales.map(sale => sale.product)).size, 'Unique product types', 'amber')}
      ${metricCard('users', 'Team members', state.summary.employeeCount || 0, 'In this workspace', 'coral')}
    </section>
    <article class="panel table-panel">
      <div class="panel-heading"><div><h2>Sales ledger</h2><p>Entries are ordered by record ID</p></div><button class="icon-button" type="button" data-action="refresh" title="Refresh data" aria-label="Refresh data"><svg><use href="#i-refresh"></use></svg></button></div>
      <div class="table-toolbar"><span class="record-count">${sales.length} ${sales.length === 1 ? 'transaction' : 'transactions'}</span></div>
      <div class="table-wrap"><table><thead><tr><th>PRODUCT</th><th>SALE REFERENCE</th><th>AMOUNT</th><th class="table-actions"><span class="sr-only">Actions</span></th></tr></thead><tbody>${sales.length ? sales.map(sale => `<tr><td class="primary-cell"><strong>${escapeHtml(sale.product)}</strong><small>Recorded sale</small></td><td>#${escapeHtml(sale.id)}</td><td class="amount-cell">${formatMoney(sale.amount)}</td><td class="table-actions">${deleteButton('sales', sale.id, sale.product)}</td></tr>`).join('') : emptyRow(4, 'No sales match this search.')}</tbody></table></div>
    </article>`;
}

function renderReports() {
  const averageSale = state.sales.length ? state.summary.revenue / state.sales.length : 0;
  const lowStock = filtered(state.inventory, ['name', 'id', 'quantity']).filter(item => Number(item.quantity) <= 5);
  return `
    <section class="page-heading"><div><span class="eyebrow">INSIGHTS / WORKSPACE DATA</span><h1>Reports</h1><p>A current snapshot of the records in your local workspace.</p></div><div class="heading-actions"><button class="button button-quiet" type="button" data-action="export"><svg><use href="#i-download"></use></svg>Export report</button><button class="button button-primary" type="button" data-action="refresh"><svg><use href="#i-refresh"></use></svg>Refresh data</button></div></section>
    <section class="report-grid">
      <article class="report-note"><h2>RECORDED REVENUE</h2><strong>${formatMoney(state.summary.revenue || 0)}</strong><p>${state.summary.salesCount || 0} sale entries</p></article>
      <article class="report-note"><h2>AVERAGE SALE</h2><strong>${formatMoney(averageSale)}</strong><p>Revenue divided by recorded sales</p></article>
      <article class="report-note"><h2>STOCK TO REVIEW</h2><strong>${lowStock.length}</strong><p>Products at five units or below</p></article>
    </section>
    <section class="dashboard-grid">
      <article class="panel"><div class="panel-heading"><div><h2>Revenue by product</h2><p>Recorded amount grouped by product name</p></div></div><div class="revenue-chart">${revenueRows()}</div></article>
      <article class="panel"><div class="panel-heading"><div><h2>Workspace totals</h2><p>Current record counts</p></div></div><div class="stock-list">${reportTotal('Employees', state.summary.employeeCount || 0)}${reportTotal('Product types', state.summary.productCount || 0)}${reportTotal('Units on hand', state.summary.stockUnits || 0)}${reportTotal('Sales entries', state.summary.salesCount || 0)}</div></article>
    </section>`;
}

function pageHeading(eyebrow, title, description, kind, actionLabel) {
  return `<section class="page-heading"><div><span class="eyebrow">${eyebrow}</span><h1>${title}</h1><p>${description}</p></div><div class="heading-actions"><button class="button button-primary" type="button" data-action="open-form" data-kind="${kind}"><svg><use href="#i-plus"></use></svg>${actionLabel}</button></div></section>`;
}

function metricCard(iconName, label, value, detail, tone) {
  return `<article class="metric-card"><div class="metric-top"><span>${label}</span><span class="metric-icon ${tone}"><svg><use href="#i-${iconName}"></use></svg></span></div><strong class="metric-value">${value}</strong><div class="metric-foot">${detail}</div></article>`;
}

function salesTable(sales, actions) {
  if (!sales.length) return '<div class="empty-state"><strong>No sales recorded yet</strong><span>Record a sale to begin building your ledger.</span></div>';
  return `<div class="table-wrap"><table><thead><tr><th>PRODUCT</th><th>SALE REFERENCE</th><th>AMOUNT</th>${actions ? '<th class="table-actions"><span class="sr-only">Actions</span></th>' : ''}</tr></thead><tbody>${sales.map(sale => `<tr><td class="primary-cell"><strong>${escapeHtml(sale.product)}</strong><small>Sales ledger</small></td><td>#${escapeHtml(sale.id)}</td><td class="amount-cell">${formatMoney(sale.amount)}</td>${actions ? `<td class="table-actions">${deleteButton('sales', sale.id, sale.product)}</td>` : ''}</tr>`).join('')}</tbody></table></div>`;
}

function deleteButton(resource, id, label) {
  return `<button class="row-delete" type="button" data-action="delete" data-resource="${resource}" data-id="${escapeHtml(id)}" data-label="${escapeHtml(label)}" title="Delete ${escapeHtml(label)}" aria-label="Delete ${escapeHtml(label)}"><svg><use href="#i-trash"></use></svg></button>`;
}

function emptyRow(columns, message) {
  return `<tr><td colspan="${columns}"><div class="empty-state"><strong>Nothing to show</strong><span>${message}</span></div></td></tr>`;
}

function stockStatus(quantity) {
  if (quantity <= 0) return '<span class="status-pill out"><i class="status-dot"></i>Out of stock</span>';
  if (quantity <= 5) return '<span class="status-pill low"><i class="status-dot"></i>Reorder soon</span>';
  return '<span class="status-pill"><i class="status-dot"></i>In stock</span>';
}

function revenueRows() {
  const totals = filtered(state.sales, ['product', 'id', 'amount']).reduce((groups, sale) => {
    const name = sale.product || 'Unassigned';
    groups[name] = (groups[name] || 0) + Number(sale.amount || 0);
    return groups;
  }, {});
  const entries = Object.entries(totals).sort((first, second) => second[1] - first[1]).slice(0, 6);
  const max = Math.max(0, ...entries.map(([, amount]) => amount));
  return entries.length ? entries.map(([name, amount]) => `<div class="bar-row"><span class="bar-label" title="${escapeHtml(name)}">${escapeHtml(name)}</span><span class="bar-track"><span class="bar-fill" style="width:${max ? Math.max(3, amount / max * 100) : 0}%"></span></span><strong class="bar-amount">${formatMoney(amount)}</strong></div>`).join('') : '<div class="chart-empty">There is no sales data to summarize yet.</div>';
}

function reportTotal(label, value) {
  return `<div class="stock-row"><span class="stock-name">${label}</span><strong class="amount-cell">${value}</strong></div>`;
}

function buildIntelligenceInsights() {
  if (state.intelligence && state.intelligence.length) {
    return state.intelligence.slice(0, 3).map(item => ({
      priority: item.priority || 'OPTIONAL',
      level: item.level || 'info',
      source: item.source || 'Insight',
      title: item.title || 'Action recommended',
      reason: item.reason || 'Review this item to keep operations on track.',
      action: item.action || 'Review detail',
      route: item.route || 'overview'
    }));
  }

  const lowStockItems = state.inventory.filter(item => Number(item.quantity) <= 5);
  const revenueItems = Object.entries(
    state.sales.reduce((groups, sale) => {
      const product = sale.product || 'Unassigned';
      groups[product] = (groups[product] || 0) + Number(sale.amount || 0);
      return groups;
    }, {})
  ).sort((first, second) => second[1] - first[1]);

  const insights = [];

  if (lowStockItems.length) {
    const item = lowStockItems[0];
    insights.push({
      priority: 'URGENT',
      level: 'critical',
      source: 'Inventory',
      title: `${lowStockItems.length} product${lowStockItems.length === 1 ? '' : 's'} need attention`,
      reason: `${item.name} is down to ${item.quantity} units and should be reviewed before the next cycle.`,
      action: 'Review stock levels',
      route: 'inventory'
    });
  }

  if (revenueItems.length) {
    const [topProduct, topRevenue] = revenueItems[0];
    insights.push({
      priority: 'RECOMMENDED',
      level: 'warning',
      source: 'Sales',
      title: `${topProduct} is leading the sales mix`,
      reason: `${formatMoney(topRevenue)} in recorded sales suggests a strong demand pattern worth monitoring.`,
      action: 'View sales performance',
      route: 'sales'
    });
  }

  if (state.employees.length) {
    insights.push({
      priority: 'OPTIONAL',
      level: 'info',
      source: 'People',
      title: 'Team capacity looks balanced',
      reason: 'The current team size supports active monitoring without immediate staffing risk.',
      action: 'Review workforce overview',
      route: 'employees'
    });
  }

  return insights.slice(0, 3);
}

function filtered(records, keys) {
  return records.filter(record => matchesQuery(record, keys));
}

function matchesQuery(record, keys) {
  const query = state.query.trim().toLowerCase();
  return !query || keys.some(key => String(record[key] ?? '').toLowerCase().includes(query));
}

function openForm(kind) {
  const forms = {
    employee: {
      title: 'Add employee',
      endpoint: '/api/employees',
      fields: [['name', 'Full name', 'text', 'e.g. Priya Mehta'], ['salary', 'Monthly salary (INR)', 'number', 'e.g. 42000']]
    },
    inventory: {
      title: 'Add product',
      endpoint: '/api/inventory',
      fields: [['name', 'Product name', 'text', 'e.g. Canvas tote'], ['quantity', 'Quantity on hand', 'number', 'e.g. 24']]
    },
    sale: {
      title: 'Record sale',
      endpoint: '/api/sales',
      fields: [['product', 'Product', 'text', 'e.g. Canvas tote'], ['amount', 'Sale amount (INR)', 'number', 'e.g. 1800']]
    }
  };
  const config = forms[kind];
  if (!config) return;
  recordForm.dataset.endpoint = config.endpoint;
  document.querySelector('#dialog-title').textContent = config.title;
  document.querySelector('#dialog-fields').innerHTML = config.fields.map(([name, label, type, placeholder], index) => {
    const numberAttributes = name === 'quantity' ? 'min="1" step="1"' : 'min="0.01" step="any"';
    return `<label class="field-label">${label}<input name="${name}" type="${type}" placeholder="${placeholder}" ${type === 'number' ? numberAttributes : 'maxlength="80"'} required ${index === 0 ? 'autofocus' : ''}>${name === 'name' ? '<span class="field-hint">Use up to 80 characters; commas are not supported by the file format.</span>' : ''}</label>`;
  }).join('');
  dialog.showModal();
  dialog.querySelector('input')?.focus();
}

async function saveRecord(event) {
  event.preventDefault();
  const button = recordForm.querySelector('[type="submit"]');
  button.disabled = true;
  try {
    await api(recordForm.dataset.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: new URLSearchParams(new FormData(recordForm))
    });
    dialog.close();
    recordForm.reset();
    state.query = '';
    document.querySelector('#global-search').value = '';
    await refreshData();
    toast('Record saved to the local workspace.');
  } catch (error) {
    toast(error.message, true);
  } finally {
    button.disabled = false;
  }
}

async function deleteRecord(button) {
  const { resource, id, label } = button.dataset;
  if (!window.confirm(`Delete ${label} from this workspace?`)) return;
  try {
    await api(`/api/${resource}/${encodeURIComponent(id)}`, { method: 'DELETE' });
    await refreshData();
    toast('Record deleted.');
  } catch (error) {
    toast(error.message, true);
  }
}

function exportReport() {
  const rows = [
    ['Section', 'Reference', 'Name / Product', 'Value'],
    ...state.employees.map(item => ['Employees', item.id, item.name, item.salary]),
    ...state.inventory.map(item => ['Inventory', item.id, item.name, item.quantity]),
    ...state.sales.map(item => ['Sales', item.id, item.product, item.amount])
  ];
  const csv = rows.map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'erm-workspace-report.csv';
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  toast('Report exported as CSV.');
}

function toast(message, isError = false) {
  const element = document.createElement('div');
  element.className = `toast${isError ? ' error' : ''}`;
  element.innerHTML = `<svg><use href="#i-${isError ? 'bell' : 'check'}"></use></svg><span>${escapeHtml(message)}</span>`;
  document.querySelector('#toast-region').append(element);
  window.setTimeout(() => element.remove(), 3600);
}

function formatMoney(value) {
  return currency.format(Number.isFinite(Number(value)) ? Number(value) : 0);
}

function formatDate(date) {
  return new Intl.DateTimeFormat('en-IN', { weekday: 'long', day: 'numeric', month: 'long' }).format(date);
}

function greeting() {
  const hour = new Date().getHours();
  return hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening';
}

function initials(name) {
  return String(name || '?').trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
}

document.addEventListener('click', event => {
  const routeButton = event.target.closest('[data-route]');
  if (routeButton) {
    state.route = routeButton.dataset.route;
    state.query = '';
    document.querySelector('#global-search').value = '';
    render();
    return;
  }
  const actionButton = event.target.closest('[data-action]');
  if (!actionButton) return;
  if (actionButton.dataset.action === 'open-form') openForm(actionButton.dataset.kind);
  if (actionButton.dataset.action === 'close-dialog') dialog.close();
  if (actionButton.dataset.action === 'refresh') refreshData(true);
  if (actionButton.dataset.action === 'export') exportReport();
  if (actionButton.dataset.action === 'delete') deleteRecord(actionButton);
});

document.querySelector('#global-search').addEventListener('input', event => {
  state.query = event.target.value;
  render();
});

recordForm.addEventListener('submit', saveRecord);
dialog.addEventListener('click', event => {
  if (event.target === dialog) dialog.close();
});

document.addEventListener('keydown', event => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault();
    document.querySelector('#global-search').focus();
  }
  if ((event.metaKey || event.ctrlKey) && /^[1-4]$/.test(event.key)) {
    const routes = ['overview', 'employees', 'inventory', 'sales'];
    state.route = routes[Number(event.key) - 1];
    state.query = '';
    document.querySelector('#global-search').value = '';
    render();
  }
});

refreshData();
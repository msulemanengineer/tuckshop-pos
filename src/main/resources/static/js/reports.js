var presets = [
  { label: 'Today', days: 0 },
  { label: 'Yesterday', days: 1, single: true },
  { label: 'Last 7 days', days: 6 },
  { label: 'This month', month: true },
  { label: 'Last month', lastMonth: true },
  { label: 'This quarter', quarter: true },
  { label: 'This year', year: true },
  { label: 'All time', all: true }
];

function toISO(date) {
  return date.toISOString().slice(0, 10);
}

function applyPreset(preset) {
  var today = new Date();
  var from, to;

  if (preset.single) {
    var y = new Date(today); y.setDate(y.getDate() - preset.days);
    from = y; to = y;
  } else if (preset.month) {
    from = new Date(today.getFullYear(), today.getMonth(), 1);
    to = today;
  } else if (preset.lastMonth) {
    from = new Date(today.getFullYear(), today.getMonth() - 1, 1);
    to = new Date(today.getFullYear(), today.getMonth(), 0);
  } else if (preset.quarter) {
    var q = Math.floor(today.getMonth() / 3);
    from = new Date(today.getFullYear(), q * 3, 1);
    to = today;
  } else if (preset.year) {
    from = new Date(today.getFullYear(), 0, 1);
    to = today;
  } else if (preset.all) {
    from = new Date(2020, 0, 1);
    to = today;
  } else {
    from = new Date(today); from.setDate(from.getDate() - preset.days);
    to = today;
  }

  document.getElementById('from-date').value = toISO(from);
  document.getElementById('to-date').value = toISO(to);
  runReport();
}

function renderPresets() {
  var row = document.getElementById('preset-row');
  row.innerHTML = presets.map(function (p, i) {
    return '<button class="preset-btn" data-i="' + i + '">' + p.label + '</button>';
  }).join('');
  row.querySelectorAll('.preset-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      row.querySelectorAll('.preset-btn').forEach(function (b) { b.classList.remove('active'); });
      btn.classList.add('active');
      applyPreset(presets[parseInt(btn.getAttribute('data-i'))]);
    });
  });
}

function loadCustomerFilter() {
  apiCall('/api/customers').then(function (customers) {
    var select = document.getElementById('customer-filter');
    customers.forEach(function (c) {
      var opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = c.name;
      select.appendChild(opt);
    });
  });
}

function runReport() {
  var from = document.getElementById('from-date').value;
  var to = document.getElementById('to-date').value;
  if (!from || !to) return;

  var customerId = document.getElementById('customer-filter').value;
  var paymentMethod = document.getElementById('payment-filter').value;

  var params = new URLSearchParams({ from: from, to: to });
  if (customerId) params.set('customerId', customerId);
  if (paymentMethod) params.set('paymentMethod', paymentMethod);

  apiCall('/api/reports?' + params.toString()).then(renderReport)
    .catch(function (err) { showToast(err.message, 'error'); });
}

function renderReport(r) {
  countUp(document.getElementById('stat-revenue'), Math.round(r.totalRevenue), 'Rs ');
  countUp(document.getElementById('stat-cash'), Math.round(r.cashCollected), 'Rs ');
  countUp(document.getElementById('stat-transactions'), r.totalTransactions, '');
  countUp(document.getElementById('stat-items'), r.totalItemsSold, '');
  countUp(document.getElementById('stat-profit'), Math.round(r.totalProfit), 'Rs ');

  var maxDay = Math.max.apply(null, r.byDay.map(function (d) { return Number(d.total); }).concat([1]));
  var chart = document.getElementById('day-chart');
  chart.innerHTML = '';
  r.byDay.forEach(function (d) {
    var col = document.createElement('div');
    col.className = 'bar-col';
    var bar = document.createElement('div');
    bar.className = 'bar';
    bar.title = formatMoney(d.total);
    var label = document.createElement('div');
    label.className = 'day-label';
    label.textContent = d.date.slice(5);
    col.appendChild(bar);
    col.appendChild(label);
    chart.appendChild(col);
    var pct = maxDay > 0 ? (Number(d.total) / maxDay) * 130 : 0;
    setTimeout(function () { bar.style.height = pct + 'px'; }, 60);
  });

  var methodEl = document.getElementById('method-breakdown');
  methodEl.innerHTML = r.byPaymentMethod.length === 0
    ? '<div class="empty-state">No sales in this range.</div>'
    : r.byPaymentMethod.map(function (m) {
        return '<div class="cart-item"><div class="name">' + m.method + '</div><div class="name">' + formatMoney(m.total) + '</div></div>';
      }).join('');

  var productsTable = document.getElementById('products-table');
  productsTable.innerHTML = r.topProducts.map(function (p) {
    return '<tr><td>' + p.name + '</td><td>' + p.qty + '</td>' +
      '<td style="text-align:right;">' + formatMoney(p.costValue) + '</td>' +
      '<td style="text-align:right;">' + formatMoney(p.revenue) + '</td>' +
      '<td style="text-align:right; font-weight:700; color:' + (Number(p.profit) >= 0 ? 'var(--green-dark)' : 'var(--red)') + ';">' + formatMoney(p.profit) + '</td></tr>';
  }).join('');

  var customersTable = document.getElementById('customers-table');
  var customersEmpty = document.getElementById('customers-empty');
  if (r.byCustomer.length === 0) {
    customersTable.innerHTML = '';
    customersEmpty.style.display = 'block';
  } else {
    customersEmpty.style.display = 'none';
    customersTable.innerHTML = r.byCustomer.map(function (c) {
      return '<tr><td>' + c.customer + '</td><td style="text-align:right;">' + formatMoney(c.total) + '</td></tr>';
    }).join('');
  }

  var txTable = document.getElementById('transactions-table');
  txTable.innerHTML = r.transactions.map(function (t) {
    var dt = new Date(t.date).toLocaleString('en-PK', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    return '<tr>' +
      '<td>#' + t.id + '</td><td>' + dt + '</td><td>' + t.itemCount + '</td>' +
      '<td><span class="badge badge-ok">' + t.paymentMethod + '</span></td>' +
      '<td>' + (t.cashier || '-') + '</td><td>' + (t.customer || '-') + '</td>' +
      '<td style="text-align:right; font-weight:700;">' + formatMoney(t.total) + '</td>' +
    '</tr>';
  }).join('');
}

function exportReport(format) {
  var from = document.getElementById('from-date').value;
  var to = document.getElementById('to-date').value;
  if (!from || !to) { showToast('Pick a date range first.', 'error'); return; }
  var customerId = document.getElementById('customer-filter').value;
  var paymentMethod = document.getElementById('payment-filter').value;
  var params = new URLSearchParams({ from: from, to: to });
  if (customerId) params.set('customerId', customerId);
  if (paymentMethod) params.set('paymentMethod', paymentMethod);
  window.open('/api/reports/export/' + format + '?' + params.toString(), '_blank');
}

document.getElementById('from-date').addEventListener('change', runReport);
document.getElementById('to-date').addEventListener('change', runReport);
document.getElementById('customer-filter').addEventListener('change', runReport);
document.getElementById('payment-filter').addEventListener('change', runReport);

function loadActivityLog() {
  apiCall('/api/activity-log').then(function (logs) {
    var el = document.getElementById('activity-log');
    if (logs.length === 0) {
      el.innerHTML = '<div class="empty-state">No staff activity recorded yet.</div>';
      return;
    }
    el.innerHTML = logs.map(function (log) {
      var dt = new Date(log.createdAt).toLocaleString('en-PK', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
      return '<div class="cart-item"><div><div class="name">' + log.action.replace(/_/g, ' ') +
        '</div><div class="meta">' + log.username + ' &middot; ' + dt + ' &middot; ' + log.details + '</div></div></div>';
    }).join('');
  });
}

renderPresets();
loadCustomerFilter();
loadActivityLog();
applyPreset(presets[0]); // default to "Today"
document.querySelector('.preset-btn').classList.add('active');

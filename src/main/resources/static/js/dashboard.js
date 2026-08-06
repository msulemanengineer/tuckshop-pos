document.getElementById('today-date').textContent =
  new Date().toLocaleDateString('en-PK', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

function loadStats() {
  apiCall('/api/dashboard/stats')
    .then(renderStats)
    .catch(function (err) { showToast(err.message, 'error'); });
}

function renderStats(stats) {
  countUp(document.getElementById('stat-sales'), Math.round(stats.todaySales), 'Rs ');
  countUp(document.getElementById('stat-cash'), Math.round(stats.cashCollectedToday), 'Rs ');
  var profitEl = document.getElementById('stat-profit');
  if (profitEl) {
    countUp(profitEl, Math.round(stats.todayProfit), 'Rs ');
  }
  countUp(document.getElementById('stat-transactions'), stats.todayTransactions, '');
  var stockValueEl = document.getElementById('stat-stock-value');
  if (stockValueEl) {
    countUp(stockValueEl, Math.round(stats.stockValue), 'Rs ');
  }
  document.getElementById('stat-low-stock').textContent = stats.lowStockCount;

  // Weekly bar chart, hand-rolled with CSS bars -> no external chart library needed offline
  var maxTotal = Math.max.apply(null, stats.weeklySales.map(function (d) { return Number(d.total); }).concat([1]));
  var chart = document.getElementById('weekly-chart');
  chart.innerHTML = '';
  stats.weeklySales.forEach(function (day) {
    var col = document.createElement('div');
    col.className = 'bar-col';
    var bar = document.createElement('div');
    bar.className = 'bar';
    bar.title = formatMoney(day.total);
    col.innerHTML = '<div class="bar-wrap" style="width:100%; display:flex; justify-content:center;"></div>';
    var label = document.createElement('div');
    label.className = 'day-label';
    label.textContent = day.label;
    col.appendChild(bar);
    col.appendChild(label);
    chart.appendChild(col);
    var pct = maxTotal > 0 ? (Number(day.total) / maxTotal) * 130 : 0;
    setTimeout(function () { bar.style.height = pct + 'px'; }, 60);
  });

  var topSellers = document.getElementById('top-sellers');
  if (stats.topSellers.length === 0) {
    topSellers.innerHTML = '<div class="empty-state">No sales yet this week.</div>';
  } else {
    topSellers.innerHTML = stats.topSellers.map(function (item) {
      return '<div class="cart-item"><div class="name">' + item.name +
        '</div><div class="meta">' + item.qty + ' sold</div></div>';
    }).join('');
  }

  var lowStock = document.getElementById('low-stock-list');
  if (stats.lowStockItems.length === 0) {
    lowStock.innerHTML = '<div class="empty-state">All stock levels look healthy.</div>';
  } else {
    lowStock.innerHTML = stats.lowStockItems.map(function (item) {
      return '<div class="cart-item"><div class="name">' + item.name +
        '</div><div class="badge badge-danger">' + item.quantity + ' left</div></div>';
    }).join('');
  }

  var recent = document.getElementById('recent-sales');
  if (stats.recentSales.length === 0) {
    recent.innerHTML = '<div class="empty-state">No transactions yet.</div>';
  } else {
    recent.innerHTML = stats.recentSales.map(function (sale) {
      var time = new Date(sale.time).toLocaleTimeString('en-PK', { hour: '2-digit', minute: '2-digit' });
      return '<div class="cart-item"><div><div class="name">#' + sale.id + ' &middot; ' + sale.itemCount +
        ' items</div><div class="meta">' + time + '</div></div><div class="name">' + formatMoney(sale.total) + '</div></div>';
    }).join('');
  }
}

loadStats();
setInterval(loadStats, 30000); // auto-refresh every 30s so the owner sees near-live numbers

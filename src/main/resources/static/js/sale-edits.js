function loadSaleEdits() {
  apiCall('/api/sale-edits').then(function (edits) {
    renderSummary(edits);
    renderEdits(edits);
  }).catch(function (err) { showToast(err.message, 'error'); });
}

function renderSummary(edits) {
  var rows = document.getElementById('cashier-summary-rows');
  var empty = document.getElementById('summary-empty');
  if (!edits.length) {
    rows.innerHTML = '';
    empty.style.display = 'block';
    return;
  }
  empty.style.display = 'none';

  var byCashier = {};
  edits.forEach(function (e) {
    var key = e.cashierUsername || 'unknown';
    if (!byCashier[key]) byCashier[key] = { count: 0, total: 0 };
    byCashier[key].count += 1;
    byCashier[key].total += Number(e.amountRemoved || 0);
  });

  rows.innerHTML = Object.keys(byCashier).sort(function (a, b) {
    return byCashier[b].total - byCashier[a].total;
  }).map(function (cashier) {
    var s = byCashier[cashier];
    return '<tr>' +
      '<td><strong>' + cashier + '</strong></td>' +
      '<td>' + s.count + '</td>' +
      '<td style="text-align:right; font-weight:700;">' + formatMoney(s.total) + '</td>' +
    '</tr>';
  }).join('');
}

function renderEdits(edits) {
  var rows = document.getElementById('edit-rows');
  var empty = document.getElementById('edits-empty');
  if (!edits.length) {
    rows.innerHTML = '';
    empty.style.display = 'block';
    return;
  }
  empty.style.display = 'none';

  rows.innerHTML = edits.map(function (e) {
    var dt = new Date(e.editedAt).toLocaleString('en-PK', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
    return '<tr>' +
      '<td>' + dt + '</td>' +
      '<td><strong>#' + e.saleId + '</strong></td>' +
      '<td>' + e.cashierUsername + '</td>' +
      '<td>' + e.productName + '</td>' +
      '<td>' + e.oldQuantity + ' &rarr; ' + e.newQuantity + '</td>' +
      '<td style="text-align:right; font-weight:700; color:#B91C1C;">-' + formatMoney(e.amountRemoved) + '</td>' +
      '<td>' + (e.reason || '-') + '</td>' +
    '</tr>';
  }).join('');
}

loadSaleEdits();

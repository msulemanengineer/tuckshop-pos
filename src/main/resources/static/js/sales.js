var allSales = [];

function loadSales() {
  apiCall('/api/sales').then(function (sales) {
    allSales = sales;
    var rows = document.getElementById('sales-rows');
    var empty = document.getElementById('sales-empty');

    if (sales.length === 0) {
      empty.style.display = 'block';
      return;
    }
    empty.style.display = 'none';

    rows.innerHTML = sales.map(function (sale) {
      var dt = new Date(sale.saleDate).toLocaleString('en-PK', {
        day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
      });
      var itemsSummary = sale.items.map(function (i) { return i.productName + ' x' + i.quantity; }).join(', ');
      var isVoided = sale.status === 'VOIDED';
      var paymentBadge = sale.paymentMethod === 'KHATA'
        ? '<span class="badge badge-warn">KHATA' + (sale.customerNameSnapshot ? ' - ' + sale.customerNameSnapshot : '') + '</span>'
        : '<span class="badge badge-ok">' + sale.paymentMethod + '</span>';

      return '<tr style="' + (isVoided ? 'opacity:0.5;' : '') + '">' +
        '<td><strong>#' + sale.id + '</strong>' + (isVoided ? ' <span class="badge badge-danger">VOIDED</span>' : '') + '</td>' +
        '<td>' + dt + '</td>' +
        '<td>' + itemsSummary + '</td>' +
        '<td>' + (sale.cashierUsername || '-') + '</td>' +
        '<td>' + paymentBadge + '</td>' +
        '<td style="text-align:right; font-weight:700;">' + formatMoney(sale.totalAmount) + '</td>' +
        '<td style="text-align:right; white-space:nowrap;">' +
          (isVoided ? '' :
            '<button class="btn" style="padding:8px 14px; font-size:13px;" onclick="openEditModal(' + sale.id + ')">Edit qty</button> ' +
            '<button class="btn btn-danger" style="padding:8px 14px; font-size:13px;" onclick="openVoidModal(' + sale.id + ')">Void</button>') +
        '</td>' +
      '</tr>';
    }).join('');
  }).catch(function (err) { showToast(err.message, 'error'); });
}

function closeModal() {
  document.getElementById('modal-root').innerHTML = '';
}

function openVoidModal(saleId) {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Void sale #' + saleId + '</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="subtitle" style="margin-bottom:16px;">This restocks the items and reverses any khata charge. Needs the owner\'s PIN - call the owner if they\'re not around.</div>' +
        '<div class="field"><label>Reason</label><input id="void-reason" placeholder="e.g. wrong item scanned" autofocus></div>' +
        '<div class="field"><label>Owner PIN</label><input id="void-pin" type="password" placeholder="4-digit PIN"></div>' +
        '<button class="btn btn-danger btn-lg btn-block" onclick="submitVoid(' + saleId + ')">Void this sale</button>' +
      '</div>' +
    '</div>';
}

function submitVoid(saleId) {
  var payload = {
    reason: document.getElementById('void-reason').value.trim(),
    ownerPin: document.getElementById('void-pin').value.trim()
  };
  if (!payload.reason) {
    showToast('Enter a reason for the void.', 'error');
    return;
  }
  apiCall('/api/sales/' + saleId + '/void', { method: 'POST', body: JSON.stringify(payload) })
    .then(function () {
      showToast('Sale #' + saleId + ' voided.', 'success');
      closeModal();
      loadSales();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function findSale(saleId) {
  return allSales.find(function (s) { return s.id === saleId; });
}

function openEditModal(saleId) {
  var sale = findSale(saleId);
  if (!sale) return;

  var itemRows = sale.items.map(function (item) {
    return '<div class="field" style="display:flex; align-items:center; gap:10px; margin-bottom:10px;">' +
      '<div style="flex:1;">' + item.productName + ' <span style="color:var(--ink-soft);">(currently ' + item.quantity + ')</span></div>' +
      '<input type="number" min="0" max="' + item.quantity + '" value="' + item.quantity + '" ' +
        'id="edit-qty-' + item.id + '" data-item-id="' + item.id + '" data-old-qty="' + item.quantity + '" style="width:80px;">' +
    '</div>';
  }).join('');

  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Edit sale #' + saleId + '</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="subtitle" style="margin-bottom:16px;">Lower a quantity if the customer wanted less than what was rung up. Stock, total, and khata (if any) update automatically. Needs the owner\'s PIN - call the owner if they\'re not around.</div>' +
        itemRows +
        '<div class="field"><label>Reason</label><input id="edit-reason" placeholder="e.g. customer returned 1 item" autofocus></div>' +
        '<div class="field"><label>Owner PIN</label><input id="edit-pin" type="password" placeholder="4-digit PIN"></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitEdit(' + saleId + ')">Save changes</button>' +
      '</div>' +
    '</div>';
}

function submitEdit(saleId) {
  var sale = findSale(saleId);
  if (!sale) return;

  var reason = document.getElementById('edit-reason').value.trim();
  var ownerPin = document.getElementById('edit-pin').value.trim();
  if (!reason) {
    showToast('Enter a reason for the change.', 'error');
    return;
  }
  if (!ownerPin) {
    showToast('Enter the owner PIN.', 'error');
    return;
  }

  var changes = sale.items.map(function (item) {
    var input = document.getElementById('edit-qty-' + item.id);
    return { itemId: item.id, oldQty: Number(input.dataset.oldQty), newQty: Number(input.value) };
  }).filter(function (c) { return c.newQty !== c.oldQty; });

  if (changes.length === 0) {
    showToast('No quantities were changed.', 'error');
    return;
  }
  for (var i = 0; i < changes.length; i++) {
    if (changes[i].newQty < 0 || changes[i].newQty >= changes[i].oldQty) {
      showToast('New quantity must be less than the current quantity.', 'error');
      return;
    }
  }

  var chain = Promise.resolve();
  changes.forEach(function (c) {
    chain = chain.then(function () {
      return apiCall('/api/sales/' + saleId + '/items/' + c.itemId + '/edit', {
        method: 'POST',
        body: JSON.stringify({ newQuantity: c.newQty, reason: reason, ownerPin: ownerPin })
      });
    });
  });

  chain.then(function () {
    showToast('Sale #' + saleId + ' updated.', 'success');
    closeModal();
    loadSales();
  }).catch(function (err) { showToast(err.message, 'error'); });
}

loadSales();

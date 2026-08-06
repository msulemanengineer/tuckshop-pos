var allCustomers = [];

function loadCustomers(term) {
  var url = '/api/customers' + (term ? '?q=' + encodeURIComponent(term) : '');
  apiCall(url).then(function (customers) {
    allCustomers = customers;
    renderCustomers(customers);
  }).catch(function (err) { showToast(err.message, 'error'); });

  apiCall('/api/customers/outstanding-total').then(function (total) {
    var el = document.getElementById('stat-total-outstanding');
    countUp(el, Math.round(total), 'Rs ');
  });
}

function renderCustomers(customers) {
  var rows = document.getElementById('customer-rows');
  var empty = document.getElementById('customer-empty');

  if (customers.length === 0) {
    rows.innerHTML = '';
    empty.style.display = 'block';
    return;
  }
  empty.style.display = 'none';

  rows.innerHTML = customers.map(function (c) {
    var overLimit = Number(c.currentBalance) > Number(c.creditLimit);
    var badgeClass = Number(c.currentBalance) === 0 ? 'badge-ok' : (overLimit ? 'badge-danger' : 'badge-warn');
    return '<tr>' +
      '<td><strong style="cursor:pointer;" onclick="openLedger(' + c.id + ')">' + c.name + '</strong></td>' +
      '<td>' + (c.phone || '-') + '</td>' +
      '<td>' + formatMoney(c.creditLimit) + '</td>' +
      '<td><span class="badge ' + badgeClass + '">' + formatMoney(c.currentBalance) + '</span></td>' +
      '<td style="text-align:right; white-space:nowrap;">' +
        '<button class="btn" style="padding:8px 14px; font-size:14px;" onclick="openPaymentModal(' + c.id + ', \'' + c.name.replace(/'/g, "\\'") + '\')">Record payment</button> ' +
        (window.isOwner ? '<button class="btn" style="padding:8px 14px; font-size:14px;" onclick="openEditCustomerModal(' + c.id + ')">Edit</button>' : '') +
      '</td>' +
    '</tr>';
  }).join('');
}

var searchTimer = null;
document.getElementById('customer-search').addEventListener('input', function (e) {
  clearTimeout(searchTimer);
  var term = e.target.value.trim();
  searchTimer = setTimeout(function () { loadCustomers(term); }, 250);
});

function closeModal() {
  document.getElementById('modal-root').innerHTML = '';
}

function openAddCustomerModal() {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Add customer</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>Name</label><input id="c-name" placeholder="e.g. Malik Sahab" autofocus></div>' +
        '<div class="field"><label>Phone (optional)</label><input id="c-phone" placeholder="03xx-xxxxxxx"></div>' +
        '<div class="field"><label>Credit limit (Rs)</label><input id="c-limit" type="number" value="2000"></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitAddCustomer()">Add customer</button>' +
      '</div>' +
    '</div>';
}

function submitAddCustomer() {
  var payload = {
    name: document.getElementById('c-name').value.trim(),
    phone: document.getElementById('c-phone').value.trim(),
    creditLimit: parseFloat(document.getElementById('c-limit').value) || 2000,
    currentBalance: 0
  };
  if (!payload.name) {
    showToast('Enter the customer\'s name.', 'error');
    return;
  }
  apiCall('/api/customers', { method: 'POST', body: JSON.stringify(payload) })
    .then(function () {
      showToast('Customer added.', 'success');
      closeModal();
      loadCustomers();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function openEditCustomerModal(id) {
  var c = allCustomers.find(function (x) { return x.id === id; });
  if (!c) return;
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Edit ' + c.name + '</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>Name</label><input id="c-name" value="' + c.name.replace(/"/g,'&quot;') + '"></div>' +
        '<div class="field"><label>Phone</label><input id="c-phone" value="' + (c.phone || '').replace(/"/g,'&quot;') + '"></div>' +
        '<div class="field"><label>Credit limit (Rs)</label><input id="c-limit" type="number" value="' + c.creditLimit + '"></div>' +
        '<button class="btn btn-success btn-lg btn-block" style="margin-bottom:10px;" onclick="submitEditCustomer(' + c.id + ')">Save changes</button>' +
        '<button class="btn btn-danger btn-block" onclick="deleteCustomer(' + c.id + ', \'' + c.name.replace(/'/g, "\\'") + '\')">Delete customer</button>' +
      '</div>' +
    '</div>';
}

function submitEditCustomer(id) {
  var c = allCustomers.find(function (x) { return x.id === id; });
  var payload = {
    name: document.getElementById('c-name').value.trim(),
    phone: document.getElementById('c-phone').value.trim(),
    creditLimit: parseFloat(document.getElementById('c-limit').value) || 0,
    currentBalance: c.currentBalance
  };
  apiCall('/api/customers/' + id, { method: 'PUT', body: JSON.stringify(payload) })
    .then(function () {
      showToast('Saved.', 'success');
      closeModal();
      loadCustomers();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function deleteCustomer(id, name) {
  if (!confirm('Delete "' + name + '"? This cannot be undone.')) return;
  apiCall('/api/customers/' + id, { method: 'DELETE' })
    .then(function () {
      showToast('Customer deleted.', 'success');
      closeModal();
      loadCustomers();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function openPaymentModal(id, name) {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Payment from ' + name + '</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>Amount received (Rs)</label><input id="p-amount" type="number" autofocus></div>' +
        '<div class="field"><label>Note (optional)</label><input id="p-note" placeholder="e.g. partial payment"></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitPayment(' + id + ')">Record payment</button>' +
      '</div>' +
    '</div>';
}

function submitPayment(id) {
  var payload = {
    amount: parseFloat(document.getElementById('p-amount').value) || 0,
    note: document.getElementById('p-note').value.trim()
  };
  apiCall('/api/customers/' + id + '/payments', { method: 'POST', body: JSON.stringify(payload) })
    .then(function () {
      showToast('Payment recorded.', 'success');
      closeModal();
      loadCustomers();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function openLedger(id) {
  var c = allCustomers.find(function (x) { return x.id === id; });
  apiCall('/api/customers/' + id + '/ledger').then(function (ledger) {
    var rows = ledger.length === 0
      ? '<div class="empty-state">No transactions yet.</div>'
      : ledger.map(function (tx) {
          var isCharge = tx.type === 'CHARGE';
          return '<div class="cart-item">' +
            '<div><div class="name">' + (isCharge ? 'Bought on credit' : 'Payment received') +
            '</div><div class="meta">' + new Date(tx.createdAt).toLocaleString('en-PK', {day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit'}) +
            (tx.note ? ' &middot; ' + tx.note : '') + '</div></div>' +
            '<div class="badge ' + (isCharge ? 'badge-danger' : 'badge-ok') + '">' + (isCharge ? '+' : '-') + formatMoney(tx.amount) + '</div></div>';
        }).join('');

    document.getElementById('modal-root').innerHTML =
      '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
        '<div class="modal">' +
          '<div class="modal-header"><h2>' + c.name + '\'s ledger</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
          '<div style="margin-bottom:14px; font-weight:700;">Currently owes: ' + formatMoney(c.currentBalance) + '</div>' +
          rows +
        '</div>' +
      '</div>';
  });
}

loadCustomers();

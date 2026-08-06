var cart = []; // { productId, name, unitPrice, quantity, availableQty, unit }
var selectedPayment = 'CASH';
var selectedCustomer = null; // { id, name } - only used for KHATA

var scanInput = document.getElementById('scan-input');
var searchTimer = null;

scanInput.addEventListener('keydown', function (e) {
  if (e.key === 'Enter') {
    e.preventDefault();
    handleScanOrSearch(scanInput.value.trim());
  }
});

scanInput.addEventListener('input', function () {
  clearTimeout(searchTimer);
  var term = scanInput.value.trim();
  if (term.length < 2) {
    document.getElementById('search-results').innerHTML = '';
    return;
  }
  searchTimer = setTimeout(function () { liveSearch(term); }, 250);
});

function handleScanOrSearch(term) {
  if (!term) return;
  apiCall('/api/products/barcode/' + encodeURIComponent(term))
    .then(function (product) {
      addToCart(product);
      scanInput.value = '';
      document.getElementById('search-results').innerHTML = '';
      scanInput.focus();
    })
    .catch(function () {
      liveSearch(term);
    });
}

function liveSearch(term) {
  apiCall('/api/products?q=' + encodeURIComponent(term)).then(function (results) {
    var container = document.getElementById('search-results');
    if (results.length === 0) {
      container.innerHTML = '<div class="empty-state">No product found for "' + term + '"</div>';
      return;
    }
    container.innerHTML = results.slice(0, 8).map(function (p) {
      var lowClass = p.quantity <= p.lowStockThreshold ? 'badge-danger' : 'badge-ok';
      return '<div class="cart-item" style="cursor:pointer" onclick="addProductById(' + p.id + ')">' +
        '<div><div class="name">' + p.name + '</div><div class="meta">' + p.barcode + ' &middot; ' + formatMoney(p.sellingPrice) + '</div></div>' +
        '<div class="badge ' + lowClass + '">' + p.quantity + ' in stock</div></div>';
    }).join('');
  });
}

function addProductById(id) {
  apiCall('/api/products/' + id).then(function (product) {
    addToCart(product);
    scanInput.value = '';
    scanInput.focus();
  });
}

function addToCart(product) {
  if (product.quantity <= 0) {
    showToast(product.name + ' is out of stock.', 'error');
    return;
  }
  var existing = cart.find(function (c) { return c.productId === product.id; });
  if (existing) {
    if (existing.quantity + 1 > product.quantity) {
      showToast('Only ' + product.quantity + ' of ' + product.name + ' left in stock.', 'error');
      return;
    }
    existing.quantity += 1;
  } else {
    cart.push({
      productId: product.id,
      name: product.name,
      unitPrice: Number(product.sellingPrice),
      quantity: 1,
      availableQty: product.quantity,
      unit: product.unit
    });
  }
  renderCart();
  showToast(product.name + ' added', 'success');
}

function changeQty(productId, delta) {
  var item = cart.find(function (c) { return c.productId === productId; });
  if (!item) return;
  var newQty = item.quantity + delta;
  if (newQty <= 0) {
    cart = cart.filter(function (c) { return c.productId !== productId; });
  } else if (newQty > item.availableQty) {
    showToast('Only ' + item.availableQty + ' left in stock.', 'error');
    return;
  } else {
    item.quantity = newQty;
  }
  renderCart();
}

function removeItem(productId) {
  cart = cart.filter(function (c) { return c.productId !== productId; });
  renderCart();
}

function renderCart() {
  var listEl = document.getElementById('cart-list');
  var checkoutBtn = document.getElementById('checkout-btn');

  if (cart.length === 0) {
    listEl.innerHTML = '<div class="empty-state" id="cart-empty">' +
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/></svg>' +
      '<div>Cart is empty. Scan an item to begin.</div></div>';
  } else {
    listEl.innerHTML = cart.map(function (item) {
      var subtotal = item.unitPrice * item.quantity;
      return '<div class="cart-item">' +
        '<div><div class="name">' + item.name + '</div><div class="meta">' + formatMoney(item.unitPrice) + ' each</div></div>' +
        '<div class="qty-control">' +
          '<button class="qty-btn" onclick="changeQty(' + item.productId + ', -1)">-</button>' +
          '<span style="min-width:20px; text-align:center; font-weight:700;">' + item.quantity + '</span>' +
          '<button class="qty-btn" onclick="changeQty(' + item.productId + ', 1)">+</button>' +
        '</div>' +
        '<div class="name" style="min-width:70px; text-align:right;">' + formatMoney(subtotal) + '</div>' +
      '</div>';
    }).join('');
  }

  var total = cart.reduce(function (sum, item) { return sum + item.unitPrice * item.quantity; }, 0);
  document.getElementById('cart-total').textContent = formatMoney(total);
  updateCheckoutEnabled();
}

function updateCheckoutEnabled() {
  var btn = document.getElementById('checkout-btn');
  var needsCustomer = selectedPayment === 'KHATA' && !selectedCustomer;
  btn.disabled = cart.length === 0 || needsCustomer;
}

document.querySelectorAll('.pay-option').forEach(function (el) {
  el.addEventListener('click', function () {
    document.querySelectorAll('.pay-option').forEach(function (o) { o.classList.remove('selected'); });
    el.classList.add('selected');
    selectedPayment = el.getAttribute('data-method');
    document.getElementById('khata-picker').style.display = selectedPayment === 'KHATA' ? 'block' : 'none';
    updateCheckoutEnabled();
  });
});

// ---------- Khata customer picker ----------
var khataSearchTimer = null;
document.getElementById('khata-customer-search').addEventListener('input', function (e) {
  clearTimeout(khataSearchTimer);
  var term = e.target.value.trim();
  if (term.length < 2) {
    document.getElementById('khata-customer-results').innerHTML = '';
    return;
  }
  khataSearchTimer = setTimeout(function () {
    apiCall('/api/customers?q=' + encodeURIComponent(term)).then(function (customers) {
      var container = document.getElementById('khata-customer-results');
      if (customers.length === 0) {
        container.innerHTML = '<div class="empty-state">No customer found. Add them on the Khata page first.</div>';
        return;
      }
      container.innerHTML = customers.slice(0, 6).map(function (c) {
        var overLimit = Number(c.currentBalance) > Number(c.creditLimit);
        return '<div class="cart-item" style="cursor:pointer" onclick=\'selectKhataCustomer(' + JSON.stringify(c) + ')\'>' +
          '<div><div class="name">' + c.name + '</div><div class="meta">' + (c.phone || '') + '</div></div>' +
          '<div class="badge ' + (overLimit ? 'badge-danger' : 'badge-ok') + '">' + formatMoney(c.currentBalance) + ' owed</div></div>';
      }).join('');
    });
  }, 250);
});

function selectKhataCustomer(customer) {
  selectedCustomer = customer;
  var box = document.getElementById('khata-selected');
  box.style.display = 'block';
  box.textContent = 'Selling on credit to: ' + customer.name;
  document.getElementById('khata-customer-results').innerHTML = '';
  document.getElementById('khata-customer-search').value = '';
  updateCheckoutEnabled();
}

// ---------- Checkout ----------
document.getElementById('checkout-btn').addEventListener('click', function () {
  if (cart.length === 0) return;
  var btn = this;
  btn.disabled = true;
  btn.textContent = 'Processing...';

  var payload = {
    items: cart.map(function (item) { return { productId: item.productId, quantity: item.quantity }; }),
    paymentMethod: selectedPayment,
    customerId: selectedPayment === 'KHATA' && selectedCustomer ? selectedCustomer.id : null
  };

  apiCall('/api/pos/checkout', { method: 'POST', body: JSON.stringify(payload) })
    .then(function (sale) {
      showSaleCompleteModal(sale);
      cart = [];
      selectedCustomer = null;
      document.getElementById('khata-selected').style.display = 'none';
      renderCart();
      document.getElementById('search-results').innerHTML = '';
      scanInput.value = '';
      scanInput.focus();
    })
    .catch(function (err) {
      showToast(err.message, 'error');
    })
    .finally(function () {
      btn.textContent = 'Complete sale';
      updateCheckoutEnabled();
    });
});

// Printing is optional and off by default - the cashier chooses whether to print,
// since most sales here won't need a slip.
function showSaleCompleteModal(sale) {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal" style="text-align:center;">' +
        '<div style="font-size:40px; margin-bottom:10px;">&#x2705;</div>' +
        '<h2 style="margin-bottom:6px;">Sale #' + sale.id + ' completed</h2>' +
        '<div style="font-size:26px; font-weight:800; margin-bottom:20px;">' + formatMoney(sale.totalAmount) + '</div>' +
        '<button class="btn btn-primary btn-lg btn-block" style="margin-bottom:10px;" onclick="window.open(\'/receipt/' + sale.id + '\', \'_blank\')">' +
          'Print receipt (optional)' +
        '</button>' +
        '<button class="btn btn-block" onclick="closeModal()">Done, no receipt needed</button>' +
      '</div>' +
    '</div>';
}

function closeModal() {
  document.getElementById('modal-root').innerHTML = '';
}

// ---------- Shift banner ----------
function loadShiftBanner() {
  apiCall('/api/shifts/current').then(function (shift) {
    var el = document.getElementById('shift-banner');
    if (shift && shift.status === 'OPEN') {
      el.innerHTML = '<span class="badge badge-ok" style="padding:8px 14px; cursor:pointer;" onclick="openCloseShiftModal(' + shift.openingCash + ')">' +
        'Shift open since ' + new Date(shift.openedAt).toLocaleTimeString('en-PK', {hour:'2-digit', minute:'2-digit'}) +
        ' &middot; tap to close</span>';
    } else {
      el.innerHTML = '<button class="btn btn-primary" onclick="openStartShiftModal()">Start shift</button>';
    }
  });
}

function openStartShiftModal() {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Start your shift</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>Cash in the drawer right now (Rs)</label><input id="opening-cash" type="number" placeholder="e.g. 2000" autofocus></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitStartShift()">Start shift</button>' +
      '</div>' +
    '</div>';
}

function submitStartShift() {
  var amount = parseFloat(document.getElementById('opening-cash').value) || 0;
  apiCall('/api/shifts/open', { method: 'POST', body: JSON.stringify({ openingCash: amount }) })
    .then(function () {
      showToast('Shift started.', 'success');
      closeModal();
      loadShiftBanner();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function openCloseShiftModal(openingCash) {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Close your shift</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>Count the cash in the drawer now and enter it (Rs)</label><input id="closing-cash" type="number" placeholder="Actual cash counted" autofocus></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitCloseShift()">Close shift</button>' +
      '</div>' +
    '</div>';
}

function submitCloseShift() {
  var amount = parseFloat(document.getElementById('closing-cash').value) || 0;
  apiCall('/api/shifts/close', { method: 'POST', body: JSON.stringify({ actualClosingCash: amount }) })
    .then(function (shift) {
      var diff = Number(shift.difference);
      if (Math.abs(diff) < 1) {
        showToast('Shift closed. Cash matches perfectly.', 'success');
      } else if (diff > 0) {
        showToast('Shift closed. Rs ' + diff.toFixed(0) + ' more than expected.', 'success');
      } else {
        showToast('Shift closed. Rs ' + Math.abs(diff).toFixed(0) + ' short - the owner will see this.', 'error');
      }
      closeModal();
      loadShiftBanner();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

renderCart();
loadShiftBanner();
scanInput.focus();

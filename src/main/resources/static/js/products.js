var allProducts = [];

function loadProducts(term) {
  var url = '/api/products' + (term ? '?q=' + encodeURIComponent(term) : '');
  apiCall(url).then(function (products) {
    allProducts = products;
    renderProducts(products);
  }).catch(function (err) { showToast(err.message, 'error'); });
}

function renderProducts(products) {
  var rows = document.getElementById('product-rows');
  var empty = document.getElementById('products-empty');

  if (products.length === 0) {
    rows.innerHTML = '';
    empty.style.display = 'block';
    return;
  }
  empty.style.display = 'none';

  rows.innerHTML = products.map(function (p) {
    var stockBadge = p.quantity <= p.lowStockThreshold
      ? '<span class="badge badge-danger">' + p.quantity + ' ' + p.unit + '</span>'
      : '<span class="badge badge-ok">' + p.quantity + ' ' + p.unit + '</span>';
    var actions = window.isOwner
      ? '<button class="btn" style="padding:8px 14px; font-size:14px;" onclick="openEditModal(' + p.id + ')">Edit</button> ' +
        '<button class="btn btn-danger" style="padding:8px 14px; font-size:14px;" onclick="deleteProduct(' + p.id + ', \'' + p.name.replace(/'/g, "\\'") + '\')">Delete</button>'
      : '<span class="meta">View only</span>';
    return '<tr>' +
      '<td><strong>' + p.name + '</strong></td>' +
      '<td>' + p.barcode + '</td>' +
      '<td>' + (p.category || '-') + '</td>' +
      '<td>' + formatMoney(p.sellingPrice) + '</td>' +
      '<td>' + stockBadge + '</td>' +
      '<td style="text-align:right; white-space:nowrap;">' + actions + '</td>' +
    '</tr>';
  }).join('');
}

var searchTimer = null;
document.getElementById('product-search').addEventListener('input', function (e) {
  clearTimeout(searchTimer);
  var term = e.target.value.trim();
  searchTimer = setTimeout(function () { loadProducts(term); }, 250);
});

function modalHtml(product) {
  var isEdit = !!product;
  var p = product || { name: '', barcode: '', category: '', unit: 'pcs', costPrice: 0, sellingPrice: '', quantity: 0, lowStockThreshold: 10 };
  return '' +
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header">' +
          '<h2>' + (isEdit ? 'Edit product' : 'Add product') + '</h2>' +
          '<button class="modal-close" onclick="closeModal()">&times;</button>' +
        '</div>' +
        '<div class="field"><label>Product name</label><input id="f-name" value="' + escapeAttr(p.name) + '" placeholder="e.g. Lays chips 30g"></div>' +
        '<div class="field"><label>Barcode</label><input id="f-barcode" value="' + escapeAttr(p.barcode) + '" placeholder="Scan or type barcode"></div>' +
        '<div class="grid grid-2">' +
          '<div class="field"><label>Category</label><input id="f-category" value="' + escapeAttr(p.category || '') + '" placeholder="e.g. Snacks"></div>' +
          '<div class="field"><label>Unit</label><input id="f-unit" value="' + escapeAttr(p.unit || 'pcs') + '" placeholder="pcs, bottle, pack..."></div>' +
        '</div>' +
        '<div class="card" style="background:#F9FAFB; padding:14px 16px; margin-bottom:16px;">' +
          '<div style="font-weight:700; font-size:14px; margin-bottom:10px;">Bought in a carton/box? Work out the cost per item</div>' +
          '<div class="grid grid-2" style="margin-bottom:0;">' +
            '<div class="field" style="margin-bottom:0;"><label>Total price paid for the carton (Rs)</label><input id="carton-price" type="number" step="0.01" placeholder="e.g. 2400"></div>' +
            '<div class="field" style="margin-bottom:0;"><label>Number of items in the carton</label><input id="carton-units" type="number" placeholder="e.g. 24"></div>' +
          '</div>' +
          '<button type="button" class="btn" style="margin-top:10px; padding:10px 16px; font-size:14px;" onclick="calculateCartonCost()">Calculate &amp; fill cost price below</button>' +
        '</div>' +
        '<div class="grid grid-2">' +
          '<div class="field"><label>Cost price per item (Rs)</label><input id="f-cost" type="number" step="0.01" value="' + p.costPrice + '"></div>' +
          '<div class="field"><label>Selling price (Rs)</label><input id="f-price" type="number" step="0.01" value="' + p.sellingPrice + '"></div>' +
        '</div>' +
        '<div class="grid grid-2">' +
          '<div class="field"><label>Current stock</label><input id="f-qty" type="number" value="' + p.quantity + '"></div>' +
          '<div class="field"><label>Low stock alert below</label><input id="f-threshold" type="number" value="' + p.lowStockThreshold + '"></div>' +
        '</div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="saveProduct(' + (isEdit ? p.id : 'null') + ')">' +
          (isEdit ? 'Save changes' : 'Add product') +
        '</button>' +
      '</div>' +
    '</div>';
}

function calculateCartonCost() {
  var cartonPrice = parseFloat(document.getElementById('carton-price').value);
  var cartonUnits = parseFloat(document.getElementById('carton-units').value);

  if (!cartonPrice || !cartonUnits || cartonUnits <= 0) {
    showToast('Enter both the carton price and number of items in it.', 'error');
    return;
  }

  var perItemCost = cartonPrice / cartonUnits;
  document.getElementById('f-cost').value = perItemCost.toFixed(2);
  showToast('Cost price set to Rs ' + perItemCost.toFixed(2) + ' per item.', 'success');
}

function escapeAttr(str) {
  return String(str).replace(/"/g, '&quot;');
}

function openAddModal() {
  document.getElementById('modal-root').innerHTML = modalHtml(null);
  setTimeout(function () { document.getElementById('f-name').focus(); }, 50);
}

function openEditModal(id) {
  var product = allProducts.find(function (p) { return p.id === id; });
  if (!product) return;
  document.getElementById('modal-root').innerHTML = modalHtml(product);
}

function closeModal() {
  document.getElementById('modal-root').innerHTML = '';
}

function saveProduct(id) {
  var payload = {
    name: document.getElementById('f-name').value.trim(),
    barcode: document.getElementById('f-barcode').value.trim(),
    category: document.getElementById('f-category').value.trim(),
    unit: document.getElementById('f-unit').value.trim() || 'pcs',
    costPrice: parseFloat(document.getElementById('f-cost').value) || 0,
    sellingPrice: parseFloat(document.getElementById('f-price').value) || 0,
    quantity: parseInt(document.getElementById('f-qty').value) || 0,
    lowStockThreshold: parseInt(document.getElementById('f-threshold').value) || 10
  };

  if (!payload.name || !payload.barcode || !payload.sellingPrice) {
    showToast('Name, barcode and selling price are required.', 'error');
    return;
  }

  var url = id ? '/api/products/' + id : '/api/products';
  var method = id ? 'PUT' : 'POST';

  apiCall(url, { method: method, body: JSON.stringify(payload) })
    .then(function () {
      showToast('Product saved.', 'success');
      closeModal();
      loadProducts();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function deleteProduct(id, name) {
  if (!confirm('Delete "' + name + '"? This cannot be undone.')) return;
  apiCall('/api/products/' + id, { method: 'DELETE' })
    .then(function () {
      showToast('Product deleted.', 'success');
      loadProducts();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

loadProducts();

var allUsers = [];

function loadUsers() {
  apiCall('/api/users').then(function (users) {
    allUsers = users;
    document.getElementById('user-rows').innerHTML = users.map(function (u) {
      var roleLabel = u.role === 'OWNER' ? 'Owner (full access)' : 'Cashier (billing only)';
      var statusBadge = u.active ? '<span class="badge badge-ok">Active</span>' : '<span class="badge badge-danger">Disabled</span>';
      return '<tr>' +
        '<td><strong>' + u.fullName + '</strong></td>' +
        '<td>' + u.username + '</td>' +
        '<td>' + roleLabel + '</td>' +
        '<td>' + statusBadge + '</td>' +
        '<td style="text-align:right; white-space:nowrap;">' +
          '<button class="btn" style="padding:8px 14px; font-size:14px;" onclick="openResetPasswordModal(' + u.id + ', \'' + u.fullName.replace(/'/g,"\\'") + '\')">Reset password</button> ' +
          '<button class="btn ' + (u.active ? 'btn-danger' : 'btn-success') + '" style="padding:8px 14px; font-size:14px;" onclick="toggleActive(' + u.id + ', ' + !u.active + ')">' +
            (u.active ? 'Disable' : 'Enable') +
          '</button>' +
        '</td>' +
      '</tr>';
    }).join('');
  }).catch(function (err) { showToast(err.message, 'error'); });
}

function closeModal() {
  document.getElementById('modal-root').innerHTML = '';
}

function openAddUserModal() {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Add staff account</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>Full name</label><input id="u-fullname" placeholder="e.g. Ahmed Khan" autofocus></div>' +
        '<div class="field"><label>Username (for login)</label><input id="u-username" placeholder="e.g. ahmed"></div>' +
        '<div class="field"><label>Password</label><input id="u-password" type="text" placeholder="At least 4 characters"></div>' +
        '<div class="field"><label>Role</label>' +
          '<select id="u-role" onchange="togglePinField()">' +
            '<option value="CASHIER">Cashier - billing only, cannot edit products or see reports</option>' +
            '<option value="OWNER">Owner - full access</option>' +
          '</select>' +
        '</div>' +
        '<div class="field" id="pin-field" style="display:none;"><label>4-digit approval PIN (for approving voided sales by phone)</label><input id="u-pin" placeholder="e.g. 1234"></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitAddUser()">Create account</button>' +
      '</div>' +
    '</div>';
}

function togglePinField() {
  var role = document.getElementById('u-role').value;
  document.getElementById('pin-field').style.display = role === 'OWNER' ? 'block' : 'none';
}

function submitAddUser() {
  var payload = {
    fullName: document.getElementById('u-fullname').value.trim(),
    username: document.getElementById('u-username').value.trim().toLowerCase(),
    password: document.getElementById('u-password').value,
    role: document.getElementById('u-role').value,
    pin: document.getElementById('u-pin') ? document.getElementById('u-pin').value : null
  };
  if (!payload.fullName || !payload.username || !payload.password) {
    showToast('Fill in all fields.', 'error');
    return;
  }
  apiCall('/api/users', { method: 'POST', body: JSON.stringify(payload) })
    .then(function () {
      showToast('Account created.', 'success');
      closeModal();
      loadUsers();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function toggleActive(id, active) {
  apiCall('/api/users/' + id + '/active', { method: 'POST', body: JSON.stringify({ active: active }) })
    .then(function () {
      showToast(active ? 'Account enabled.' : 'Account disabled.', 'success');
      loadUsers();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

function openResetPasswordModal(id, name) {
  document.getElementById('modal-root').innerHTML =
    '<div class="modal-overlay" onclick="if(event.target===this) closeModal()">' +
      '<div class="modal">' +
        '<div class="modal-header"><h2>Reset password for ' + name + '</h2><button class="modal-close" onclick="closeModal()">&times;</button></div>' +
        '<div class="field"><label>New password</label><input id="new-password" type="text" placeholder="At least 4 characters" autofocus></div>' +
        '<button class="btn btn-success btn-lg btn-block" onclick="submitResetPassword(' + id + ')">Reset password</button>' +
      '</div>' +
    '</div>';
}

function submitResetPassword(id) {
  var password = document.getElementById('new-password').value;
  apiCall('/api/users/' + id + '/reset-password', { method: 'POST', body: JSON.stringify({ password: password }) })
    .then(function () {
      showToast('Password reset.', 'success');
      closeModal();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

loadUsers();

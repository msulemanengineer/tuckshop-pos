function loadLicenseStatus() {
  apiCall('/api/license').then(function (info) {
    document.getElementById('device-id').value = info.deviceId;
    document.getElementById('stat-expiry').textContent = info.expiresOn || '-';
    document.getElementById('stat-days').textContent = info.daysRemaining;
    var statusEl = document.getElementById('stat-status');
    if (info.valid) {
      statusEl.textContent = 'Active';
      statusEl.style.color = 'var(--amber-dark)';
    } else {
      statusEl.textContent = 'Expired';
      statusEl.style.color = '#B91C1C';
    }
  }).catch(function (err) { showToast(err.message, 'error'); });
}

function activateLicense() {
  var key = document.getElementById('license-key').value.trim();
  if (!key) {
    showToast('Enter a license key.', 'error');
    return;
  }
  apiCall('/api/license/activate', { method: 'POST', body: JSON.stringify({ key: key }) })
    .then(function (res) {
      showToast('License activated - valid until ' + res.expiresOn, 'success');
      document.getElementById('license-key').value = '';
      loadLicenseStatus();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

loadLicenseStatus();

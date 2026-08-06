function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function formatDate(iso) {
  var d = new Date(iso);
  return d.toLocaleString('en-PK', { dateStyle: 'medium', timeStyle: 'short' });
}

function loadBackups() {
  apiCall('/api/backups').then(function (backups) {
    var rows = document.getElementById('backup-rows');
    var empty = document.getElementById('empty-state');
    if (!backups.length) {
      rows.innerHTML = '';
      empty.style.display = 'block';
      return;
    }
    empty.style.display = 'none';
    rows.innerHTML = backups.map(function (b) {
      return '<tr>' +
        '<td>' + b.filename + '</td>' +
        '<td>' + formatDate(b.createdAt) + '</td>' +
        '<td>' + formatBytes(b.sizeBytes) + '</td>' +
        '<td style="text-align:right;">' +
          '<a class="btn" style="padding:8px 14px; font-size:14px;" href="/api/backups/download/' + encodeURIComponent(b.filename) + '">Download</a>' +
        '</td>' +
      '</tr>';
    }).join('');
  }).catch(function (err) { showToast(err.message, 'error'); });
}

function runBackupNow() {
  apiCall('/api/backups/run', { method: 'POST' })
    .then(function (res) {
      showToast('Backup created: ' + res.filename, 'success');
      loadBackups();
    })
    .catch(function (err) { showToast(err.message, 'error'); });
}

loadBackups();

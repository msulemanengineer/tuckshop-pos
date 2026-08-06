// Shared helpers used across all pages

function showToast(message, type) {
  var container = document.getElementById('toast-container');
  if (!container) return;
  var el = document.createElement('div');
  el.className = 'toast ' + (type || '');
  el.textContent = message;
  container.appendChild(el);
  setTimeout(function () {
    el.style.transition = 'opacity 0.3s ease';
    el.style.opacity = '0';
    setTimeout(function () { el.remove(); }, 300);
  }, 2800);
}

function formatMoney(value) {
  var num = Number(value || 0);
  return 'Rs ' + num.toLocaleString('en-PK', { maximumFractionDigits: 0 });
}

function apiCall(url, options) {
  return fetch(url, Object.assign({
    headers: { 'Content-Type': 'application/json' }
  }, options || {})).then(function (res) {
    if (!res.ok) {
      return res.json().then(function (err) {
        throw new Error(err.message || 'Something went wrong.');
      }).catch(function (e) {
        throw new Error(e.message || 'Something went wrong.');
      });
    }
    if (res.status === 204) return null;
    return res.json();
  });
}

// Animate a number counting up, used on the dashboard
function countUp(el, target, prefix) {
  var start = 0;
  var duration = 700;
  var startTime = null;
  function step(ts) {
    if (!startTime) startTime = ts;
    var progress = Math.min((ts - startTime) / duration, 1);
    var eased = 1 - Math.pow(1 - progress, 3);
    var value = Math.round(start + (target - start) * eased);
    el.textContent = (prefix || '') + value.toLocaleString('en-PK');
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

/**
 * StaySphere AOS - Main JavaScript
 * Handles: AI Concierge, Search, Booking Widget, Property Loading
 */

const API = window.StaySphere.apiUrl || '';

/* =====================
   AI CONCIERGE WIDGET
   ===================== */
class ConciergeWidget {
  constructor() {
    this.widget = document.getElementById('ai-concierge-widget');
    this.toggle = document.getElementById('concierge-toggle');
    this.messages = null;
    this.input = null;
    this.sendBtn = null;
    this.sessionId = this._uuid();
    this.init();
  }

  init() {
    if (!this.toggle) return;
    this.toggle.addEventListener('click', () => this.open());
    this._buildWidget();
  }

  _buildWidget() {
    this.widget.innerHTML = `
      <div class="concierge-header">
        <h3>🤖 AI Travel Concierge</h3>
        <button onclick="this.closest('.concierge-widget').classList.add('hidden')" style="background:none;border:none;color:#aab4c4;cursor:pointer;font-size:20px;">✕</button>
      </div>
      <div class="concierge-messages" id="concierge-msgs">
        <div class="message-bubble message-ai">
          Hi! I'm your StaySphere AI concierge. Tell me where you want to go in Namibia, your dates, and what you're looking for. I'll find the perfect stay! 🦒
        </div>
      </div>
      <div class="concierge-input-row">
        <input type="text" class="concierge-input" id="concierge-input"
               placeholder="e.g. Cozy lodge near Etosha for 2, mid-July…" maxlength="500">
        <button class="btn-concierge-send" id="concierge-send">→</button>
      </div>`;
    this.messages = document.getElementById('concierge-msgs');
    this.input = document.getElementById('concierge-input');
    this.sendBtn = document.getElementById('concierge-send');
    this.sendBtn.addEventListener('click', () => this._send());
    this.input.addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this._send(); } });
  }

  open() {
    this.widget.classList.remove('hidden');
    this.input && this.input.focus();
  }

  async _send() {
    const query = this.input.value.trim();
    if (!query) return;
    this._addMessage(query, 'user');
    this.input.value = '';
    this.input.disabled = true;
    this.sendBtn.disabled = true;
    const typing = this._showTyping();

    try {
      const token = this._getToken();
      const endpoint = token ? `${API}/api/v1/ai/concierge` : `${API}/api/v1/ai/concierge/public`;
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify({ query, sessionId: this.sessionId })
      });
      const data = await res.json();
      typing.remove();
      if (data.success) {
        this._addMessage(data.data.message, 'ai');
        if (data.data.properties && data.data.properties.length > 0) {
          this._addPropertyCards(data.data.properties.slice(0, 3));
        }
      } else {
        this._addMessage('Sorry, I had trouble processing that. Please try again.', 'ai');
      }
    } catch (e) {
      typing.remove();
      this._addMessage('Connection error. Please check your internet and try again.', 'ai');
    } finally {
      this.input.disabled = false;
      this.sendBtn.disabled = false;
      this.input.focus();
    }
  }

  _addMessage(text, role) {
    const div = document.createElement('div');
    div.className = `message-bubble message-${role}`;
    div.textContent = text;
    this.messages.appendChild(div);
    this.messages.scrollTop = this.messages.scrollHeight;
    return div;
  }

  _addPropertyCards(properties) {
    const wrap = document.createElement('div');
    wrap.style.cssText = 'display:flex;flex-direction:column;gap:8px;margin-top:4px;';
    properties.forEach(p => {
      const card = document.createElement('a');
      card.href = `/products/${p.id}`;
      card.style.cssText = 'display:block;background:#f8f9fa;border-radius:8px;padding:10px 12px;text-decoration:none;color:#1f2937;font-size:13px;border:1px solid #e5e7eb;';
      card.innerHTML = `
        <strong style="color:#1a1a2e">${p.title}</strong>
        <span style="display:block;color:#6b7280;margin-top:2px">
          📍 ${p.location?.city || ''} · 🛏 ${p.bedrooms || '?'} bed ·
          <span style="color:#f0b429;font-weight:600">NAD ${p.pricing?.currentDynamicRate || '?'}/night</span>
          ${p.averageRating ? `· ⭐ ${p.averageRating.toFixed(1)}` : ''}
        </span>`;
      wrap.appendChild(card);
    });
    this.messages.appendChild(wrap);
    this.messages.scrollTop = this.messages.scrollHeight;
  }

  _showTyping() {
    const div = document.createElement('div');
    div.className = 'message-typing';
    div.innerHTML = '<span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>';
    this.messages.appendChild(div);
    this.messages.scrollTop = this.messages.scrollHeight;
    return div;
  }

  _getToken() {
    return localStorage.getItem('staysphere_token') || null;
  }

  _uuid() { return crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2); }
}

/* =====================
   HERO SEARCH
   ===================== */
class HeroSearch {
  constructor() {
    this.init();
  }
  init() {
    const searchBtn = document.getElementById('search-btn');
    const aiBtn = document.getElementById('ai-search-btn');
    if (searchBtn) searchBtn.addEventListener('click', () => this._doSearch());
    if (aiBtn) aiBtn.addEventListener('click', () => this._doAiSearch());

    // Quick filter pills
    document.querySelectorAll('.quick-filter-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const dest = document.getElementById('destination');
        if (dest) { dest.value = btn.dataset.filter; dest.dispatchEvent(new Event('input')); }
      });
    });
  }

  _doSearch() {
    const destination = document.getElementById('destination')?.value;
    const checkIn = document.getElementById('check-in')?.value;
    const checkOut = document.getElementById('check-out')?.value;
    const guests = document.getElementById('guests')?.value;
    const params = new URLSearchParams();
    if (destination) params.set('city', destination);
    if (checkIn) params.set('checkIn', checkIn);
    if (checkOut) params.set('checkOut', checkOut);
    if (guests) params.set('guests', guests);
    window.location.href = `/collections/all?${params.toString()}`;
  }

  _doAiSearch() {
    const query = document.getElementById('ai-search-input')?.value;
    if (!query) return;
    window.location.href = `/collections/all?aiQuery=${encodeURIComponent(query)}`;
  }
}

/* =====================
   BOOKING WIDGET
   ===================== */
class BookingWidget {
  constructor() {
    this.widget = document.getElementById('booking-widget');
    if (!this.widget) return;
    this.propertyId = this.widget.dataset.propertyId;
    this.checkin = document.getElementById('booking-checkin');
    this.checkout = document.getElementById('booking-checkout');
    this.guestsEl = document.getElementById('booking-guests');
    this.bookBtn = document.getElementById('btn-book');
    this.negotiateBtn = document.getElementById('btn-negotiate');
    this.priceBreakdown = document.getElementById('price-breakdown');
    this.init();
  }

  init() {
    if (!this.propertyId) return;
    this.checkin?.addEventListener('change', () => this._updatePrice());
    this.checkout?.addEventListener('change', () => this._updatePrice());
    this.bookBtn?.addEventListener('click', () => this._reserve());
    this.negotiateBtn?.addEventListener('click', () => this._openNegotiation());

    // Load area intelligence
    this._loadAreaIntelligence();
  }

  async _updatePrice() {
    const ci = this.checkin?.value;
    const co = this.checkout?.value;
    if (!ci || !co) return;

    try {
      const res = await fetch(
        `${API}/api/v1/bookings/price-estimate?propertyId=${this.propertyId}&checkIn=${ci}&checkOut=${co}`);
      const data = await res.json();
      if (data.success) {
        const total = data.data;
        const nights = Math.ceil((new Date(co) - new Date(ci)) / 86400000);
        const nightly = (total * 0.75 / nights).toFixed(0);

        document.getElementById('nights-label').textContent = `${nights} nights × NAD ${nightly}`;
        document.getElementById('nights-total').textContent = `NAD ${(nightly * nights).toFixed(0)}`;
        document.getElementById('cleaning-fee').textContent = `NAD ${(total * 0.08).toFixed(0)}`;
        document.getElementById('service-fee').textContent = `NAD ${(total * 0.10).toFixed(0)}`;
        document.getElementById('total-amount').textContent = `NAD ${total.toFixed(0)}`;
        this.priceBreakdown.style.display = 'block';

        // Load calendar insights
        this._loadCalendarInsights(ci, co);
      }
    } catch (e) { console.error('Price estimate error:', e); }
  }

  async _loadCalendarInsights(checkIn, checkOut) {
    const insightsEl = document.getElementById('calendar-insights');
    const contentEl = document.getElementById('insights-content');
    if (!insightsEl || !contentEl) return;
    insightsEl.style.display = 'block';
    contentEl.textContent = 'Loading AI insights…';
    try {
      const res = await fetch(`${API}/api/v1/ai/calendar-insights?propertyId=${this.propertyId}&checkIn=${checkIn}&checkOut=${checkOut}`);
      const data = await res.json();
      if (data.success) contentEl.textContent = data.data;
    } catch (e) { insightsEl.style.display = 'none'; }
  }

  async _loadAreaIntelligence() {
    const areaEl = document.getElementById('area-content');
    if (!areaEl) return;
    const lat = document.querySelector('[data-lat]')?.dataset.lat;
    const lon = document.querySelector('[data-lon]')?.dataset.lon;
    const city = document.querySelector('[data-city]')?.dataset.city;
    if (!city) return;
    try {
      const res = await fetch(`${API}/api/v1/ai/area-intelligence?lat=${lat || 0}&lon=${lon || 0}&city=${encodeURIComponent(city)}`);
      const data = await res.json();
      if (data.success) areaEl.textContent = data.data;
    } catch (e) { areaEl.textContent = 'Area information unavailable.'; }
  }

  _reserve() {
    const ci = this.checkin?.value;
    const co = this.checkout?.value;
    const g = this.guestsEl?.value;
    if (!ci || !co) { alert('Please select check-in and check-out dates.'); return; }
    const token = localStorage.getItem('staysphere_token');
    if (!token) { window.location.href = `/account/login?return_to=${encodeURIComponent(window.location.pathname)}`; return; }
    window.location.href = `/pages/checkout?propertyId=${this.propertyId}&checkIn=${ci}&checkOut=${co}&guests=${g || 2}`;
  }

  _openNegotiation() {
    const token = localStorage.getItem('staysphere_token');
    if (!token) { window.location.href = `/account/login?return_to=${encodeURIComponent(window.location.pathname)}`; return; }
    const modal = document.getElementById('negotiation-modal');
    if (modal) modal.classList.remove('hidden');
    else alert('Negotiation feature - enter your offer price to the host directly via the platform.');
  }
}

/* =====================
   INIT
   ===================== */
document.addEventListener('DOMContentLoaded', () => {
  new ConciergeWidget();
  new HeroSearch();
  new BookingWidget();
});

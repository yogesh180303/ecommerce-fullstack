import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import './layout-overrides.css';
import {
  ArrowDown, ArrowRight, ArrowUp, ArrowUpRight, Check, ChevronDown, ChevronLeft,
  ChevronRight, CircleUserRound, Heart, Leaf, Menu, Minus, Plus, Search,
  ShieldCheck, ShoppingBag, SlidersHorizontal, Sparkles, Truck, X,
} from 'lucide-react';

const API = import.meta.env.VITE_API_BASE_URL ?? '/api';
const apiClient = axios.create({ baseURL: API, timeout: 15000 });
const starterProducts = [
  { id: 1, name: 'Forma Ceramic Vase', category: 'Home & Living', price: 68, originalPrice: 82, rating: 4.9, reviews: 124, badge: 'BESTSELLER', image: 'photo-1578500494198-246f612d3b3d', tone: 'sand' },
  { id: 2, name: 'Everyday Linen Tote', category: 'Accessories', price: 42, rating: 4.8, reviews: 86, badge: 'NEW ARRIVAL', image: 'photo-1544816155-12df9643f363', tone: 'rose' },
  { id: 3, name: 'Cloud Knit Throw', category: 'Textiles', price: 115, rating: 5.0, reviews: 57, image: 'photo-1600369671236-e74521d4b6ad', tone: 'blue' },
  { id: 4, name: 'Sunday Pour-over Set', category: 'Kitchen', price: 74, rating: 4.9, reviews: 93, badge: 'LOW STOCK', image: 'photo-1495474472287-4d71bcdd2085', tone: 'green' },
  { id: 5, name: 'Arc Sculptural Lamp', category: 'Home & Living', price: 189, rating: 4.7, reviews: 39, image: 'photo-1507473885765-e6ed057f782c', tone: 'lavender' },
  { id: 6, name: 'Soft Form Candle', category: 'Wellness', price: 36, rating: 4.9, reviews: 211, badge: 'MADE TO GIFT', image: 'photo-1603006905003-be475563bc59', tone: 'peach' },
  { id: 7, name: 'Pebble Catchall', category: 'Home & Living', price: 48, rating: 4.8, reviews: 62, image: 'photo-1618221195710-dd6b41faaea6', tone: 'sand' },
  { id: 8, name: 'Ribbed Glass Pair', category: 'Kitchen', price: 52, rating: 4.9, reviews: 78, image: 'photo-1514228742587-6b1558fcca3d', tone: 'blue' },
];
const initialCategories = ['All objects', 'Home & Living', 'Kitchen', 'Textiles', 'Accessories', 'Wellness'];
const img = (id, width = 800) => id?.startsWith('http') ? id : `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=85`;
const money = (value) => `$${Number(value).toFixed(2)}`;

async function request(path, options = {}, token) {
  const { body, ...requestOptions } = options;
  try {
    const response = await apiClient.request({
      url: path,
      ...requestOptions,
      data: body,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    return response.status === 204 ? null : response.data;
  } catch (error) {
    const message = error.response?.data?.message || error.response?.data?.error
      || (error.code === 'ERR_NETWORK' ? 'The store API could not be reached.' : 'Something went wrong. Please try again.');
    throw new Error(message);
  }
}

function IconButton({ label, onClick, children, className = '' }) {
  return <button className={`icon-button ${className}`} aria-label={label} onClick={onClick}>{children}</button>;
}

function Store() {
  const [products, setProducts] = useState(starterProducts);
  const [category, setCategory] = useState('All objects');
  const [categories, setCategories] = useState(initialCategories);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('featured');
  const [cart, setCart] = useState(() => JSON.parse(localStorage.getItem('atelier-cart') || '[]'));
  const [drawer, setDrawer] = useState('');
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('atelier-user') || 'null'));
  const [token, setToken] = useState(() => localStorage.getItem('atelier-token'));
  const [orders, setOrders] = useState([]);
  const [toast, setToast] = useState('');
  const [loading, setLoading] = useState(true);
  const [authMode, setAuthMode] = useState('login');
  const [error, setError] = useState('');

  const setServerCart = (data) => setCart(data.items.map((item) => ({
    ...item,
    id: item.productId,
    cartItemId: item.id,
    image: item.image || starterProducts.find((product) => product.id === item.productId)?.image || starterProducts[0].image,
  })));

  useEffect(() => {
    request('/products?page=0&size=100')
      .then((data) => {
        const list = Array.isArray(data) ? data : data.content;
        if (list?.length) setProducts(list.map((product) => ({ ...product, image: product.image || starterProducts.find((p) => p.name === product.name)?.image || starterProducts[0].image })));
      })
      .catch((error) => setToast(`Showing the sample collection: ${error.message}`))
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => {
    request('/categories')
      .then((data) => setCategories(['All objects', ...data.map((item) => item.name)]))
      .catch((categoryError) => setToast(`Could not load categories: ${categoryError.message}`));
  }, []);

  useEffect(() => {
    if (!token) localStorage.setItem('atelier-cart', JSON.stringify(cart));
  }, [cart, token]);
  useEffect(() => {
    if (!token) return;
    async function synchronizeCart() {
      try {
        let serverCart = await request('/cart', {}, token);
        const guestCart = JSON.parse(localStorage.getItem('atelier-cart') || '[]');
        if (serverCart.items.length === 0 && guestCart.length > 0) {
          for (const item of guestCart) {
            await request('/cart/items', {
              method: 'POST',
              body: JSON.stringify({ productId: item.id, quantity: item.quantity }),
            }, token);
          }
          serverCart = await request('/cart', {}, token);
        }
        localStorage.removeItem('atelier-cart');
        setServerCart(serverCart);
      } catch (syncError) {
        setToast(`Could not load your saved bag: ${syncError.message}`);
      }
    }
    synchronizeCart();
  }, [token]);
  useEffect(() => { if (toast) { const timer = setTimeout(() => setToast(''), 2600); return () => clearTimeout(timer); } }, [toast]);
  useEffect(() => {
    if (drawer === 'orders' && token) request('/orders', {}, token).then(setOrders).catch((e) => setToast(e.message));
  }, [drawer, token]);

  const visibleProducts = useMemo(() => {
    const result = products.filter((product) => {
      const categoryMatch = category === 'All objects' || product.category === category;
      const searchMatch = `${product.name} ${product.category}`.toLowerCase().includes(search.toLowerCase());
      return categoryMatch && searchMatch;
    });
    if (sort === 'price-low') result.sort((a, b) => a.price - b.price);
    if (sort === 'price-high') result.sort((a, b) => b.price - a.price);
    if (sort === 'rating') result.sort((a, b) => b.rating - a.rating);
    return result;
  }, [products, category, search, sort]);
  const count = cart.reduce((total, line) => total + line.quantity, 0);
  const subtotal = cart.reduce((total, line) => total + line.price * line.quantity, 0);
  const addToCart = async (product) => {
    if (token) {
      try {
        const result = await request('/cart/items', {
          method: 'POST',
          body: JSON.stringify({ productId: product.id, quantity: 1 }),
        }, token);
        setServerCart(result);
        setToast(`${product.name} added to your bag`);
      } catch (addError) {
        setToast(addError.message);
      }
      return;
    }
    setCart((current) => {
      const found = current.find((line) => line.id === product.id);
      return found ? current.map((line) => line.id === product.id ? { ...line, quantity: line.quantity + 1 } : line) : [...current, { ...product, quantity: 1 }];
    });
    setToast(`${product.name} added to your bag`);
  };
  const updateQuantity = async (id, delta) => {
    const item = cart.find((line) => line.id === id);
    if (!item) return;
    const quantity = item.quantity + delta;
    if (token) {
      try {
        const result = quantity > 0
          ? await request(`/cart/items/${item.cartItemId}`, {
            method: 'PUT',
            body: JSON.stringify({ quantity }),
          }, token)
          : await request(`/cart/items/${item.cartItemId}`, { method: 'DELETE' }, token);
        setServerCart(result);
      } catch (updateError) {
        setToast(updateError.message);
      }
      return;
    }
    setCart((items) => items.map((line) => line.id === id ? { ...line, quantity } : line).filter((line) => line.quantity > 0));
  };

  async function submitAuth(event) {
    event.preventDefault();
    setError('');
    const form = new FormData(event.currentTarget);
    try {
      const result = await request(`/auth/${authMode}`, {
        method: 'POST',
        body: JSON.stringify({ name: form.get('name'), email: form.get('email'), password: form.get('password') }),
      });
      localStorage.setItem('atelier-token', result.token);
      localStorage.setItem('atelier-user', JSON.stringify(result.user));
      setToken(result.token);
      setUser(result.user);
      setDrawer('');
      setToast(`Welcome${result.user.name ? `, ${result.user.name.split(' ')[0]}` : ''}`);
    } catch (e) { setError(e.message); }
  }

  async function checkout(event) {
    event.preventDefault();
    if (!token) { setDrawer('account'); setToast('Sign in to complete your order'); return; }
    const address = new FormData(event.currentTarget).get('address');
    try {
      await request('/orders/checkout', { method: 'POST', body: JSON.stringify({ address }) }, token);
      setCart([]);
      setDrawer('success');
      setToast('Your order is on its way to being wonderful');
    } catch (e) { setToast(e.message); }
  }

  function signOut() {
    localStorage.removeItem('atelier-token');
    localStorage.removeItem('atelier-user');
    setToken(null);
    setUser(null);
    setCart([]);
    setDrawer('');
    setToast('You have signed out');
  }

  return (
    <div className="app-shell">
      <div className="announcement"><span>Thoughtfully made. Always considered.</span><span className="announcement-right"><Truck size={13} /> Complimentary shipping on orders over $100</span></div>
      <header className="site-header">
        <div className="header-left"><IconButton label="Menu" onClick={() => setDrawer('menu')}><Menu size={21} /></IconButton><button className="header-link" onClick={() => document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' })}>Shop</button><button className="header-link" onClick={() => setCategory('Home & Living')}>Our world</button></div>
        <button className="wordmark" onClick={() => { setCategory('All objects'); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>atelier<span>.</span></button>
        <div className="header-right"><div className="search-wrap"><Search size={17} /><input aria-label="Search objects" placeholder="Search objects..." value={search} onChange={(e) => { setSearch(e.target.value); document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' }); }} /><kbd>⌘ K</kbd></div><IconButton label="Your account" onClick={() => setDrawer(user ? 'account' : 'account')}><CircleUserRound size={20} /></IconButton><IconButton label={`Shopping bag, ${count} items`} onClick={() => setDrawer('cart')} className="bag-button"><ShoppingBag size={20} />{count > 0 && <span className="bag-count">{count}</span>}</IconButton></div>
      </header>

      <main>
        <section className="hero">
          <img className="hero-image" src={img('photo-1616486338812-3dadae4b4ace', 1800)} alt="Sunlit, thoughtfully styled living room" />
          <div className="hero-overlay" />
          <div className="hero-copy"><div className="eyebrow light"><Sparkles size={14} /> THE ART OF EVERYDAY</div><h1>Make room for<br /><em>the meaningful.</em></h1><p>Objects with a little more thought, and a lot more feeling.</p><button className="button button-light" onClick={() => document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' })}>Explore the collection <ArrowRight size={16} /></button></div>
          <div className="hero-caption"><span>01 / 03</span><span>Objects that make a house feel like yours.</span><span><ChevronLeft size={17} /><ChevronRight size={17} /></span></div>
        </section>

        <section className="value-strip"><div><Leaf size={17} /><span>Made with intention</span></div><i /><div><ShieldCheck size={17} /><span>Quality that lasts</span></div><i /><div><Heart size={17} /><span>Good things, thoughtfully</span></div></section>

        <section className="collection section-wrap" id="collection">
          <div className="collection-heading"><div><div className="eyebrow"><span className="eyebrow-line" /> THE GOOD THINGS</div><h2>Find your <em>everyday.</em></h2><p>Considered pieces for the moments that make a life.</p></div><button className="text-link" onClick={() => { setCategory('All objects'); setSearch(''); }}>View all objects <ArrowUpRight size={15} /></button></div>
          <div className="collection-tools">
            <div className="category-list">{categories.map((item) => <button key={item} className={`category-chip ${category === item ? 'active' : ''}`} onClick={() => setCategory(item)}>{item}</button>)}</div>
            <label className="sort-control"><SlidersHorizontal size={15} /><span>Sort:</span><select aria-label="Sort products" value={sort} onChange={(e) => setSort(e.target.value)}><option value="featured">Featured</option><option value="price-low">Price: low to high</option><option value="price-high">Price: high to low</option><option value="rating">Top rated</option></select><ChevronDown size={13} /></label>
          </div>
          {loading && <div className="load-note">Finding the good things…</div>}
          <div className="product-grid">
            {visibleProducts.map((product, index) => <article className="product-card" key={product.id}>
              <div className={`product-image ${product.tone || 'sand'}`}><img src={img(product.image, 720)} alt={product.name} loading="lazy" /><button className="favorite-button" aria-label={`Save ${product.name}`} onClick={(e) => { e.currentTarget.classList.toggle('saved'); setToast('Saved for another day'); }}><Heart size={17} /></button>{product.badge && <span className="product-badge">{product.badge}</span>}<button className="quick-add" onClick={() => addToCart(product)}><Plus size={15} /> Add to bag</button></div>
              <div className="product-info"><div><h3>{product.name}</h3><span>{product.category}</span></div><strong>{money(product.price)}</strong></div>
              <div className="rating"><span>★★★★★</span><small>{product.rating || '4.9'} <i>({product.reviews || 24})</i></small></div>
              {index === 2 && <div className="editor-note"><Sparkles size={14} /> A studio favorite</div>}
            </article>)}
          </div>
          {visibleProducts.length === 0 && <div className="empty-results"><Search size={24} /><h3>Nothing here just yet.</h3><p>Try another search or browse all objects.</p><button className="text-link" onClick={() => { setSearch(''); setCategory('All objects'); }}>Show me everything <ArrowRight size={15} /></button></div>}
          <div className="collection-end"><span>Showing {visibleProducts.length} of {products.length} considered objects</span><button onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>Back to top <ArrowUp size={13} /></button></div>
        </section>

        <section className="story-banner"><div className="story-photo"><img src={img('photo-1494438639946-1ebd1d20bf85', 1000)} alt="A collection of artful home objects" /></div><div className="story-copy"><div className="eyebrow"><span className="eyebrow-line" /> A NOTE FROM US</div><h2>Less, but a little<br /><em>more lovely.</em></h2><p>We believe the best things aren't just things. They're the cup you reach for every morning, the throw that knows your favorite spot, the light that makes 6 pm feel like a deep breath.</p><button className="text-link" onClick={() => setToast('A little more about us, coming soon')}>A little about Atelier <ArrowRight size={15} /></button><div className="story-stamp">A<br />✳<br />01</div></div></section>

        <section className="newsletter"><div className="eyebrow light"><Sparkles size={13} /> LETTERS FROM ATELIER</div><h2>Good things, <em>in your inbox.</em></h2><p>New finds, small rituals, and the occasional very good recipe.</p><form onSubmit={(e) => { e.preventDefault(); setToast('You’re on the list. Look out for a little note from us.'); e.currentTarget.reset(); }}><input type="email" aria-label="Your email address" placeholder="Your email address" required /><button aria-label="Subscribe"><ArrowRight size={18} /></button></form><small>By subscribing, you agree to our very occasional emails. No noise, promise.</small></section>
      </main>
      <footer className="site-footer"><button className="wordmark footer-mark" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>atelier<span>.</span></button><span>Thoughtfully made for wherever life happens.</span><span>© 2026 Atelier Studio</span><div><button onClick={() => setToast('Our story, coming soon')}>Our story</button><button onClick={() => setToast('Contact: hello@atelier.studio')}>Contact</button><button onClick={() => setDrawer('orders')}>Orders</button></div></footer>

      {toast && <div className="toast"><Check size={16} /> {toast}<button aria-label="Dismiss" onClick={() => setToast('')}><X size={15} /></button></div>}
      {drawer && <div className="overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) setDrawer(''); }}><aside className={`drawer ${drawer === 'menu' ? 'menu-drawer' : ''}`}>
        <div className="drawer-header"><span className="drawer-title">{drawer === 'cart' ? 'Your bag' : drawer === 'account' ? (user ? 'Your account' : 'A little hello') : drawer === 'checkout' ? 'Almost yours' : drawer === 'orders' ? 'Your orders' : drawer === 'success' ? 'Lovely choice.' : 'A little inspiration'}</span><IconButton label="Close panel" onClick={() => setDrawer('')}><X size={20} /></IconButton></div>
        {drawer === 'cart' && <div className="drawer-body">{cart.length === 0 ? <div className="drawer-empty"><ShoppingBag size={28} /><h3>Your bag is taking a little breather.</h3><p>Find something that feels like you.</p><button className="button button-dark" onClick={() => { setDrawer(''); document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' }); }}>Explore objects <ArrowRight size={15} /></button></div> : <><div className="cart-lines">{cart.map((item) => <div className="cart-line" key={item.id}><img src={img(item.image, 240)} alt="" /><div className="cart-line-copy"><h3>{item.name}</h3><span>{money(item.price)}</span><div className="quantity"><button aria-label="Decrease quantity" onClick={() => updateQuantity(item.id, -1)}><Minus size={13} /></button><span>{item.quantity}</span><button aria-label="Increase quantity" onClick={() => updateQuantity(item.id, 1)}><Plus size={13} /></button></div></div><strong>{money(item.price * item.quantity)}</strong></div>)}</div><div className="cart-summary"><div><span>Subtotal</span><strong>{money(subtotal)}</strong></div><p>{subtotal >= 100 ? 'Complimentary shipping is yours.' : `You’re ${money(100 - subtotal)} away from complimentary shipping.`}</p><button className="button button-dark full-button" onClick={() => setDrawer('checkout')}>Continue to checkout <ArrowRight size={15} /></button><span className="secure-note"><ShieldCheck size={14} /> A thoughtful, secure checkout</span></div></>}</div>}
        {drawer === 'account' && <div className="drawer-body">{user ? <div className="account-panel"><div className="account-avatar">{user.name?.[0]?.toUpperCase() || 'A'}</div><h3>Hello, {user.name || 'there'}.</h3><p>{user.email}</p><button className="button button-outline full-button" onClick={() => { setDrawer('orders'); }}>View your orders</button><button className="text-link sign-out" onClick={signOut}>Sign out <ArrowRight size={14} /></button>{user.role === 'ADMIN' && <AdminPanel token={token} products={products} setProducts={setProducts} onDone={() => setDrawer('')} />}</div> : <form className="auth-form" onSubmit={submitAuth}>{authMode === 'register' && <label>Your name<input name="name" placeholder="Avery Smith" required /></label>}<label>Email address<input type="email" name="email" placeholder="you@example.com" required /></label><label>Password<input type="password" name="password" minLength="8" placeholder="At least 8 characters" required /></label>{error && <p className="form-error">{error}</p>}<button className="button button-dark full-button">{authMode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={15} /></button><p className="auth-switch">{authMode === 'login' ? 'New around here?' : 'Already have an account?'} <button type="button" onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setError(''); }}>{authMode === 'login' ? 'Create an account' : 'Sign in'}</button></p></form>}</div>}
        {drawer === 'checkout' && <form className="checkout-form" onSubmit={checkout}><p>One last detail before your new favorite arrives.</p>{!user && <div className="checkout-signin">You’ll need to <button type="button" onClick={() => setDrawer('account')}>sign in</button> before placing your order.</div>}<label>Shipping address<textarea name="address" placeholder="Street, city, state and ZIP code" rows="4" required /></label><div className="checkout-total"><span>Total</span><strong>{money(subtotal)}</strong></div><button className="button button-dark full-button">Place order <ArrowRight size={15} /></button><span className="secure-note"><ShieldCheck size={14} /> Secure checkout · Free returns within 30 days</span></form>}
        {drawer === 'orders' && <div className="drawer-body">{!user ? <div className="drawer-empty"><CircleUserRound size={27} /><h3>Your orders are waiting for you.</h3><button className="button button-dark" onClick={() => setDrawer('account')}>Sign in <ArrowRight size={15} /></button></div> : orders.length ? <div className="order-list">{orders.map((order) => <article className="order-card" key={order.id}><div><strong>Order #{String(order.id).slice(-6)}</strong><span>{new Date(order.createdAt).toLocaleDateString()}</span></div><span className="order-status">{order.status}</span><div className="order-items">{order.items?.map((item) => <span key={item.productName}>{item.quantity} × {item.productName}</span>)}</div><strong>{money(order.total)}</strong></article>)}</div> : <div className="drawer-empty"><ShoppingBag size={27} /><h3>No orders just yet.</h3><p>Your next favorite thing is out there.</p></div>}</div>}
        {drawer === 'success' && <div className="drawer-body"><div className="drawer-empty success-empty"><div className="success-check"><Check size={25} /></div><h3>Good things are on their way.</h3><p>Your order is confirmed. We’ll take good care of it.</p><button className="button button-dark" onClick={() => { setDrawer('orders'); }}>View your order <ArrowRight size={15} /></button></div></div>}
        {drawer === 'menu' && <nav className="menu-links">{categories.map((item) => <button key={item} onClick={() => { setCategory(item); setDrawer(''); document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' }); }}>{item}<ArrowRight size={18} /></button>)}<button onClick={() => setDrawer('orders')}>Your orders<ArrowRight size={18} /></button></nav>}
      </aside></div>}
    </div>
  );
}

function AdminPanel({ token, products, setProducts, onDone }) {
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [orders, setOrders] = useState([]);
  const [editing, setEditing] = useState(null);
  const [categories, setCategories] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [editingCategory, setEditingCategory] = useState(null);
  useEffect(() => {
    request('/admin/orders', {}, token).then(setOrders).catch((error) => setMessage(error.message));
    request('/categories').then(setCategories).catch((error) => setMessage(error.message));
    request('/admin/dashboard', {}, token).then(setDashboard).catch((error) => setMessage(error.message));
  }, [token]);
  async function addProduct(event) {
    event.preventDefault();
    setMessage('');
    const formElement = event.currentTarget;
    const form = new FormData(event.currentTarget);
    setSaving(true);
    try {
      const product = await request(editing ? `/admin/products/${editing.id}` : '/admin/products', { method: editing ? 'PUT' : 'POST', body: JSON.stringify({ name: form.get('name'), category: form.get('category'), description: form.get('description'), price: Number(form.get('price')), image: form.get('image'), stock: Number(form.get('stock')) }) }, token);
      setProducts((current) => editing ? current.map((item) => item.id === product.id ? product : item) : [...current, product]);
      setMessage(editing ? 'Product changes saved.' : 'Product added to the collection.');
      setEditing(null);
      formElement.reset();
    } catch (error) { setMessage(error.message); }
    finally { setSaving(false); }
  }
  async function removeProduct(product) {
    if (!window.confirm(`Remove ${product.name} from the collection?`)) return;
    try {
      await request(`/admin/products/${product.id}`, { method: 'DELETE' }, token);
      setProducts((current) => current.filter((item) => item.id !== product.id));
      if (editing?.id === product.id) setEditing(null);
      setMessage(`${product.name} removed from the collection.`);
    } catch (error) { setMessage(error.message); }
  }
  async function updateOrder(order, status) {
    try {
      const updated = await request(`/admin/orders/${order.id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }, token);
      setOrders((current) => current.map((item) => item.id === updated.id ? updated : item));
      setMessage(`Order #${String(order.id).slice(-6)} marked ${status}.`);
    } catch (error) { setMessage(error.message); }
  }
  async function saveCategory(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const payload = { name: form.get('name'), description: form.get('description') };
    try {
      const saved = await request(editingCategory ? `/admin/categories/${editingCategory.id}` : '/admin/categories', {
        method: editingCategory ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      }, token);
      setCategories((current) => editingCategory
        ? current.map((item) => item.id === saved.id ? saved : item).sort((a, b) => a.name.localeCompare(b.name))
        : [...current, saved].sort((a, b) => a.name.localeCompare(b.name)));
      setEditingCategory(null);
      setMessage(editingCategory ? 'Category changes saved.' : 'Category created.');
      formElement.reset();
    } catch (error) { setMessage(error.message); }
  }
  async function removeCategory(category) {
    if (!window.confirm(`Delete category ${category.name}?`)) return;
    try {
      await request(`/admin/categories/${category.id}`, { method: 'DELETE' }, token);
      setCategories((current) => current.filter((item) => item.id !== category.id));
      setMessage(`${category.name} deleted.`);
    } catch (error) { setMessage(error.message); }
  }
  return <section className="admin-panel"><div className="eyebrow"><span className="eyebrow-line" /> STUDIO TOOLS</div><h3>Manage the collection</h3><p>{products.length} objects on the shop floor · Categories are created as you add products.</p>{dashboard && <div className="admin-product-list"><h4>Store overview</h4><p>{dashboard.productCount} products · {dashboard.orderCount} orders · {dashboard.userCount} customers · {dashboard.inventoryUnits} units in stock</p></div>}<form key={editing?.id ?? 'new-product'} onSubmit={addProduct}><label>Product name<input name="name" required placeholder="Object name" defaultValue={editing?.name ?? ''} /></label><label>Category<input name="category" required placeholder="Home & Living" defaultValue={editing?.category ?? ''} /></label><label>Price<input type="number" min="0.01" step="0.01" name="price" required placeholder="48.00" defaultValue={editing?.price ?? ''} /></label><label>Image URL or Unsplash photo ID<input name="image" placeholder="photo-…" defaultValue={editing?.image ?? ''} /></label><label>In stock<input type="number" min="0" name="stock" defaultValue={editing?.stock ?? 20} required /></label><label>Description<textarea name="description" required placeholder="A few words about this object" defaultValue={editing?.description ?? ''} /></label><button className="button button-dark full-button" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Add object'} {editing ? <Check size={15} /> : <Plus size={15} />}</button>{editing && <button type="button" className="text-link cancel-edit" onClick={() => setEditing(null)}>Cancel editing <X size={14} /></button>}</form><div className="admin-product-list"><h4>Categories</h4><form key={editingCategory?.id ?? 'new-category'} onSubmit={saveCategory}><label>Category name<input name="name" required maxLength="100" defaultValue={editingCategory?.name ?? ''} /></label><label>Description<textarea name="description" maxLength="500" defaultValue={editingCategory?.description ?? ''} /></label><button className="button button-dark full-button">{editingCategory ? 'Save category' : 'Add category'}</button>{editingCategory && <button type="button" className="text-link cancel-edit" onClick={() => setEditingCategory(null)}>Cancel editing</button>}</form>{categories.map((item) => <div className="admin-product-row" key={item.id}><span>{item.name}<small>{item.description || 'No description'}</small></span><div><button aria-label={`Edit category ${item.name}`} onClick={() => setEditingCategory(item)}><span className="edit-label">Edit</span></button><button aria-label={`Delete category ${item.name}`} onClick={() => removeCategory(item)}><X size={15} /></button></div></div>)}</div><div className="admin-product-list"><h4>Live objects</h4>{products.map((product) => <div className="admin-product-row" key={product.id}><span>{product.name}<small>{money(product.price)} · {product.stock ?? 0} in stock</small></span><div><button aria-label={`Edit ${product.name}`} onClick={() => setEditing(product)}><span className="edit-label">Edit</span></button><button aria-label={`Remove ${product.name}`} onClick={() => removeProduct(product)}><X size={15} /></button></div></div>)}</div><div className="admin-product-list"><h4>Recent orders <span>{orders.length}</span></h4>{orders.length === 0 ? <p>No orders have been placed yet.</p> : orders.map((order) => <div className="admin-order-row" key={order.id}><span><strong>#{String(order.id).slice(-6)} · {money(order.total)}</strong><small>{order.userName || order.items?.length + ' line items'}</small></span><select aria-label={`Status for order ${order.id}`} value={order.status} onChange={(event) => updateOrder(order, event.target.value)}><option value="processing">Processing</option><option value="shipped">Shipped</option><option value="delivered">Delivered</option><option value="cancelled">Cancelled</option></select></div>)}</div>{message && <p className="admin-message">{message}</p>}<button className="text-link admin-orders" onClick={() => { onDone(); }}>Close studio tools <X size={14} /></button></section>;
}

export default Store;

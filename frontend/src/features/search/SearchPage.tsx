import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  searchCourses,
  autocomplete,
  type CourseHit,
  type SearchResponse,
  type SortOption,
  type DurationBucket,
  type PriceFilter,
} from '../../api/search';
import './lumen.css';

// ─── Types ────────────────────────────────────────────────────────────────────

type LumenFilters = Partial<{
  rating: number;
  language: string[];
  duration: string[];
  level: string[];
  price: string;
  features: string[];
  subs: string[];
}>;

// ─── Gradient / glyph helpers ────────────────────────────────────────────────

const GRADIENTS = [
  { a: '#7C3AED', b: '#1E1B4B' }, { a: '#06B6D4', b: '#0C4A6E' },
  { a: '#F59E0B', b: '#7C2D12' }, { a: '#10B981', b: '#064E3B' },
  { a: '#EC4899', b: '#500724' }, { a: '#8B5CF6', b: '#2E1065' },
  { a: '#0EA5E9', b: '#082F49' }, { a: '#84CC16', b: '#1A2E05' },
  { a: '#EF4444', b: '#450A0A' }, { a: '#14B8A6', b: '#042F2E' },
  { a: '#A855F7', b: '#3B0764' }, { a: '#F97316', b: '#431407' },
];
function hashStr(s: string) {
  let h = 0; for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}
const getGradient = (id: string) => GRADIENTS[hashStr(id) % GRADIENTS.length];
const getGlyph    = (title: string) => title.slice(0, 2).toUpperCase();
const fmtHours    = (min: number | null) => min ? (min / 60).toFixed(1) + 'h' : null;
const estLectures = (min: number | null) => min ? Math.round(min / 8) : 0;
const estOriginal = (p: number | null) => p ? Math.round(p * 1.85 / 10) * 10 - 1 : null;
const discountPct = (p: number | null, o: number | null) =>
  p && o && o > p ? Math.round((1 - p / o) * 100) : 0;

// ─── Level mapping ────────────────────────────────────────────────────────────

const LEVEL_TO_API: Record<string, string> = {
  Beginner: 'BEGINNER', Intermediate: 'INTERMEDIATE',
  Advanced: 'ADVANCED', 'All Levels': 'ALL_LEVELS',
};
const LEVEL_DISPLAY: Record<string, string> = {
  BEGINNER: 'Beginner', INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced', ALL_LEVELS: 'All Levels',
};

function durationToApiBuckets(durations: string[]): DurationBucket[] {
  const s = new Set<DurationBucket>();
  durations.forEach(d => {
    if (d === '0-3')  s.add('SHORT');
    if (d === '3-6')  s.add('MEDIUM');
    if (d === '6-17') { s.add('MEDIUM'); s.add('LONG'); }
    if (d === '17+')  s.add('LONG');
  });
  return Array.from(s);
}

// ─── Icons ────────────────────────────────────────────────────────────────────

type SvgP = React.SVGProps<SVGSVGElement>;
const IcoSearch  = (p: SvgP) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>;
const IcoCart    = (p: SvgP) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M3 4h2l2.4 11.4a2 2 0 0 0 2 1.6h8.6a2 2 0 0 0 2-1.5L22 8H6"/><circle cx="9" cy="20" r="1.5"/><circle cx="18" cy="20" r="1.5"/></svg>;
const IcoStar    = (p: SvgP) => <svg viewBox="0 0 24 24" fill="currentColor" {...p}><path d="M12 2.5l2.95 6 6.6.96-4.78 4.66 1.13 6.58L12 17.7l-5.9 3 1.13-6.58L2.45 9.46l6.6-.96L12 2.5z"/></svg>;
const IcoChev    = (p: SvgP) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="m6 9 6 6 6-6"/></svg>;
const IcoFilter  = (p: SvgP) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M3 5h18M6 12h12M10 19h4"/></svg>;
const IcoClose   = (p: SvgP) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M6 6l12 12M18 6 6 18"/></svg>;
const IcoTrash   = (p: SvgP) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6"/></svg>;
const IcoBolt    = (p: SvgP) => <svg viewBox="0 0 24 24" fill="currentColor" {...p}><path d="M13 2 3 14h7l-1 8 10-12h-7l1-8z"/></svg>;

// ─── Logo ─────────────────────────────────────────────────────────────────────

function Logo() {
  return (
    <a href="/search" className="flex items-center gap-2 select-none" style={{ color: 'var(--lm-ink)' }}>
      <span className="inline-grid place-items-center w-8 h-8 rounded-[10px]" style={{ background: 'var(--lm-accent)', flexShrink: 0 }}>
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="white" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
          <path d="M5 18h14M5 18l4-12 3 8 3-5 4 9"/>
        </svg>
      </span>
      <span className="font-extrabold tracking-tight text-[19px]">Lumen</span>
    </a>
  );
}

// ─── Stars ────────────────────────────────────────────────────────────────────

function Stars({ value }: { value: number }) {
  const full = Math.floor(value);
  const half = value - full >= 0.25 && value - full < 0.75;
  const empty = 5 - full - (half ? 1 : 0);
  const id = `hg${value.toFixed(1).replace('.', '')}`;
  return (
    <span className="inline-flex items-center" style={{ color: 'var(--lm-amber)' }}>
      {Array.from({ length: full }).map((_, i) => <IcoStar key={`f${i}`} width="14" height="14"/>)}
      {half && (
        <svg viewBox="0 0 24 24" width="14" height="14">
          <defs><linearGradient id={id} x1="0" x2="1"><stop offset="50%" stopColor="currentColor"/><stop offset="50%" stopColor="currentColor" stopOpacity=".22"/></linearGradient></defs>
          <path fill={`url(#${id})`} d="M12 2.5l2.95 6 6.6.96-4.78 4.66 1.13 6.58L12 17.7l-5.9 3 1.13-6.58L2.45 9.46l6.6-.96L12 2.5z"/>
        </svg>
      )}
      {Array.from({ length: empty }).map((_, i) => <IcoStar key={`e${i}`} width="14" height="14" style={{ opacity: 0.22 }}/>)}
    </span>
  );
}

// ─── Categories mega-menu ─────────────────────────────────────────────────────

const CATEGORIES = [
  { id: 'dev',    label: 'Development',  subs: ['Web Development','Mobile Development','Game Development','Data Engineering','DevOps & Cloud','Programming Languages','Software Architecture','Embedded Systems'] },
  { id: 'data',   label: 'Data & AI',    subs: ['Machine Learning','Deep Learning','Data Science','Analytics & BI','MLOps','NLP & LLMs','Computer Vision','Statistics'] },
  { id: 'design', label: 'Design',       subs: ['UX & UI Design','Graphic Design','Motion & Animation','3D & Modeling','Type & Lettering','Illustration','Tools (Figma, Blender)','Design Systems'] },
  { id: 'biz',    label: 'Business',     subs: ['Entrepreneurship','Operations','Strategy','Sales','Project Management','Leadership','Negotiation','Communication'] },
  { id: 'mkt',    label: 'Marketing',    subs: ['Digital Marketing','SEO & SEM','Content & Copy','Branding','Social Media','Analytics','Email & CRM','Growth'] },
  { id: 'fin',    label: 'Finance',      subs: ['Personal Finance','Investing','Quantitative Finance','Accounting','Crypto & Web3','Real Estate','FinTech','Economics'] },
  { id: 'media',  label: 'Photo & Video',subs: ['Photography','Videography','Editing & Color','Lighting','Cinematography','Drone & Aerial','Mobile Filmmaking','Sound for Video'] },
  { id: 'music',  label: 'Music',        subs: ['Production','Music Theory','Instruments','Mixing & Mastering','Songwriting','DJ & Performance','Sound Design','Vocal'] },
  { id: 'life',   label: 'Lifestyle',    subs: ['Cooking','Fitness','Travel','Crafts & DIY','Languages','Mindfulness','Gardening','Pet Care'] },
];

function CategoriesMenu() {
  const [open, setOpen]   = useState(false);
  const [active, setActive] = useState(CATEGORIES[0].id);
  const ref       = useRef<HTMLDivElement>(null);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const openMenu     = () => { clearTimeout(closeTimer.current); setOpen(true); };
  const scheduleClose = () => { clearTimeout(closeTimer.current); closeTimer.current = setTimeout(() => setOpen(false), 140); };

  useEffect(() => {
    const h1 = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false); };
    const h2 = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('mousedown', h1);
    document.addEventListener('keydown', h2);
    return () => { document.removeEventListener('mousedown', h1); document.removeEventListener('keydown', h2); };
  }, []);

  const cat = CATEGORIES.find(c => c.id === active) ?? CATEGORIES[0];

  return (
    <div ref={ref} className="relative" onMouseEnter={openMenu} onMouseLeave={scheduleClose}>
      <button
        onClick={() => setOpen(o => !o)}
        className="lm-nav-link"
        style={{ background: open ? 'var(--lm-line-2)' : undefined }}
        aria-expanded={open}
      >
        Categories
        <IcoChev width="14" height="14" style={{ transform: open ? 'rotate(180deg)' : undefined, transition: 'transform .2s' }}/>
      </button>

      {open && (
        <div
          className="absolute left-0 top-[calc(100%+8px)] z-50 lm-fade"
          style={{ width: 720, background: 'var(--lm-surface)', border: '1px solid var(--lm-line)', borderRadius: 16, boxShadow: '0 24px 60px -20px rgba(20,15,40,.25)', overflow: 'hidden' }}
          onMouseEnter={openMenu}
          onMouseLeave={scheduleClose}
        >
          <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr' }}>
            <ul style={{ listStyle: 'none', margin: 0, padding: '8px 0', borderRight: '1px solid var(--lm-line-2)', background: 'var(--lm-bg)' }}>
              {CATEGORIES.map(c => {
                const on = c.id === active;
                return (
                  <li key={c.id}>
                    <button
                      onMouseEnter={() => setActive(c.id)}
                      onFocus={() => setActive(c.id)}
                      style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, padding: '10px 16px', background: on ? 'var(--lm-surface)' : 'transparent', color: on ? 'var(--lm-accent)' : 'var(--lm-ink)', borderTop: 'none', borderRight: 'none', borderBottom: 'none', borderLeft: on ? '2px solid var(--lm-accent)' : '2px solid transparent', fontSize: 13, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit', textAlign: 'left', transition: 'background 0.12s' }}
                    >
                      {c.label}
                      <IcoChev width="12" height="12" style={{ transform: 'rotate(-90deg)', opacity: on ? 1 : 0.4 }}/>
                    </button>
                  </li>
                );
              })}
            </ul>
            <div className="p-5">
              <div className="flex items-baseline justify-between mb-3">
                <span className="text-[15px] font-bold" style={{ color: 'var(--lm-ink)' }}>{cat.label}</span>
                <a href="#" className="text-[12px] font-medium" style={{ color: 'var(--lm-accent)' }}>Browse all →</a>
              </div>
              <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2px 16px' }}>
                {cat.subs.map(s => (
                  <li key={s}>
                    <a
                      href="#"
                      className="lm-cat-sub"
                      style={{ display: 'block', padding: '7px 8px', borderRadius: 6, fontSize: 13, color: 'var(--lm-ink)', transition: 'background 0.12s' }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'var(--lm-line-2)')}
                      onMouseLeave={e => (e.currentTarget.style.background = '')}
                    >{s}</a>
                  </li>
                ))}
              </ul>
              <div className="flex items-center justify-between gap-2 mt-4 pt-4" style={{ borderTop: '1px solid var(--lm-line-2)' }}>
                <span style={{ color: 'var(--lm-muted)', fontFamily: "'JetBrains Mono',monospace", fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.07em' }}>
                  trending in {cat.label.toLowerCase()}
                </span>
                <div className="flex items-center gap-1.5">
                  {cat.subs.slice(0, 2).map(s => <a key={s} href="#" className="lm-pill" style={{ fontSize: 11 }}>{s}</a>)}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Navbar ───────────────────────────────────────────────────────────────────

interface NavbarProps {
  inputValue: string;
  onInputChange: (v: string) => void;
  onSearch: () => void;
  onClear: () => void;
  onSuggestionClick: (s: string) => void;
  suggestions: string[];
  showSuggestions: boolean;
  setShowSuggestions: (v: boolean) => void;
  cart: CourseHit[];
  removeFromCart: (id: string) => void;
}

function Navbar({ inputValue, onInputChange, onSearch, onClear, onSuggestionClick, suggestions, showSuggestions, setShowSuggestions, cart, removeFromCart }: NavbarProps) {
  const [cartOpen, setCartOpen] = useState(false);
  const cartRef = useRef<HTMLDivElement>(null);
  const cartTotal = cart.reduce((s, c) => s + (c.price ?? 0), 0);

  useEffect(() => {
    const h = (e: MouseEvent) => { if (cartRef.current && !cartRef.current.contains(e.target as Node)) setCartOpen(false); };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);

  const iconBtn = (onClick: () => void, children: React.ReactNode, label: string) => (
    <button
      onClick={onClick}
      aria-label={label}
      style={{ position: 'relative', width: 40, height: 40, display: 'grid', placeContent: 'center', borderRadius: 8, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--lm-ink)', transition: 'background 0.15s', flexShrink: 0 }}
      onMouseEnter={e => (e.currentTarget.style.background = 'var(--lm-line-2)')}
      onMouseLeave={e => (e.currentTarget.style.background = '')}
    >{children}</button>
  );

  return (
    <header className="lm-navbar">
      <div className="lm-navbar-inner">
        <Logo />

        <nav className="lm-navbar-cats">
          <CategoriesMenu />
        </nav>

        {/* Search bar */}
        <div className="lm-navbar-search-wrap">
          <label className="lm-search w-full flex items-center gap-2 px-4 cursor-text" style={{ height: 40 }}>
            <IcoSearch width="16" height="16" style={{ color: 'var(--lm-muted)', flexShrink: 0 }}/>
            <input
              type="text"
              value={inputValue}
              onChange={e => onInputChange(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && onSearch()}
              onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
              onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
              placeholder="Search anything — python, design, finance…"
              style={{ flex: 1, minWidth: 0 }}
            />
            {inputValue && (
              <button
                onClick={onClear}
                style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', color: 'var(--lm-muted)', display: 'flex', alignItems: 'center', flexShrink: 0 }}
                onMouseEnter={e => (e.currentTarget.style.color = 'var(--lm-ink)')}
                onMouseLeave={e => (e.currentTarget.style.color = 'var(--lm-muted)')}
              ><IcoClose width="14" height="14"/></button>
            )}
          </label>

          {/* Autocomplete dropdown */}
          {showSuggestions && suggestions.length > 0 && (
            <ul
              className="lm-fade"
              style={{ position: 'absolute', top: 'calc(100% + 4px)', left: 0, right: 0, background: 'var(--lm-surface)', border: '1px solid var(--lm-line)', borderRadius: 12, boxShadow: '0 12px 32px -8px rgba(20,15,40,.18)', overflow: 'hidden', zIndex: 60, listStyle: 'none', margin: 0, padding: '4px 0' }}
            >
              {suggestions.map(s => (
                <li key={s}>
                  <button
                    onMouseDown={() => onSuggestionClick(s)}
                    className="w-full text-left flex items-center gap-3 px-4 py-2.5 text-[13px] transition-colors"
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'inherit', color: 'var(--lm-ink)' }}
                    onMouseEnter={e => (e.currentTarget.style.background = 'var(--lm-line-2)')}
                    onMouseLeave={e => (e.currentTarget.style.background = '')}
                  >
                    <IcoSearch width="13" height="13" style={{ color: 'var(--lm-muted)', flexShrink: 0 }}/>
                    {s}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="lm-navbar-right">
          {/* Cart */}
          <div ref={cartRef} style={{ position: 'relative' }}>
            {iconBtn(
              () => setCartOpen(o => !o),
              <>
                <IcoCart width="20" height="20"/>
                {cart.length > 0 && (
                  <span style={{ position: 'absolute', top: 4, right: 4, minWidth: 18, height: 18, padding: '0 4px', display: 'grid', placeContent: 'center', borderRadius: 999, fontSize: 10, fontWeight: 700, color: '#fff', background: 'var(--lm-accent)' }}>
                    {cart.length}
                  </span>
                )}
              </>,
              'Cart'
            )}

            {cartOpen && (
              <div className="lm-pop lm-fade">
                <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--lm-line-2)' }}>
                  <div className="font-semibold text-[14px]" style={{ color: 'var(--lm-ink)' }}>Your cart</div>
                  <div className="text-[12px]" style={{ color: 'var(--lm-muted)' }}>{cart.length} item{cart.length !== 1 ? 's' : ''}</div>
                </div>
                <div className="lm-scroll" style={{ maxHeight: 320, overflowY: 'auto' }}>
                  {cart.length === 0 ? (
                    <div className="px-4 py-10 text-center" style={{ color: 'var(--lm-muted)' }}>
                      <div className="text-[13px]">Your cart is empty.</div>
                      <div className="text-[12px] mt-1">Add a course to see it here.</div>
                    </div>
                  ) : cart.map(c => {
                    const { a, b } = getGradient(c.courseId);
                    return (
                      <div key={c.courseId} className="flex gap-3 px-4 py-3" style={{ borderBottom: '1px solid var(--lm-line-2)' }}>
                        <div style={{ width: 56, height: 40, borderRadius: 6, background: `linear-gradient(135deg,${a},${b})`, flexShrink: 0 }}/>
                        <div className="flex-1 min-w-0">
                          <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--lm-ink)', overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', lineHeight: 1.35 }}>{c.title}</div>
                          <div style={{ fontSize: 11, marginTop: 2, color: 'var(--lm-muted)' }}>{c.instructorName}</div>
                        </div>
                        <div style={{ textAlign: 'right', flexShrink: 0 }}>
                          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--lm-ink)' }}>{c.isFree ? 'Free' : `$${c.price?.toFixed(0)}`}</div>
                          <button onClick={() => removeFromCart(c.courseId)} style={{ marginTop: 4, background: 'none', border: 'none', padding: 0, cursor: 'pointer', color: 'var(--lm-muted)', display: 'flex' }}
                            onMouseEnter={e => (e.currentTarget.style.color = 'var(--lm-ink)')}
                            onMouseLeave={e => (e.currentTarget.style.color = 'var(--lm-muted)')}>
                            <IcoTrash width="14" height="14"/>
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
                {cart.length > 0 && (
                  <div className="flex items-center justify-between p-4" style={{ borderTop: '1px solid var(--lm-line-2)' }}>
                    <div>
                      <div style={{ fontSize: 11, color: 'var(--lm-muted)' }}>Total</div>
                      <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--lm-ink)' }}>${cartTotal.toFixed(0)}</div>
                    </div>
                    <button className="lm-btn lm-btn-primary" style={{ height: 40, fontSize: 13 }}>Checkout</button>
                  </div>
                )}
              </div>
            )}
          </div>

          <button className="lm-btn lm-btn-ghost" style={{ height: 36, fontSize: 13 }}>Log in</button>
          <button className="lm-btn lm-btn-primary" style={{ height: 36, fontSize: 13 }}>Sign up</button>
        </div>
      </div>
    </header>
  );
}

// ─── Filter groups ────────────────────────────────────────────────────────────

interface FilterGroup {
  id: string;
  label: string;
  kind: 'radio' | 'check';
  options: Array<string | { v: string | number; label: string }>;
}

const FILTER_GROUPS: FilterGroup[] = [
  { id: 'rating',   label: 'Ratings',        kind: 'radio',
    options: [{ v: 4.5, label: '4.5 & up' }, { v: 4.0, label: '4.0 & up' }, { v: 3.5, label: '3.5 & up' }, { v: 3.0, label: '3.0 & up' }] },
  { id: 'language', label: 'Language',        kind: 'check',
    options: ['English', 'Spanish', 'French', 'German', 'Portuguese', 'Hindi', 'Arabic', 'Chinese'] },
  { id: 'duration', label: 'Video Duration',  kind: 'check',
    options: [{ v: '0-3', label: '0–3 hours' }, { v: '3-6', label: '3–6 hours' }, { v: '6-17', label: '6–17 hours' }, { v: '17+', label: '17+ hours' }] },
  { id: 'level',    label: 'Skill Level',     kind: 'check',
    options: ['Beginner', 'Intermediate', 'Advanced', 'All Levels'] },
  { id: 'price',    label: 'Price',           kind: 'radio',
    options: [{ v: 'all', label: 'Any price' }, { v: 'free', label: 'Free' }, { v: 'paid', label: 'Paid' }, { v: 'u50', label: 'Under $50' }] },
  { id: 'features', label: 'Features',        kind: 'check',
    options: [{ v: 'captions', label: 'Closed captions' }, { v: 'quizzes', label: 'Quizzes' }, { v: 'exercises', label: 'Coding exercises' }, { v: 'certificate', label: 'Certificate' }] },
];

// ─── Accordion ────────────────────────────────────────────────────────────────

function Accordion({ label, defaultOpen, count, children }: {
  label: string; defaultOpen: boolean; count: number; children: React.ReactNode;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className={open ? 'lm-acc-open' : ''} style={{ borderBottom: '1px solid var(--lm-line-2)' }}>
      <button onClick={() => setOpen(o => !o)} className="lm-acc-head">
        <span className="font-semibold flex items-center gap-2" style={{ fontSize: 14, color: 'var(--lm-ink)' }}>
          {label}
          {count > 0 && (
            <span style={{ fontSize: 10, fontFamily: "'JetBrains Mono',monospace", padding: '2px 6px', borderRadius: 5, background: 'var(--lm-accent-2)', color: 'var(--lm-accent-ink)' }}>
              {count}
            </span>
          )}
        </span>
        <IcoChev className="lm-acc-chevron" width="16" height="16"/>
      </button>
      <div className="lm-acc-body">
        <div>
          <div className="pb-4 flex flex-col gap-2">{children}</div>
        </div>
      </div>
    </div>
  );
}

// ─── Filter Sidebar ───────────────────────────────────────────────────────────

function FilterSidebar({ filters, setFilters, embedded }: {
  filters: LumenFilters;
  setFilters: React.Dispatch<React.SetStateAction<LumenFilters>>;
  embedded?: boolean;
}) {
  type FKey = keyof LumenFilters;
  const getArr = (id: FKey) => (filters[id] as string[] | undefined) ?? [];
  const getVal = (id: FKey) => filters[id];

  const onCheck = (id: FKey, v: string | number) => setFilters(prev => {
    const cur = new Set((prev[id] as string[] | undefined) ?? []);
    cur.has(String(v)) ? cur.delete(String(v)) : cur.add(String(v));
    return { ...prev, [id]: Array.from(cur) };
  });
  const onRadio = (id: FKey, v: string | number) => setFilters(prev => ({ ...prev, [id]: v }));
  const optVal  = (o: string | { v: string | number; label: string }) => typeof o === 'string' ? o : o.v;
  const optLbl  = (o: string | { v: string | number; label: string }) => typeof o === 'string' ? o : o.label;

  const totalActive = useMemo(() => {
    let n = 0;
    FILTER_GROUPS.forEach(g => {
      const v = filters[g.id as FKey];
      if (g.kind === 'check') n += ((v as string[]) || []).length;
      else if (g.kind === 'radio' && v != null && v !== 'all') n += 1;
    });
    return n;
  }, [filters]);

  return (
    <aside style={{ display: 'flex', flexDirection: 'column', flex: embedded ? 1 : undefined, minHeight: 0 }}>
      <div className="flex items-center justify-between pb-3 mb-1" style={{ borderBottom: '1px solid var(--lm-line-2)' }}>
        <span style={{ fontWeight: 700, fontSize: 15, color: 'var(--lm-ink)' }}>
          Filters{totalActive > 0 && <span style={{ marginLeft: 8, fontSize: 12, fontFamily: "'JetBrains Mono',monospace", color: 'var(--lm-muted)' }}>{totalActive}</span>}
        </span>
        <button
          onClick={() => setFilters({})}
          style={{ fontSize: 12, fontWeight: 500, color: 'var(--lm-accent)', background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'inherit' }}
          onMouseEnter={e => (e.currentTarget.style.textDecoration = 'underline')}
          onMouseLeave={e => (e.currentTarget.style.textDecoration = '')}
        >Reset</button>
      </div>

      <div
        style={{ flex: 1, minHeight: 0, overflowY: embedded ? 'auto' : undefined }}
        className={embedded ? 'lm-scroll' : ''}
      >
        {FILTER_GROUPS.map((g, i) => {
          const fv = filters[g.id as FKey];
          const count = g.kind === 'check' ? ((fv as string[]) || []).length : (fv != null && fv !== 'all' ? 1 : 0);
          return (
            <Accordion key={g.id} label={g.label} defaultOpen={i < 4} count={count}>
              {g.options.map(o => {
                const v = optVal(o), l = optLbl(o);
                if (g.kind === 'check') {
                  const checked = getArr(g.id as FKey).includes(String(v));
                  return (
                    <label key={String(v)} className="flex items-center gap-2.5" style={{ cursor: 'pointer', fontSize: 13 }}>
                      <input type="checkbox" className="lm-check" checked={checked} onChange={() => onCheck(g.id as FKey, v)}/>
                      <span style={{ flex: 1, color: checked ? 'var(--lm-ink)' : 'var(--lm-muted)' }}>{l}</span>
                    </label>
                  );
                }
                const checked = getVal(g.id as FKey) === v || (g.id === 'rating' && Number(getVal(g.id as FKey)) === Number(v));
                return (
                  <label key={String(v)} className="flex items-center gap-2.5" style={{ cursor: 'pointer', fontSize: 13 }}>
                    <input type="radio" name={g.id} className="lm-radio" checked={checked} onChange={() => onRadio(g.id as FKey, v)}/>
                    <span className="flex-1 inline-flex items-center gap-2" style={{ color: checked ? 'var(--lm-ink)' : 'var(--lm-muted)' }}>
                      {g.id === 'rating' && <Stars value={Number(v)}/>}
                      {l}
                    </span>
                  </label>
                );
              })}
            </Accordion>
          );
        })}
        <div style={{ padding: '24px 0', fontSize: 10, fontFamily: "'JetBrains Mono',monospace", textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--lm-muted)' }}>
          End of filters
        </div>
      </div>
    </aside>
  );
}

// ─── Course Card ──────────────────────────────────────────────────────────────

function CourseCard({ course, inCart, onAdd, onRemove }: {
  course: CourseHit; inCart: boolean;
  onAdd: (c: CourseHit) => void; onRemove: (id: string) => void;
}) {
  const c = course;
  const { a, b } = getGradient(c.courseId);
  const levelLabel  = LEVEL_DISPLAY[c.level] ?? c.level ?? 'All Levels';
  const hours       = fmtHours(c.durationMinutes);
  const lectures    = estLectures(c.durationMinutes);
  const origPrice   = estOriginal(c.price);
  const discount    = discountPct(c.price, origPrice);
  const bestseller  = (c.enrollments ?? 0) > 5000 || (c.rating ?? 0) >= 4.7;

  return (
    <article className="lm-card overflow-hidden flex flex-col lm-fade">
      <div className="p-3 pb-0">
        <div className="lm-thumb" style={{ background: `linear-gradient(135deg,${a} 0%,${b} 100%)` }}>
          <div className="lm-thumb-tag">{(c.subcategory || c.category || 'COURSE').slice(0, 6).toUpperCase()}</div>
          <div className="lm-thumb-glyph">{getGlyph(c.title)}</div>
        </div>
      </div>

      <div className="p-4 pt-3 flex flex-col gap-2 flex-1">
        {/* Meta row */}
        <div className="flex items-center gap-2 flex-wrap" style={{ fontSize: 11, fontFamily: "'JetBrains Mono',monospace", textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--lm-muted)' }}>
          <span style={{ padding: '2px 6px', borderRadius: 4, background: 'var(--lm-line-2)' }}>{levelLabel}</span>
          {hours && <><span>·</span><span>{hours}</span></>}
          {lectures > 0 && <><span>·</span><span>{lectures} lec</span></>}
          {bestseller && (
            <span className="ml-auto inline-flex items-center gap-1" style={{ padding: '2px 6px', borderRadius: 4, background: 'oklch(95% 0.07 70)', color: 'oklch(38% 0.14 70)' }}>
              <IcoBolt width="9" height="9"/> bestseller
            </span>
          )}
        </div>

        <h3 style={{ fontSize: 15, fontWeight: 700, lineHeight: 1.35, color: 'var(--lm-ink)', textWrap: 'balance' } as React.CSSProperties}>{c.title}</h3>

        {c.description && (
          <p style={{ fontSize: 13, lineHeight: 1.45, color: 'var(--lm-muted)', overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{c.description}</p>
        )}

        <div style={{ fontSize: 12, color: 'var(--lm-muted)' }}>
          By <span style={{ fontWeight: 500, color: 'var(--lm-ink)' }}>{c.instructorName}</span>
        </div>

        {c.rating != null && (
          <div className="flex items-center gap-2" style={{ fontSize: 12 }}>
            <span style={{ fontWeight: 700, color: 'var(--lm-ink)' }}>{c.rating.toFixed(1)}</span>
            <Stars value={c.rating}/>
            {c.ratingCount != null && <span style={{ color: 'var(--lm-muted)' }}>({c.ratingCount.toLocaleString()})</span>}
          </div>
        )}

        {/* Price + CTA */}
        <div className="mt-auto pt-3 flex items-end justify-between gap-2">
          <div>
            {c.isFree ? (
              <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.5, color: 'oklch(46% 0.18 145)' }}>Free</div>
            ) : c.price != null ? (
              <>
                <div className="flex items-baseline gap-2">
                  <span style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.5, color: 'var(--lm-ink)' }}>${c.price.toFixed(0)}</span>
                  {origPrice && <span style={{ fontSize: 12, textDecoration: 'line-through', color: 'var(--lm-muted)' }}>${origPrice}</span>}
                </div>
                {discount > 0 && (
                  <div style={{ fontSize: 10, fontFamily: "'JetBrains Mono',monospace", textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--lm-accent)', marginTop: 2 }}>
                    {discount}% off · ends soon
                  </div>
                )}
              </>
            ) : null}
          </div>
          <div className="lm-card-cta">
            {inCart
              ? <button onClick={() => onRemove(c.courseId)} className="lm-btn lm-btn-soft" style={{ height: 36, fontSize: 12 }}>In cart ✓</button>
              : <button onClick={() => onAdd(c)} className="lm-btn lm-btn-primary" style={{ height: 36, fontSize: 12 }}>Add to cart</button>
            }
          </div>
        </div>
      </div>
    </article>
  );
}

// ─── Skeleton card ────────────────────────────────────────────────────────────

function SkeletonCard({ delay = 0 }: { delay?: number }) {
  return (
    <div className="lm-card overflow-hidden" style={{ animationDelay: `${delay}ms` }}>
      <div className="p-3 pb-0">
        <div className="lm-skeleton" style={{ aspectRatio: '16/10', borderRadius: 12 }}/>
      </div>
      <div className="p-4 pt-3 flex flex-col gap-3">
        {([1, 0.75, 0.5, 0.65] as const).map((w, i) => (
          <div key={i} className="lm-skeleton" style={{ height: i === 0 ? 17 : 13, width: `${w * 100}%`, animationDelay: `${i * 80}ms` }}/>
        ))}
      </div>
    </div>
  );
}

// ─── Footer ───────────────────────────────────────────────────────────────────

function FooterCol({ title, links }: { title: string; links: string[] }) {
  return (
    <div>
      <div style={{ fontSize: 10, fontFamily: "'JetBrains Mono',monospace", textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--lm-muted)', marginBottom: 12 }}>{title}</div>
      <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {links.map(l => (
          <li key={l}>
            <a href="#" style={{ fontSize: 13, color: 'var(--lm-ink)', transition: 'color 0.15s' }}
               onMouseEnter={e => (e.currentTarget.style.color = 'var(--lm-accent)')}
               onMouseLeave={e => (e.currentTarget.style.color = 'var(--lm-ink)')}>
              {l}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ─── URL ↔ filter serialisation ───────────────────────────────────────────────

function parseFiltersFromParams(sp: URLSearchParams): LumenFilters {
  const f: LumenFilters = {};
  const r    = sp.get('rating');   if (r)    f.rating   = Number(r);
  const p    = sp.get('price');    if (p)    f.price    = p;
  const lang = sp.get('language'); if (lang) f.language = lang.split(',');
  const dur  = sp.get('duration'); if (dur)  f.duration = dur.split(',');
  const lvl  = sp.get('level');    if (lvl)  f.level    = lvl.split(',');
  const feat = sp.get('features'); if (feat) f.features = feat.split(',');
  return f;
}

function toUrlParams(q: string, sort: SortOption, filters: LumenFilters, page: number): Record<string, string> {
  const p: Record<string, string> = {};
  if (q) p.q = q;
  if (sort !== 'RELEVANCE') p.sort = sort;
  if (filters.rating != null) p.rating = String(filters.rating);
  if (filters.price && filters.price !== 'all') p.price = filters.price;
  if (filters.language?.length) p.language = filters.language.join(',');
  if (filters.duration?.length) p.duration = filters.duration.join(',');
  if (filters.level?.length)    p.level    = filters.level.join(',');
  if (filters.features?.length) p.features = filters.features.join(',');
  if (page > 0) p.page = String(page);
  return p;
}

// ─── Sort options ─────────────────────────────────────────────────────────────

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'RELEVANCE',  label: 'Most relevant'     },
  { value: 'RATING',     label: 'Highest rated'     },
  { value: 'NEWEST',     label: 'Newest'             },
  { value: 'POPULARITY', label: 'Most popular'      },
  { value: 'PRICE_ASC',  label: 'Price: low → high' },
  { value: 'PRICE_DESC', label: 'Price: high → low' },
];

// ─── Main SearchPage ──────────────────────────────────────────────────────────

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [query,      setQuery]      = useState(searchParams.get('q') ?? '');
  const [inputValue, setInputValue] = useState(searchParams.get('q') ?? '');
  const [sort,       setSort]       = useState<SortOption>((searchParams.get('sort') as SortOption) ?? 'RELEVANCE');
  const [filters,    setFilters]    = useState<LumenFilters>(() => parseFiltersFromParams(searchParams));
  const [page,       setPage]       = useState(Number(searchParams.get('page') ?? 0));
  const [sidebarHidden, setSidebarHidden] = useState(false);
  const [drawerOpen,    setDrawerOpen]    = useState(false);
  const [cart,       setCart]       = useState<CourseHit[]>([]);

  const [result,  setResult]  = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  const [suggestions,     setSuggestions]     = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const suggestTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  // Sync all search/filter state to URL (read-back is safe: searchParams not in deps)
  useEffect(() => {
    setSearchParams(toUrlParams(query, sort, filters, page), { replace: true });
  }, [query, sort, filters, page, setSearchParams]); // eslint-disable-line react-hooks/exhaustive-deps

  // Update filters and always reset to page 0
  const applyFilters = useCallback((updater: React.SetStateAction<LumenFilters>) => {
    setFilters(updater);
    setPage(0);
  }, []);

  // Build API params from Lumen filter state
  const buildParams = useCallback(() => {
    const levels    = (filters.level ?? []).map(l => LEVEL_TO_API[l] ?? l.toUpperCase());
    const durations = durationToApiBuckets(filters.duration ?? []);
    const apiPrice: PriceFilter =
      filters.price === 'free' ? 'FREE' :
      filters.price === 'paid' || filters.price === 'u50' ? 'PAID' : 'ALL';

    return {
      q:         query || undefined,
      sort,
      minRating: filters.rating,
      price:     apiPrice !== 'ALL' ? apiPrice : undefined,
      duration:  durations.length ? durations : undefined,
      level:     levels.length ? levels : undefined,
      language:  (filters.language ?? []).length ? filters.language : undefined,
      page,
      size: 12,
    };
  }, [query, sort, filters, page]);

  useEffect(() => {
    setLoading(true);
    setError('');
    searchCourses(buildParams())
      .then(res => setResult(res.data.data))
      .catch(() => setError('Search failed. Make sure the search service is running.'))
      .finally(() => setLoading(false));
  }, [buildParams]);

  const handleInputChange = (val: string) => {
    setInputValue(val);
    clearTimeout(suggestTimer.current);
    if (val.length < 2) { setSuggestions([]); return; }
    suggestTimer.current = setTimeout(() => {
      autocomplete(val).then(res => {
        const d = res.data.data;
        const all = [...d.courses, ...d.instructors, ...d.categories, ...d.popularTerms].map(x => x.text);
        setSuggestions([...new Set(all)].slice(0, 8));
        setShowSuggestions(true);
      }).catch(() => {});
    }, 200);
  };

  const doSearch = useCallback((q: string) => {
    setQuery(q);
    setInputValue(q);
    setPage(0);
    setShowSuggestions(false);
  }, []);

  const addToCart    = (c: CourseHit) => setCart(cur => cur.find(x => x.courseId === c.courseId) ? cur : [...cur, c]);
  const removeFromCart = (id: string) => setCart(cur => cur.filter(x => x.courseId !== id));

  // Active filter chips
  const activeChips = useMemo(() => {
    type Chip = { gid: keyof LumenFilters; v: string | number; label: string; single: boolean };
    const chips: Chip[] = [];
    FILTER_GROUPS.forEach(g => {
      const fv = filters[g.id as keyof LumenFilters];
      if (g.kind === 'check' && Array.isArray(fv) && fv.length) {
        (fv as string[]).forEach(val => {
          const opt = g.options.find(o => String(typeof o === 'string' ? o : o.v) === val);
          chips.push({ gid: g.id as keyof LumenFilters, v: val, label: opt ? (typeof opt === 'string' ? opt : opt.label) : val, single: false });
        });
      } else if (g.kind === 'radio' && fv != null && fv !== 'all') {
        const opt = g.options.find(o => (typeof o === 'string' ? o : String(o.v)) === String(fv));
        chips.push({ gid: g.id as keyof LumenFilters, v: fv as string | number, label: opt ? (typeof opt === 'string' ? opt : opt.label) : String(fv), single: true });
      }
    });
    return chips;
  }, [filters]);

  const clearChip = (chip: { gid: keyof LumenFilters; v: string | number; single: boolean }) => {
    applyFilters(prev => {
      if (chip.single) { const n = { ...prev }; delete n[chip.gid]; return n; }
      const cur = new Set<string>((prev[chip.gid] as string[]) ?? []);
      cur.delete(String(chip.v));
      return { ...prev, [chip.gid]: Array.from(cur) };
    });
  };

  const totalPages = result ? Math.ceil(result.total / 12) : 0;

  return (
    <div className="lumen-page">
      <Navbar
        inputValue={inputValue}
        onInputChange={handleInputChange}
        onSearch={() => doSearch(inputValue)}
        onClear={() => { setInputValue(''); setQuery(''); setPage(0); }}
        onSuggestionClick={doSearch}
        suggestions={suggestions}
        showSuggestions={showSuggestions}
        setShowSuggestions={setShowSuggestions}
        cart={cart}
        removeFromCart={removeFromCart}
      />

      <main className="lm-page-main">
        <div className={`lm-two-col${!sidebarHidden ? ' lm-has-sidebar' : ''}`}>

          {/* Desktop sidebar */}
          {!sidebarHidden && (
            <div className="lm-sidebar-col">
              <div className="lm-sidebar-sticky">
                <FilterSidebar filters={filters} setFilters={applyFilters} embedded/>
              </div>
            </div>
          )}

          {/* Main content */}
          <div style={{ minWidth: 0 }}>
            {/* Toolbar */}
            <div className="lm-toolbar">
              <div>
                <h2 style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.4, color: 'var(--lm-ink)' }}>
                  {loading ? (
                    <span style={{ opacity: 0.4 }}>Searching…</span>
                  ) : result ? (
                    <>{result.total.toLocaleString()} results{query && <> for <span style={{ color: 'var(--lm-accent)' }}>"{query}"</span></>}</>
                  ) : '…'}
                </h2>
                <p style={{ fontSize: 13, marginTop: 3, color: 'var(--lm-muted)' }}>Updated daily · ranked by Lumen relevance</p>
              </div>

              <div className="lm-toolbar-actions">
                {/* Mobile: all filters button */}
                <button onClick={() => setDrawerOpen(true)} className="lm-filter-mobile lm-btn lm-btn-ghost" style={{ height: 40, fontSize: 13 }}>
                  <IcoFilter width="14" height="14"/> All filters
                </button>
                {/* Desktop: toggle sidebar */}
                <button onClick={() => setSidebarHidden(h => !h)} className="lm-filter-desktop lm-btn lm-btn-ghost" style={{ height: 40, fontSize: 13 }}>
                  <IcoFilter width="14" height="14"/> {sidebarHidden ? 'Show' : 'Hide'} filters
                </button>
                {/* Sort */}
                <div className="lm-sort-row">
                  <span style={{ fontSize: 11, fontFamily: "'JetBrains Mono',monospace", textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--lm-muted)' }}>sort</span>
                  <select
                    value={sort}
                    onChange={e => { setSort(e.target.value as SortOption); setPage(0); }}
                    style={{ height: 40, padding: '0 10px', border: '1px solid var(--lm-line)', borderRadius: 10, fontSize: 13, fontWeight: 500, fontFamily: 'inherit', background: 'var(--lm-surface)', color: 'var(--lm-ink)', cursor: 'pointer', outline: 'none' }}
                    onFocus={e => (e.currentTarget.style.borderColor = 'var(--lm-accent)')}
                    onBlur={e => (e.currentTarget.style.borderColor = 'var(--lm-line)')}
                  >
                    {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </div>
              </div>
            </div>

            {/* Active filter chips */}
            {activeChips.length > 0 && (
              <div className="flex items-center flex-wrap gap-2 mb-5">
                {activeChips.map((chip, i) => (
                  <button key={i} onClick={() => clearChip(chip)} className="lm-pill lm-pill-active">
                    {chip.label} <IcoClose width="12" height="12"/>
                  </button>
                ))}
                <button
                  onClick={() => applyFilters({})}
                  style={{ fontSize: 12, fontWeight: 500, marginLeft: 4, background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'inherit', color: 'var(--lm-muted)' }}
                  onMouseEnter={e => (e.currentTarget.style.textDecoration = 'underline')}
                  onMouseLeave={e => (e.currentTarget.style.textDecoration = '')}
                >Clear all</button>
              </div>
            )}

            {/* Error */}
            {error && (
              <div className="rounded-2xl px-4 py-3 mb-4" style={{ fontSize: 13, background: 'oklch(97% 0.02 30)', border: '1px solid oklch(88% 0.05 30)', color: 'oklch(42% 0.15 25)' }}>
                {error}
              </div>
            )}

            {/* Grid */}
            {loading ? (
              <div className={`lm-card-grid ${sidebarHidden ? 'lm-grid-nosidebar' : 'lm-grid-sidebar'}`}>
                {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} delay={i * 60}/>)}
              </div>
            ) : result?.hits.length === 0 ? (
              <div className="text-center py-20 rounded-2xl" style={{ border: '1px solid var(--lm-line-2)' }}>
                <div style={{ fontSize: 16, fontWeight: 600, color: 'var(--lm-ink)', marginBottom: 6 }}>No courses match those filters</div>
                <div style={{ fontSize: 13, color: 'var(--lm-muted)', marginBottom: 16 }}>Try removing a filter, or search a broader keyword.</div>
                <button onClick={() => applyFilters({})} className="lm-btn lm-btn-soft" style={{ height: 40, fontSize: 13 }}>Reset filters</button>
              </div>
            ) : (
              <div className={`lm-card-grid ${sidebarHidden ? 'lm-grid-nosidebar' : 'lm-grid-sidebar'}`}>
                {result?.hits.map(c => (
                  <CourseCard
                    key={c.courseId}
                    course={c}
                    inCart={!!cart.find(x => x.courseId === c.courseId)}
                    onAdd={addToCart}
                    onRemove={removeFromCart}
                  />
                ))}
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="mt-12 pt-6 flex items-center justify-between flex-wrap gap-3" style={{ borderTop: '1px solid var(--lm-line-2)', fontSize: 12, fontFamily: "'JetBrains Mono',monospace", textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--lm-muted)' }}>
                <span>showing {(page * 12 + 1).toLocaleString()}–{Math.min((page + 1) * 12, result?.total ?? 0).toLocaleString()} of {result?.total.toLocaleString()}</span>
                <div className="flex items-center gap-1">
                  <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="lm-btn lm-btn-ghost" style={{ height: 32, fontSize: 11 }}>Prev</button>
                  {Array.from({ length: Math.min(totalPages, 5) }).map((_, i) => {
                    const p = page <= 2 ? i : page >= totalPages - 3 ? totalPages - 5 + i : page - 2 + i;
                    if (p < 0 || p >= totalPages) return null;
                    return (
                      <button key={p} onClick={() => setPage(p)} className="lm-btn" style={{ height: 32, fontSize: 11, background: p === page ? 'var(--lm-accent)' : 'transparent', color: p === page ? '#fff' : 'var(--lm-muted)', border: p === page ? 'none' : '1px solid var(--lm-line)' }}>{p + 1}</button>
                    );
                  })}
                  <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} className="lm-btn lm-btn-ghost" style={{ height: 32, fontSize: 11 }}>Next</button>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer style={{ borderTop: '1px solid var(--lm-line-2)', background: 'var(--lm-surface)' }}>
        <div className="lm-footer-inner">
          <div>
            <Logo/>
            <p style={{ fontSize: 13, marginTop: 12, lineHeight: 1.6, color: 'var(--lm-muted)' }}>
              A marketplace for grounded, practitioner-led courses. Built for curious people.
            </p>
          </div>
          <FooterCol title="Learn"   links={['Browse all', 'Categories', 'Live cohorts', 'Free previews']}/>
          <FooterCol title="Company" links={['About', 'Instructors', 'Press', 'Careers']}/>
          <FooterCol title="Support" links={['Help center', 'Contact', 'Refund policy', 'Status']}/>
        </div>
        <div style={{ borderTop: '1px solid var(--lm-line-2)' }}>
          <div className="lm-footer-bottom" style={{ fontSize: 12, color: 'var(--lm-muted)' }}>
            <span>© 2026 Lumen Learning, Inc.</span>
            <span style={{ fontFamily: "'JetBrains Mono',monospace" }}>made for curious people</span>
          </div>
        </div>
      </footer>

      {/* Mobile drawer overlay */}
      <div className={`lm-drawer-overlay${drawerOpen ? ' open' : ''}`} onClick={() => setDrawerOpen(false)}/>

      {/* Mobile drawer */}
      <aside className={`lm-drawer${drawerOpen ? ' open' : ''}`}>
        <div className="flex items-center justify-between px-5 py-4" style={{ borderBottom: '1px solid var(--lm-line-2)', flexShrink: 0 }}>
          <span style={{ fontWeight: 700, fontSize: 15, color: 'var(--lm-ink)' }}>All filters</span>
          <button
            onClick={() => setDrawerOpen(false)}
            style={{ width: 36, height: 36, display: 'grid', placeContent: 'center', borderRadius: 8, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--lm-ink)' }}
            onMouseEnter={e => (e.currentTarget.style.background = 'var(--lm-line-2)')}
            onMouseLeave={e => (e.currentTarget.style.background = '')}
          ><IcoClose width="16" height="16"/></button>
        </div>
        <div className="flex-1 lm-scroll px-5 py-3" style={{ overflowY: 'auto' }}>
          <FilterSidebar filters={filters} setFilters={applyFilters} embedded/>
        </div>
        <div className="flex gap-2 p-4" style={{ borderTop: '1px solid var(--lm-line-2)', flexShrink: 0 }}>
          <button onClick={() => applyFilters({})} className="lm-btn lm-btn-ghost flex-1" style={{ height: 44, fontSize: 13 }}>Reset</button>
          <button onClick={() => setDrawerOpen(false)} className="lm-btn lm-btn-primary flex-1" style={{ height: 44, fontSize: 13 }}>
            Show {result?.total.toLocaleString() ?? '…'}
          </button>
        </div>
      </aside>
    </div>
  );
}

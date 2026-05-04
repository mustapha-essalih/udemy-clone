import { useEffect, useState } from 'react';
import type { BasicInfo, ValidationErrors, CategoryOption } from './types';
import { fetchCategories } from '../../api/categories';

interface Props {
  data: BasicInfo;
  set: (k: keyof BasicInfo, v: string | boolean) => void;
  errors: ValidationErrors;
}

function Counter({ value, max }: { value: string; max: number }) {
  const len = (value || '').length;
  const pct = len / max;
  const mod = pct >= 1 ? ' over' : pct > 0.9 ? ' warn' : '';
  return <div className={`counter${mod}`}>{len} / {max}</div>;
}

function WarnSvg() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 8v5M12 16.5v.5M3 19h18L12 3z"/>
    </svg>
  );
}

export default function StepBasicInfo({ data, set, errors }: Props) {
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [catLoading, setCatLoading] = useState(true);
  const [catError, setCatError] = useState(false);

  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch(() => setCatError(true))
      .finally(() => setCatLoading(false));
  }, []);

  const selectedCat = categories.find((c) => c.id === data.category);
  const subcats = selectedCat?.subCategories ?? [];

  return (
    <div className="step-content">

      {/* Card 1 — About */}
      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <div className="card-h">
          <h3>Tell us about your course</h3>
          <span className="count">SECTION 01 / 03</span>
        </div>
        <p className="card-desc">A clear, specific title and description help students decide if your course is right for them.</p>

        <div className="field">
          <label className="label">
            Course title <span className="req">*</span>
            <span className="hint">60 chars max</span>
          </label>
          <input
            type="text"
            maxLength={60}
            value={data.title}
            onChange={(e) => set('title', e.target.value)}
            placeholder="e.g. Advanced TypeScript: Patterns for Production"
            className={`input${errors.title ? ' error' : ''}`}
          />
          <Counter value={data.title} max={60} />
          {errors.title && <div className="error-msg"><WarnSvg />{errors.title}</div>}
        </div>

        <div className="field">
          <label className="label">
            Subtitle
            <span className="hint">120 chars</span>
          </label>
          <input
            type="text"
            maxLength={120}
            value={data.subtitle}
            onChange={(e) => set('subtitle', e.target.value)}
            placeholder="What students will be able to do after taking this course"
            className="input"
          />
          <Counter value={data.subtitle} max={120} />
          <div className="help">A short, punchy line beneath your title.</div>
        </div>

        <div className="field">
          <label className="label">
            Course description <span className="req">*</span>
          </label>
          <textarea
            rows={6}
            maxLength={2000}
            value={data.description}
            onChange={(e) => set('description', e.target.value)}
            placeholder="Describe what students will learn, the projects they'll build, and who this course is for."
            className={`textarea${errors.description ? ' error' : ''}`}
          />
          <Counter value={data.description} max={2000} />
          <div className="help">Markdown supported. Aim for 200+ characters.</div>
          {errors.description && <div className="error-msg"><WarnSvg />{errors.description}</div>}
        </div>
      </div>

      {/* Card 2 — Category */}
      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <div className="card-h">
          <h3>Category</h3>
          <span className="count">SECTION 02 / 03</span>
        </div>
        <p className="card-desc">Help students find your course in the right place.</p>

        <div className="field">
          <label className="label">
            Top-level category <span className="req">*</span>
          </label>
          {catLoading ? (
            <div className="muted" style={{ fontSize: 13, padding: '8px 0' }}>Loading categories…</div>
          ) : catError ? (
            <div className="error-msg"><WarnSvg />Failed to load categories. Please refresh and try again.</div>
          ) : (
            <div className="cat-grid">
              {categories.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  className={`cat-tile${data.category === c.id ? ' active' : ''}`}
                  onClick={() => { set('category', c.id); set('subcategory', ''); }}
                >
                  <span className="ct-name">{c.name}</span>
                  <span className="ct-meta">{c.subCategories.length} subcategories</span>
                </button>
              ))}
            </div>
          )}
          {errors.category && <div className="error-msg" style={{ marginTop: 8 }}><WarnSvg />{errors.category}</div>}
        </div>

        {data.category && subcats.length > 0 && (
          <div className="field">
            <label className="label">
              Subcategory <span className="req">*</span>
            </label>
            <select
              value={data.subcategory}
              onChange={(e) => set('subcategory', e.target.value)}
              className={`select${errors.subcategory ? ' error' : ''}`}
            >
              <option value="">Select a subcategory…</option>
              {subcats.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
            {errors.subcategory && <div className="error-msg"><WarnSvg />{errors.subcategory}</div>}
          </div>
        )}

        <div className="field-row">
          <div className="field" style={{ marginBottom: 0 }}>
            <label className="label">Difficulty level <span className="req">*</span></label>
            <div className="segmented">
              {(['beginner', 'intermediate', 'advanced', 'all'] as const).map((v) => (
                <button
                  key={v}
                  type="button"
                  className={`seg${data.level === v ? ' active' : ''}`}
                  onClick={() => set('level', v)}
                >
                  {v === 'all' ? 'All levels' : v.charAt(0).toUpperCase() + v.slice(1)}
                </button>
              ))}
            </div>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label className="label">Primary language</label>
            <select
              value={data.language}
              onChange={(e) => set('language', e.target.value)}
              className="select"
            >
              {['English', 'Spanish', 'French', 'German', 'Japanese', 'Portuguese', 'Arabic', 'Chinese'].map((l) => (
                <option key={l}>{l}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Card 3 — Pricing */}
      <div className="card card-pad">
        <div className="card-h">
          <h3>Pricing</h3>
          <span className="count">SECTION 03 / 03</span>
        </div>
        <p className="card-desc">Set a price or offer your course for free.</p>

        <div className="toggle-row">
          <div className="toggle-text">
            Make this course free
            <small>Free courses reach more learners but don't earn revenue.</small>
          </div>
          <button
            type="button"
            className={`switch${data.free ? ' on' : ''}`}
            onClick={() => set('free', !data.free)}
          />
        </div>

        {!data.free && (
          <div className="field">
            <label className="label">
              Price <span className="req">*</span>
              <span className="hint">Enter any amount</span>
            </label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={data.price}
              onChange={(e) => set('price', e.target.value)}
              placeholder="e.g. 29.99"
              className={`input${errors.price ? ' error' : ''}`}
              style={{ maxWidth: 220 }}
            />
            {errors.price && <div className="error-msg"><WarnSvg />{errors.price}</div>}
            <div className="help">Displayed in the currency your platform is configured for.</div>
          </div>
        )}
      </div>
    </div>
  );
}

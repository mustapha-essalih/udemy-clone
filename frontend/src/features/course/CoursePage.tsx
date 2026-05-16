import { useState, useRef, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getCourse } from '../../api/courses';
import type { CourseResponse, SectionResponse, LessonResponse } from '../../api/courses';
import './course.css';

type Svg = React.SVGProps<SVGSVGElement>;
const IcoSearch = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" {...p}><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>;
const IcoMenu   = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" {...p}><path d="M4 6h16M4 12h16M4 18h16"/></svg>;
const IcoPlay   = (p: Svg) => <svg viewBox="0 0 24 24" fill="currentColor" {...p}><path d="M8 5v14l11-7z"/></svg>;
const IcoCart   = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" {...p}><path d="M3 4h2l3 12h11l2-8H7"/><circle cx="9" cy="20" r="1.6"/><circle cx="18" cy="20" r="1.6"/></svg>;
const IcoHeart  = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" {...p}><path d="M20.5 7.5a5 5 0 0 0-8.5-3.4A5 5 0 0 0 3.5 7.5c0 6 8.5 11 8.5 11s8.5-5 8.5-11Z"/></svg>;
const IcoStar   = (p: Svg) => <svg viewBox="0 0 24 24" fill="currentColor" {...p}><path d="M12 2l2.9 6.9L22 10l-5.5 4.8L18 22l-6-3.6L6 22l1.5-7.2L2 10l7.1-1.1z"/></svg>;
const IcoChev   = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" {...p}><path d="m9 6 6 6-6 6"/></svg>;
const IcoCheck  = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" {...p}><path d="m4 12 5 5L20 6"/></svg>;
const IcoUser   = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" {...p}><path d="M16 11a4 4 0 1 0-8 0M4 21a8 8 0 0 1 16 0"/></svg>;
const IcoClock  = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" {...p}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>;
const IcoGlobe  = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" {...p}><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/></svg>;
const IcoDoc    = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" {...p}><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>;
const IcoQuiz   = (p: Svg) => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" {...p}><circle cx="12" cy="12" r="9"/><path d="M9 12l2 2 4-4"/></svg>;

const LEARN_ITEMS = [
  'Use generative AI tools confidently to draft, summarise and analyse work documents in minutes.',
  'Write reliable prompts using a repeatable five-part framework that works across models.',
  'Build no-code automations that connect your inbox, spreadsheets and team chat.',
  'Identify the right AI tool for the job — and recognise when it\'s the wrong one.',
  'Apply guardrails for privacy, accuracy and bias before sharing AI-generated work.',
  'Lead a small team through their first 90 days of practical AI adoption.',
  'Translate vague requests from leadership into measurable AI projects.',
  'Build a personal portfolio of AI workflows you\'ll actually use the next morning.',
];

const REQUIREMENTS = [
  'No prior coding, statistics or machine-learning experience required.',
  'A laptop with Chrome, Edge or Safari and a stable internet connection.',
  'Free accounts on at least one chat-based AI tool (we\'ll show you how).',
  'Roughly 30 minutes a day for two weeks to get the most out of the workshops.',
];

const REVIEWS = [
  {
    initials: 'JS', colorClass: 'ts-r-avatar-a', name: 'Jamal S.', time: '2 weeks ago', stars: 5,
    body: "Honestly the first AI course I've taken that didn't either condescend to me or assume I wanted to fine-tune a model. The 90-day plan alone is worth the price.",
  },
  {
    initials: 'PR', colorClass: 'ts-r-avatar-b', name: 'Priya R.', time: 'a month ago', stars: 4,
    body: "Tight pacing and very little fluff. The prompt framework felt obvious in hindsight, which is the highest compliment I can give a teaching framework. I've already shared it with my team.",
  },
  {
    initials: 'DV', colorClass: 'ts-r-avatar-c', name: 'Daniel V.', time: '2 months ago', stars: 5,
    body: "As a project manager who'd been quietly avoiding all of this, I finished the course actually excited about the next quarter. The 'first cheap project' idea is gold.",
  },
];

const REC_COURSES = [
  { thumbClass: 'ts-rec-thumb-1', thumbLabel: 'prompt engineering',   title: 'Prompt Engineering for Operators & PMs', author: 'Maya Lindquist', rating: 4.6, count: '2,108', price: '$12.99', was: '$74.99' },
  { thumbClass: 'ts-rec-thumb-2', thumbLabel: 'no-code automation',    title: 'Build a No-Code AI Workflow in a Weekend', author: 'Tomás Reyes',   rating: 4.8, count: '4,902', price: '$15.99', was: '$89.99' },
  { thumbClass: 'ts-rec-thumb-3', thumbLabel: 'data storytelling',     title: 'Data Storytelling Without a Statistics Degree', author: 'Hye-Jin Park',  rating: 4.5, count: '1,541', price: '$13.99', was: '$69.99' },
  { thumbClass: 'ts-rec-thumb-4', thumbLabel: 'change management',     title: 'Leading AI Adoption on Small Teams', author: 'Anouk Verhoeven', rating: 4.6, count: '986',   price: '$14.99', was: '$79.99' },
];

function Stars({ value, size = 16 }: { value: number; size?: number }) {
  const full  = Math.min(5, Math.floor(value));
  const half  = value - full >= 0.25 && value - full < 0.75;
  const empty = 5 - full - (half ? 1 : 0);
  return (
    <>
      {Array.from({ length: full }).map((_, i)  => <IcoStar key={`f${i}`} width={size} height={size}/>)}
      {half && <IcoStar width={size} height={size} style={{ opacity: 0.5 }}/>}
      {Array.from({ length: empty }).map((_, i) => <IcoStar key={`e${i}`} width={size} height={size} style={{ opacity: 0.22 }}/>)}
    </>
  );
}

function fmtDuration(minutes: number): string {
  const h = Math.floor(minutes / 60), m = minutes % 60;
  return h > 0 ? (m > 0 ? `${h}h ${m}m` : `${h}h`) : `${m}m`;
}

function getInitials(name: string): string {
  return name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
}

function getLessonIcon(lesson: LessonResponse): React.ReactNode {
  if (lesson.lessonType === 'VIDEO') return <IcoPlay className="ts-lecture-ico" width={16} height={16}/>;
  if (lesson.lessonType === 'TEXT' || lesson.lessonType === 'ARTICLE') return <IcoDoc className="ts-lecture-ico" width={16} height={16}/>;
  return <IcoQuiz className="ts-lecture-ico" width={16} height={16}/>;
}

function Header() {
  return (
    <header className="ts-header">
      <div className="ts-header-inner">
        <Link to="/search" className="ts-logo">
          <span className="ts-logo-mark">L</span>
          <span>lumen</span>
        </Link>

        <button className="ts-nav-cat" aria-label="Browse categories">
          <IcoMenu width={16} height={16}/>
          <span>Categories</span>
        </button>

        <form className="ts-search" role="search" onSubmit={e => e.preventDefault()}>
          <IcoSearch width={18} height={18}/>
          <input type="search" placeholder="Search 240,000+ courses, instructors, and topics" aria-label="Search"/>
          <span className="ts-search-kbd"><span className="ts-kbd">⌘</span><span className="ts-kbd">K</span></span>
        </form>

        <nav className="ts-nav-links" aria-label="Primary">
          <a href="#" className="ts-nav-link ts-hide-md">Teach</a>
          <a href="#" className="ts-nav-link ts-hide-md">My learning</a>
          <button className="ts-icon-btn" aria-label="Wishlist"><IcoHeart width={20} height={20}/></button>
          <button className="ts-icon-btn" aria-label="Cart"><IcoCart width={20} height={20}/></button>
          <span className="ts-avatar">RM</span>
        </nav>
      </div>
    </header>
  );
}

function CurriculumSection({ section, index }: { section: SectionResponse; index: number }) {
  const totalMin = section.lessons.reduce((s, l) => s + (l.durationMinutes ?? 0), 0);
  return (
    <details className="ts-section" open={index === 0}>
      <summary className="ts-section-summary">
        <IcoChev className="ts-chev" width={18} height={18}/>
        <span className="ts-sec-title">{index + 1} · {section.title}</span>
        <span className="ts-sec-meta">{section.lessons.length} lecture{section.lessons.length !== 1 ? 's' : ''}{totalMin > 0 ? ` · ${fmtDuration(totalMin)}` : ''}</span>
      </summary>
      <div className="ts-lectures">
        {section.lessons.map(lesson => (
          <div key={lesson.lessonId} className="ts-lecture">
            {getLessonIcon(lesson)}
            <span className="ts-lecture-name">{lesson.title}</span>
            {lesson.isPreview && <a href="#" className="ts-preview-link">Preview</a>}
            {!lesson.isPreview && <span/>}
            <span className="ts-lecture-dur">
              {lesson.durationMinutes ? fmtDuration(lesson.durationMinutes) : ''}
            </span>
          </div>
        ))}
      </div>
    </details>
  );
}

function PurchaseCard({ course }: { course: CourseResponse }) {
  const originalPrice = course.price ? Math.round(course.price * 5.5 / 10) * 10 - 1 : null;
  const discount = course.price && originalPrice && originalPrice > course.price
    ? Math.round((1 - course.price / originalPrice) * 100) : 0;

  return (
    <div className="ts-purchase-card" aria-label="Purchase course">
      <div className="ts-preview">
        <button className="ts-play-btn" aria-label="Play preview">
          <IcoPlay width={22} height={22}/>
        </button>
        <div className="ts-preview-cap">Preview this course</div>
      </div>
      <div className="ts-card-body">
        <div className="ts-price">
          {course.isFree ? (
            <span className="ts-price-now" style={{ color: '#1f8a5b' }}>Free</span>
          ) : (
            <>
              <span className="ts-price-now">${course.price?.toFixed(2)}</span>
              {originalPrice && <span className="ts-price-was">${originalPrice}</span>}
              {discount > 0 && <span className="ts-price-off">{discount}% off</span>}
            </>
          )}
        </div>
        {course.couponCode && (
          <p className="ts-timer">
            <b>3 days</b> left at this price! · Coupon <span className="ts-mono" style={{ color: 'var(--violet-700)' }}>{course.couponCode}</span> applied
          </p>
        )}

        <button className="ts-btn ts-btn-primary" type="button">
          <IcoCart width={18} height={18}/>
          Add to cart
        </button>
        <button className="ts-btn ts-btn-ghost" type="button">Buy now</button>
        <p className="ts-guarantee">30-day money-back guarantee · Full lifetime access</p>

        <div className="ts-actions-row">
          <a href="#"><span aria-hidden="true">♡</span>&nbsp;Wishlist</a>
          <a href="#"><span aria-hidden="true">↗</span>&nbsp;Share</a>
          <a href="#"><span aria-hidden="true">＋</span>&nbsp;Gift</a>
        </div>
      </div>
    </div>
  );
}

export default function CoursePage() {
  const { id } = useParams<{ id: string }>();
  const [course, setCourse]       = useState<CourseResponse | null>(null);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [descCollapsed, setDescCollapsed] = useState(true);
  const [showAllSections, setShowAllSections] = useState(false);
  const curriculumRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!id) return;
    getCourse(id)
      .then(res => setCourse(res.data.data))
      .catch(() => setError('Failed to load course. Make sure the course service is running.'))
      .finally(() => setLoading(false));
  }, [id]);

  function handleExpandAll() {
    const details = curriculumRef.current?.querySelectorAll<HTMLDetailsElement>('details.ts-section');
    if (!details) return;
    const anyClosed = [...details].some(d => !d.open);
    details.forEach(d => { d.open = anyClosed; });
  }

  if (loading) {
    return (
      <div className="tessera-page">
        <Header/>
        <div style={{ padding: '80px 24px', textAlign: 'center', color: 'var(--ink-500)' }}>Loading course…</div>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="tessera-page">
        <Header/>
        <div style={{ padding: '80px 24px', textAlign: 'center' }}>
          <p style={{ color: '#a02020', fontSize: 16 }}>{error || 'Course not found.'}</p>
          <Link to="/search" style={{ marginTop: 16, display: 'inline-block', color: 'var(--violet-700)' }}>← Back to search</Link>
        </div>
      </div>
    );
  }

  const totalLectures = course.sections.reduce((s, sec) => s + sec.lessons.length, 0);
  const totalMin      = course.courseDurationMinutes;
  const instructorInitials = getInitials(course.instructorId.slice(0, 6));
  const ratingDisplay = course.rating > 0 ? course.rating : 4.7;
  const updatedDate = new Date(course.createdAt).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  const visibleSections = showAllSections ? course.sections : course.sections.slice(0, 5);

  return (
    <div className="tessera-page">
      {}
      <link rel="preconnect" href="https://fonts.googleapis.com"/>
      <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin=""/>
      <link href="https://fonts.googleapis.com/css2?family=Geist:wght@300;400;500;600;700;800&family=Source+Serif+4:ital,opsz,wght@0,8..60,400;0,8..60,500;0,8..60,600;1,8..60,400&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet"/>

      <Header/>

      {}
      <section className="ts-hero">
        <div className="ts-container">
          <div className="ts-hero-inner">
            <div className="ts-hero-main">

              <nav className="ts-crumbs" aria-label="Breadcrumb">
                <Link to="/search">Courses</Link>
                <span className="ts-sep">/</span>
                <span className="ts-current">{course.title}</span>
              </nav>

              <span className="ts-eyebrow"><span className="ts-eyebrow-dot"/>&nbsp;Bestseller · Trending</span>

              <h1 className="ts-title">{course.title}</h1>

              {course.subTitle && <p className="ts-subtitle">{course.subTitle}</p>}

              <div className="ts-meta-row">
                <span className="ts-bestseller">★ Bestseller</span>

                <span className="ts-stars" aria-label={`Rating ${ratingDisplay} out of 5`}>
                  <b>{ratingDisplay.toFixed(1)}</b>
                  <span aria-hidden="true" style={{ display: 'flex', gap: 2, color: 'var(--violet-600)' }}>
                    <Stars value={ratingDisplay} size={16}/>
                  </span>
                  <a href="#reviews" className="ts-num">(3,184 ratings)</a>
                </span>

                <span className="ts-meta-pill">
                  <IcoUser width={15} height={15}/>
                  18,742 students
                </span>

                <span className="ts-meta-pill">
                  <IcoClock width={15} height={15}/>
                  Updated {updatedDate}
                </span>

                <span className="ts-meta-pill">
                  <IcoGlobe width={15} height={15}/>
                  {course.language || 'English'} · CC in 6 languages
                </span>
              </div>

              <div className="ts-author-row">
                <span className="ts-author-avatar">EM</span>
                <span>Created by <a href="#instructor">Elena Marchetti</a> · Senior Solutions Architect</span>
              </div>
            </div>

            {}
            <aside className="ts-rail-wrap">
              <PurchaseCard course={course}/>
            </aside>
          </div>
        </div>
      </section>

      {}
      <div className="ts-container">
        <div className="ts-main">
          <div className="ts-content">

            {}
            <section className="ts-block" id="learn">
              <div className="ts-learn-card">
                <h2 className="ts-block-h2">What you'll <em>learn</em></h2>
                <ul className="ts-learn-grid">
                  {LEARN_ITEMS.map((item, i) => (
                    <li key={i} className="ts-learn-item">
                      <IcoCheck width={18} height={18}/>
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </section>

            {}
            <section className="ts-block" id="curriculum">
              <h2 className="ts-block-h2">Course <em>content</em></h2>
              <div className="ts-curriculum-meta">
                <span>
                  <b style={{ color: 'var(--ink-900)' }}>{course.sections.length} section{course.sections.length !== 1 ? 's' : ''}</b>
                  {' '}· {totalLectures} lecture{totalLectures !== 1 ? 's' : ''}
                  {totalMin > 0 ? ` · ${fmtDuration(totalMin)} total` : ''}
                </span>
                <button className="ts-expand-all" onClick={handleExpandAll} type="button">
                  Expand all sections
                </button>
              </div>

              <div className="ts-curriculum" ref={curriculumRef}>
                {visibleSections.map((section, i) => (
                  <CurriculumSection key={section.sectionId} section={section} index={i}/>
                ))}
                {!showAllSections && course.sections.length > 5 && (
                  <button className="ts-show-all" onClick={() => setShowAllSections(true)} type="button">
                    Show all {course.sections.length} sections ↓
                  </button>
                )}
              </div>
            </section>

            {}
            <section className="ts-block" id="requirements">
              <h2 className="ts-block-h2">Requirements</h2>
              <ul className="ts-req-list">
                {REQUIREMENTS.map((req, i) => <li key={i}>{req}</li>)}
              </ul>
            </section>

            {}
            <section className="ts-block" id="description">
              <h2 className="ts-block-h2">Description</h2>
              <div className={`ts-show-more-wrap${descCollapsed ? ' ts-collapsed' : ''}`}>
                <div className="ts-desc">
                  {course.description ? (
                    <p>{course.description}</p>
                  ) : (
                    <>
                      <p>If your inbox is full of links to AI tools and your to-do list is full of "figure out AI" — this course is the calm, practical map you've been looking for. It's built for the analyst, the program manager, the recruiter, the lawyer, the marketer; for anyone whose job is mostly thinking, writing and meetings.</p>
                      <p>Over focused hours, you'll move from "I've heard of it" to confidently shipping AI-powered work that actually holds up under scrutiny. Every module ends with a small artifact you can show your manager on Monday morning — not a toy demo, but a real spreadsheet, draft, or workflow.</p>
                      <div className="ts-pull">"This is the version of the course I wish someone had handed me three years ago — when I had to learn this the slow way."</div>
                      <h3>Who this course is for</h3>
                      <p>This course is for non-technical professionals — operators, analysts, managers, designers, salespeople, founders — who feel a quiet pressure to "get good at AI" but aren't sure where to start without wading through tutorials aimed at engineers.</p>
                      <h3>What you'll walk away with</h3>
                      <p>A working prompt library tailored to your role, production-ready automations connected to the tools you already use, a one-page risk &amp; privacy checklist that passes most enterprise reviews, and the confidence to lead — rather than survive — your team's AI adoption.</p>
                    </>
                  )}
                </div>
                <button className="ts-show-more-btn" onClick={() => setDescCollapsed(c => !c)} type="button">
                  <span>{descCollapsed ? 'Show more' : 'Show less'}</span>
                  <IcoChev
                    width={14} height={14}
                    style={{ transform: descCollapsed ? '' : 'rotate(90deg)', transition: 'transform .2s' }}
                  />
                </button>
              </div>
            </section>

            {}
            <section className="ts-block" id="instructor">
              <h2 className="ts-block-h2">Instructor</h2>
              <div className="ts-instructor-card">
                <div className="ts-instructor-top">
                  <div className="ts-ins-avatar">EM</div>
                  <div className="ts-ins-info">
                    <h3>Elena Marchetti</h3>
                    <div className="ts-ins-role">Senior Solutions Architect · Former Head of Workflow at Lattice Labs</div>
                    <div className="ts-ins-stats">
                      <span className="ts-ins-stat">
                        <IcoStar width={14} height={14}/>
                        <b>{ratingDisplay.toFixed(1)}</b>&nbsp;Instructor rating
                      </span>
                      <span className="ts-ins-stat">
                        <svg width={14} height={14} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M21 15a4 4 0 0 1-4 4H7l-4 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/></svg>
                        9,214 reviews
                      </span>
                      <span className="ts-ins-stat">
                        <IcoUser width={14} height={14}/>
                        47,803 students
                      </span>
                      <span className="ts-ins-stat">
                        <svg width={14} height={14} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="3" y="4" width="18" height="14" rx="2"/><path d="M3 10h18"/></svg>
                        {course.sections.length} courses
                      </span>
                    </div>
                  </div>
                </div>
                <p className="ts-ins-bio">Elena has spent the last decade helping non-technical teams adopt the kind of tools engineers take for granted. After leading workflow design at Lattice Labs and consulting for a dozen Fortune 500 communications teams, she now writes, teaches, and occasionally still ships prompts that other people pretend they wrote. She believes the fastest path to good AI work is short, honest feedback loops — and a healthy refusal to be impressed by demos.</p>
              </div>
            </section>

            {}
            <section className="ts-block" id="reviews">
              <h2 className="ts-block-h2">Student feedback</h2>

              <div className="ts-reviews-summary">
                <div className="ts-big-rating">
                  <div className="ts-big-rating-num">{ratingDisplay.toFixed(1)}</div>
                  <div className="ts-big-rating-stars" aria-hidden="true">
                    <Stars value={ratingDisplay} size={18}/>
                  </div>
                  <div className="ts-big-rating-lbl">Course rating</div>
                </div>
                <div className="ts-bars">
                  {[{ stars: 5, pct: 68 }, { stars: 4, pct: 22 }, { stars: 3, pct: 7 }, { stars: 2, pct: 2 }, { stars: 1, pct: 1 }].map(({ stars, pct }) => (
                    <div key={stars} className="ts-bar-row">
                      <span className="ts-bar-stars" aria-hidden="true">
                        {Array.from({ length: stars }).map((_, i) => <IcoStar key={i} width={13} height={13}/>)}
                      </span>
                      <span className="ts-bar-track"><span className="ts-bar-fill" style={{ width: `${pct}%` }}/></span>
                      <span className="ts-bar-pct">{pct}%</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="ts-review-list">
                {REVIEWS.map((r, i) => (
                  <article key={i} className="ts-review">
                    <div className={`ts-r-avatar ${r.colorClass}`}>{r.initials}</div>
                    <div>
                      <div className="ts-r-name">{r.name}</div>
                      <div className="ts-r-meta">
                        <span className="ts-r-stars" aria-hidden="true">
                          <Stars value={r.stars} size={13}/>
                        </span>
                        <span>{r.time}</span>
                      </div>
                      <p className="ts-r-body">{r.body}</p>
                      <div className="ts-r-actions">
                        <span>Was this review helpful?</span>
                        <button>👍 Yes</button>
                        <button>👎 No</button>
                        <button>Report</button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>

            {}
            <section className="ts-block" id="recommended">
              <h2 className="ts-block-h2">Students also bought</h2>
              <p className="ts-block-sub">Continue building your AI fluency with hand-picked next steps from instructors students rated highly.</p>
              <div className="ts-rec-grid">
                {REC_COURSES.map((rc, i) => (
                  <a key={i} className="ts-rec" href="#">
                    <div className={`ts-rec-thumb ${rc.thumbClass}`}>{rc.thumbLabel}</div>
                    <div className="ts-rec-body">
                      <div className="ts-rec-title">{rc.title}</div>
                      <div className="ts-rec-author">{rc.author}</div>
                      <div className="ts-rec-rating">
                        {rc.rating.toFixed(1)}
                        <span className="ts-rec-stars" aria-hidden="true">
                          <Stars value={rc.rating} size={12}/>
                        </span>
                        <span className="ts-rec-count">({rc.count})</span>
                      </div>
                      <div className="ts-rec-foot">
                        <span className="ts-rec-now">{rc.price}</span>
                        <span className="ts-rec-was">{rc.was}</span>
                      </div>
                    </div>
                  </a>
                ))}
              </div>
            </section>

          </div>

          {}
          <div className="ts-rail" aria-hidden="true"/>
        </div>
      </div>
    </div>
  );
}

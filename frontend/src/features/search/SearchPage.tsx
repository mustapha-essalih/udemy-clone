import { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  searchCourses,
  autocomplete,
  type CourseHit,
  type SearchResponse,
  type FacetBucket,
  type SortOption,
  type DurationBucket,
  type PriceFilter,
} from '../../api/search';

const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL_LEVELS'];
const RATINGS = [4.5, 4.0, 3.5, 3.0];
const DURATIONS: { value: DurationBucket; label: string }[] = [
  { value: 'SHORT', label: '0–2 hours' },
  { value: 'MEDIUM', label: '2–10 hours' },
  { value: 'LONG', label: '10+ hours' },
];
const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'RELEVANCE', label: 'Most Relevant' },
  { value: 'RATING', label: 'Highest Rated' },
  { value: 'NEWEST', label: 'Newest' },
  { value: 'POPULARITY', label: 'Most Popular' },
  { value: 'PRICE_ASC', label: 'Price: Low to High' },
  { value: 'PRICE_DESC', label: 'Price: High to Low' },
];

function StarRating({ rating }: { rating: number | null }) {
  if (rating == null) return <span className="text-xs text-gray-400">No rating</span>;
  const full = Math.floor(rating);
  const partial = rating - full;
  return (
    <span className="flex items-center gap-1">
      <span className="text-yellow-400 text-sm font-semibold">{rating.toFixed(1)}</span>
      <span className="flex">
        {[1, 2, 3, 4, 5].map((i) => (
          <svg key={i} className="w-3.5 h-3.5" viewBox="0 0 20 20">
            <defs>
              <linearGradient id={`g-${i}-${rating}`}>
                <stop offset={i <= full ? '100%' : i === full + 1 ? `${partial * 100}%` : '0%'} stopColor="#FBBF24" />
                <stop offset={i <= full ? '100%' : i === full + 1 ? `${partial * 100}%` : '0%'} stopColor="#E5E7EB" />
              </linearGradient>
            </defs>
            <path fill={i <= full ? '#FBBF24' : i === full + 1 && partial > 0 ? `url(#g-${i}-${rating})` : '#E5E7EB'}
              d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
          </svg>
        ))}
      </span>
    </span>
  );
}

function DurationLabel({ minutes }: { minutes: number | null }) {
  if (minutes == null) return null;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return <span>{h > 0 ? `${h}h` : ''}{m > 0 ? ` ${m}m` : ''}</span>;
}

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [query, setQuery] = useState(searchParams.get('q') ?? '');
  const [inputValue, setInputValue] = useState(searchParams.get('q') ?? '');
  const [sort, setSort] = useState<SortOption>((searchParams.get('sort') as SortOption) ?? 'RELEVANCE');
  const [minRating, setMinRating] = useState<number | undefined>(
    searchParams.get('minRating') ? Number(searchParams.get('minRating')) : undefined
  );
  const [price, setPrice] = useState<PriceFilter>((searchParams.get('price') as PriceFilter) ?? 'ALL');
  const [durations, setDurations] = useState<DurationBucket[]>(
    searchParams.getAll('duration') as DurationBucket[]
  );
  const [levels, setLevels] = useState<string[]>(searchParams.getAll('level'));
  const [page, setPage] = useState(0);

  const [result, setResult] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const suggestTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const buildParams = useCallback(() => ({
    q: query || undefined,
    sort,
    minRating,
    price: price !== 'ALL' ? price : undefined,
    duration: durations.length ? durations : undefined,
    level: levels.length ? levels : undefined,
    page,
    size: 12,
  }), [query, sort, minRating, price, durations, levels, page]);

  useEffect(() => {
    setLoading(true);
    setError('');
    searchCourses(buildParams())
      .then((res) => setResult(res.data.data))
      .catch(() => setError('Search failed. Make sure the search service is running.'))
      .finally(() => setLoading(false));
  }, [buildParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setQuery(inputValue);
    setPage(0);
    setShowSuggestions(false);
    const sp: Record<string, string | string[]> = {};
    if (inputValue) sp['q'] = inputValue;
    if (sort !== 'RELEVANCE') sp['sort'] = sort;
    setSearchParams(sp, { replace: true });
  };

  const handleInputChange = (val: string) => {
    setInputValue(val);
    clearTimeout(suggestTimer.current);
    if (val.length < 2) { setSuggestions([]); return; }
    suggestTimer.current = setTimeout(() => {
      autocomplete(val).then((res) => {
        const d = res.data.data;
        const all = [
          ...d.courses.map((s) => s.text),
          ...d.instructors.map((s) => s.text),
          ...d.categories.map((s) => s.text),
          ...d.popularTerms.map((s) => s.text),
        ];
        setSuggestions([...new Set(all)].slice(0, 8));
        setShowSuggestions(true);
      }).catch(() => {});
    }, 200);
  };

  const toggleDuration = (d: DurationBucket) =>
    setDurations((prev) => prev.includes(d) ? prev.filter((x) => x !== d) : [...prev, d]);

  const toggleLevel = (l: string) =>
    setLevels((prev) => prev.includes(l) ? prev.filter((x) => x !== l) : [...prev, l]);

  const facetCategories = result?.facets['by_category'] ?? [];
  const facetLevels = result?.facets['by_level'] ?? [];

  const totalPages = result ? Math.ceil(result.total / 12) : 0;

  return (
    <div className="min-h-screen bg-gray-50 font-[var(--font-ui)]">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-4">
          <Link to="/dashboard" className="text-purple-700 font-bold text-xl tracking-tight shrink-0">
            LMS
          </Link>
          <form onSubmit={handleSearch} className="flex-1 relative">
            <div className="flex items-center bg-gray-100 rounded-full px-4 py-2 gap-2 max-w-2xl">
              <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                ref={searchInputRef}
                type="text"
                value={inputValue}
                onChange={(e) => handleInputChange(e.target.value)}
                onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
                placeholder="Search for courses, instructors..."
                className="flex-1 bg-transparent outline-none text-sm text-gray-800 placeholder-gray-400"
              />
              <button type="submit" className="bg-purple-700 text-white text-xs font-semibold px-4 py-1.5 rounded-full hover:bg-purple-800 transition-colors">
                Search
              </button>
            </div>
            {showSuggestions && suggestions.length > 0 && (
              <ul className="absolute top-full mt-1 left-0 right-0 max-w-2xl bg-white border border-gray-200 rounded-xl shadow-lg overflow-hidden z-50">
                {suggestions.map((s) => (
                  <li
                    key={s}
                    onMouseDown={() => {
                      setInputValue(s);
                      setQuery(s);
                      setPage(0);
                      setShowSuggestions(false);
                    }}
                    className="flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50 cursor-pointer text-sm text-gray-700"
                  >
                    <svg className="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    {s}
                  </li>
                ))}
              </ul>
            )}
          </form>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 py-6 flex gap-6">
        <aside className="w-56 shrink-0 hidden lg:block">
          <div className="bg-white rounded-2xl border border-gray-200 p-5 space-y-6 sticky top-20">

            <div>
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Price</h3>
              <div className="space-y-1.5">
                {(['ALL', 'FREE', 'PAID'] as PriceFilter[]).map((p) => (
                  <label key={p} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="price"
                      checked={price === p}
                      onChange={() => { setPrice(p); setPage(0); }}
                      className="accent-purple-700"
                    />
                    <span className="text-sm text-gray-700">{p === 'ALL' ? 'All' : p === 'FREE' ? 'Free' : 'Paid'}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Rating</h3>
              <div className="space-y-1.5">
                {RATINGS.map((r) => (
                  <label key={r} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="rating"
                      checked={minRating === r}
                      onChange={() => { setMinRating(r); setPage(0); }}
                      className="accent-purple-700"
                    />
                    <StarRating rating={r} />
                    <span className="text-xs text-gray-500">& up</span>
                  </label>
                ))}
                {minRating != null && (
                  <button onClick={() => { setMinRating(undefined); setPage(0); }} className="text-xs text-purple-600 hover:underline">
                    Clear
                  </button>
                )}
              </div>
            </div>

            <div>
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Duration</h3>
              <div className="space-y-1.5">
                {DURATIONS.map(({ value, label }) => (
                  <label key={value} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={durations.includes(value)}
                      onChange={() => { toggleDuration(value); setPage(0); }}
                      className="accent-purple-700 rounded"
                    />
                    <span className="text-sm text-gray-700">{label}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Level</h3>
              <div className="space-y-1.5">
                {(facetLevels.length > 0
                  ? facetLevels.map((b: FacetBucket) => b.key)
                  : LEVELS
                ).map((l) => (
                  <label key={l} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={levels.includes(l)}
                      onChange={() => { toggleLevel(l); setPage(0); }}
                      className="accent-purple-700 rounded"
                    />
                    <span className="text-sm text-gray-700 capitalize">{l.replace('_', ' ').toLowerCase()}</span>
                  </label>
                ))}
              </div>
            </div>

            {facetCategories.length > 0 && (
              <div>
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Category</h3>
                <div className="space-y-1.5">
                  {facetCategories.slice(0, 8).map((b: FacetBucket) => (
                    <div key={b.key} className="flex items-center justify-between text-sm">
                      <span className="text-gray-700">{b.key}</span>
                      <span className="text-xs text-gray-400">{b.count}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </aside>

        <main className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-4">
            <div className="text-sm text-gray-500">
              {loading ? (
                <span className="animate-pulse">Searching...</span>
              ) : result ? (
                <span>
                  <span className="font-semibold text-gray-800">{result.total.toLocaleString()}</span> results
                  {query && <> for <span className="font-semibold text-gray-800">"{query}"</span></>}
                  <span className="text-gray-400 ml-2">({result.tookMillis}ms)</span>
                </span>
              ) : null}
            </div>
            <select
              value={sort}
              onChange={(e) => { setSort(e.target.value as SortOption); setPage(0); }}
              className="text-sm border border-gray-200 rounded-lg px-3 py-1.5 bg-white text-gray-700 outline-none focus:ring-2 focus:ring-purple-300"
            >
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm mb-4">
              {error}
            </div>
          )}

          {!loading && result?.hits.length === 0 && (
            <div className="text-center py-20 text-gray-400">
              <svg className="w-12 h-12 mx-auto mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-lg font-medium text-gray-500">No courses found</p>
              <p className="text-sm mt-1">Try adjusting your filters or search term</p>
            </div>
          )}

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="bg-white rounded-2xl border border-gray-100 overflow-hidden animate-pulse">
                  <div className="h-36 bg-gray-100" />
                  <div className="p-4 space-y-2">
                    <div className="h-4 bg-gray-100 rounded w-3/4" />
                    <div className="h-3 bg-gray-100 rounded w-1/2" />
                    <div className="h-3 bg-gray-100 rounded w-2/3" />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
              {result?.hits.map((course) => (
                <CourseCard key={course.courseId} course={course} />
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              {Array.from({ length: Math.min(totalPages, 7) }).map((_, i) => {
                const p = page <= 3 ? i : page >= totalPages - 4 ? totalPages - 7 + i : page - 3 + i;
                if (p < 0 || p >= totalPages) return null;
                return (
                  <button
                    key={p}
                    onClick={() => setPage(p)}
                    className={`px-3 py-1.5 text-sm rounded-lg border ${p === page ? 'bg-purple-700 text-white border-purple-700' : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'}`}
                  >
                    {p + 1}
                  </button>
                );
              })}
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 text-sm rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

function CourseCard({ course }: { course: CourseHit }) {
  const levelColors: Record<string, string> = {
    BEGINNER: 'bg-green-100 text-green-700',
    INTERMEDIATE: 'bg-yellow-100 text-yellow-700',
    ADVANCED: 'bg-red-100 text-red-700',
    ALL_LEVELS: 'bg-blue-100 text-blue-700',
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col">
      <div className="h-36 bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-2 left-3 w-16 h-16 rounded-full bg-white" />
          <div className="absolute bottom-2 right-3 w-24 h-24 rounded-full bg-white" />
        </div>
        <span className="text-white text-2xl font-bold tracking-tight z-10 px-4 text-center leading-tight line-clamp-2">
          {course.title.substring(0, 2).toUpperCase()}
        </span>
      </div>

      <div className="p-4 flex flex-col flex-1">
        <h3 className="font-semibold text-gray-900 text-sm leading-snug line-clamp-2 mb-1">
          {course.title}
        </h3>
        <p className="text-xs text-gray-500 mb-2">{course.instructorName}</p>

        <div className="flex items-center gap-2 mb-2">
          <StarRating rating={course.rating} />
          {course.ratingCount != null && (
            <span className="text-xs text-gray-400">({course.ratingCount.toLocaleString()})</span>
          )}
        </div>

        <div className="flex items-center gap-2 flex-wrap mb-3">
          {course.level && (
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${levelColors[course.level] ?? 'bg-gray-100 text-gray-600'}`}>
              {course.level.replace('_', ' ')}
            </span>
          )}
          {course.durationMinutes != null && (
            <span className="text-xs text-gray-400 flex items-center gap-1">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <DurationLabel minutes={course.durationMinutes} />
            </span>
          )}
          {course.language && (
            <span className="text-xs text-gray-400">{course.language}</span>
          )}
        </div>

        <div className="mt-auto flex items-center justify-between">
          {course.isFree ? (
            <span className="text-green-600 font-bold text-sm">Free</span>
          ) : course.price != null ? (
            <span className="text-gray-900 font-bold text-sm">${course.price.toFixed(2)}</span>
          ) : (
            <span className="text-gray-400 text-sm">—</span>
          )}
          {course.enrollments != null && (
            <span className="text-xs text-gray-400">{course.enrollments.toLocaleString()} enrolled</span>
          )}
        </div>
      </div>
    </div>
  );
}

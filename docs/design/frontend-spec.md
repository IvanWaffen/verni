# VERNI — Frontend Specification

> **Version:** 1.0  
> **Date:** 2026-10-11  
> **Status:** binding for frontend issues; deviations require an update of this document  
> **Related:** [business-requirements.md](../business-requirements.md), [architecture-rules.md](../architecture-rules.md) §7, [ADR-0003](../adr/0003-public-location-coarsening.md), [ADR-0004](../adr/0004-client-side-draft.md)

## 0. Sources

The mockups are AI-generated UI concepts. **Binding:** layout, information hierarchy, tokens, component set, tone. **Not binding:** pixel-exact sizes, raster details, sample data and numbers.

| File | Screen | Issues |
|---|---|---|
| [00-design-system](mockups/00-design-system.webp) | Brand, palette, typography, spacing, icons, components, tone | #4 |
| [01-home](mockups/01-home.webp) | Home (desktop + mobile) | #11, #4 |
| [02-map](mockups/02-map.webp) | Map with list (desktop + mobile) | #17 |
| [03-create-listing](mockups/03-create-listing.webp) | Create listing (desktop + mobile) | #10 |
| [04-listing-card](mockups/04-listing-card.webp) | Listing card (desktop + mobile) | #11 |
| [05-my-listings-board](mockups/05-my-listings-board.webp) | Personal Kanban board (desktop + mobile) | #19 |

## 1. Deviations from the mockups (decided 2026-10-11)

Business rules and ADRs take precedence over the mockups.

| Mockup shows | Implement instead | Why |
|---|---|---|
| Public "Особые приметы" block with all distinguishing details | Two fields: **public features** (color, brand, material; shown on the card) and **hidden features for verification** (visible only to the author, never in public DTOs) | BR-05, AR-29, AR-36 |
| "Как с вами связаться?": phone option "покажем после проверки" | Removed. Contact **only via the service**; no phone field | Product principle 4 |
| Toggle "Показывать только район находки", "точное место будет видно после начала переписки" | Removed. The public point is **always** coarsened; the exact point is visible only to the author. Hint text: "На карте будет показан только примерный район" | ADR-0003 |
| Home stats "12 340 объявлений / 4 850 найдено / 1 220 возвращено" | Real numbers from the API, shown only above a configurable threshold; otherwise the block is hidden | "No fictional KPIs" (BR §9) |
| Like counters ♡24 and comment counters on listing cards | Removed from public cards | Social features are out of MVP (BR §11); responses are private |
| Favorites (heart "Сохранить"), view counters, in-app notifications (bell, "Последняя активность"), meeting time on handover, "Истории возвратов" page | **Deferred** to the post-1.0 backlog (#30). Hide these controls; leave layout space where natural | Not in BR; owner decision |
| Public "История объявления" with views and "3 человека заинтересовались" | Public history limited to: published, updated, closed with outcome. Full history only for the author (#6, #19) | Response count and activity are private |
| "Похожие объявления" on a public card | Allowed as a **discovery** feature (same category, nearby, public data only), Release 1.0 (#17). Not the same as match suggestions (BR-06, own listings only) | BR-06 scope |
| Exact-looking pins on maps | Pins are placed at the **public (coarsened) point**; the card map shows an **area circle**, never a "right here" pin | ADR-0003 |
| Terracotta buttons with white text, terracotta links | Use `accent-strong` for text and filled buttons with white text; `accent` only for fills, icons and large decorative elements without text | WCAG 2.2 AA (§2.1) |
| Light theme only | MVP ships the **light theme only**; tokens are defined so that dark mode can be added later | No dark mockups |

## 2. Design tokens

Implemented as CSS variables in `globals.css` + Tailwind theme (`@theme`), consumed by shadcn/ui components.

### 2.1 Colors

| Token | Value | Use |
|---|---|---|
| `accent` | `#F3704B` (terracotta) | Brand fills: logo tile, FAB "+", icon backgrounds, lost-pin fill, illustrations |
| `accent-strong` | `#C2410C` | Text/links in accent color, filled accent buttons with white text (5.2:1 on white text, 4.9:1 on cream) |
| `primary` | `#1E4D3A` (dark green) | Primary buttons, active tabs/segments, headings |
| `background` | `#FEF9F2` (cream) | Page background |
| `surface` | `#FFFFFF` | Cards, inputs, sheets |
| `surface-muted` | `#F6E7D6` (sand) | Secondary blocks, image placeholders |
| `surface-green` | `#DCEDE2` (light green) | Tags, "found" tints, info blocks |
| `text` | `#1E4D3A` / near-black `#1F2A24` for body | Main text |
| `text-secondary` | `#5E6B64` | Secondary text (the mockup's `#6B7B73` is 4.3:1, below AA) |
| `success` | `#22C55E` | Icons, borders; text on tints uses a darker shade (`#15803D`) |
| `danger` | `#EF4444` | Errors, destructive actions; text uses `#B91C1C` |
| `info` | `#3B82F6` | Info, "Возвращено" badge; text uses `#1D4ED8` |
| `warning` | `#F59E0B` | Warnings; text uses `#B45309` |

**Status badges** (tinted background + dark text, never white on saturated color):

| Status | Label | Colors |
|---|---|---|
| Listing type FOUND | "Найдено" | `surface-green` bg, `#15803D` text |
| Listing type LOST | "Потеряно" | `#FDE8E4` bg, `#B91C1C` text |
| Closed RETURNED | "Возвращено" | `#DBEAFE` bg, `#1D4ED8` text |
| Hidden / other closed | "Скрыто" / "Закрыто" | neutral bg, `text-secondary` |

Every color pair used for text must pass 4.5:1 (3:1 for ≥ 24 px or ≥ 18.66 px bold and for UI component boundaries). The axe check in CI enforces it.

### 2.2 Typography

- **Font:** Rubik (Latin + Cyrillic) via `next/font/google`, replacing Geist.
- **Handwritten accent** ("Хорошие вещи возвращаются", "Маленькие потери — большие встречи"): Caveat (Cyrillic). Decorative only, `aria-hidden`, never carries information.

| Style | Size / line height | Weight |
|---|---|---|
| Display | 56 / 120% (mobile 36) | 700 |
| H1 | 40 / 120% (mobile 28) | 700 |
| H2 | 28 / 120% (mobile 22) | 600 |
| H3 | 20 / 130% | 600 |
| Body | 16 / 140% | 400 |
| Caption | 14 / 140% | 400 |
| Button | 16 / 140% | 600 |

Minimum text size on mobile: 14 px; form inputs: 16 px (prevents iOS zoom).

### 2.3 Spacing, radii, shadows, breakpoints

- **Spacing scale (8 px base):** 4, 8, 12, 16, 24, 32, 48.
- **Radii:** 8 (chips, small), 12 (inputs, buttons), 16 (cards), 24 (large panels, sheets).
- **Shadows:** card `0 2 8 rgba(16,24,40,.06)`; elevated `0 8 24 rgba(16,24,40,.08)`; modal `0 20 40 rgba(16,24,40,.12)`.
- **Breakpoints:** base 360, `md` 768, `lg` 1024, `xl` 1280. Content max width 1200.
- **Touch targets:** ≥ 44×44 px.

### 2.4 Icons

`lucide-react` (bundled with shadcn/ui): line icons, 24 px, stroke 2, rounded. The category icons in the mockups map to Lucide equivalents (key-round, backpack, wallet, file-text, smartphone, headphones, bike, more-horizontal).

## 3. Component inventory

Built on shadcn/ui in `src/shared/ui`, documented with examples on an internal `/dev/ui` page (excluded from production builds).

| Component | Notes |
|---|---|
| `Button` | Variants: `primary` (dark green), `accent` (`accent-strong`), `outline`, `ghost`, `link`; sizes `md` (44 px), `lg` (52 px); icon-left support; loading state |
| `Chip` / `ChipGroup` | Single/multi-select filter chips (categories, "Все") |
| `StatusBadge` | Values from §2.1 |
| `SearchBar` | Text input + filter button; desktop variant with category and city selects and "Найти" |
| `Select` | City select ("Жуковский"), category select |
| `SegmentedControl` | "Все / Найдено / Потеряно / Рядом", "На карте / Списком" |
| `CategoryPicker` | Icon tiles (create form), horizontally scrollable on mobile |
| `ListingCard` | Variants: `grid` (photo on top: badge, title, area · relative time), `row` (thumbnail left, used in map list and board), `compact` (similar listings) |
| `PhotoUploader` | Drop zone + camera on mobile, thumbnails with remove, counter "3/10", client downscale (ADR-0004) |
| `FormField` | Label, required mark, hint, error, character counter "17/100" |
| `Stepper` | 4 steps; mobile shows short labels |
| `MapView` / `MapPin` / `ClusterPin` | MapLibre, client-only; FOUND pins `primary`, LOST pins `accent`; clusters show count; area circle for a single listing |
| `BottomSheet` | Mobile map: selected listing card |
| `Toast` | sonner; "Объявление опубликовано!" |
| `MessagePreview` | Avatar (initials), name, snippet, time, unread counter |
| `Timeline` | Listing history |
| `SafetyTips` | "Безопасно и надёжно" checklist card |
| `StatCard` | Icon + number + caption |
| `EmptyState`, `ErrorState`, `Skeleton` | For every list and page |
| `AppHeader` | Desktop: logo, nav (Объявления, Карта, Как это работает, Помощь), "Войти" or avatar menu, CTA "Разместить объявление" |
| `BottomNav` | Mobile: Главная, Карта, **+** (raised accent button), Мои, Профиль; active item in `accent-strong` |
| `Breadcrumbs`, `Avatar` (initials) | — |

## 4. Screens

### 4.1 Home — [01-home](mockups/01-home.webp) (#11)

1. Eyebrow "ГОРОД ПОМОГАЕТ ГОРОДУ"; Display "У каждой потери есть шанс **найтись.**" (accent word in `accent-strong`); subtitle.
2. CTAs: "Я нашёл вещь" (primary), "Я потерял вещь" (outline): both open the create flow with the type preselected.
3. Hero illustration (placeholder, §6) with floating chips ("Жуковский · Сегодня / Найдены ключи", "Найдён владелец!"). Chips are illustrative and must not show fabricated data as real.
4. Search: desktop = query + category + city + "Найти"; mobile = query + filter button. Submits to the feed with filters.
5. Stats row: real numbers only (§1).
6. "Последние объявления": horizontally scrollable `ListingCard grid`, "Все объявления →".
7. "Популярные категории": category tiles with real counts.
8. "Как это работает": 3 steps (Нашли или потеряли → Разместили объявление → Связались и вернули).
9. Footer: rules, safety, privacy, help.

Also: a feed page (`/listings`) with the full list, filters (type, category, text) and cursor pagination ("Показать ещё").

### 4.2 Map — [02-map](mockups/02-map.webp) (#17)

- Filters row: search, Категория, Статус (Найдено/Потеряно), Когда, Радиус (default 5 км), Город.
- Desktop: map + right panel "Объявления рядом · N найдено" with `ListingCard row`, toggle "На карте / Списком".
- Mobile: full-screen map, filter chips, bottom sheet with the selected listing; controls: zoom, "Моё местоположение" (browser permission, manual fallback).
- Distances are rounded ("400 м", "1,2 км") and computed from public points.
- The map is optional (AR-44): the list works if tiles fail.

### 4.3 Create listing — [03-create-listing](mockups/03-create-listing.webp) (#10)

Type switch "Я нашёл вещь / Я потерял вещь". Steps: **1. Основная информация → 2. Место и время → 3. Детали → 4. Проверка и публикация**. Desktop shows the steps as sections of one page with a stepper; mobile shows one step per screen with "Далее: Место и время →".

| Field | Rules |
|---|---|
| Фотографии | Up to 10, JPG/PNG/HEIC/WebP, up to 10 MB each before client downscale (final limits in #9). Optional for LOST; for FOUND per the #9 decision |
| Название вещи * | 3–100 chars, counter |
| Категория * | `CategoryPicker` (§5) |
| Описание * | 10–500 chars, counter |
| Где это произошло? * | Address search or point on the map; hint "На карте будет показан только примерный район" |
| Дата и время * | Date (not in the future) + optional time |
| Публичные приметы | Up to 300 chars; placeholder "Цвет, бренд, материал" |
| Скрытые приметы для проверки | Up to 300 chars, lock icon; hint "Их увидите только вы. Попросите владельца назвать их, чтобы убедиться, что вещь его" (FOUND); for LOST: "Детали, которые знаете только вы" |

- Side panel "Несколько простых правил для быстрой встречи" (do not publish passport data; add distinguishing details to the hidden field; clear photos; moderation notice).
- "Сохранить черновик" saves locally (IndexedDB, ADR-0004); the draft is restored on return.
- Step 4 shows the preview of the **public** card exactly as others will see it, then "Опубликовать объявление". A guest goes through login (#7) and returns to publishing without losing the draft.
- Contact section removed (§1).

### 4.4 Listing card — [04-listing-card](mockups/04-listing-card.webp) (#11)

- Breadcrumbs; gallery with thumbnails and counter "1/4" (swipe on mobile).
- Status badge, event date/time, title, area + "Показать на карте" (opens the map centered on the public point), description, public features.
- Map block: area circle + "Примерное место находки" + note "Показано примерное место".
- Primary CTA: FOUND → "Это может быть моя вещь"; LOST → "Кажется, я нашёл эту вещь". Both open a response (#12); a guest goes through login. Hidden for the author (author sees "Редактировать" / "Закрыть объявление" from `allowedActions`).
- Secondary: "Поделиться" (Web Share / copy link), "Пожаловаться" (#14). "Сохранить" is deferred (§1).
- Side: "Безопасно и надёжно" tips; "Похожие объявления" (#17, Release 1.0); public history (§1).
- Closed listing: badge "Возвращено" or "Закрыто", CTA hidden, explanatory banner.
- SEO/OG: title, area, category, first photo; no private data (AR-39).

### 4.5 My listings board — [05-my-listings-board](mockups/05-my-listings-board.webp) (#19)

- Header "МОЙ КАБИНЕТ / Мои объявления", stat cards: Активно, Есть отклики, Возвращено.
- Tabs: "Мои вещи" | "Отклики (N)" (responses I sent, #12).
- Columns (computed on the server, AR-11): **Активные** (Публично на карте), **Есть отклики** (Нужно ответить), **Передача согласована** (Ожидает подтверждения: shows whose confirmation is pending, ADR-0002), **Завершено** (Успешные возвраты; "Показать все").
- Mobile: tabs with counters instead of columns.
- Card: thumbnail, badge, title, area · relative time, new responses count, "⋯" menu built from `allowedActions`.
- "Разместить ещё одно объявление".
- "Последняя активность" feed, avatars stack, meeting time, views: deferred (§1). The "Больше добрых историй" illustration panel is optional decoration.

### 4.6 Screens without mockups

Designed by the implementer within this system, mobile-first:

| Screen | Issue |
|---|---|
| Login: email → code/link → back to the previous action | #7 |
| Conversation: listing summary, messages (polling), "Согласовать передачу" | #12 |
| Handover agreed / confirm return / outcome "Возвращено владельцу" | #13 |
| Report dialog; moderation queue (desktop-first) | #14 |
| Profile (display name, email, logout, delete account) | #7, #16 |
| How it works, Help, Rules, Safety, Privacy | #16 |
| Demo landing, persona switcher, "Демо" badge | #20 |
| 404, error, offline shell | #4 |

## 5. Categories (proposal for #18)

| Category | Icon | Sensitive (1500 m grid) |
|---|---|---|
| Ключи | key-round | yes |
| Сумки и рюкзаки | backpack | no |
| Кошельки и банковские карты | wallet | yes |
| Документы | file-text | yes |
| Телефоны | smartphone | no |
| Наушники | headphones | no |
| Другая электроника | laptop | no |
| Транспорт (велосипеды, самокаты) | bike | no |
| Другое | more-horizontal | no |

## 6. Illustrations and brand assets

- **Now:** placeholders cropped from the mockups (hero backpack, city skyline, "how it works" icons), exported as WebP with `width`/`height`, decorative `alt=""`. Logo: recreate as SVG (arrow in a rounded terracotta tile + "ВЕРНИ" wordmark).
- **Later:** final illustrations as SVG/WebP (#29).

## 7. Copy and formatting

- UI language: Russian; tone: warm, calm, helpful (design system "Тон интерфейса"). Avoid alarming wording; errors explain what to do next.
- Relative time via `Intl.RelativeTimeFormat('ru')` ("2 ч назад", "Вчера"); absolute "14 октября 2026, 16:40".
- Plurals via `Intl.PluralRules('ru')` ("1 объявление / 3 объявления / 36 объявлений").
- Distances: "400 м", "1,2 км".
- UI texts live in one module per feature (`texts.ts`), no hard-coded strings scattered in JSX, to allow later i18n.

## 8. Acceptance criteria for every screen

- No horizontal scroll at 360 px; usable one-handed (primary CTA reachable at the bottom on mobile).
- Touch targets ≥ 44 px; keyboard navigation and visible focus; labeled fields.
- axe: no violations; contrast per §2.1.
- Loading (skeleton), empty and error states exist.
- Buttons and actions come from server `allowedActions` (AR-42).
- Playwright screenshot of the screen at 360 px and 1280 px attached to the PR.

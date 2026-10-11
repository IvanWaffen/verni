# VERNI — Business and Functional Requirements

> **Version:** 1.1 (concept and MVP)  
> **Date:** 2026-10-11  
> **Status:** baseline for task breakdown and acceptance criteria  
> **Related documents:** [tech-stack.md](tech-stack.md), [architecture-rules.md](architecture-rules.md)

## 1. Product idea

**VERNI** ("give it back" in Russian) is a public web platform, free for users at launch, that helps people in Russia find and return lost items.

**One-liner:** "Lost or found something? Post it in a minute, reach people nearby, and help the item get back home."

The product must not feel like a CRM, Jira, or a task board. For a visitor it is **a map and a feed of real stories**. Kanban is used only in the personal area to manage one's own posts and responses.

### Primary business goal

Shorten the path **from noticing a loss or a find to a confirmed return of the item to its owner**, lower the barrier to posting, and increase the likelihood that the two sides get in touch.

### Product value

- **The finder** quickly posts an item without publicly revealing their phone number or exact address.
- **The owner** finds matching posts nearby or posts their loss.
- **Both sides** get in touch safely, verify ownership, and agree on a handover.
- **The author** closes the story with a clear outcome.
- **A guest** can open a public link and help spread the post.

## 2. Target audience and scenarios

| Segment | Goal | Desired result |
|---|---|---|
| Person who found an item | Report the find near where it was found | Get a relevant response and return the item |
| Person who lost an item | Check finds and post the loss | Find the item and contact the finder |
| Helping user | Share a link | Increase the post's reach |
| Moderator | Remove spam, fraudulent and dangerous posts | Preserve trust in the platform |
| Organizations (later) | Manage many found items | Shared workspace and staff roles — out of MVP |

**Initial geographic focus:** Russia. The pilot may be limited to one city or region. The UI is in Russian; there are no ruble transactions in the MVP.

## 3. Product principles

1. **Mobile-first:** most key operations work on a phone without zooming.
2. **Browse without signing up:** the map, the feed and item cards are available to guests.
3. **Fast creation:** a draft is created before login; the account is confirmed right before publishing.
4. **No public exposure of personal contacts:** communication happens through an in-app response.
5. **Approximate location:** the public point does not reveal a home address or the exact place where a valuable item is kept.
6. **One card = one specific item or a logically single set**, not an abstract task.
7. **Verifiable outcome:** a post cannot be considered "returned" after a single message or a mere algorithmic match.
8. **Shareable by link:** a post can be shared without installing an app.
9. **Trust over growth:** reports, blocking and moderation are part of the first public version.

## 4. MVP scope (Release 1.0)

### BR-01. Public feed and map

**Features:**

- browsing published lost and found posts without logging in;
- two post types: `Lost` and `Found`;
- cards with photo, title, type, category, approximate location, event date;
- search by title/description; filters by type, category, date, city/radius;
- `Feed / Map` toggle and opening a detailed card;
- empty states, pagination or infinite loading, phone layout;
- public link to a post, sharing.

**Acceptance:** a guest opens the app on a phone, selects Zhukovsky or the currently available area, finds a published item by category/radius, and sends the link to someone else.

### BR-02. Fast publishing

**Features:**

- choosing `Lost` / `Found`;
- title, category, description and event date;
- attaching one or more photos (limits defined by the UX/technical specification);
- approximate location via the map or manual selection;
- card preview;
- keeping the draft while going through authentication;
- email confirmation before publishing;
- editing and withdrawing a post by its author.

**Acceptance:** a new user on a smartphone creates a card with a photo and a location, confirms their email, and gets a working public link. **Product target:** publishing within 60–90 seconds of opening the form, given ready photos and an accessible mailbox; verify with UX tests, do not promise it as a guaranteed SLA.

### BR-03. User account

- login with a one-time email code or a magic link (pick one before implementation);
- minimal profile: display name, email (private), my posts;
- logout, session expiry, safe handling of repeated codes;
- viewing one's own responses;
- the right to edit only one's own posts.

**Acceptance:** after logging in, a user sees their posts and has no access to other accounts' private data.

### BR-04. Responses and communication

- a registered user can respond to someone else's active post;
- a response creates a private conversation between the author and the responder;
- text messaging; email notifications about new responses and important events;
- the author can reject a response or continue the conversation;
- an agreed handover is recorded by a separate action;
- personal contacts are not required; if the sides exchange them in the chat, the service does not publish them separately.

**Acceptance:** two accounts agree on a return without publicly posting a phone number or address. A third user cannot see the conversation.

### BR-05. Verification and completing the return

- the author of a found post can ask the presumed owner to name **distinguishing features** that are not shown publicly;
- whether the features match is judged by people, not by an algorithm;
- a handover can be agreed with only one response at a time;
- after the actual return, **both sides** confirm completion; if the other side does not respond within a timeout, the author can complete it alone and the outcome is marked as one-sided ([ADR-0002](adr/0002-two-party-return-confirmation.md));
- the card gets the outcome `Returned to owner` and stops accepting new responses;
- there are separate closing outcomes: `Item found another way`, `Post no longer relevant`;
- the history of significant actions is preserved.

**Acceptance:** even if two users respond at the same time, it is impossible to end up with two active agreed handovers for one item; a closed card does not accept new responses.

### BR-06. Rule-based (non-AI) match suggestions

- when viewing one's own post, show possible posts of the **opposite type**;
- take into account category, distance, time window and public text features;
- show the reasons for the suggestion: "same category", "nearby", "close date";
- a suggestion is not a confirmation of ownership;
- the user can ignore an unsuitable suggestion.

**Acceptance:** for a test pair `lost backpack ↔ found backpack` close in place and time the system shows the candidate; a find of another category in another city must not appear among priority suggestions.

### BR-07. Personal Kanban board

**Purpose of the board:** not team tasks, but **my stories and their progress**.

Recommended UX:

| Board column | Meaning | Notes |
|---|---|---|
| Published | My open posts without an active response | The post is visible to everyone |
| Has responses | At least one open response | Derived from responses |
| Handover agreed | A handover is agreed with one response | Waiting for return confirmation; shows whose confirmation is pending |
| Completed | Posts are closed | Closing reason is visible |

**Important:** columns are a **derived view** of the post and response state, not standalone arbitrary statuses that the user can drag without performing a business operation. Moving a card, if implemented, triggers an allowed command with checks.

**Acceptance:** after a new response the card moves to the right column automatically; after a confirmed return it moves to "Completed", and the history shows who completed the process and when.

### BR-08. Moderation and trust

- the ability to report a post;
- reasons: fraud, prohibited content, spam, personal data, other;
- a report queue for moderators;
- hiding/blocking a post and recording the reason;
- rate limits on posts, responses and messages;
- a confirmed email is required for interactions;
- safe-handover guidelines, warnings against publishing documents, secrets or exact addresses.

**Acceptance:** a moderator hides a post; it disappears from public search but remains available for internal audit.

### BR-09. Demo mode for the portfolio

- a landing page with a prominent `View demo` button;
- a pre-filled story: **backpack lost → backpack found → possible match → response → verification → return**;
- an example with two competing claimants to demonstrate business invariants;
- the mode does not change real user data and is clearly labeled "Demo" ([ADR-0001](adr/0001-demo-server-sandbox.md));
- the product link opens without registration; the visitor walks through the story and then opens the technical case study.

**Acceptance:** a first-time visitor understands the product's value on their own and completes the demo route without instructions.

## 5. Core user stories

| ID | Story | Priority |
|---|---|---|
| US-01 | As a guest, I want to see found items on a map near me to check whether someone found my item | P0 |
| US-02 | As a finder, I want to quickly post a photo and an approximate location so that the owner can find me | P0 |
| US-03 | As an owner, I want to post a description of my lost item and receive responses | P0 |
| US-04 | As a post author, I want to chat privately with a responder without publicly revealing my contacts | P0 |
| US-05 | As a finder, I want to verify a hidden feature and agree on a handover with only one person | P0 |
| US-06 | As a participant, I want to record a successful return so that the story has a clear ending | P0 |
| US-07 | As a user, I want to see my stories by stage on a Kanban board | P1 |
| US-08 | As a user, I want to get explainable match suggestions | P1 |
| US-09 | As a moderator, I want to process a report and hide a violating post | P0 |
| US-10 | As a recruiter/visitor, I want to see a real user scenario without registration | P0 for the portfolio |

`P0` — required for the public MVP; `P1` — part of the target Release 1.0 but does not block an early pilot.

## 6. Lifecycles and rules

### Post (listing)

```text
DRAFT → PUBLISHED → CLOSED
                      └─ reason: RETURNED | FOUND_OTHER_WAY | NO_LONGER_RELEVANT
PUBLISHED → HIDDEN_BY_MODERATOR
CLOSED → ARCHIVED  (later / retention policy)
```

For **lost** posts the outcome `RETURNED` means "the item was found and returned to the owner"; for **found** posts it means "handed over to the identified owner". A match suggestion by itself does not close a post.

### Response

```text
NEW → DISCUSSING → HANDOVER_AGREED → COMPLETED
  ├──────────────→ REJECTED     ↑  │
  └──────────────→ WITHDRAWN    └──┘ cancel-handover (back to DISCUSSING)
```

The `HANDOVER_AGREED → COMPLETED` transition requires return confirmation from **both sides**. If the other side does not respond, after a timeout the author can complete it alone; such an outcome is marked as confirmed by one side ([ADR-0002](adr/0002-two-party-return-confirmation.md)).

**Invariants:**

1. A responder cannot respond to their own post.
2. Multiple responses are allowed, but only **one agreed handover** per post at a time.
3. Only the post author can agree a handover; completion happens after the actual handover.
4. State changes are atomic and validated on the server.
5. Repeated completion does not produce duplicate events or notifications.
6. A closed or blocked card does not accept new responses.
7. The author can close a post without any responses by giving a valid reason.
8. Hidden features / proof of ownership are never published in public cards.
9. Messages are available only to the participants of the conversation and to an authorized moderator within a defined process.

## 7. Screen map

| Screen | Main elements | Primary CTA |
|---|---|---|
| Home | Search, filters, feed/map, nearby posts | `Found an item` / `Lost an item` |
| Create post | Type, photos, category, location, date, description, preview | `Publish` |
| Item card | Photo, date, area, category, description, status, share | `Contact the author` |
| Login | One-time email code/link | `Continue` |
| My stories | Kanban, active responses, completed stories | `Open card` |
| Conversation | Messages, post details, handover agreement | `Agree handover` |
| Outcome | Return confirmation, story summary | `Confirm return` |
| Moderation | Report queue, reasons and actions | `Hide / dismiss report` |
| Demo | Step-by-step return story | `Walk through the story` |

**Mobile navigation (hypothesis):** bottom bar `Feed`, `Map`, `Add`, `Mine`; `Mine` combines the Kanban, messages and profile. On desktop — full navigation and an extended Kanban.

## 8. Critical end-to-end acceptance scenarios

### E2E-01. A found item is published

**Given:** the user is not logged in and opened the site on a smartphone.  
**When:** they choose "Found", attach a photo, pick an approximate point, set a category and confirm their email.  
**Then:** the post is published, opens by URL and is visible in the feed/on the map.

### E2E-02. The item is successfully returned

**Given:** there is a published lost post and an independent found post.  
**When:** one participant finds the candidate, sends a response, the sides chat, agree on a handover, and both confirm the return.  
**Then:** the post has the outcome "Returned to owner", the transition history is saved, new responses are impossible.

### E2E-03. Two claimants

**Given:** a found post has two active responses.  
**When:** the author agrees a handover with the first one.  
**Then:** agreeing with the second one is blocked by the business rule, including concurrent requests from two sessions.

### E2E-04. Personal data protection

**Given:** a user views a public card and the API network responses.  
**Then:** email, hidden features, original EXIF coordinates, the author's exact address and private messages are not visible.

### E2E-05. Moderation

**Given:** a post violates the rules.  
**When:** it is reported and a moderator confirms the report.  
**Then:** the card is unavailable in public results and the decision is logged.

### E2E-06. Demo for an employer

**Given:** a new visitor opened the link from a CV.  
**When:** they click "View demo".  
**Then:** without logging in they see the story "lost → found → returned", can explore the cards and the business mechanics, and real user data is not changed.

## 9. Product metrics

Track with a minimal amount of anonymized analytics and a clear notice to users:

| Metric | Why it matters |
|---|---|
| Time to create a post | Tests the "fast and easy" hypothesis |
| Share of completed post forms | Shows where we lose users |
| Share of posts with responses | Shows the usefulness of the audience and geo search |
| Share of stories with a confirmed return | The main indicator of product value |
| Median time from publishing to first response | Speed of connecting the two sides |
| Share of wrong candidates among match suggestions | Quality of matching rules |
| Reports per 100 posts | Trust and safety |
| Share of successful demo runs | Portfolio and UX quality |

**Do not set fictional KPIs for a non-existent user base.** Before the pilot the targets are hypotheses; actual values appear only after observation.

## 10. Constraints, risks and anti-fraud

- **Cold start:** begin with one geography; make it easy to spread cards via links and in local communities.
- **False claimants:** verify non-obvious features and advise against handing over valuables before ownership is confirmed; the service does not guarantee ownership.
- **Dangerous meetings:** guidelines on public handover places and caution with addresses/contacts.
- **Spam/fakes:** action limits, reports, manual moderation and blocking.
- **Personal data:** data minimization, hidden contacts, retention and deletion policy, review of Russian legal requirements before public launch.
- **Photo abuse:** metadata stripping and moderation; content rights belong to the author / per the service license.
- **Weak map:** the map is not the only way to find posts; the feed and filters are available.
- **Unrealistic promises:** the app helps establish contact but does not guarantee a return and does not verify ownership automatically.

## 11. Out of MVP scope

- Native Android/iOS apps: responsive site/PWA only.
- Organizations, teams, collective roles and multi-accounts.
- Payments, rewards, paid promotion and monetization.
- Automatic AI/ML photo search.
- Automatic ownership determination based on a match.
- Telegram bot, SMS, complex external integrations.
- Video calls, movement tracking and public address disclosure.
- Ratings, gamification and social network features.
- A general marketplace or selling items.

## 12. Release plan

### Pilot / vertical slice — can be shown to people

Public feed, publishing with photo and approximate location, email login, card, private response, conversation, closing with an outcome, minimal moderation.

**Definition of Done:** E2E-01, E2E-02, E2E-04 and E2E-05 pass; the app is available over HTTPS from a phone.

### Release 1.0 — a demo-complete product

Everything from the Pilot + PostGIS geo search, filters, match suggestions, personal Kanban, full state history, concurrency invariants, a separate demo scenario, analytics and a technical case study.

**Definition of Done:** all E2E-01…E2E-06 pass; architecture, API, setup and security are documented.

### Later, only if demand is confirmed

Organizations/lost-and-found offices; subscriptions to areas/categories; push; integrations with transport companies and city venues; advanced anti-fraud; automation of suitable matches.

## 13. How the product is presented in a CV

The public demo link must allow, within **2–3 minutes**, to:

1. See a real lost-and-found scenario.
2. View the map and two related cards.
3. Make sure the system suggests a match with an explanation of reasons.
4. Go through the response, handover confirmation and the "Returned" ending.
5. Open the engineering write-up: modular monolith architecture, PostGIS, transactions/races, tests, CI/CD, data protection.

**Emotional promise:** "You didn't just post an ad. You helped an important thing get back to its person."

**Engineering promise:** "The author took the product from an identified need and UX to reliable business rules and a deployed service."

## 14. Decisions to be made

- Primary login method: email OTP or magic link — **open**.
- Mandatory photos and photo limits by category — **open**.
- ~~Public location precision and address hiding rules.~~ Decided: [ADR-0003](adr/0003-public-location-coarsening.md).
- ~~Closing on confirmation by one or both sides.~~ Decided: both sides, with a timeout for one-sided completion ([ADR-0002](adr/0002-two-party-return-confirmation.md)).
- Categories and minimal restrictions for posts about documents, bank cards, keys, dangerous items — sensitive categories decided in [ADR-0003](adr/0003-public-location-coarsening.md); content restrictions are **open**.
- Deletion/archiving policy, retention periods and account recovery — **open**.
- Moderation procedure and appeals — **open**.

Decisions from this list must be made **before implementing the affected functionality**, but they do not block the architectural skeleton and the design of the main user scenario.

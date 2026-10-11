# 0004. The pre-login draft is stored on the client

- **Status:** accepted
- **Date:** 2026-10-11
- **Affects:** frontend, `media` module; AR-43; BR-02, product principle 3

## Context

A draft is created before login; the account is confirmed right before publishing. Accepting anonymous photo uploads on the server makes storage easy to abuse.

## Decision

- The draft, including photos (Blobs), is stored in the browser's IndexedDB.
- After email confirmation the frontend uploads the photos and creates the listing.
- The server does not accept uploads from unauthenticated users.
- Resubmission does not create a duplicate: the frontend generates a `draftId`, which the server accepts as the idempotency key for creation.

## Alternatives

- **An anonymous draft on the server.** Handier when switching devices, but opens anonymous uploads and needs cleanup.

## Consequences

- The draft does not move between devices. Acceptable for the MVP.
- If IndexedDB is unavailable (private mode), the draft lives in tab memory and the user is warned.
- Photos are downscaled on the client before being saved. The server strips EXIF anyway (AR-37).

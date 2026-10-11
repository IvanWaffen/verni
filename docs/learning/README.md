# Learning notes

One note per PR with code changes. The goal is to learn Java and Spring from real project code, using Laravel as the reference point.

- File name: `NNNN-<slug>.md`, numbered sequentially (not by issue number).
- Keep it short: a note is read in 5–10 minutes.
- Each new concept pair also goes into the cumulative [Laravel → Spring glossary](laravel-to-spring.md).

## Template

```markdown
# NNNN. <Title>

- **PR:** #<n> · **Issue:** #<n>
- **Topics:** <comma-separated tags, e.g. jpa, transactions, testing>

## What was done

Two or three sentences.

## New concepts

### <Concept>

**In Laravel:** the closest analogue and how it differs.
**In Java/Spring:** explanation with a short snippet.
**In our code:** `path/to/File.java:42`

## Pitfalls coming from PHP

- ...

## Read more

- <official docs link>

## Exercise (optional)

A small change to make by hand to reinforce the topic.
```

## Index

| # | Note | Topics |
|---|---|---|
| 0001 | [Project bootstrap](0001-project-bootstrap.md) | maven, spring-boot, configuration, modulith, security, flyway, testcontainers |

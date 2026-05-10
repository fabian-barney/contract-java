# ADR-0001: Install ai-rules in git mode

## Status

Accepted.

## Context

The project needs shared AI guidance that is versioned with the repository and
available to all contributors and review agents.

## Decision

Install ai-rules as a tracked git subtree under `ai/AI-RULES` and keep
project-owned extensions under `ai/PROJECT`.

## Consequences

The team can update the baseline through the ai-rules subtree update workflow,
while project-specific guidance remains outside the vendored subtree.

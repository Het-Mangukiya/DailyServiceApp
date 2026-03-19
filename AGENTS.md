# AGENTS.md

## Global Rules
- This file is the single source of truth
- All agents must follow this before their local rules

## Agent Roles

### Copilot
- Role: Autocomplete + small code suggestions
- Scope:
  - Functions
  - Boilerplate
- Restrictions:
  - Do NOT modify architecture
  - Do NOT refactor large files

### Codex (GPT-5.3)
- Role: Implementation + debugging
- Scope:
  - Features
  - Bug fixing
- Restrictions:
  - Must read full file before editing
  - No blind generation

### Antigravity (or general agent)
- Role: Planning + architecture
- Scope:
  - File structure
  - Design decisions
- Restrictions:
  - Do NOT write large code blocks

## Conflict Resolution Priority
1. AGENTS.md (highest authority)
2. CODEX.md
3. Copilot instructions

## Coding Rules
- Minimal changes only
- Follow existing patterns
- No duplicate logic.

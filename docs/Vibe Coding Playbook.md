# Vibe Coding Rules

## Concepts

1. Design: You must be able to visualize the system structure
2. Constraints: You must define the Non-Functional Requirements (Performance, Security, Scalability)?
3. Orchestration: Guide the intelligent agent to build exactly what you envisioned, you do not need to micromanage every keystroke

## Memory

## Key Steps

This means:

- Blueprint: Create a Blueprint that captures the vision, philosophy, and technical constraints for the project before any code is created.
- Non-Goals, Invariants: These define explicit boundaries that tell the Agent what it cannot do
- Contracts: Freeze the contracts (database schemas, API specs) so the AI implements against fixed targets.
- Fitness Functions: Encode the constraints as fitness functions, these are automated tests that enforce architectural rules on every commit.
- Validation: Integrate validation into CI/CD so violations are caught automatically.

## Techniques

- Vibe Coding required the foundations of test driven development (TDD) And spec driven development (SDD)
- Use Architecture-as-Code � the scaffolding that keeps every feature aligned with the project vision, enforced automatically through fitness functions and CI/CD integration.

## System

The three pillars of professional development:

1) Spec driven development
2) Test driven development
3) AI Agents that use 1 & 2

## Environment

- VS Code is running in Windows 11 supported by Windows Sub-system for Linux (Ubuntu)

## Tools

1. Vision: Copilot can �see� the file structure, read the terminal errors, and diff the git history.
2. Action: Copilot can create files, delete code, run terminal commands, and browse the documentation
3. Reasoning: Copilot will be used to plan before writing a single line of code.

## Process

- Prefer well-documented, mainstream stacks.
- Favour technologies with rich, up-to-date documentation, examples, and community support. This gives Copilot something reliable to lean on and drastically reduces hallucinations around obscure APIs.
- Design and constraints are defined before implementation begins
- Select a reasoning-oriented model to define the architecture, data model, interfaces, edge cases, and non-functional requirements  such as performance, security, scalability, observability.
- Adopt a �Spec First� mentality. Capture the outcome of that reasoning as written and numbered spec files (e.g. 01-database-spec.md).
- Work in small, explicit tasks.
- Break the spec into atomic, named implementation tasks that can be completed in one to three prompts
- Sculp the solution, iterate the specs as the code evolves.
- Explore trade-offs, alternatives, and failure modes using conversation with Copilot.
- Switch between models depending on the size and complexity of the task. Get a second opinion.
- Code with one model (e.g. GPT), test with another (e.g. Claude)
- Always confirm what has been done or not been done. Interrogate generated code and tests. Never accept code you don�t understand.
- Ask the AI to explain unfamiliar mocks, patterns, or APIs until you can confidently own the result.
- Choose the right brain for the job. Use fast, cheap models for small, mechanical changes; use large, reasoning-heavy models for design, refactors, and complex debugging. Don�t waste a �big brain� on adding imports.
- Switch perspectives when stuck. When a conversation degrades (the AI loops or digs deeper into a bad approach), start a new chat, change the prompt angle, or even switch models to get a fresh viewpoint.
- Commit and checkpoint aggressively. Commit on green tests, not on �finished features,� and use editor checkpoints where available.
- Use explicit rules and documentation. Capture project rules, style guides, and architecture in documents.
- Index the right sources of truth. Connect Copilot to the documentation, schemas, and services it must respect (via MCP or other integration), instead of letting it guess from training data.
- Bake in security and quality checks. Use specialized �Security Auditor� and �Refactor� commands to review features before merge, and explicitly ask for checks against common vulnerability classes.
- Start a new conversation after each commit and checkpoint

## Context

- Specify exactly what you need in the context, no more, no less.
- Understand how Copilot works and what is always placed in context.
- Monitor the size of the context window, compact the conversation or start a new one when needed.

## Model Selection

- Prefer to use Auto mode. It is the most cost effective solution.
- Switch to a different model if Auto mode does not solve the problem.
- Use small Models (Flash / Haiku / Mini) for fixing syntax errors or Writing simple boilerplate.
- Use Large Models (Claude Sonnet / GPT/ Gemini Pro) for complex planning, architecture and refactoring

## Map

- Create a dedicated docs/ folder in every project containing three specific files:

1. API.md: A rough sketch of the API endpoints and data shapes.
2. ARCHITECTURE.md: A high-level explanation of how the system fits together (e.g., �Frontend talks to Backend, Backend talks to Core Services�).
3. IMPLEMENTATION_STATUS.md: It is a running log of what we have built, what is currently broken, and what is next.

The Workflow:

At the end of every significant coding session review completed. Update IMPLEMENTATION_STATUS.md to check off completed tasks, add any new technical debt we discovered to the Known Issues section, and list the next logical steps.

This creates a Self-Healing Context. When I come back to the project tomorrow (or when I hand it off to another developer), I don't have to remember where I left off. I just tell the AI:  

## SDD - Example

Consider other relevant aspects like:

- Non-functional requirements
- Phased Rollout Plan: Starting with a core MVP and planning future feature releases.
- Technology Stack & Tools: Recommended languages, frameworks, and services.
- Deployment strategy and estimated Cloud Costs.
- Risks & Mitigations: Identifying potential problems and how to handle them.
- Iterate on this plan until you have a solid blueprint. Discuss trade-offs (e.g., Should we use a Sliding Window or Token Bucket algorithm?).
- Finally ask it to generated a spec.md file in Markdown format that will serve as the contract for the AI agent implementing this idea.

## Specification Levels

- Level 1 � Master Architecture (Frozen), system boundaries, technology choices, security posture. Rarely changes.
- Level 2 � Domain Specs (Source of Truth), schemas, API contracts, state machines. Defines reality.
- Level 3 � Feature Specs (Work Units) Broken into Epics and Stories.

## Acceptance Tests

1. Given [precondition], when [action], then [result]
2. Given [precondition], when [action], then [result]
3. [Edge case test] 

## Dependencies

- Requires: [Story IDs that must complete first]
- Blocks: [Story IDs that depend on this]

## Governance Automation

AaC automates architectural governance that was previously manual:

### ADR Compliance Checking

- Detect architectural changes (new services, major refactors)
- Require ADR for changes to API, services, or workers

### Validate ADR template structure Specification Drift Detection

- Compare SQLAlchemy models to schema.sql
- Compare resolvers to api.graphql
- Compare webhook handlers to webhooks.openapi.yaml

### Alert on drift, block on critical violations C4 Model Drift Detection

- Generate expected C4 model from codebase
- Compare to stored diagrams
- Report differences, ensure documentation stays accurate

## Validation

- Identify deviations, missed edge cases, or violations.
- Verify TDD was strictly followed.
- Verify no scope creep beyond non-goals.
- Verify that the work is fully completed.
- Verify that no business logic is implemented in the frontend.

## Hierarchy of Authority

The full system establishes a clear hierarchy:

1. Blueprint defines vision (Philosophy, design principles, differentiation)
2. Master Plan defines architecture (Tech stack, C4 models, ADRs, NFRs)
3. Execution Playbook defines process (Gates, phases, AI protocol)
4. Non-Goals define boundaries (Explicit exclusions, forbidden features)
5. Domain Specs define contracts (Glossary, invariants, schema, API, states)
6. Fitness Functions enforce contracts (Automated validation)
7. CI/CD Pipeline enforces continuously (Pre-commit, PR checks, deploy gates)
8. Stories define work units (Atomic, testable, AI-executable)
9. AI implements within constraints (Mechanical coding)
10. AI validates as auditor (Reviews, security checks, alignment verification)

## Checklist

Created for each project:

### Genesis Layer (Design)

- [ ] Blueprint completed (Philosophy ? Design ? Market ? Architecture)
- [ ] Master Plan extracted and expanded
- [ ] Execution Playbook defined with phases and gates
- [ ] Non-Goals documented (40+ explicit exclusions recommended)

### Governance Layer (Constraints)

- [ ] Glossary with all domain terms
- [ ] Invariants with all business rules
- [ ] ADRs for all significant decisions

### Technical Layer (Contracts)

- [ ] Database schema frozen (schema.sql)
- [ ] API contracts frozen (GraphQL/OpenAPI)
- [ ] State machines documented as tables
- [ ] .cursorrules pointing to specs

### Automation Layer (Fitness Functions)

- [ ] All invariants encoded as fitness functions
- [ ] Layer boundary tests implemented
- [ ] Security boundary tests implemented
- [ ] Spec compliance tests implemented
- [ ] C4 model validation implemented
- [ ] Pre-commit hooks configured (<5 seconds)
- [ ] CI/CD pipeline with architecture validation
- [ ] Deployment gates on BLOCKING violations

### Per Epic

- [ ] Epic broken into atomic Stories
- [ ] Each Story has clear acceptance tests
- [ ] Dependencies mapped between Stories
- [ ] Relevant specs identified for each Story

### Per Story (Golden Trinity + AaC)

- [ ] Story spec complete with all sections (SDD)
- [ ] Tests written before implementation (TDD)
- [ ] Implementation generated under constraints (Vibe Coding)
- [ ] Refactor pass completed
- [ ] Security audit completed (if applicable)
- [ ] Paranoid review completed
- [ ] Pre-commit hooks pass
- [ ] Fitness functions pass
- [ ] CI pipeline passes (architecture validation)
- [ ] Documentation updated

## Objectives

- Violations are caught automatically, on every commit
- Specifications provide unambiguous answers to implementation questions
- Documentation stays synchronized with code automatically
- Architectural decisions are traceable and enforceable
- Structure: Test files must mirror the source directory structure
- Documentation: After a test passes, you must update the docs/IMPLEMENTATION_STATUS.md file to reflect the progress.
- Edge Cases: Tests must explicitly cover boundary conditions (e.g., negative inputs, 0, max memory limits).
- Write the content of this rule in valid Markdown format

## 8. Workflow Checklist

Before writing any implementation code, verify:

- [ ] Test file exists in correct location
- [ ] Test function is written and comprehensive
- [ ] Test is run and **fails** (RED phase confirmed)
- [ ] Error message is clear and expected
- [ ] Only then: Write minimum implementation code
- [ ] Run test again and confirm it **passes** (GREEN phase)
- [ ] Refactor if needed, ensuring test still passes
- [ ] Update docs/IMPLEMENTATION_STATUS.md
- [ ] Run full test suite: mvn test
- [ ] Check coverage: mvn jacoco:report

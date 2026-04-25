# Vibe Coding Rules

## Concepts 
1. Design: You must be able to visualize the system structure 
2. Constraints: You must define the Non-Functional Requirements (Performance, Security, Scalability)? 
3. Orchestration: Guide the intelligent agent to build exactly what you envisioned, you do not need to micromanage every keystroke 


## Memory

## Key Steps
This means: 
• Blueprint: Create a Blueprint that captures the vision, philosophy, and technical constraints for the project before any code is created.
• Non-Goals, Invariants: These define explicit boundaries that tell the Agent what it cannot do
• Contracts: Freeze the contracts (database schemas, API specs) so the AI implements against fixed targets.
• Fitness Functions: Encode the constraints as fitness functions, these are automated tests that enforce architectural rules on every commit.
• Validation: Integrate validation into CI/CD so violations are caught automatically. 


## Techniques
• Vibe Coding required the foundations of test driven development (TDD) And spec driven development (SDD) 
• Use Architecture-as-Code — the scaffolding that keeps every feature aligned with the project vision, enforced automatically through fitness functions and CI/CD integration. 


## System
The three pillars of professional development:
1) Spec driven development
2) Test driven development
3) AI Agents that use 1 & 2
 
 
## Environment
- VS Code is running in Windows 11 supported by Windows Sub-system for Linux (Ubuntu)

## Tools
1. Vision: Copilot can “see” the file structure, read the terminal errors, and diff the git history. 
2. Action: Copilot can create files, delete code, run terminal commands, and browse the documentation. 
3. Reasoning: Copilot will be used to plan before writing a single line of code. 

## Process

- Prefer well-documented, mainstream stacks. 
- Favour technologies with rich, up-to-date documentation, examples, and community support. This gives Copilot something reliable to lean on and drastically reduces hallucinations around obscure APIs. 
- Design and constraints are defined before implementation begins
- Select a reasoning-oriented model to define the architecture, data model, interfaces, edge cases, and non-functional requirements  such as performance, security, scalability, observability. 
- Adopt a “Spec First” mentality. Capture the outcome of that reasoning as written and numbered spec files (e.g. 01-database-spec.md). 
- Work in small, explicit tasks. 
- Break the spec into atomic, named implementation tasks that can be completed in one to three prompts
- Sculp the solution, iterate the specs as the code evolves. 
- Explore trade-offs, alternatives, and failure modes using conversation with Copilot.
- Switch between models depending on the size and complexity of the task. Get a second opinion.
- Code with one model (e.g. GPT), test with another (e.g. Claude)
- Always confirm what has been done or not been done. Interrogate generated code and tests. Never accept code you don’t understand. 
- Ask the AI to explain unfamiliar mocks, patterns, or APIs until you can confidently own the result. 
- Choose the right brain for the job. Use fast, cheap models for small, mechanical changes; use large, reasoning-heavy models for design, refactors, and complex debugging. Don’t waste a “big brain” on adding imports. 
- Switch perspectives when stuck. When a conversation degrades (the AI loops or digs deeper into a bad approach), start a new chat, change the prompt angle, or even switch models to get a fresh viewpoint. 
- Commit and checkpoint aggressively. Commit on green tests, not on “finished features,” and use editor checkpoints where available. 
- Use explicit rules and documentation. Capture project rules, style guides, and architecture in documents.
- Index the right sources of truth. Connect Copilot to the documentation, schemas, and services it must respect (via MCP or other integration), instead of letting it guess from training data. 
- Bake in security and quality checks. Use specialized “Security Auditor” and “Refactor” commands to review features before merge, and explicitly ask for checks against common vulnerability classes. 
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
2. ARCHITECTURE.md: A high-level explanation of how the system fits together (e.g., “Frontend talks to Backend, Backend talks to Core Services”). 
3. IMPLEMENTATION_STATUS.md: It is a running log of what we have built, what is currently broken, and what is next. 

The Workflow: 
At the end of every significant coding session, I give the AI this command: 
 
> “Review the work we just did. Update IMPLEMENTATION_STATUS.md to check off completed tasks, add any new technical debt we discovered to the ‘Known Issues’ section, and list the next logical steps.” 
This creates a Self-Healing Context. When I come back to the project tomorrow (or when I hand it off to another developer), I don’t have to remember where I left off. I just tell the AI:  
 

## SDD - Example
1. Open your “External Brain” (Gemini/ChatGPT). 
2. Prompt it with your raw, messy idea: “Following a spec-driven development approach, I want to build a rate-limiting system for my API using Redis.” 
3. Ask for the Design-First Approach (Tip #11): “Define the Data Model, the API Interface, and the edge cases. Don’t write code yet. Just architect it.” 
4. Also ask it to consider other relevant aspects like: 
• Non-functional requirements 
• Phased Rollout Plan: Starting with a core MVP and planning future feature releases. 
• Technology Stack & Tools: Recommended languages, frameworks, and services. 
• Deployment strategy and estimated Cloud Costs. 
• Risks & Mitigations: Identifying potential problems and how to handle them. 
5. Iterate on this plan until you have a solid blueprint. Discuss trade-offs (e.g., “Should we use a Sliding Window or Token Bucket algorithm?”). 
6. Finally ask it to generated a spec.md file in Markdown format that will serve as the contract for the AI agent implementing this idea. 

 
## Specification Levels 
- Level 1 — Master Architecture (Frozen), system boundaries, technology choices, security posture. Rarely changes. 
- Level 2 — Domain Specs (Source of Truth), schemas, API contracts, state machines. Defines reality. 
- Level 3 — Feature Specs (Work Units) Broken into Epics and Stories. 
 
# Story: [ID] - [Title] 
 
**Epic:** [Parent Epic Name] 
**Priority:** [High/Medium/Low] 
**Estimated Prompts:** [1-3] 
 

## Acceptance Tests 
1. Given [precondition], when [action], then [result] 
2. Given [precondition], when [action], then [result] 
3. [Edge case test] 
 
## Dependencies 
- Requires: [Story IDs that must complete first] 
- Blocks: [Story IDs that depend on this] 
 
 
## Governance Automation 
AaC automates architectural governance that was previously manual: 
ADR Compliance Checking: 
- Detect architectural changes (new services, major refactors) 
- Require ADR for changes to API, services, or workers 
- Validate ADR template structure Specification Drift Detection: 
- Compare SQLAlchemy models to schema.sql 
- Compare resolvers to api.graphql 
- Compare webhook handlers to webhooks.openapi.yaml 
- Alert on drift, block on critical violations C4 Model Drift Detection: 
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
Genesis Layer (Design) 
• [ ] Blueprint completed (Philosophy ? Design ? Market ? Architecture) 
• [ ] Master Plan extracted and expanded 
• [ ] Execution Playbook defined with phases and gates 
• [ ] Non-Goals documented (40+ explicit exclusions recommended) 
Governance Layer (Constraints) 
• [ ] Glossary with all domain terms 
• [ ] Invariants with all business rules 
• [ ] ADRs for all significant decisions 
Technical Layer (Contracts) 
• [ ] Database schema frozen (schema.sql) 
• [ ] API contracts frozen (GraphQL/OpenAPI) 
• [ ] State machines documented as tables 
• [ ] .cursorrules pointing to specs 
Automation Layer (Fitness Functions) 
• [ ] All invariants encoded as fitness functions 
• [ ] Layer boundary tests implemented 
• [ ] Security boundary tests implemented 
• [ ] Spec compliance tests implemented 
• [ ] C4 model validation implemented 
• [ ] Pre-commit hooks configured (<5 seconds) 
• [ ] CI/CD pipeline with architecture validation 
• [ ] Deployment gates on BLOCKING violations 
Per Epic 
• [ ] Epic broken into atomic Stories 
• [ ] Each Story has clear acceptance tests 
• [ ] Dependencies mapped between Stories 
• [ ] Relevant specs identified for each Story 
Per Story (Golden Trinity + AaC) 
• [ ] Story spec complete with all sections (SDD) 
• [ ] Tests written before implementation (TDD) 
• [ ] Implementation generated under constraints (Vibe Coding) 
• [ ] Refactor pass completed 
• [ ] Security audit completed (if applicable) 
• [ ] Paranoid review completed 
• [ ] Pre-commit hooks pass 
• [ ] Fitness functions pass 
• [ ] CI pipeline passes (architecture validation) 
• [ ] Documentation updated 
 
## Objectives:
- Violations are caught automatically, on every commit 
- Specifications provide unambiguous answers to implementation questions 
- Documentation stays synchronized with code automatically 
- Architectural decisions are traceable and enforceable 
 
o Use unittest.mock for isolating dependencies. 
o Use conftest.py for shared fixtures. 
4. Structure: Test files must mirror the source directory structure (e.g., src/calculators/fib.py -> tests/calculators/test_fib.py). 
5. Documentation: After a test passes, you must update the docs/IMPLEMENTATION_STATUS.md file to reflect the progress. 
6. Edge Cases: Tests must explicitly cover boundary conditions (e.g., negative inputs, 0, max memory limits). 
Write the content of this rule in valid Markdown format suitable for Cursor.” The output will be something like this: 
 
--- 
alwaysApply: true 
--- 
# Strict Test-Driven Development (TDD) Workflow 
 
## Overview 
This rule enforces a **strict Test-Driven Development (TDD)** workflow for every single code generation task in the `py_mega_calc` project. No exceptions. 
 
## 1. Enforcement Rule 
**CRITICAL:** You (the AI) are **FORBIDDEN** from writing implementation code (logic) until a failing test case has been written and confirmed. 
 
### What This Means: 
- ? **DO NOT** write `def calculate_by_index(...)` until there's a test that calls it and fails 
- ? **DO NOT** write algorithm logic until tests define the expected behavior 
- ? **DO** write the test first, run it, confirm it fails for the right reason 
- ? **DO** then write the minimum implementation to make it pass  
## 2. The Red-Green-Refactor Cycle 
 
You must strictly follow this cycle for **every feature**: 
 
### RED Phase: Write Failing Test 
1. Write a comprehensive unit test using `pytest` that tests the desired functionality 
2. The test **MUST fail** because the feature doesn't exist yet 
3. Run the test: `pytest tests/path/to/test_file.py -v` 
4. Confirm the test fails with a clear, expected error (e.g., `AttributeError`, `NotImplementedError`) 
5. **Only proceed to GREEN phase after confirming the test fails**  
### GREEN Phase: Make Test Pass 
1. Write the **minimum amount of code** required to make the test pass 
2. No premature optimization 
3. No extra features 
4. Just enough to satisfy the test 
5. Run the test again: `pytest tests/path/to/test_file.py -v` 
6. Confirm the test passes 
 
### REFACTOR Phase: Optimize 
1. Once the test passes, optimize the code: 
- Clean up code style (PEP 8) 
- Improve performance 
- Apply DRY principles 
- Add type hints 
- Improve docstrings 
2. **After each refactoring step, run the test again** 
3. Ensure the test still passes 
4. If the test breaks, revert and try a different refactoring approach  
## 3. Tooling Requirements 
 
### Testing Framework 
- **Primary:** `pytest` for all tests 
- Run tests with: `pytest tests/ -v` or `pytest tests/path/to/test_file.py::test_function_name -v` 
- Use pytest fixtures for setup/teardown 
 
### Mocking 
- Use `unittest.mock` (or `pytest-mock`) for isolating dependencies 
- Mock external resources (file I/O, system calls, etc.) - Mock time-dependent operations for consistent testing  
### Shared Fixtures 
- Use `tests/conftest.py` for shared fixtures 
- Define reusable test data and setup logic there 
- Import fixtures in test files as needed 
 
## 4. Test File Structure 
 
Test files **MUST** mirror the source directory structure: 
 ``` src/calculators/fibonacci.py  ?  tests/test_fibonacci.py src/calculators/factorial.py  ?  tests/test_factorial.py src/calculators/primes.py     ?  tests/test_primes.py src/core/resource_manager.py  ?  tests/test_resources.py src/core/estimator.py         ?  tests/test_estimator.py ``` 
 
### Naming Conventions 
- Test files: `test_<module_name>.py` 
- Test functions: `test_<function_name>_<scenario>()` 
- Test classes: `Test<ClassName>` 
 
### Example Structure 
```python 
# tests/test_fibonacci.py import pytest from src.calculators.fibonacci import FibonacciCalculator 
 class TestFibonacciCalculator: 

def test_calculate_by_index_zero(self): 
        """Test that index 0 returns the first Fibonacci number.""" 
        calc = FibonacciCalculator()         result = calc.calculate_by_index(0)         assert result == 0 
         def test_calculate_by_index_large(self): 
        """Test calculation for large indices.""" 
        # ... test implementation 
``` 
 
## 5. Documentation Updates 
 
**After a test passes**, you **MUST** update `docs/IMPLEMENTATION_STATUS.md`: 
 
1. Mark the corresponding task as complete: `[x]` 
2. Mark tests as passing: `[x]` 
3. Add notes about implementation details or decisions 
4. Update the commit message to reference the status update  
### Example Update 
```markdown 
| **Fib**   | Fast Doubling Algo | [x] | [x] | Implemented with O(log n) complexity | 
``` 
 
## 6. Edge Cases and Boundary Conditions 
 
Tests **MUST** explicitly cover: 
 
### Input Validation 
- Negative numbers ? Should raise `InputError` 
- Zero ? Define expected behavior 
- Non-integers ? Should raise `InputError` 
- Very large numbers ? Test resource limits 
 
### Resource Limits 
- Memory limit (24GB) ? Test `ResourceExhaustedError` 
- Time limit (5 minutes) ? Test `TimeoutError` 
- Large digit outputs (>10,000 digits) ? Test estimation logic  
### Boundary Conditions 
- Minimum valid input (e.g., index 0, 1 digit) 
- Maximum practical input (within resource constraints) 
- Edge cases specific to each calculator: 
- **Fibonacci:** F(0), F(1), large n 
- **Factorial:** 0!, 1!, very large n 
- **Primes:** First prime, large primes, prime gaps 
 
### Example Edge Case Tests 
```python def test_calculate_by_index_negative_raises_error(self):     """Test that negative index raises InputError."""     calc = FibonacciCalculator()     with pytest.raises(InputError):         calc.calculate_by_index(-1) 
 def test_calculate_exceeds_memory_limit(self): 
    """Test that exceeding 24GB memory raises ResourceExhaustedError.""" 
    # ... test implementation 
``` 
 
## 7. Test Quality Standards 
 
### Coverage Requirements 
- **100% code coverage** for logic (as per spec) 
- Use `pytest-cov` to verify: `pytest --cov=src --cov-report=term-missing` 
 
### Test Assertions 
- Use descriptive assertion messages 
- Test one thing per test function 
- Use `pytest.raises()` for exception testing 
- Use `pytest.approx()` for floating-point comparisons (if needed, though we use integers) 
 
### Gold Standard Values 
- Include verified values from OEIS (Online Encyclopedia of Integer Sequences) 
- Test known large number calculations 
- Verify against authoritative sources 
 
## 8. Workflow Checklist 
 
Before writing any implementation code, verify: 
 
- [ ] Test file exists in correct location 
- [ ] Test function is written and comprehensive 
- [ ] Test is run and **fails** (RED phase confirmed) 
- [ ] Error message is clear and expected 
- [ ] Only then: Write minimum implementation code 
- [ ] Run test again and confirm it **passes** (GREEN phase) 
- [ ] Refactor if needed, ensuring test still passes 
- [ ] Update `docs/IMPLEMENTATION_STATUS.md` 
- [ ] Run full test suite: `pytest tests/ -v` 
- [ ] Check coverage: `pytest --cov=src --cov-report=term-missing`  
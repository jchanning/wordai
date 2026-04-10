# React Frontend Upgrade Plan for WordAI

This document outlines the strategy to migrate the WordAI frontend from its current imperative, vanilla JavaScript structure to a modern, declarative React architecture.

## 🎯 Goal
To improve maintainability, scalability, and developer experience by adopting React's component-based model, moving away from direct DOM manipulation and global mutable state.

## 🗺️ Migration Phases

### Phase 1: State Centralization & Abstraction (The Foundation)
**Objective:** Establish a single source of truth for the entire application state.

1.  **State Management Overhaul (Targeting `state.js`):**
    *   **Action:** Implement a dedicated state management library (e.g., Zustand or Redux Toolkit).
    *   **Result:** Break the monolithic state into domain-specific slices (e.g., `gameStateSlice`, `dictionarySlice`). State changes must be initiated via explicit **Actions**.
2.  **API Service Layer (Targeting `api.js`):**
    *   **Action:** Refactor API wrappers into a dedicated service module.
    *   **Result:** These services must be updated to **dispatch actions** to the state store upon successful API interaction, rather than just returning raw data.

### Phase 2: Componentization (The View Layer)
**Objective:** Replace all direct DOM manipulation with React's declarative rendering model.

1.  **Component Breakdown:** Decompose the UI into small, reusable, and isolated components.
    *   **Key Components:** `<GameContainer />` (Orchestrator), `<StatusToast />`, `<LetterInputGrid />`, `<HistoryDisplay />`, `<AnalyticsDashboard />`.
2.  **Input Handling:**
    *   **Action:** All inputs must become **controlled components**. Their `value` must be derived from the React state, and changes must be handled via `onChange` props that trigger state updates.
3.  **Event Handling:**
    *   **Action:** Replace global event listeners with React's synthetic event system (`onClick`, `onSubmit`).

### Phase 3: Integration & Cleanup (The Polish)
**Objective:** Systematically replace old code paths with the new React structure.

1.  **Refactor `game.js`:** This file becomes the main application wrapper, initializing the state store and rendering the top-level component.
2.  **Refactor `api.js`:** Update all calls to use **custom hooks** (e.g., `useMakeGuess(...)`). These hooks manage the API call lifecycle and dispatch the resulting state changes.
3.  **Cleanup:** Remove all global event listeners, global functions exposed on `window`, and direct DOM manipulation calls from the core logic files.

## 🔄 Data Flow Comparison

| Aspect | Current Flow (Imperative) | Target Flow (Declarative/React) |
| :--- | :--- | :--- |
| **State Change** | Direct mutation of global objects (`state.x = y`). | Dispatching an action to the central store (`dispatch(SET_X(y))`). |
| **UI Update** | Manually finding and updating DOM elements (`document.getElementById('id').innerHTML = 'new'`). | The component automatically re-renders when the state changes, receiving the new data via props. |
| **Logic Flow** | Functions call each other sequentially, modifying shared global state along the way. | Components consume state via hooks, and the state store dictates what the UI *should* look like at any given moment. |

## 📂 Key Files Impacted
*   `state.js`: **Rewrite** using a state management library.
*   `game.js`: **Refactor** to be the root component wrapper.
*   `ui.js`: **Deconstruct** into multiple, smaller, pure presentation components.
*   `api.js`: **Wrap** functions into custom hooks.
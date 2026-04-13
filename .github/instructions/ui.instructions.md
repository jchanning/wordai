# WordAI User Interface Conventions

# These rules apply to all UI code in the `client` package. Follow them exactly — do not deviate without a stated reason.
## Framework

## Guidelines
- Always use responsive web design principles — the UI should be usable on both desktop and mobile.
- Start with a mobile-first approach: design for the smallest screen and scale up.
- User fluid layouts with CSS Flexbox or Grid to adapt to different screen sizes.
- Instead of relying only on viewport breakpoints, style components based on the size of their container.
- Images and video and should scale within their containers using `max-width: 100%` and `height: auto`.
- Don't design for a specific screen size — ensure the UI gracefully adapts to a range of widths (e.g. 320px to 1920px).
- User the viewport meta tag in HTML to control layout on mobile browsers:
```html<meta name="viewport" content="width=device-width, initial-scale=1.0">
```
- Modification to the mobile UI should not impact the desktop UI and vice versa. Use media queries and container queries to ensure styles are applied appropriately based on screen size and component size.
- The mobile UI may need to show less information or use different navigation patterns (e.g. hamburger menu) than the desktop UI, but the core functionality and visual identity should remain consistent across all screen sizes.

## Mobile-first design
- Use touch-friendly controls with sufficient spacing (at least 48x48 pixels).
- Buttons and interactive elements must be large enough for fingers, not just mouse pointers.

# Responsive Web Application Best Practices  
*A practical guide for building applications that work seamlessly across mobile, desktop, and all screen resolutions.*

## Overview
This document outlines modern best practices for building responsive, accessible, and performant web applications. It is designed for engineering teams, designers, and anyone building cross‑device interfaces.

## 1. Core Principles

### Mobile‑First Architecture
- Begin with the smallest viewport.
- Add enhancements as screen real estate increases.
- Ensures clarity, performance, and predictable scaling.

### Fluid Layouts
- Use flexible units: `%`, `fr`, `auto`, `minmax()`.
- Avoid fixed pixel widths.
- Prefer CSS Grid and Flexbox for adaptive layouts.

### Container Queries
- Style components based on the size of their container.
- Enables reusable, self‑contained components.
- Reduces reliance on global viewport breakpoints.

### Fluid Typography
```css
font-size: clamp(1rem, 2vw, 1.5rem);
```

### Flexible Media
```css
img, video {
  max-width: 100%;
  height: auto;
}
```

### Content‑Driven Breakpoints
- Break when the layout *needs* to change.
- Avoid device‑specific breakpoints.

### Viewport Meta Tag
```html
<meta name="viewport" content="width=device-width, initial-scale=1">
```


## 2. UX Best Practices

### Touch‑Friendly Interactions
- Minimum target size: ~44px.
- Adequate spacing between interactive elements.

### Progressive Disclosure
- Prioritise essential content.
- Reveal secondary content on demand.

### Responsive Navigation Patterns
- **Mobile:** hamburger menus, bottom navigation, collapsible menus.
- **Desktop:** horizontal menus, mega‑menus.

### Use SVGs for Icons & Logos
- Crisp at any resolution.
- Easy to style and animate.


## 3. Performance Best Practices

### Load Only What’s Needed
- Code‑split by route or component.
- Lazy‑load images and non‑critical scripts.

### Optimise for Core Web Vitals
- **LCP:** optimise hero images and above‑the‑fold content.
- **CLS:** reserve space for images and ads.
- **INP:** reduce heavy JavaScript and long tasks.

### Image Optimisation
- Prefer modern formats (WebP, AVIF).
- Use `srcset` for responsive images.


## 4. Architectural Best Practices

### Component‑Driven Design
- Build isolated, reusable components.
- Pair with a design system for consistency.

### Use Design Tokens
Centralise:
- spacing  
- colours  
- typography  
- breakpoints  

### Test Across Real Devices
Emulators miss:
- touch latency  
- keyboard overlays  
- viewport quirks  

### Accessibility as a First‑Class Requirement
- Semantic HTML.
- ARIA only when necessary.
- Support high‑contrast and reduced‑motion modes.


## 5. CSS Techniques & Patterns

### Layout Systems
- **Flexbox:** one‑dimensional layouts.
- **Grid:** two‑dimensional layouts.

### Modern CSS Tools
- `clamp()`, `min()`, `max()`
- `aspect-ratio`
- `:has()` for parent‑aware styling
- `@container` for component‑level responsiveness

### Example: Responsive Card Component
```css
.card {
  display: grid;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
  background: var(--surface);
}

@container (min-width: 500px) {
  .card {
    grid-template-columns: 1fr 2fr;
  }
}
```



## 6. Testing & QA Checklist

- [ ] Layout scales from 320px → 4K  
- [ ] Typography remains readable at all sizes  
- [ ] Images scale without distortion  
- [ ] Navigation adapts cleanly across breakpoints  
- [ ] Touch targets meet minimum size  
- [ ] No horizontal scrolling on mobile  
- [ ] No layout shift during load  
- [ ] Works in light/dark mode  
- [ ] Works with reduced motion enabled  
- [ ] Tested on at least one real mobile device  


## 7. Authoritative References

- MDN Web Docs — Responsive Web Design  
- UXPin — Responsive Design Best Practices  
- Google Web.dev — Core Web Vitals  
- W3C — WCAG 2.2 Accessibility Standards




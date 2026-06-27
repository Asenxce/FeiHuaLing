---
name: "frontend-design-system"
description: "Enforces consistent frontend UI design rules including colors, typography, spacing, components, and icons. Invoke when creating or modifying any frontend pages, components, or styles."
---

# Frontend Design System

This skill enforces a strict design system for all frontend development. Apply these rules whenever you write or modify frontend pages, components, or styles.

## Color System

- Define CSS variables: `--color-primary`, `--color-secondary`, `--color-neutral-*`, `--color-success`, `--color-warning`, `--color-error`
- Backgrounds: white (`#ffffff`) or light gray (`#f8f9fa`, `#f3f4f6`) only
- No gradients on backgrounds or buttons
- No blue-purple gradients (`linear-gradient` with purple/violet/indigo) anywhere
- No neon colors, no rainbow palettes
- No more than 3 brand colors in any single view
- Text colors: `#111827` (primary), `#6b7280` (secondary), `#9ca3af` (tertiary)

## Typography

- Font scale: `12px` / `14px` / `16px` / `20px` / `24px` / `32px`
- CSS variables: `--text-xs`, `--text-sm`, `--text-base`, `--text-lg`, `--text-xl`, `--text-2xl`
- Body: font-weight 400, line-height 1.5
- Headings: font-weight 600, line-height 1.25
- Do not mix px, rem, em — pick one unit system
- No arbitrary font sizes outside the defined scale

## Spacing

- 4px base grid: `4 / 8 / 12 / 16 / 24 / 32 / 48 / 64px`
- CSS variables: `--space-1` through `--space-16`
- No magic numbers (13px, 7px, 23px, etc.)
- Consistent padding within component families

## Components

### Cards

- Use either border (`1px solid #e5e7eb`) OR shadow, not both
- Shadow level 1: `0 1px 3px rgba(0,0,0,0.08)`
- Shadow level 2: `0 4px 12px rgba(0,0,0,0.1)`
- Border radius: `6px` or `8px`, no `16px+`

### Buttons

- Primary: solid fill, no gradient
- Secondary: outline or ghost
- Hover: darken by 10%, not color switch
- No `rounded-full` on rectangular buttons

### Inputs

- Border: `1px solid #d1d5db`
- Border radius: `6px`
- Focus: border-color change + outline, no glow

## Icons

- Use one icon set consistently: Lucide / Heroicons / Phosphor
- Size: `16px` inline, `20px` standalone
- No emoji as functional icons

## Forbidden Patterns

- No blue-purple gradients
- No glassmorphism unless explicitly requested
- No emoji icons
- No excessive shadows on every element
- No inline styles for color, spacing, or typography
- No magic numbers — every value must reference a design token
- No more than 2 shadow depth levels per page

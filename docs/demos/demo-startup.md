# Startup Demo Documentation

## Overview
**startup.demo** (Nexus) is a comprehensive multi-page SaaS marketing site. It demonstrates how to handle multiple distinct pages (10 total) using a single entry-point routing system within the PHP CMS.

## Key Features
- **10 Pages Routing**: Implements a clean URL dispatcher for pages like Features, Pricing, About, Docs, etc.
- **SaaS Componentry**: Includes a hero section with email capture, feature cards, and triple-tier pricing tables.
- **Sticky Navigation**: A refined header that stays at the top with active state highlighting.
- **Responsive Footer**: Multi-column footer layout typical of professional SaaS platforms.

## Pages Index
1. `Home` (/) - Hero and CTA.
2. `Features` (/features) - Service grid.
3. `Pricing` (/pricing) - Plan comparisons.
4. `About` (/about) - Mission text.
5. `Contact` (/contact) - Support info.
6. `Docs` (/docs) - Technical guide mockup.
7. `Login` (/login) - Authentication UI.
8. `Signup` (/signup) - Conversion UI.
9. `Terms` (/terms) - Legal content.
10. `Privacy` (/privacy) - Data protection content.

## Technical Implementation
The site uses a `$pages` array to define titles and views. The `REQUEST_URI` is parsed to determine which content block to render while maintaining a shared layout (header/footer).

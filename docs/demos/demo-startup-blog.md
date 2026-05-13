# Startup Blog Demo Documentation

## Overview
**startup-blog.demo** (Vantage) is a content-heavy SaaS platform that integrates a full-scale blog system. It showcases the CMS's ability to handle large volumes of dynamic content (30+ articles) alongside standard marketing pages.

## Key Features
- **Hybrid Content Model**: 10 static marketing pages + 30 dynamic blog articles.
- **Dynamic Slug Routing**: Implements pattern matching for `/blog/post-*` routes.
- **Rich Typography**: Uses the 'Outfit' font and Tailwind's Prose plugin for beautiful long-form reading.
- **Automated Metadata**: Titles and meta tags update dynamically based on the current post.
- **Visuals**: Seeded with unique cover images for every post using placeholder services.

## Architecture
- **Marketing Pages**: Similar to the Startup demo, covering core business info.
- **Blog Engine**:
    - Listing View: Shows all 30 posts with excerpts and categories.
    - Single View: Full article layout with author info, dates, and related tags.

## Scaling Note
This demo proves the CMS can handle hierarchical data structures where some nodes lead to deep, dynamic sub-trees (like a blog or knowledge base).

# Practical Math & Statistics for AI Engineers

Software engineering is often about logic, but AI Engineering and Data Visualization are about patterns, probability, and vectors.

## 1. Linear Algebra & Embeddings (The AI Foundation)
**Goal**: Understand how AI "sees" data as coordinates in multi-dimensional space.

- **Lab 1: The Distance Between Ideas**
    - **Concept**: Cosine Similarity.
    - **Task**: Read `docs/ai-agents/ai-refactoring-algorithm.md`.
    - **Exercise**: Imagine three words: "Code", "Script", and "Apple". If "Code" is at (1,1) and "Script" is at (1.1, 1), where would "Apple" be? Use the [AI Student Prompts](./AI-STUDENT-PROMPTS.md) to calculate the "Distance" between these concepts.
- **Lab 2: Scaling & Normalization**
    - **Task**: Understand why we scale data between 0 and 1.
    - **Exercise**: Take the benchmarking scores from `src/views/SiteBenchmarker.vue`. Write a JS function that normalizes these scores (0 to 100) so they can be compared fairly regardless of the unit (ms vs bytes).

## 2. Statistics for Performance Optimization
**Goal**: Stop guessing and start measuring with confidence.

- **Lab 1: Mean, Median, and Outliers**
    - **Reference**: `docs/devops/stack-performance-benchmarking.md`
    - **Task**: Run 50 tests of the `repo-php` load time.
    - **Exercise**: Calculate the **Mean** and the **99th Percentile (P99)**. Explain why P99 is more important for User Experience (UX) than the average.
- **Lab 2: Probability in LLMs**
    - **Task**: Understand "Temperature" and "Top-P" settings in the Gemini API.
    - **Exercise**: Experiment with the AI by asking it to complete a sentence with temperature 0.1 vs 0.9. Document how the statistical randomness changes the output.

## 3. Geometry for UI & Visualization
**Goal**: Master the math of the screen.

- **Lab 1: Responsive Calculations**
    - **Task**: Look at `src/style.css`.
    - **Exercise**: Calculate the Aspect Ratio of a hero banner that must stay consistent across mobile (375px wide) and desktop (1440px wide).
- **Lab 2: Data Mapping with D3/Recharts**
    - **Reference**: `src/views/SiteDashboard.vue`
    - **Task**: Study how a bar chart represents data.
    - **Exercise**: Create a calculation that maps a range of "Number of Files" (0 to 5000) to a Pixel Height (0 to 300px) for a custom chart.

## Complexity Levels
- **Basic**: Mean/Median and UI Ratios.
- **Advanced**: Cosine Similarity and Distribution Analysis.

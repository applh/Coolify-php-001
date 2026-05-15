# Practical Math & Statistics for AI Engineers

Software engineering is often about logic, but AI Engineering and Data Visualization are about patterns, probability, and vectors.

## 1. Linear Algebra, Matrices & Dimensionality (The AI Foundation)
**Goal**: Understand how AI "sees" data as coordinates in multi-dimensional space, and how to manipulate these dimensions.

<div align="center">
  <svg viewBox="0 0 200 100" xmlns="http://www.w3.org/2000/svg" style="max-width: 400px; margin: 1em 0;">
    <defs>
      <marker id="arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="4" markerHeight="4" orient="auto-start-reverse">
        <path d="M 0 0 L 10 5 L 0 10 z" fill="#3b82f6" />
      </marker>
    </defs>
    <line x1="20" y1="80" x2="180" y2="80" stroke="#9ca3af" stroke-width="2" />
    <line x1="20" y1="80" x2="20" y2="10" stroke="#9ca3af" stroke-width="2" />
    <line x1="20" y1="80" x2="120" y2="40" stroke="#3b82f6" stroke-width="3" marker-end="url(#arrow)" />
    <text x="125" y="35" fill="#3b82f6" font-size="12" font-family="sans-serif">Vector A (Math)</text>
    <line x1="20" y1="80" x2="160" y2="70" stroke="#ef4444" stroke-width="3" />
    <polygon points="160,70 152,66 154,74" fill="#ef4444" />
    <text x="145" y="85" fill="#ef4444" font-size="12" font-family="sans-serif">Vector B (Stats)</text>
    <path d="M 50 68 A 40 40 0 0 0 54 53" fill="none" stroke="#10b981" stroke-width="2"/>
    <text x="60" y="65" fill="#10b981" font-size="12" font-family="sans-serif">θ (Similarity)</text>
  </svg>
</div>

- **Practical Lab: The Distance Between Ideas**
    - **Goal**: Master Cosine Similarity & Vectors.
    - **Reference**: `docs/ai-agents/ai-refactoring-algorithm.md`
    - **Exercise**: Imagine three words: "Code", "Script", and "Apple". If "Code" is at (1,1) and "Script" is at (1.1, 1), where would "Apple" be? Use the [AI Student Prompts](./AI-STUDENT-PROMPTS.md) to calculate the "Distance" between these concepts.
    - **Complexity**: Phase 9 (Advanced)

- **Practical Lab: Matrix Operations**
    - **Goal**: Understand matrices as transformations.
    - **Reference**: `src/views/SiteBenchmarker.vue` 
    - **Exercise**: Write a JS function that takes a 2D array representing a matrix and a 1D array representing a vector, and returns the dot product. Understand how multiplying a vector by a matrix translates, scales, or rotates it.
    - **Complexity**: Phase 9 (Advanced)

- **Practical Lab: Principal Component Analysis (PCA) & Dimension Reduction**
    - **Goal**: Learn to reduce the number of variables in a dataset while preserving as much information as possible.
    - **Reference**: `docs/training/PRACTICAL-BUSINESS-DEVOPS.md`
    - **Exercise**: Given a dataset with 5 dimensions (features) related to website performance, conceptualize how you would project this down to 2 dimensions for visualization using a PCA algorithm. Write down the conceptual steps emphasizing eigenvalue extraction.
    - **Complexity**: Phase 10 (Advanced)

## 2. Statistics & Data Cleanup
**Goal**: Stop guessing, start measuring with confidence, and ensure your data is reliable.

<div align="center">
  <svg viewBox="0 0 300 150" xmlns="http://www.w3.org/2000/svg" style="max-width: 500px; margin: 1em 0;">
    <line x1="20" y1="130" x2="280" y2="130" stroke="#374151" stroke-width="2"/>
    <path d="M 30 130 Q 80 130 100 80 T 150 20 T 200 80 T 270 130" fill="none" stroke="#3b82f6" stroke-width="3"/>
    <line x1="150" y1="130" x2="150" y2="20" stroke="#9ca3af" stroke-dasharray="4,4"/>
    <text x="135" y="145" font-size="12" font-family="sans-serif" fill="#4b5563">Mean</text>
    <line x1="230" y1="130" x2="230" y2="90" stroke="#ef4444" stroke-dasharray="4,4"/>
    <text x="215" y="145" font-size="12" fill="#ef4444" font-family="sans-serif">P99 (Outliers)</text>
  </svg>
</div>

- **Practical Lab: Mean, Median, and Outliers**
    - **Goal**: Learn descriptive statistics for performance evaluation.
    - **Reference**: `docs/devops/stack-performance-benchmarking.md`
    - **Exercise**: Run 50 tests of the `repo-php` load time. Calculate the **Mean** and the **99th Percentile (P99)**. Explain why P99 is more important for User Experience (UX) than the average.
    - **Complexity**: Phase 8 (Intermediate)

- **Practical Lab: Probability in LLMs**
    - **Goal**: Understand "Temperature" and "Top-P" settings in inference APIs.
    - **Reference**: `docs/ai-agents/gemini-api-interactions.md`
    - **Exercise**: Experiment with the AI by asking it to complete a sentence with temperature 0.1 vs 0.9. Document how the statistical randomness changes the output.
    - **Complexity**: Phase 9 (Advanced)

- **Practical Lab: Data Cleanup Techniques**
    - **Goal**: Deal with missing values, normalization, and standardization to prevent "garbage in, garbage out".
    - **Reference**: `src/views/SiteDashboard.vue`
    - **Exercise**: Write a script that takes an array of objects representing server requests, removes any objects with missing `responseTime` values, and standardizes the remaining numbers (Z-score normalization).
    - **Complexity**: Phase 5 (Intermediate)

## 3. Geometry & Projections for UI & Visualization
**Goal**: Master the math of the screen and 3D to 2D mapping.

- **Practical Lab: Responsive Calculations & Basic Geometry**
    - **Goal**: Master fluid scaling and aspect ratios.
    - **Reference**: `src/style.css`
    - **Exercise**: Calculate the Aspect Ratio of a hero banner that must stay consistent across mobile (375px wide) and desktop (1440px wide). Write the generic formula for proportional scaling.
    - **Complexity**: Phase 2 (Basic)

- **Practical Lab: Data Mapping & Projections**
    - **Goal**: Map numeric domains to geometric ranges using visualization concepts.
    - **Reference**: `src/views/SiteDashboard.vue`
    - **Exercise**: Create a calculation that maps a range of "Number of Files" (0 to 5000) to a Pixel Height (0 to 300px) for a custom chart. Then, articulate how an orthographic projection differs from a perspective projection if we were to render this chart in 3D using coordinate transformation!
    - **Complexity**: Phase 4 (Intermediate)

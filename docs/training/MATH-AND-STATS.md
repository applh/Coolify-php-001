# Practical Math & Statistics for AI Engineers

Software engineering is often about logic, but AI Engineering and Data Visualization are about patterns, probability, and vectors.

## 1. Linear Algebra, Matrices & Dimensionality (The AI Foundation)
**Goal**: Understand how AI "sees" data as coordinates in multi-dimensional space, and how to manipulate these dimensions.

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

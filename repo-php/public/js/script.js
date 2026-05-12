// Basic JavaScript
document.addEventListener('DOMContentLoaded', () => {
    console.log('Script loaded successfully!');
    
    // Example interactive element
    const title = document.querySelector('h1');
    if (title) {
        title.addEventListener('click', () => {
            title.style.color = '#3b82f6';
            setTimeout(() => {
                title.style.color = '';
            }, 500);
        });
    // Async Component Hydration
    const hydrateAsyncComponents = async () => {
        const components = document.querySelectorAll('[data-component]');
        
        for (const el of components) {
            const name = el.getAttribute('data-component');
            const props = el.getAttribute('data-props');
            
            try {
                const response = await fetch(`/api/render-component?name=${name}&props=${props}`);
                if (response.ok) {
                    const html = await response.text();
                    el.outerHTML = html;
                } else {
                    el.innerHTML = `<div class="p-4 text-red-500 bg-red-50 text-xs">Failed to load component: ${name}</div>`;
                }
            } catch (error) {
                console.error(`Error loading component ${name}:`, error);
            }
        }
    };

    hydrateAsyncComponents();
});

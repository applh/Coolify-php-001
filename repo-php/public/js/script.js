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
    }
});

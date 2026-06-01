# Use the official PHP image with FPM
FROM php:8.5-fpm

# Install system dependencies
RUN apt-get update && apt-get install -y \
    libzip-dev \
    zip \
    unzip \
    git \
    vim \
    && docker-php-ext-install zip \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Set PHP timezone
RUN printf '[Date]\ndate.timezone = "Europe/Paris"\n' > /usr/local/etc/php/conf.d/timezone.ini

# Set working directory
WORKDIR /var/www/html

# Copy application files
COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ]; then \
      echo "📦 [PHP CLI] Found archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [PHP CLI] No archive.zip found. Proceeding with standard git synchronized files."; \
    fi

# Add entrypoint script
RUN chmod +x /var/www/html/entrypoint.sh

# Adjust permissions
RUN chown -R www-data:www-data /var/www/html

# PHP-FPM runs on port 9000 by default
EXPOSE 9000

ENTRYPOINT ["/var/www/html/entrypoint.sh"]
CMD ["php-fpm"]

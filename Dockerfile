# Use the official PHP image with FPM
FROM php:8.5-fpm

# Install system dependencies
RUN apt-get update && apt-get install -y \
    libpng-dev \
    libonig-dev \
    libxml2-dev \
    zip \
    unzip

# Clear cache
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Install PHP extensions
RUN docker-php-ext-install pdo_mysql mbstring exif pcntl bcmath gd

# Set PHP timezone
RUN printf '[Date]\ndate.timezone = "Europe/Paris"\n' > /usr/local/etc/php/conf.d/timezone.ini

# Set working directory
WORKDIR /var/www/html

# Copy application files
COPY . .

# Adjust permissions
RUN chown -R www-data:www-data /var/www/html

# PHP-FPM runs on port 9000 by default
EXPOSE 9000

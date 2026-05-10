#!/bin/bash
set -e

# Setup my-data volume if missing config.php
if [ ! -f "/var/www/html/my-data/config.php" ]; then
    echo "Initializing my-data volume from content..."
    # Ensure directory exists in case volume is not mounted
    mkdir -p /var/www/html/my-data
    
    # Copy content if available
    if [ -d "/var/www/html/content" ]; then
        cp -r /var/www/html/content/. /var/www/html/my-data/
        
        # Rename config.php.example to config.php if it was copied
        if [ -f "/var/www/html/my-data/config.php.example" ]; then
            mv /var/www/html/my-data/config.php.example /var/www/html/my-data/config.php
        fi
    fi
    
    chown -R www-data:www-data /var/www/html/my-data
fi

# Execute the main container command
exec "$@"

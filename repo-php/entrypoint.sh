#!/bin/bash
set -e

# Setup my-data volume if missing config.php or forced via APP_DATA_RESET
if [ ! -f "/var/www/html/my-data/config.php" ] || [ "${APP_DATA_RESET}" = "true" ] || [ "${app_data_reset}" = "true" ]; then
    echo "Initializing my-data volume from content... (Forced reset: ${APP_DATA_RESET:-${app_data_reset:-false}})"
    # Ensure directory exists in case volume is not mounted
    mkdir -p /var/www/html/my-data
    
    # If forced reset, we might want to clear old files first to ensure a clean state
    if [ "${APP_DATA_RESET}" = "true" ] || [ "${app_data_reset}" = "true" ]; then
        echo "Clearing existing data for reset..."
        rm -rf /var/www/html/my-data/*
        rm -rf /var/www/html/my-data/.* 2>/dev/null || true # Also hidden files, ignoring errors for . and ..
    fi
    
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

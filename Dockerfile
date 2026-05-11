FROM php:8.2-apache

# Install required PHP extensions
RUN apt-get update && apt-get install -y \
    git unzip libzip-dev libpng-dev libjpeg-dev libfreetype6-dev libicu-dev \
    && docker-php-ext-configure gd --with-freetype --with-jpeg \
    && docker-php-ext-install pdo pdo_mysql zip gd intl opcache

# Enable Apache mod_rewrite (required by Symfony)
RUN a2enmod rewrite headers

# Set document root to Symfony public/ folder
ENV APACHE_DOCUMENT_ROOT /var/www/html/public
RUN sed -ri -e 's!/var/www/html!${APACHE_DOCUMENT_ROOT}!g' \
    /etc/apache2/sites-available/*.conf \
    /etc/apache2/apache2.conf \
    /etc/apache2/conf-available/*.conf

# Allow .htaccess overrides
RUN sed -i '/<Directory \/var\/www\/>/,/<\/Directory>/ s/AllowOverride None/AllowOverride All/' \
    /etc/apache2/apache2.conf

# Install Composer
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

# Copy project files
WORKDIR /var/www/html
COPY . .

# Install dependencies (production mode)
RUN composer install --no-dev --optimize-autoloader --no-interaction

# Create required directories and set permissions
RUN mkdir -p var/cache var/log public/uploads/profiles \
    && chown -R www-data:www-data var/ public/uploads/ \
    && chmod -R 775 var/ public/uploads/

# Build Symfony cache for production
RUN APP_ENV=prod php bin/console cache:clear --no-warmup \
    && APP_ENV=prod php bin/console cache:warmup

EXPOSE 80

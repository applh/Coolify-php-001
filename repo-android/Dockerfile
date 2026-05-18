FROM eclipse-temurin:17-jdk-jammy AS builder

# Set up environment variables
ENV ANDROID_SDK_ROOT /opt/android-sdk
ENV ANDROID_HOME /opt/android-sdk
ENV PATH $PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install dependencies with retries and quiet mode
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget unzip curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Set up Android SDK directory structure
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools

# Download and install command line tools (using a stable version)
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install platform components
# Using quiet mode where possible to reduce log noise
RUN yes | sdkmanager --licenses && \
    sdkmanager --update && \
    sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.2"

# Install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.2-bin.zip -P /tmp && \
    unzip -q -d /opt/gradle /tmp/gradle-8.2-bin.zip && \
    rm /tmp/gradle-8.2-bin.zip
ENV PATH $PATH:/opt/gradle/gradle-8.2/bin

WORKDIR /app
COPY . .

# Build APK with memory optimizations and quiet output
# --no-daemon is critical for Docker builds
# --max-workers 1 reduces memory overhead
# -Dorg.gradle.jvmargs is already set in gradle.properties, but we reinforce it here
RUN gradle assembleDebug --no-daemon --max-workers 1 --stacktrace

FROM nginx:alpine

# Remove default assets
RUN rm -rf /usr/share/nginx/html/*

# Copy build artifacts and serving page
COPY --from=builder /app/app/build/outputs/apk/debug/app-debug.apk /usr/share/nginx/html/app.apk
COPY index.html /usr/share/nginx/html/index.html

# Simple and effective Nginx configuration for port 3000
RUN printf "server {\n\
    listen 3000;\n\
    root /usr/share/nginx/html;\n\
    index index.html;\n\
    location / {\n\
        try_files \$uri \$uri/ /index.html;\n\
    }\n\
    location = /app.apk {\n\
        add_header Content-Type application/vnd.android.package-archive;\n\
        add_header Content-Disposition 'attachment; filename=\"app.apk\"';\n\
    }\n\
}\n" > /etc/nginx/conf.d/default.conf

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]

FROM eclipse-temurin:17-jdk-jammy AS builder

# Set up environment variables
ENV ANDROID_SDK_ROOT /opt/android-sdk
ENV ANDROID_HOME /opt/android-sdk
ENV PATH $PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install dependencies with retries and quiet mode
RUN apt-get update --fix-missing && \
    apt-get install -y --no-install-recommends wget unzip curl ca-certificates libstdc++6 zlib1g && \
    rm -rf /var/lib/apt/lists/*

# Set up Android SDK
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install platform components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.7-bin.zip -P /tmp && \
    unzip -q -d /opt/gradle /tmp/gradle-8.7-bin.zip && \
    rm /tmp/gradle-8.7-bin.zip
ENV PATH $PATH:/opt/gradle/gradle-8.7/bin
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.jvmargs=\"-Xmx1024m -XX:MaxMetaspaceSize=256m\""

WORKDIR /app
COPY . .

# Build APK with memory optimizations
RUN gradle assembleDebug --no-daemon --max-workers 1 --stacktrace --console=plain

FROM nginx:alpine

# Install curl for healthcheck
RUN apk add --no-cache curl

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

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:3000/ || exit 1

CMD ["nginx", "-g", "daemon off;"]

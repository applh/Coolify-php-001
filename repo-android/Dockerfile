FROM eclipse-temurin:17-jdk-jammy

# Set up environment variables
ENV ANDROID_SDK_ROOT /opt/android-sdk
ENV ANDROID_HOME /opt/android-sdk
ENV PATH $PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install dependencies with retries and quiet mode, and install nodejs for the build server
RUN apt-get update --fix-missing && \
    apt-get install -y --no-install-recommends wget unzip curl ca-certificates libstdc++6 zlib1g nodejs && \
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

# Hook for restoring codebases via binary compactions (unzip is already installed via apt-get in line 10)
RUN if [ -f archive.zip ] && unzip -tq archive.zip >/dev/null 2>&1; then \
      echo "📦 [Android Builder] Found valid archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [Android Builder] No valid archive.zip found. Proceeding with standard git synchronized files."; \
      rm -f archive.zip; \
    fi

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:3000/ || exit 1

CMD ["node", "server.js"]

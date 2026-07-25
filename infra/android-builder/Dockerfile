FROM eclipse-temurin:21-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip wget git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
    && mkdir -p ${ANDROID_HOME}/cmdline-tools/latest \
    && mv /tmp/cmdline-tools/cmdline-tools/* ${ANDROID_HOME}/cmdline-tools/latest/ \
    && rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses \
    && sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"

WORKDIR /workspace

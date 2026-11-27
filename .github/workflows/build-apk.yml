name: Build Android APK

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Locate gradlew and build Debug APK
        run: |
          set -e
          GRADLEW_PATH=$(find . -maxdepth 4 -name "gradlew" -type f | head -n 1)

          if [ -z "$GRADLEW_PATH" ]; then
            echo "Error: gradlew was not found in the repository."
            exit 1
          fi

          echo "Found gradlew at: $GRADLEW_PATH"
          PROJECT_DIR=$(dirname "$GRADLEW_PATH")
          chmod +x "$GRADLEW_PATH"

          cd "$PROJECT_DIR"
          ./gradlew assembleDebug --stacktrace

      - name: Upload Debug APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: souq-al-matajer-apk
          path: "**/*.apk"
          if-no-files-found: error

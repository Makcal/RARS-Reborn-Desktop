jpackage \
  --type deb \
  --app-version 1.0 \
  --name "RARS-REBORN" \
  --input target/ \
  --main-jar RARS-REBORN-1.0-SNAPSHOT.jar \
  --main-class rarsreborn.Main \
  --runtime-image bundled_java/ \
  --dest builds/

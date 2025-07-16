if ! [ -f mabl/src/main/assets/minilm-l6-v2-qint8-arm64.onnx ]; then
  curl -L -o mabl/src/main/assets/minilm-l6-v2-qint8-arm64.onnx https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx?download=true
fi
./gradlew :plugins:demo:installDebug :plugins:aipinsystem:installDebug :plugins:system:installDebug :plugins:openai:installDebug :mabl:installAipinDebug
adb shell appops set com.penumbraos.plugins.openai MANAGE_EXTERNAL_STORAGE allow
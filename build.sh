if ! [ -f mabl/src/main/assets/minilm-l6-v2-qint8-arm64.onnx ]; then
  curl -L -o mabl/src/main/assets/minilm-l6-v2-qint8-arm64.onnx https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx?download=true
fi
./gradlew :plugins:demo:installDebug :plugins:aipinsystem:installDebug :plugins:system:installDebug :plugins:openai:installDebug :plugins:googlesearch:installDebug :mabl:installAipinDebug
adb shell appops set com.penumbraos.mabl.pin MANAGE_EXTERNAL_STORAGE allow
adb shell appops set com.penumbraos.plugins.openai MANAGE_EXTERNAL_STORAGE allow
adb shell pm disable-user --user 0 humane.experience.systemnavigation
sleep 1
adb shell cmd package set-home-activity com.penumbraos.mabl/.MainActivity
sleep 1
# I think one of these works
adb shell settings put secure launcher_component com.penumbraos.mabl/.MainActivity
adb shell settings put system home_app com.penumbraos.mabl
adb shell settings put global default_launcher com.penumbraos.mabl/.MainActivity
echo "Built on $(date '+%Y-%m-%d %H:%M:%S')"
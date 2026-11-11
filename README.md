# torch-tile

A minimal Android Quick Settings tile ("Taschenlampe"/Torch) that turns the camera
flashlight on and off via `CameraManager.setTorchMode()` — a workaround for devices
where the built-in SystemUI flashlight tile is broken.

Tested on: Samsung Galaxy M32 4G (SM-M325FV), LineageOS 23.2 unofficial
(Android 16, BP4A.251205.006) — where the stock `FlashlightTileWithLevel` tile is a
silent no-op while the torch API itself works fine.

## Why the stock tile breaks (Android 16 SystemUI bug)

Layer-by-layer diagnosis showed hardware, kernel driver (`sm5714-fled`), camera HAL
and `CameraManager.setTorchMode()` all work. The stock tile is dead because
`FlashlightTileUserActionInteractor.handleInput` only reacts to
`FlashlightModel.Available.Level`; on devices where the HAL reports a single torch
strength level (`torchStrengthMaxLevel=1`) the repository emits
`Available.Binary`/`Unavailable` instead — every click becomes a silent no-op.
Setting the flag `com.android.systemui.flashlight_strength` to `false` does **not**
help: `isAvailable()` then hard-returns `false` and the tile stays dead.

## Install

```bash
adb install -r -g torch-tile.apk              # -g grants CAMERA at install
adb shell cmd appops set de.alem.torch CAMERA allow   # tile runs in background
adb shell cmd statusbar add-tile de.alem.torch/.TorchTileService
```

The tile appears in the Quick Settings edit panel as "Taschenlampe". Tap to toggle.

The `appops` line matters: AppOps CAMERA is `foreground` by default, but a
TileService runs in the background — without it `setTorchMode` fails silently.

## Build (no Gradle)

Needs the Android SDK (platform android-36, build-tools 35.x) and a JDK:

```bash
javac -source 8 -target 8 -cp $ANDROID_JAR -d classes src/de/alem/torch/TorchTileService.java
$BT/d8 --output build --lib $ANDROID_JAR --min-api 30 classes/de/alem/torch/*.class
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I $ANDROID_JAR --manifest AndroidManifest.xml build/res.zip --min-sdk-version 30 --target-sdk-version 34
cp build/base.apk build/app-unsigned.apk && (cd build && zip -qj app-unsigned.apk classes.dex)
$BT/zipalign -f 4 build/app-unsigned.apk build/app-aligned.apk
keytool -genkeypair -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -dname "CN=Android Debug" -keyalg RSA -keysize 2048 -validity 10000
$BT/apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out torch-tile.apk build/app-aligned.apk
```

## License

MIT

package de.alem.torch;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class TorchTileService extends TileService {
    private CameraManager cm;
    private String camId;
    private volatile boolean on = false;

    private final CameraManager.TorchCallback cb = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (cameraId != null && cameraId.equals(camId)) {
                on = enabled;
                updateTile();
            }
        }

        @Override
        public void onTorchModeUnavailable(String cameraId) {
            if (cameraId != null && cameraId.equals(camId)) {
                on = false;
                updateTile();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        cm = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String id : cm.getCameraIdList()) {
                Boolean hasFlash = cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(hasFlash)) {
                    camId = id;
                    break;
                }
            }
        } catch (Exception e) {
            camId = null;
        }
        cm.registerTorchCallback(cb, new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onStartListening() {
        updateTile();
    }

    @Override
    public void onClick() {
        if (camId == null || cm == null) {
            return;
        }
        try {
            cm.setTorchMode(camId, !on);
        } catch (Exception e) {
            // Camera nicht verfügbar - Tile bleibt unverändert
        }
        updateTile();
    }

    private void updateTile() {
        Tile t = getQsTile();
        if (t == null) {
            return;
        }
        t.setLabel("Taschenlampe");
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }

    @Override
    public void onDestroy() {
        cm.unregisterTorchCallback(cb);
        super.onDestroy();
    }
}

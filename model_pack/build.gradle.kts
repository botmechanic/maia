plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("model_pack")
    dynamicDelivery {
        // Install-time delivery bundles model weights through Google Play's CDN
        // so Maia keeps ZERO android.permission.INTERNET in its manifest.
        deliveryType.set("install-time")
    }
}


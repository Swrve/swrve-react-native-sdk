import { NativeModules } from 'react-native';
const { SwrveGeoPlugin } = NativeModules;

class SwrveGeoSDK {
    start() {
        SwrveGeoPlugin.start();
    }

    stop() {
        SwrveGeoPlugin.stop();
    }

    async isStarted() {
        return SwrveGeoPlugin.isStarted();
    }

    async getVersion() {
        return SwrveGeoPlugin.getVersion();
    }
}

module.exports = new SwrveGeoSDK();

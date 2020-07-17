# Public tests Associated with the react-native-swrve-plugin

The UnitTests project is a pre generated react native project which includes unit tests that verify SwrvePlugin module functions called by the plugin are using the correct functions from the native SDK dependencies

## How to run Android Tests

If you would like to test these with Android, use the following steps:

1. In terminal  go to `public/tests/UnitTests` and run `yarn install` to ensure you have the latest dependencies
1. `cd` to `android` and run `./gradlew test`

## How to run iOS Tests

If you would like to test these with iOS, use the following steps:

1. In terminal  go to `public/tests/UnitTests` and run `yarn install` to ensure you have the latest dependencies
1. `cd` to `ios` and run `pod install`
1. Run `xcodebuild test -workspace UnitTests.xcworkspace -scheme UnitTests` with a device / simulator of your choice
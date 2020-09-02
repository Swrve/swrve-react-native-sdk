#import <React/RCTBridgeModule.h>
#import <SwrveGeoSDK/SwrveGeoSDK.h>
#import <SwrveGeoSDK/SwrveGeoConfig.h>

@interface SwrveGeoPlugin : NSObject <RCTBridgeModule>

+ (void)initWithConfig:(SwrveGeoConfig *)config;

- (void)start;
- (void)stop;
- (void)isStartedWithResolver:(RCTPromiseResolveBlock)resolve
                     rejecter:(RCTPromiseRejectBlock)reject;
- (void)getVersionWithResolver:(RCTPromiseResolveBlock)resolve
                     rejecter:(RCTPromiseRejectBlock)reject;

@end

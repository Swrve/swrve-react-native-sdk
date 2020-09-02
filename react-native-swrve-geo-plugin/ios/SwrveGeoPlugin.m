#import "SwrveGeoPlugin.h"

@implementation SwrveGeoPlugin

RCT_EXPORT_MODULE()

+ (void)initWithConfig:(SwrveGeoConfig *)config {
    [SwrveGeoSDK initWithConfig:config];
}

#pragma mark - public void methods

RCT_EXPORT_METHOD(start){
    [SwrveGeoSDK start];
}

RCT_EXPORT_METHOD(stop) {
    [SwrveGeoSDK stop];
}

#pragma mark - public promise methods

RCT_REMAP_METHOD(getVersion, getVersionWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        NSString *version = [SwrveGeoSDK version];
        resolve(version);
    } @catch ( NSException *ex ) {
        reject(ex.name, ex.reason, [SwrveGeoPlugin produceErrorFromException:ex]);
    }
}


RCT_REMAP_METHOD(isStarted, isStartedWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        BOOL started = [SwrveGeoSDK isStarted];
        resolve(@(started));
    } @catch ( NSException *ex ) {
        reject(ex.name, ex.reason, [SwrveGeoPlugin produceErrorFromException:ex]);
    }
}

+ (NSError*) produceErrorFromException:(NSException *) exception {
    NSMutableDictionary *info = [NSMutableDictionary dictionary];
    [info setValue:exception forKey:NSUnderlyingErrorKey];
    [info setValue:exception.userInfo forKey:NSDebugDescriptionErrorKey];
    [info setValue:(exception.reason ?: @"unknown reason") forKey:NSLocalizedFailureReasonErrorKey];
    
    return [NSError errorWithDomain:exception.name code:0 userInfo:info];
}

@end

#import <Foundation/Foundation.h>
#import <SwrveSDK/SwrveSDK.h>

NS_ASSUME_NONNULL_BEGIN

@interface SwrvePluginDelegateHandler : NSObject<SwrveInAppMessageDelegate, SwrveDeeplinkDelegate>

+ (instancetype)sharedInstance;

@end

NS_ASSUME_NONNULL_END
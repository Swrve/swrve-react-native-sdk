#import <Foundation/Foundation.h>
#import <SwrveSDKCommon/SwrvePush.h>

NS_ASSUME_NONNULL_BEGIN

@interface SwrvePluginPushHandler : NSObject <SwrvePushResponseDelegate>

/// For the ServePlugin to let us know whether the JS side is ready for events yet
@property (nonatomic, assign) BOOL shouldBufferEvents;

+ (instancetype)sharedInstance;

- (void)flushBufferedEvents;

@end

NS_ASSUME_NONNULL_END

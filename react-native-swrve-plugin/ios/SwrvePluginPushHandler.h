#import <Foundation/Foundation.h>
#import <SwrveSDKCommon/SwrvePush.h>
#import "SwrveEmbeddedMessage.h"

NS_ASSUME_NONNULL_BEGIN

@interface SwrvePluginPushHandler : NSObject <SwrvePushResponseDelegate>

/// For the ServePlugin to let us know whether the JS side is ready for events yet
@property(nonatomic, assign) BOOL shouldBufferEvents;
@property(nonatomic, assign) BOOL shouldBufferEmbeddedEvents;

+ (instancetype)sharedInstance;

- (void)flushBufferedEvents;


- (void)bufferEmbeddedCallbackWithMessage:(SwrveEmbeddedMessage *)message
                personalizationProperties:(NSDictionary *)properties
                                isControl:(NSString *)isControl;

- (void)flushBufferedEmbeddedEvents;

@end

NS_ASSUME_NONNULL_END

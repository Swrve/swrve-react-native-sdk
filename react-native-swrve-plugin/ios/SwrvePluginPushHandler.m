#import "SwrvePluginPushHandler.h"
#import "SwrvePlugin.h"
#import "SwrvePluginUtils.h"
#import "SwrvePluginEventEmitter.h"
#import "SwrvePluginPushHandler.h"

@interface SwrvePluginPushHandler ()

// ReactNative will let us know when someone subscribes to the event listener
@property (nonatomic, assign) BOOL hasListeners;
@property (nonatomic, strong) NSMutableArray* pushEventBuffer;

@end

/// Takes the SwrvePluginPushHandler methods as of AppDelegate initialisation
/// Actions are sent to the SwrvePluginEventEmitter via NSNotificationCenter to keep things quick and avoid any lifecycle issues
@implementation SwrvePluginPushHandler

+ (instancetype)sharedInstance {
    static SwrvePluginPushHandler *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [SwrvePluginPushHandler new];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        self.pushEventBuffer = [NSMutableArray new];
        self.shouldBufferEvents = YES;
    }
    return self;
}

- (void)resetStateForTesting {
    [self.pushEventBuffer removeAllObjects];
    self.shouldBufferEvents = YES;
}


- (void) didReceiveNotificationResponse:(UNNotificationResponse *)response
                  withCompletionHandler:(void (^)(void))completionHandler  API_AVAILABLE(ios(10.0)) {
    NSDictionary *userInfo = response.notification.request.content.userInfo;
    NSLog(@"SwrvePlugin - SwrvePluginPushHandler - didReceiveNotificationResponse %@", userInfo);
    
    if (userInfo) {
        [self handleNotificationUserInfo:userInfo];
    }
    
    if (completionHandler){
        completionHandler();
    }
}

/// Separate the logic from the callback method as it's not straightforward to create UNNotificationResponse for testing
- (void) handleNotificationUserInfo:(NSDictionary *)userInfo {
    NSString* json = [SwrvePluginUtils serializeDictonaryToJson:userInfo withKey:nil];
    if (json) {
        if (self.shouldBufferEvents) {
            [self.pushEventBuffer addObject:json];
        } else {
            [[NSNotificationCenter defaultCenter] postNotificationName:PUSH_EVENT_NAME object:self userInfo:@{PUSH_EVENT_PAYLOAD: json}];
        }
    }
}

- (void) willPresentNotification:(UNNotification *)notification
           withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler  API_AVAILABLE(ios(10.0)) {
    if (completionHandler) {
        if (@available(iOS 10.0, *)) {
            completionHandler(UNNotificationPresentationOptionNone);
        } else {
            // Fallback on earlier versions
        }
    }
}

- (void)flushBufferedEvents {
    NSLog(@"SwrvePlugin - SwrvePluginPushHandler - flushBufferedEvents with %ld", self.pushEventBuffer.count);
    
    for (NSString* json in self.pushEventBuffer) {
        [[NSNotificationCenter defaultCenter] postNotificationName:PUSH_EVENT_NAME object:self userInfo:@{PUSH_EVENT_PAYLOAD: json}];
    }
    [self.pushEventBuffer removeAllObjects];
}

@end

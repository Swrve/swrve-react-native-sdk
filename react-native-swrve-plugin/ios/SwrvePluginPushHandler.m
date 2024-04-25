#import "SwrvePluginPushHandler.h"
#import "SwrvePlugin.h"
#import "SwrvePluginUtils.h"
#import "SwrvePluginEventEmitter.h"

@interface EmbeddedCallbackHolder : NSObject
@property(nonatomic, strong) SwrveEmbeddedMessage *message;
@property(nonatomic, strong) NSDictionary *personalizationProperties;
@property(nonatomic, strong) NSString *isControl;
- (instancetype)initWithMessage:(SwrveEmbeddedMessage *)message
      personalizationProperties:(NSDictionary *)properties
                      isControl:(NSString *)isControl;
@end

@implementation EmbeddedCallbackHolder
- (instancetype)initWithMessage:(SwrveEmbeddedMessage *)message
      personalizationProperties:(NSDictionary *)properties
                      isControl:(NSString *)isControl {
    self = [super init];
    if (self) {
        _message = message;
        _personalizationProperties = properties;
        _isControl = isControl;
    }
    return self;
}
@end

@interface SwrvePluginPushHandler ()

// ReactNative will let us know when someone subscribes to the event listener
@property(nonatomic, assign) BOOL hasListeners;
@property(nonatomic, strong) NSMutableArray *pushEventBuffer;
@property(nonatomic, strong) NSMutableArray *embeddedCallbackEventBuffer;

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
        self.embeddedCallbackEventBuffer = [NSMutableArray new];
        self.shouldBufferEvents = YES;
        self.shouldBufferEmbeddedEvents = YES;
    }
    return self;
}

- (void)resetStateForTesting {
    [self.pushEventBuffer removeAllObjects];
    [self.embeddedCallbackEventBuffer removeAllObjects];
    self.shouldBufferEvents = YES;
    self.shouldBufferEmbeddedEvents = YES;
}

- (void)didReceiveNotificationResponse:(UNNotificationResponse *)response
                 withCompletionHandler:(void (^)(void))completionHandler  API_AVAILABLE(ios(10.0)) {
    NSDictionary *userInfo = response.notification.request.content.userInfo;
    NSLog(@"SwrvePlugin - SwrvePluginPushHandler - didReceiveNotificationResponse %@", userInfo);

    if (userInfo) {
        [self handleNotificationUserInfo:userInfo];
    }

    if (completionHandler) {
        completionHandler();
    }
}

/// Separate the logic from the callback method as it's not straightforward to create UNNotificationResponse for testing
- (void)handleNotificationUserInfo:(NSDictionary *)userInfo {
    NSString *json = [SwrvePluginUtils serializeDictonaryToJson:userInfo withKey:nil];
    if (json) {
        if (self.shouldBufferEvents) {
            [self.pushEventBuffer addObject:json];
        } else {
            [[NSNotificationCenter defaultCenter] postNotificationName:PUSH_EVENT_NAME object:self userInfo:@{PUSH_EVENT_PAYLOAD: json}];
        }
    }
}

- (void)willPresentNotification:(UNNotification *)notification
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

    for (NSString *json in self.pushEventBuffer) {
        [[NSNotificationCenter defaultCenter] postNotificationName:PUSH_EVENT_NAME object:self userInfo:@{PUSH_EVENT_PAYLOAD: json}];
    }
    [self.pushEventBuffer removeAllObjects];
}

- (void)bufferEmbeddedCallbackWithMessage:(SwrveEmbeddedMessage *)message
                personalizationProperties:(NSDictionary *)properties
                                isControl:(NSString *)isControl {
    EmbeddedCallbackHolder *holder = [[EmbeddedCallbackHolder alloc] initWithMessage:message
                                                           personalizationProperties:properties
                                                                           isControl:isControl];
    [self.embeddedCallbackEventBuffer addObject:holder];
}

- (void)flushBufferedEmbeddedEvents {
    NSLog(@"SwrvePlugin - SwrvePluginPushHandler - flushBufferedEmbeddedEvents with %ld", self.embeddedCallbackEventBuffer.count);
    for (EmbeddedCallbackHolder *holder in self.embeddedCallbackEventBuffer) {
        [[NSNotificationCenter defaultCenter] postNotificationName:EMBEDDED_CALLBACK_EVENT_NAME object:self userInfo:@{
                CALLBACK_KEY_EMBEDDED_MESSAGE_MAP: holder.message,
                CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP: holder.personalizationProperties,
                CALLBACK_KEY_EMBEDDED_CONTROL_ACTION: holder.isControl}
        ];
    }
    [self.embeddedCallbackEventBuffer removeAllObjects];
}

@end

#import "SwrvePluginEventEmitter.h"


@interface SwrvePluginEventEmitter ()

// ReactNative will let us know when someone subscribes to the event listener
@property (nonatomic, assign) BOOL hasListeners;
@property (nonatomic, strong) NSMutableArray* pushEventBuffer;
@property (nonatomic, strong) NSMutableArray* silentPushEventBuffer;

@end


@implementation SwrvePluginEventEmitter

NSString *const PUSH_EVENT_NAME = @"PushNotification";
NSString *const SILENT_PUSH_EVENT_NAME = @"SilentPushNotification";
NSString *const PUSH_EVENT_PAYLOAD = @"PushEventPayload";
NSString *const CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL = @"appStoreUrl";
NSString *const CALLBACK_KEY_CUSTOM_BUTTON_ACTION = @"customAction";
NSString *const CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT = @"campaignSubject";
NSString *const CALLBACK_KEY_DISMISS_BUTTON_NAME = @"buttonName";
NSString *const CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT = @"clipboardContents";

// Event Names
NSString *const RESOURCES_UPDATED_EVENT_NAME = @"SwrveUserResourcesUpdated";
NSString *const MESSAGE_CALLBACK_INSTALL_EVENT_NAME = @"SwrveMessageInstallCallback";
NSString *const MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = @"SwrveMessageCustomCallback";
NSString *const MESSAGE_CALLBACK_DISMISS_EVENT_NAME = @"SwrveMessageDismissCallback";
NSString *const MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = @"SwrveMessageClipboardCallback";
NSString *const MODULE_NAME = @"SwrvePluginEventEmitter";

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
  return @[PUSH_EVENT_NAME, SILENT_PUSH_EVENT_NAME, RESOURCES_UPDATED_EVENT_NAME, MESSAGE_CALLBACK_INSTALL_EVENT_NAME, MESSAGE_CALLBACK_CUSTOM_EVENT_NAME, MESSAGE_CALLBACK_DISMISS_EVENT_NAME, MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME];
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.pushEventBuffer = [NSMutableArray new];
        self.silentPushEventBuffer = [NSMutableArray new];
        self.hasListeners = NO;
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onPushNotification:)
                                                     name:PUSH_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onSilentPush:)
                                                     name:SILENT_PUSH_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onUserResourceUpdate:)
                                                     name:RESOURCES_UPDATED_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onMessageInstallButtonCallback:)
                                                     name:MESSAGE_CALLBACK_INSTALL_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onMessageCustomButtonCallback:)
                                                     name:MESSAGE_CALLBACK_CUSTOM_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onMessageDismissButtonCallback:)
                                                     name:MESSAGE_CALLBACK_DISMISS_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onMessageClipboardButtonCallback:)
                                                     name:MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME object:nil];

        NSLog(@"SwrvePlugin - SwrvePluginEventEmitter init");
    }
    return self;
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)onPushNotification:(NSNotification *)notification {
    NSString *payloadJsonString = notification.userInfo[PUSH_EVENT_PAYLOAD];
    NSLog(@"SwrvePlugin - SwrvePluginEventEmitter onPushNotification %@", payloadJsonString);
    
    if (self.hasListeners) {
        [self sendEventWithName:PUSH_EVENT_NAME body:@{PUSH_EVENT_PAYLOAD: payloadJsonString}];
    } else {
        [self.pushEventBuffer addObject:payloadJsonString];
    }
}

- (void)onSilentPush:(NSNotification *)notification {
    NSString *payloadJsonString = notification.userInfo[PUSH_EVENT_PAYLOAD];
    NSLog(@"SwrvePlugin - SwrvePluginEventEmitter onSilentPush %@", payloadJsonString);
    
    if (self.hasListeners) {
        [self sendEventWithName:SILENT_PUSH_EVENT_NAME body:@{PUSH_EVENT_PAYLOAD: payloadJsonString}];
    } else {
        [self.silentPushEventBuffer addObject:payloadJsonString];
    }
}

- (void)onUserResourceUpdate:(NSNotification *)notification {
    NSLog(@"SwrvePlugin - SwrvePluginEventEmitter onUserResourceUpdate");
    
    if (self.hasListeners) {
        [self sendEventWithName:RESOURCES_UPDATED_EVENT_NAME body:@{}];
    }
}
- (void)onMessageInstallButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *appStoreUrl = notification.userInfo[CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL];
        [self sendEventWithName:MESSAGE_CALLBACK_INSTALL_EVENT_NAME body:@{CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL: appStoreUrl}];
    }
}

- (void)onMessageCustomButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *customAction = notification.userInfo[CALLBACK_KEY_CUSTOM_BUTTON_ACTION];
        [self sendEventWithName:MESSAGE_CALLBACK_CUSTOM_EVENT_NAME body:@{CALLBACK_KEY_CUSTOM_BUTTON_ACTION: customAction}];
    }
}

- (void)onMessageDismissButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *campaignSubject = notification.userInfo[CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT];
        NSString *buttonName = notification.userInfo[CALLBACK_KEY_DISMISS_BUTTON_NAME];
        [self sendEventWithName:MESSAGE_CALLBACK_DISMISS_EVENT_NAME body:@{CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT: campaignSubject, CALLBACK_KEY_DISMISS_BUTTON_NAME: buttonName}];
    }
}

- (void)onMessageClipboardButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *clipboardContents = notification.userInfo[CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT];
        [self sendEventWithName:MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME body:@{CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT: clipboardContents}];
    }
}

- (void)startObserving {
    NSLog(@"SwrvePlugin - SwrvePluginEventEmitter startObserving");
    self.hasListeners = YES;
    
    // Flush push event listeners
    for (NSString* payload in self.pushEventBuffer) {
        [self sendEventWithName:PUSH_EVENT_NAME body:@{PUSH_EVENT_PAYLOAD: payload}];
    }
    [self.pushEventBuffer removeAllObjects];
    
    for (NSString* payload in self.silentPushEventBuffer) {
        [self sendEventWithName:SILENT_PUSH_EVENT_NAME body:@{PUSH_EVENT_PAYLOAD: payload}];
    }
    [self.silentPushEventBuffer removeAllObjects];
}

- (void)stopObserving {
    NSLog(@"SwrvePlugin - SwrvePluginEventEmitter stopObserving");
    self.hasListeners = NO;
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end

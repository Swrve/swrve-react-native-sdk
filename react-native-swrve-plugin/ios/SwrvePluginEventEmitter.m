#import "SwrvePluginEventEmitter.h"
#import <SwrveSDK/SwrveEmbeddedMessage.h>


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
NSString *const CALLBACK_KEY_CUSTOM_BUTTON_ACTION = @"customAction";
NSString *const CALLBACK_KEY_CUSTOM_BUTTON_ACTION_CAMPAIGN_NAME = @"campaignName";
NSString *const CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT = @"campaignSubject";
NSString *const CALLBACK_KEY_DISMISS_BUTTON_NAME = @"buttonName";
NSString *const CALLBACK_KEY_DISMISS_CAMPAIGN_NAME = @"campaignName";
NSString *const CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT = @"clipboardContents";
NSString *const CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP = @"embeddedMessagePersonalizationProperties";
NSString *const CALLBACK_KEY_EMBEDDED_MESSAGE_MAP = @"embeddedMessage";
NSString *const CALLBACK_KEY_EMBEDDED_MESSAGE_OBJECT = @"embeddedMessageObject";

// Event Names
NSString *const RESOURCES_UPDATED_EVENT_NAME = @"SwrveUserResourcesUpdated";
NSString *const MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = @"SwrveMessageCustomCallback";
NSString *const MESSAGE_CALLBACK_DISMISS_EVENT_NAME = @"SwrveMessageDismissCallback";
NSString *const MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = @"SwrveMessageClipboardCallback";
NSString *const EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME = @"SwrveEmbeddedMessageCallback";
NSString *const MODULE_NAME = @"SwrvePluginEventEmitter";

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
  return @[PUSH_EVENT_NAME, SILENT_PUSH_EVENT_NAME, RESOURCES_UPDATED_EVENT_NAME, MESSAGE_CALLBACK_CUSTOM_EVENT_NAME, MESSAGE_CALLBACK_DISMISS_EVENT_NAME, MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME, EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME];
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
                                                selector:@selector(onMessageCustomButtonCallback:)
                                                     name:MESSAGE_CALLBACK_CUSTOM_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onMessageDismissButtonCallback:)
                                                     name:MESSAGE_CALLBACK_DISMISS_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onMessageClipboardButtonCallback:)
                                                     name:MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME object:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                selector:@selector(onEmbeddedMessageCallback:)
                                                     name:EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME object:nil];

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

- (void)onMessageCustomButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *customAction = notification.userInfo[CALLBACK_KEY_CUSTOM_BUTTON_ACTION];
        NSString *campaignName = notification.userInfo[CALLBACK_KEY_CUSTOM_BUTTON_ACTION_CAMPAIGN_NAME];
        [self sendEventWithName:MESSAGE_CALLBACK_CUSTOM_EVENT_NAME body:@{CALLBACK_KEY_CUSTOM_BUTTON_ACTION: customAction, CALLBACK_KEY_CUSTOM_BUTTON_ACTION_CAMPAIGN_NAME: campaignName}];
    }
}

- (void)onMessageDismissButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *campaignSubject = notification.userInfo[CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT];
        NSString *buttonName = notification.userInfo[CALLBACK_KEY_DISMISS_BUTTON_NAME];
        NSString *campaignName = notification.userInfo[CALLBACK_KEY_DISMISS_CAMPAIGN_NAME];
        [self sendEventWithName:MESSAGE_CALLBACK_DISMISS_EVENT_NAME body:@{CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT: campaignSubject, CALLBACK_KEY_DISMISS_BUTTON_NAME: buttonName, CALLBACK_KEY_DISMISS_CAMPAIGN_NAME: campaignName}];
    }
}

- (void)onMessageClipboardButtonCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *clipboardContents = notification.userInfo[CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT];
        [self sendEventWithName:MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME body:@{CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT: clipboardContents}];
    }
}

- (void)onEmbeddedMessageCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        SwrveEmbeddedMessage *embeddedMessageObject = notification.userInfo[CALLBACK_KEY_EMBEDDED_MESSAGE_OBJECT];
        NSDictionary *personalizationProperties = notification.userInfo[CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP];
        NSDictionary *embeddedMessageDictionary = [self dictFromEmbeddedMessage:embeddedMessageObject];
        [self sendEventWithName:EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME body:@{CALLBACK_KEY_EMBEDDED_MESSAGE_MAP: embeddedMessageDictionary, CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP: personalizationProperties}];
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

- (NSDictionary*)dictFromEmbeddedMessage:(SwrveEmbeddedMessage *)embeddedMessage {
    NSString *messageType = (embeddedMessage.type == kSwrveEmbeddedDataTypeJson) ? @"json" : @"other";
    id objects[] = {
        [NSNumber numberWithUnsignedInteger:embeddedMessage.campaign.ID],
        [embeddedMessage messageID],
        [embeddedMessage priority],
        [embeddedMessage data],
        [embeddedMessage buttons],
        messageType
    };
    id keys[] = {@"campaignId", @"messageId", @"priority", @"data", @"buttons", @"type"};
    NSUInteger count = sizeof(objects) / sizeof(id);

    return [NSDictionary dictionaryWithObjects:objects forKeys:keys count:count];
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end

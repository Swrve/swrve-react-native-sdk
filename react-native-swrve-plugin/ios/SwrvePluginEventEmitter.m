#import "SwrvePluginEventEmitter.h"
#import <SwrveSDK/SwrveEmbeddedMessage.h>
#import <SwrveSDK/SwrveMessageDetails.h>
#import <SwrveSDK/SwrveButtonActions.h>
#import <SwrveSDK/SwrveMessageButtonDetails.h>


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
NSString *const CALLBACK_KEY_EMBEDDED_CONTROL_ACTION = @"embeddedMessageIsControl";

NSString *const CALLBACK_KEY_MESSAGE_DETAIL_MAP = @"messageDetail";
NSString *const CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON = @"messageDetailSelectedButton";
NSString *const CALLBACK_KEY_MESSAGE_DETAIL_ACTION = @"messageDetailAction";

NSString *const CALLBACK_KEY_DEEPLINK_ACTION_STRING = @"deeplinkDelegateActionString";

// Event Names
NSString *const RESOURCES_UPDATED_EVENT_NAME = @"SwrveUserResourcesUpdated";
NSString *const MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = @"SwrveMessageCustomCallback";
NSString *const MESSAGE_CALLBACK_DISMISS_EVENT_NAME = @"SwrveMessageDismissCallback";
NSString *const MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = @"SwrveMessageClipboardCallback";
NSString *const EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME = @"SwrveEmbeddedMessageCallback";
NSString *const EMBEDDED_CALLBACK_EVENT_NAME = @"SwrveEmbeddedMessageCallbackWithControlFlag";
NSString *const MESSAGE_DETAILS_DELEGATE_EVENT_NAME = @"SwrveMessageDetailsDelegate";
NSString *const DEEPLINK_DELEGATE_EVENT_NAME = @"SwrveMessageDeeplinkDelegate";

NSString *const MODULE_NAME = @"SwrvePluginEventEmitter";
RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
  return @[
    PUSH_EVENT_NAME,
    SILENT_PUSH_EVENT_NAME,
    RESOURCES_UPDATED_EVENT_NAME,
    MESSAGE_CALLBACK_CUSTOM_EVENT_NAME,
    MESSAGE_CALLBACK_DISMISS_EVENT_NAME,
    MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME,
    EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME,
    EMBEDDED_CALLBACK_EVENT_NAME,
    MESSAGE_DETAILS_DELEGATE_EVENT_NAME,
    DEEPLINK_DELEGATE_EVENT_NAME
    ];
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
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onEmbeddedCallback:)
                                                     name:EMBEDDED_CALLBACK_EVENT_NAME object:nil];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onMessageDelegate:)
                                                     name:MESSAGE_DETAILS_DELEGATE_EVENT_NAME object:nil];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onDeeplinkDelegate:)
                                                     name:DEEPLINK_DELEGATE_EVENT_NAME object:nil];
        
        
        
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
        SwrveEmbeddedMessage *embeddedMessageObject = notification.userInfo[CALLBACK_KEY_EMBEDDED_MESSAGE_MAP];
        NSDictionary *personalizationProperties = notification.userInfo[CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP];
        NSDictionary *embeddedMessageDictionary = [self dictFromEmbeddedMessage:embeddedMessageObject];
        [self sendEventWithName:EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME body:@{CALLBACK_KEY_EMBEDDED_MESSAGE_MAP: embeddedMessageDictionary, CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP: personalizationProperties}];
    }
}

- (void)onEmbeddedCallback:(NSNotification *)notification {
    if (self.hasListeners) {
        SwrveEmbeddedMessage *embeddedMessageObject = notification.userInfo[CALLBACK_KEY_EMBEDDED_MESSAGE_MAP];
        NSDictionary *personalizationProperties = notification.userInfo[CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP];
        NSDictionary *embeddedMessageDictionary = [self dictFromEmbeddedMessage:embeddedMessageObject];
        NSString *isControl = notification.userInfo[CALLBACK_KEY_EMBEDDED_CONTROL_ACTION];
                
        [self sendEventWithName:EMBEDDED_CALLBACK_EVENT_NAME body:@{
            CALLBACK_KEY_EMBEDDED_MESSAGE_MAP: embeddedMessageDictionary,
            CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP: personalizationProperties,
            CALLBACK_KEY_EMBEDDED_CONTROL_ACTION: isControl
        }];
    }
}

-(void)onMessageDelegate:(NSNotification *)notification {
    
    if (self.hasListeners) {
        NSString *messageAction = notification.userInfo[CALLBACK_KEY_MESSAGE_DETAIL_ACTION];
        
        SwrveMessageDetails *messageDetails = notification.userInfo[CALLBACK_KEY_MESSAGE_DETAIL_MAP];
        NSDictionary *messageDetailsDict = [self dictFromMessageDetailsObject: messageDetails];
        
        NSDictionary *selectedButtonDict = @{};
        if (notification.userInfo[CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON]) {
            SwrveMessageButtonDetails *selectedButton = notification.userInfo[CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON];
            selectedButtonDict = [self dictFromSelectedButtonObject: selectedButton];
        }
        
        [self sendEventWithName:MESSAGE_DETAILS_DELEGATE_EVENT_NAME body:@{
            CALLBACK_KEY_MESSAGE_DETAIL_ACTION: messageAction,
            CALLBACK_KEY_MESSAGE_DETAIL_MAP: messageDetailsDict,
            CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON: selectedButtonDict
        }];
    }
}

-(void)onDeeplinkDelegate:(NSNotification *)notification {
    if (self.hasListeners) {
        NSString *actionString = notification.userInfo[CALLBACK_KEY_DEEPLINK_ACTION_STRING];
        [self sendEventWithName:DEEPLINK_DELEGATE_EVENT_NAME body:@{
            CALLBACK_KEY_DEEPLINK_ACTION_STRING: actionString
        }];
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

- (NSDictionary *)dictFromEmbeddedMessage:(SwrveEmbeddedMessage *)embeddedMessage {

    NSString *messageType = @"";
    NSString *messageData = @"";

    if (embeddedMessage != nil) {
        messageType = (embeddedMessage.type == kSwrveEmbeddedDataTypeJson) ? @"json" : @"other";
    }
    
    if ([embeddedMessage data] != nil) {
        messageData = [embeddedMessage data];
    }
    
    id objects[] = {
        [NSNumber numberWithUnsignedInteger:embeddedMessage.campaign.ID],
        [embeddedMessage messageID],
        [embeddedMessage priority],
        messageData,
        [embeddedMessage buttons],
        messageType
    };
    id keys[] = {@"campaignId", @"messageId", @"priority", @"data", @"buttons", @"type"};
    NSUInteger count = sizeof(objects) / sizeof(id);

    return [NSDictionary dictionaryWithObjects:objects forKeys:keys count:count];
}

- (NSDictionary*)dictFromMessageDetailsObject:(SwrveMessageDetails *)messageDetails {

    NSMutableArray *buttons = [[NSMutableArray alloc] init];
    for (SwrveMessageButtonDetails *buttonDetails in messageDetails.buttons) {
        NSString *actionType = stringFromActionTypeEnum(buttonDetails.actionType);
        NSDictionary *dict = @{
            @"buttonName" : (buttonDetails.buttonName != nil) ? buttonDetails.buttonName : @"",
            @"buttonText" : (buttonDetails.buttonText != nil) ? buttonDetails.buttonText : @"",
            @"actionType" : actionType,
            @"actionString" : (buttonDetails.actionString != nil) ? buttonDetails.actionString : @"",
        };
        [buttons addObject:dict];
    }
    
    NSDictionary * messageDataDict = @{
        @"campaignSubject" : messageDetails.campaignSubject,
        @"campaignId": @(messageDetails.campaignId),
        @"variantId": @(messageDetails.variantId),
        @"messageName": messageDetails.messageName,
        @"buttons": buttons
    };

    return messageDataDict;
}

- (NSDictionary *)dictFromSelectedButtonObject:(SwrveMessageButtonDetails *)selectedButton {
    NSString *actionType = stringFromActionTypeEnum(selectedButton.actionType);
    NSDictionary *selectedButtonDictionary = (selectedButton == nil) ? @{} : @{
        @"buttonName" : (selectedButton.buttonName!= nil) ? selectedButton.buttonName : @"",
        @"buttonText" : (selectedButton.buttonText != nil) ? selectedButton.buttonText : @"",
        @"actionType" : actionType,
        @"actionString" : (selectedButton.actionString!= nil) ? selectedButton.actionString : @"",
    };
    return selectedButtonDictionary;
}

NSString * stringFromActionTypeEnum(SwrveActionType enumValue) {
    switch (enumValue) {
        case kSwrveActionDismiss:
            return @"Dismiss";
        case kSwrveActionCustom:
            return @"Custom";
        case kSwrveActionInstall:
            return @"Install";
        case kSwrveActionClipboard:
            return @"Clipboard";
        case kSwrveActionCapability:
            return @"Capability";
        case kSwrveActionPageLink:
            return @"PageLink";
        case kSwrveActionOpenSettings:
            return @"OpenSettings";
        case kSwrveActionOpenNotificationSettings:
            return @"OpenNotificationSettings";
        case kSwrveActionStartGeo:
            return @"StartGeo";
        default:
            return @"Unknown";
    }
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
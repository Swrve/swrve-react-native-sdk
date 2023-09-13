#import <Foundation/Foundation.h>

#import "SwrvePluginDelegateHandler.h"
#import "SwrvePluginEventEmitter.h"
#import "SwrvePlugin.h"
#import "SwrveCallbackStateManager.h"


@implementation SwrvePluginDelegateHandler

+ (instancetype)sharedInstance {
    static SwrvePluginDelegateHandler *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [SwrvePluginDelegateHandler new];
    });
    return sharedInstance;
}

- (void)onAction:(SwrveMessageAction)messageAction messageDetails:(SwrveMessageDetails *)messageDetails selectedButton:(SwrveMessageButtonDetails *)selectedButton {

    if (SwrveCallbackStateManager.sharedInstance.isListeningInAppMessage == NO) {
        switch (messageAction) {
            case SwrveActionDismiss:
                [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_DISMISS_EVENT_NAME
                                                                    object:self
                                                                  userInfo:@{
                    CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT: [messageDetails campaignSubject],
                    CALLBACK_KEY_DISMISS_BUTTON_NAME: [selectedButton buttonName],
                    CALLBACK_KEY_DISMISS_CAMPAIGN_NAME: [messageDetails messageName]
                }];
                
                break;
            case SwrveActionCustom:
                
                [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_CUSTOM_EVENT_NAME
                                                                    object:self
                                                                  userInfo:@{
                    CALLBACK_KEY_CUSTOM_BUTTON_ACTION: [selectedButton actionString],
                    CALLBACK_KEY_CUSTOM_BUTTON_ACTION_CAMPAIGN_NAME: [messageDetails messageName]
                }];
                
                break;
            case SwrveActionClipboard:
                
                [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME
                                                                    object:self
                                                                  userInfo:@{
                    CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT: [selectedButton actionString]
                    
                }];
            default:
                break;
        }
        return;
    }
    
    
    if (!selectedButton) {
        [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_DETAILS_DELEGATE_EVENT_NAME object:self userInfo: @{
            CALLBACK_KEY_MESSAGE_DETAIL_ACTION: stringFromMessageActionEnum(messageAction),
            CALLBACK_KEY_MESSAGE_DETAIL_MAP: messageDetails
        }];
        
    } else {
        [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_DETAILS_DELEGATE_EVENT_NAME object:self userInfo: @{
            CALLBACK_KEY_MESSAGE_DETAIL_ACTION: stringFromMessageActionEnum(messageAction),
            CALLBACK_KEY_MESSAGE_DETAIL_MAP: messageDetails,
            CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON: selectedButton
        }];
    }
        
}

- (void)handleDeeplink:(NSURL *)nsurl {
    if (SwrveCallbackStateManager.sharedInstance.isListeningDeeplink == NO) {
        [SwrvePlugin handleAction: [nsurl absoluteString]];
        return;
    } 
    
    [[NSNotificationCenter defaultCenter] postNotificationName: DEEPLINK_DELEGATE_EVENT_NAME object:self userInfo: @{
        CALLBACK_KEY_DEEPLINK_ACTION_STRING: [nsurl absoluteString]
    }];
}


// Helper method

NSString * stringFromMessageActionEnum(SwrveMessageAction enumValue) {
    switch (enumValue) {
        case SwrveImpression:
            return @"Impression";
        case SwrveActionCustom:
            return @"Custom";
        case SwrveActionDismiss:
            return @"Dismiss";
        case SwrveActionClipboard:
            return @"Clipboard";
        default:
            return @"Default";
    }
}

@end

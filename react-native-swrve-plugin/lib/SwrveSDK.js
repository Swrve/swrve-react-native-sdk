import { NativeModules, NativeEventEmitter } from 'react-native';
const { SwrvePlugin } = NativeModules;

class SwrveSDK {
    static PUSH_EVENT_NAME = 'PushNotification';
    static SILENT_PUSH_EVENT_NAME = 'SilentPushNotification';
    static PUSH_EVENT_PAYLOAD = 'PushEventPayload';
    static CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL = 'appStoreUrl';
    static CALLBACK_KEY_CUSTOM_BUTTON_ACTION = 'customAction';
    static CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT = 'campaignSubject';
    static CALLBACK_KEY_DISMISS_BUTTON_NAME = 'buttonName';
    static CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT = 'clipboardContents';

    static RESOURCES_UPDATED_EVENT_NAME = 'SwrveUserResourcesUpdated';
    static MESSAGE_CALLBACK_INSTALL_EVENT_NAME = 'SwrveMessageInstallCallback';
    static MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = 'SwrveMessageCustomCallback';
    static MESSAGE_CALLBACK_DISMISS_EVENT_NAME = 'SwrveMessageDismissCallback';
    static MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = 'SwrveMessageClipboardCallback';

    Notifications = {
        // Notification types
    };

    _eventEmitter;
    _eventHandlers = {};
    _pushListener = null;
    _silentPushListener = null;
    _userResourcesListener = null;
    _messageInstallButtonListener = null;
    _messageCustomButtonListener = null;
    _messageDismissButtonListener = null;
    _messageClipboardButtonListener = null;

    constructor(listener) {
        this._eventEmitter = new NativeEventEmitter(NativeModules.SwrvePluginEventEmitter);
    }

    // This method doesn't get passed to the native-side
    setListeners(listeners, pushListeners, messageListeners) {
        // Remove any pre-existing handlers in case this function has been called already
        if (this._eventHandlers.pushEventHandler) {
            this._eventHandlers.pushEventHandler.remove();
        }

        if (this._eventHandlers.silentPushEventHandler) {
            this._eventHandlers.silentPushEventHandler.remove();
        }

        if (this._eventHandlers.resourceUpdatedEventHandler) {
            this._eventHandlers.resourceUpdatedEventHandler.remove();
        }

        if (this._eventHandlers.installButtonEventHandler) {
            this._eventHandlers.installButtonEventHandler.remove();
        }

        if (this._eventHandlers.customButtonEventHandler) {
            this._eventHandlers.customButtonEventHandler.remove();
        }

        if (this._eventHandlers.dismissButtonEventHandler) {
            this._eventHandlers.dismissButtonEventHandler.remove();
        }

        if (this._eventHandlers.clipboardButtonEventHandler) {
            this._eventHandlers.clipboardButtonEventHandler.remove();
        }

        if (listeners) {
            // SwrveListeners
            this._userResourcesListener = listeners.userResourcesUpdatedListener;
            if (listeners.userResourcesUpdatedListener) {
                this._eventHandlers.resourceUpdatedEventHandler = this._eventEmitter.addListener(
                    SwrveSDK.RESOURCES_UPDATED_EVENT_NAME,
                    (event) => {
                        if (this._pushListener) {
                            console.log('SwrveSDK - pushListener - push');
                            this._userResourcesListener();
                        }
                    }
                );
            }
        }

        if (messageListeners) {
            // MessageListeners
            this._messageInstallButtonListener = messageListeners.installButtonPressedListener;
            this._messageCustomButtonListener = messageListeners.customButtonPressedListener;
            this._messageDismissButtonListener = messageListeners.dismissButtonPressedListener;
            this._messageClipboardButtonListener = messageListeners.clipboardButtonPressedListener;

            if (messageListeners.installButtonPressedListener) {
                this._eventHandlers.installButtonEventHandler = this._eventEmitter.addListener(
                    SwrveSDK.MESSAGE_CALLBACK_INSTALL_EVENT_NAME,
                    (event) => {
                        if (this._messageInstallButtonListener) {
                            console.log('SwrveSDK - installButtonListener');
                            this._messageInstallButtonListener(
                                event[SwrveSDK.CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL]
                            );
                        }
                    }
                );
                SwrvePlugin.listeningInstall();
            }

            if (messageListeners.customButtonPressedListener) {
                this._eventHandlers.customButtonEventHandler = this._eventEmitter.addListener(
                    SwrveSDK.MESSAGE_CALLBACK_CUSTOM_EVENT_NAME,
                    (event) => {
                        if (this._messageCustomButtonListener) {
                            console.log('SwrveSDK - customButtonListener');
                            this._messageCustomButtonListener(event[SwrveSDK.CALLBACK_KEY_CUSTOM_BUTTON_ACTION]);
                        }
                    }
                );
                // disable the default action for the custom button response if it's set here
                SwrvePlugin.listeningCustom();
            }

            if (messageListeners.dismissButtonPressedListener) {
                this._eventHandlers.dismissButtonEventHandler = this._eventEmitter.addListener(
                    SwrveSDK.MESSAGE_CALLBACK_DISMISS_EVENT_NAME,
                    (event) => {
                        if (this._messageDismissButtonListener) {
                            console.log('SwrveSDK - dismissButtonListener');
                            this._messageDismissButtonListener(
                                event[SwrveSDK.CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT],
                                event[SwrveSDK.CALLBACK_KEY_DISMISS_BUTTON_NAME]
                            );
                        }
                    }
                );
            }

            if (messageListeners.clipboardButtonPressedListener) {
                this._eventHandlers.clipboardButtonPressedListener = this._eventEmitter.addListener(
                    SwrveSDK.MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME,
                    (event) => {
                        if (this._messageClipboardButtonListener) {
                            console.log('SwrveSDK - clipboardButtonListener');
                            this._messageClipboardButtonListener(
                                event[SwrveSDK.CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT]
                            );
                        }
                    }
                );
            }
        }

        if (pushListeners) {
            // PushListeners
            this._pushListener = pushListeners.pushListener;
            this._silentPushListener = pushListeners.silentPushListener;

            if (pushListeners.pushListener) {
                this._eventHandlers.pushEventHandler = this._eventEmitter.addListener(
                    SwrveSDK.PUSH_EVENT_NAME,
                    (event) => {
                        if (this._pushListener) {
                            console.log('SwrveSDK - pushListener - push');
                            this._pushListener(event[SwrveSDK.PUSH_EVENT_PAYLOAD]);
                        }
                    }
                );
            }

            if (pushListeners.silentPushListener) {
                this._eventHandlers.silentPushEventHandler = this._eventEmitter.addListener(
                    SwrveSDK.SILENT_PUSH_EVENT_NAME,
                    (event) => {
                        if (this._silentPushListener) {
                            console.log('SwrveSDK - pushListener - silent push');
                            this._silentPushListener(event[SwrveSDK.PUSH_EVENT_PAYLOAD]);
                        }
                    }
                );
            }
        }

        SwrvePlugin.startedListening();
    }

    start(userId) {
        if (userId && userId !== '') {
            SwrvePlugin.start(userId);
        } else {
            SwrvePlugin.start(null);
        }
    }

    async identify(userIdentity) {
        return SwrvePlugin.identify(userIdentity);
    }

    userUpdate(attributes) {
        SwrvePlugin.userUpdate(attributes);
    }

    userUpdateDate(name, date) {
        SwrvePlugin.userUpdateDate(name, date.toISOString());
    }

    event(eventName, eventPayload = null) {
        SwrvePlugin.event(eventName, eventPayload);
    }

    sendQueuedEvents() {
        SwrvePlugin.sendQueuedEvents();
    }

    refreshCampaignsAndResources() {
        SwrvePlugin.refreshCampaignsAndResources();
    }

    currencyGiven(currency, quantity) {
        SwrvePlugin.currencyGiven(currency, quantity);
    }

    purchase(itemName, currency, quantity, cost) {
        SwrvePlugin.purchase(itemName, currency, quantity, cost);
    }

    unvalidatedIap(localCost, localCurrency, productId, quantity) {
        SwrvePlugin.unvalidatedIap(localCost, localCurrency, productId, quantity);
    }

    unvalidatedIapWithReward(localCost, localCurrency, productId, quantity, reward) {
        SwrvePlugin.unvalidatedIapWithReward(localCost, localCurrency, productId, quantity, reward);
    }

    async isStarted() {
        return SwrvePlugin.isStarted();
    }

    async getApiKey() {
        return SwrvePlugin.getApiKey();
    }

    async getUserId() {
        return SwrvePlugin.getUserId();
    }

    async getExternalUserId() {
        return SwrvePlugin.getExternalUserId();
    }

    async getUserResources() {
        return SwrvePlugin.getUserResources();
    }

    async getUserResourcesDiff() {
        return SwrvePlugin.getUserResourcesDiff();
    }

    async getRealTimeUserProperties() {
        return SwrvePlugin.getRealTimeUserProperties();
    }

    async getMessageCenterCampaigns(personalization) {
        return SwrvePlugin.getMessageCenterCampaigns(personalization);
    }

    async showMessageCenterCampaign(campaignId, personalization) {
        return SwrvePlugin.showMessageCenterCampaign(campaignId, personalization);
    }

    async removeMessageCenterCampaign(campaignId) {
        return SwrvePlugin.removeMessageCenterCampaign(campaignId);
    }

    async markMessageCenterCampaignAsSeen(campaignId) {
        return SwrvePlugin.markMessageCenterCampaignAsSeen(campaignId);
    }
}

module.exports = new SwrveSDK();

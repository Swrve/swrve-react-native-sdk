import { NativeModules, NativeEventEmitter } from 'react-native';
import type { EventSubscription } from 'react-native';

const { SwrvePlugin } = NativeModules;

// Type definitions
export type PushListener = (payload: string) => void;
export type UserResourcesListener = () => void;

/**
 * Campaign state information
 */
export interface SwrveCampaignState {
  /** Campaign status */
  status: string;
  /** Number of impressions (optional) */
  impressions?: number;
  /** Next show time timestamp (optional) */
  next?: number;
}

/**
 * Embedded message object returned from Swrve SDK.
 * Represents an embedded message configured in the Swrve dashboard.
 */
export interface SwrveEmbeddedMessage {
  /** Unique message identifier */
  ID: number;
  /** Unique campaign identifier */
  campaignId: number;
  /** Campaign name from Swrve dashboard */
  name: string;
  /** JSON string containing the embedded message data/payload */
  data: string;
  /** Raw JSON string data */
  dataRaw: string;
  /** Campaign priority (higher number = higher priority) */
  priority: number;
  /** Campaign type: "json" or "other" */
  type: 'json' | 'other';
  /** Whether this is a control message */
  control: boolean;
  /** Campaign download date (unix timestamp in seconds) */
  downloadDate: number;
  /** Campaign start date (unix timestamp in seconds) */
  dateStart: number;
  /** Campaign end date (unix timestamp in seconds) */
  dateEnd: number;
  /** Campaign state information */
  state: SwrveCampaignState;
  /** Array of button names in the message */
  buttons: string[];
}

/**
 * Message center details for campaigns displayed in message center UI
 */
export interface SwrveMessageCenterDetails {
  /** Message subject line */
  subject?: string;
  /** Message description/body text */
  description?: string;
  /** Accessibility text for the message image */
  imageAccessibilityText?: string;
  /** Remote URL of the message image */
  imageURL?: string;
  /** SHA hash of the message image */
  imageSha?: string;
  /** Local file path to cached image (platform-specific format) */
  image?: string;
}

/**
 * Message Center campaign object returned from Swrve SDK.
 * Represents an in-app message campaign configured for message center display.
 */
export interface SwrveMessageCenterCampaign {
  /** Unique campaign identifier */
  ID: number;
  /** Campaign name from Swrve dashboard */
  name: string;
  /** Maximum number of times this campaign can be shown */
  maxImpressions: number;
  /** Campaign priority (higher number = higher priority) */
  priority: number;
  /** Campaign download date (unix timestamp in seconds) */
  downloadDate: number;
  /** Campaign start date (unix timestamp in seconds) */
  dateStart: number;
  /** Campaign end date (unix timestamp in seconds) */
  dateEnd: number;
  /** Whether this campaign is configured for message center */
  messageCenter: boolean;
  /** Campaign state information */
  state: SwrveCampaignState;
  /** Optional message center display details */
  messageCenterDetails?: SwrveMessageCenterDetails;
}

export type EmbeddedCampaignListener = (
  embeddedMessage: SwrveEmbeddedMessage,
  personalizationProperties: Record<string, string>,
  isControl: boolean
) => void;

/**
 * Message action types triggered by in-app message interactions.
 * These correspond to user actions on in-app messages.
 */
export type SwrveMessageAction =
  | 'Impression'
  | 'Custom'
  | 'Dismiss'
  | 'Clipboard';

/**
 * Button action types available for in-app message buttons.
 * Defines what happens when a user taps a button.
 */
export type SwrveMessageActionType =
  | 'Dismiss'
  | 'Custom'
  | 'Install'
  | 'Clipboard'
  | 'Capability'
  | 'PageLink'
  | 'OpenAppSettings'
  | 'OpenNotificationSettings'
  | 'StartGeo'
  | 'Unknown';

/**
 * Button details for in-app message buttons.
 * Contains information about a single button in an in-app message.
 */
export interface SwrveMessageButtonDetails {
  /** Internal button identifier */
  buttonName: string;
  /** Display text shown on the button */
  buttonText: string;
  /** Type of action this button performs */
  actionType: SwrveMessageActionType;
  /** Action payload (URL, deep link, etc.) */
  actionString: string;
}

/**
 * In-app message details provided to the listener callback.
 * Contains campaign information and button configurations.
 */
export interface SwrveMessageDetails {
  /** Campaign subject line */
  campaignSubject: string;
  /** Campaign identifier */
  campaignId: number;
  /** Variant identifier for A/B testing */
  variantId: number;
  /** Message name/identifier */
  messageName: string;
  /** Array of buttons in the message */
  buttons: SwrveMessageButtonDetails[];
}

export type InAppMessageListener = (
  action: SwrveMessageAction,
  message: SwrveMessageDetails,
  selectedButton?: SwrveMessageButtonDetails
) => void;
export type DeeplinkListener = (actionString: string) => void;
export type PushInboxUpdatedListener = () => void;

/**
 * @deprecated Use Record<string, string> instead. Will be removed in v8+.
 */
export interface StringMap {
  [key: string]: string;
}

export interface SwrvePushListeners {
  pushListener?: PushListener;
  silentPushListener?: PushListener;
}

export interface SwrveListeners {
  userResourcesUpdatedListener?: UserResourcesListener;
}

export interface SwrveEmbeddedCampaignListener {
  embeddedCampaignListener?: EmbeddedCampaignListener;
}

export interface SwrveInAppMessageListener {
  inAppMessageListener?: InAppMessageListener;
}

export interface SwrveDeepLinkListener {
  deeplinkListener?: DeeplinkListener;
}

export interface IapRewardItems {
  name: string;
  amount: number;
}

export interface IapRewardCurrencies {
  name: string;
  amount: number;
}

export interface IapReward {
  items: IapRewardItems[];
  currencies: IapRewardCurrencies[];
}

/**
 * Represents a message in the Swrve push inbox.
 */
export interface SwrvePushInboxMessage {
  /** The push campaign message id */
  messageId: number;
  /** The push campaign variant id */
  variantId: number;
  /** The expiry date of the push message. After this date, the message will no longer be returned from APIs */
  endDate: number;
  /** The date the push notification was sent */
  sentDate: number;
  /** The JSON string containing customer-specific data */
  customerJson: string;
  /** The state of the message (READ or UNREAD) */
  state: SwrvePushInboxMessageState;
}

/**
 * Represents the result of a Swrve push inbox operation.
 *
 * This is used by both push inbox listener callbacks and async push inbox APIs
 * (such as engage, read, and delete).
 */
export interface SwrvePushInboxResult {
  /** Result code indicating the outcome of the operation */
  resultCode: SwrvePushInboxResultCode;
  /** Error message providing additional details in case of failure */
  errorMessage: string;
  /** HTTP response code associated with the operation, if applicable */
  httpResponseCode: number;
}

/**
 * Push inbox message state indicating whether a message has been read.
 */
export const enum SwrvePushInboxMessageState {
  /** Message has been read */
  READ = 'READ',
  /** Message has not been read */
  UNREAD = 'UNREAD',
}

/**
 * Result codes for push inbox operations.
 */
export const enum SwrvePushInboxResultCode {
  /** Success result code indicating the operation was successful */
  SUCCESS = 'SUCCESS',
  /** Error result code indicating an unknown error occurred */
  ERROR_UNKNOWN = 'ERROR_UNKNOWN',
  /** Error result code indicating an error occurred */
  ERROR = 'ERROR',
}

/**
 * Listener interface for push inbox update events.
 * Used with setPushInboxUpdateListener() to be notified when push inbox messages are loaded or changed.
 */
export interface SwrvePushInboxUpdateListener {
  pushInboxUpdateListener?: PushInboxUpdatedListener;
}

/**
 * Listener interface for push inbox operation completion callbacks.
 * Provides callback when push inbox operations (engage/read/delete) complete.
 */
export interface SwrvePushInboxListener {
  /** Called when a push inbox operation completes with the message ID and result */
  onComplete?: (messageId: number, result: SwrvePushInboxResult) => void;
}

/**
 * Result codes for refresh content operations.
 */
export const enum SwrveRefreshContentResultCode {
  /** Success result code indicating the operation was successful */
  SUCCESS = 'SUCCESS',
  /** Error result code indicating an unknown error occurred */
  ERROR_UNKNOWN = 'ERROR_UNKNOWN',
  /** Error result code indicating an error occurred */
  ERROR = 'ERROR',
}

/**
 * Result of a refresh content operation.
 * Contains the result code, error message, and HTTP response code.
 */
export interface SwrveRefreshContentResult {
  /** Result code indicating the outcome of the operation */
  resultCode: SwrveRefreshContentResultCode;
  /** Error message providing additional details in case of failure */
  errorMessage: string;
  /** HTTP response code associated with the operation, if applicable */
  httpResponseCode: number;
}

/**
 * Listener interface for refresh content completion callbacks.
 * Used with refreshContent() to be notified when content refresh completes.
 */
export interface SwrveRefreshContentListener {
  /** Called when refresh operation completes (success or error) */
  onComplete: (result: SwrveRefreshContentResult) => void;
}

class SwrveSDK {
  static readonly PUSH_EVENT_NAME = 'PushNotification';
  static readonly SILENT_PUSH_EVENT_NAME = 'SilentPushNotification';
  static readonly PUSH_EVENT_PAYLOAD = 'PushEventPayload';
  static readonly CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP =
    'embeddedMessagePersonalizationProperties';
  static readonly CALLBACK_KEY_EMBEDDED_MESSAGE_MAP = 'embeddedMessage';
  static readonly CALLBACK_KEY_EMBEDDED_CONTROL_ACTION =
    'embeddedMessageIsControl';

  static readonly CALLBACK_KEY_MESSAGE_DETAIL_MAP = 'messageDetail';
  static readonly CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON =
    'messageDetailSelectedButton';
  static readonly CALLBACK_KEY_MESSAGE_DETAIL_ACTION = 'messageDetailAction';

  static readonly CALLBACK_KEY_DEEPLINK_ACTION_STRING =
    'deeplinkDelegateActionString';

  static readonly RESOURCES_UPDATED_EVENT_NAME = 'SwrveUserResourcesUpdated';
  static readonly EMBEDDED_CALLBACK_EVENT_NAME = 'SwrveEmbeddedCallback';
  static readonly MESSAGE_DETAILS_DELEGATE_EVENT_NAME =
    'SwrveMessageDetailsDelegate';
  static readonly DEEPLINK_DELEGATE_EVENT_NAME = 'SwrveMessageDeeplinkDelegate';
  static readonly PUSHINBOX_UPDATE_EVENT_NAME = 'SwrvePushInboxUpdate';

  Notifications = {
    // Notification types
  };

  private _eventEmitter: NativeEventEmitter;
  private _eventHandlers: Record<string, EventSubscription> = {};
  private _pushListener: PushListener | null = null;
  private _silentPushListener: PushListener | null = null;
  private _userResourcesListener: UserResourcesListener | null = null;
  private _embeddedCampaignListener: EmbeddedCampaignListener | null = null;
  private _inAppMessageListener: InAppMessageListener | null = null;
  private _deeplinkListener: DeeplinkListener | null = null;
  private _pushInboxUpdateListener: PushInboxUpdatedListener | null = null;

  constructor() {
    this._eventEmitter = new NativeEventEmitter(
      NativeModules.SwrvePluginEventEmitter
    );

    if (!this.isNewArchitectureEnabled()) {
      console.warn(
        '[SwrveSDK] DEPRECATION WARNING: ' +
          'You are using the React Native legacy architecture. ' +
          'SwrveSDK support for the legacy architecture will be removed in a future version. ' +
          'Please migrate to the New Architecture.'
      );
    }
  }

  private isNewArchitectureEnabled(): boolean {
    return (
      typeof global !== 'undefined' &&
      typeof (global as any).nativeFabricUIManager !== 'undefined'
    );
  }

  /**
   * Set push notification and user resources listeners.
   * This method registers callbacks for push notifications (standard and silent) and resource updates.
   * Any previously registered handlers are automatically removed and replaced when calling this method.
   *
   * @param listeners - Optional object containing userResourcesUpdatedListener callback
   * @param pushListeners - Optional object containing pushListener and/or silentPushListener callbacks
   * @example
   * ```typescript
   * SwrveSDK.setListeners(
   *   { userResourcesUpdatedListener: () => console.log('Resources updated') },
   *   {
   *     pushListener: (payload) => console.log('Push:', payload),
   *     silentPushListener: (payload) => console.log('Silent push:', payload)
   *   }
   * );
   * ```
   */
  // This method doesn't get passed to the native-side
  setListeners(
    listeners?: SwrveListeners,
    pushListeners?: SwrvePushListeners
  ): void {
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

    if (listeners) {
      // SwrveListeners
      this._userResourcesListener =
        listeners.userResourcesUpdatedListener ?? null;
      if (listeners.userResourcesUpdatedListener) {
        this._eventHandlers.resourceUpdatedEventHandler =
          this._eventEmitter.addListener(
            SwrveSDK.RESOURCES_UPDATED_EVENT_NAME,
            (_event: any) => {
              if (this._userResourcesListener) {
                console.log('SwrveSDK - userResourcesUpdatedListener');
                this._userResourcesListener();
              }
            }
          );
      }
    }

    if (pushListeners) {
      // PushListeners
      this._pushListener = pushListeners.pushListener ?? null;
      this._silentPushListener = pushListeners.silentPushListener ?? null;

      if (pushListeners.pushListener) {
        this._eventHandlers.pushEventHandler = this._eventEmitter.addListener(
          SwrveSDK.PUSH_EVENT_NAME,
          (event: any) => {
            if (this._pushListener) {
              console.log('SwrveSDK - pushListener - push');
              this._pushListener(event[SwrveSDK.PUSH_EVENT_PAYLOAD]);
            }
          }
        );
      }

      if (pushListeners.silentPushListener) {
        this._eventHandlers.silentPushEventHandler =
          this._eventEmitter.addListener(
            SwrveSDK.SILENT_PUSH_EVENT_NAME,
            (event: any) => {
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

  /**
   * Set the embedded campaign listener.
   * This listener is called when an embedded message campaign is triggered for display.
   * The callback receives the embedded message, personalization properties, and control flag.
   *
   * @param embeddedCampaignListeners - Optional object containing embeddedCampaignListener callback
   * @example
   * ```typescript
   * SwrveSDK.setEmbeddedCampaignListener({
   *   embeddedCampaignListener: (message, properties, isControl) => {
   *     console.log('Embedded:', message.name, isControl);
   *   }
   * });
   * ```
   */
  setEmbeddedCampaignListener(
    embeddedCampaignListeners?: SwrveEmbeddedCampaignListener
  ): void {
    if (this._eventHandlers.embeddedCampaignListener) {
      this._eventHandlers.embeddedCampaignListener.remove();
    }

    if (embeddedCampaignListeners) {
      this._embeddedCampaignListener =
        embeddedCampaignListeners.embeddedCampaignListener ?? null;

      if (embeddedCampaignListeners.embeddedCampaignListener) {
        this._eventHandlers.embeddedCampaignListener =
          this._eventEmitter.addListener(
            SwrveSDK.EMBEDDED_CALLBACK_EVENT_NAME,
            (event: any) => {
              console.log('SwrveSDK - embeddedCampaignListener');
              this._embeddedCampaignListener?.(
                event[SwrveSDK.CALLBACK_KEY_EMBEDDED_MESSAGE_MAP],
                event[
                  SwrveSDK
                    .CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP
                ],
                event[SwrveSDK.CALLBACK_KEY_EMBEDDED_CONTROL_ACTION]
              );
            }
          );
      }

      SwrvePlugin.startedEmbeddedListening();
    }
  }

  /**
   * Set the in-app message listener.
   * This listener is called when an in-app message is displayed or interacted with.
   * The callback receives the action type, message details, and optionally the selected button.
   *
   * @param inAppMessageListeners - Optional object containing inAppMessageListener callback
   * @example
   * ```typescript
   * SwrveSDK.setInAppMessageListener({
   *   inAppMessageListener: (action, message, button) => {
   *     console.log('In-app action:', action, message.campaignSubject);
   *   }
   * });
   * ```
   */
  setInAppMessageListener(
    inAppMessageListeners?: SwrveInAppMessageListener
  ): void {
    if (this._eventHandlers.inAppMessageListener) {
      this._eventHandlers.inAppMessageListener.remove();
    }

    if (inAppMessageListeners) {
      this._inAppMessageListener =
        inAppMessageListeners.inAppMessageListener ?? null;
      if (inAppMessageListeners.inAppMessageListener) {
        this._eventHandlers.inAppMessageListener =
          this._eventEmitter.addListener(
            SwrveSDK.MESSAGE_DETAILS_DELEGATE_EVENT_NAME,
            (event: any) => {
              console.log('SwrveSDK - inAppMessageListener');
              this._inAppMessageListener?.(
                event[SwrveSDK.CALLBACK_KEY_MESSAGE_DETAIL_ACTION],
                event[SwrveSDK.CALLBACK_KEY_MESSAGE_DETAIL_MAP],
                event[SwrveSDK.CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON]
              );
            }
          );
      }
    }
  }

  /**
   * Set the deeplink listener.
   * This listener is called when a deeplink is triggered from an in-app message or push notification.
   * The callback receives the deeplink action string (URL or custom scheme).
   *
   * @param deeplinkListeners - Optional object containing deeplinkListener callback
   * @example
   * ```typescript
   * SwrveSDK.setDeeplinkListener({
   *   deeplinkListener: (url) => {
   *     console.log('Deeplink:', url);
   *     // Handle navigation
   *   }
   * });
   * ```
   */
  setDeeplinkListener(deeplinkListeners?: SwrveDeepLinkListener): void {
    if (this._eventHandlers.deeplinkListener) {
      this._eventHandlers.deeplinkListener.remove();
    }

    if (deeplinkListeners !== null && deeplinkListeners !== undefined) {
      this._deeplinkListener = deeplinkListeners.deeplinkListener ?? null;

      if (deeplinkListeners.deeplinkListener) {
        SwrvePlugin.listeningDeeplink();

        this._eventHandlers.deeplinkListener = this._eventEmitter.addListener(
          SwrveSDK.DEEPLINK_DELEGATE_EVENT_NAME,
          (event: any) => {
            console.log('SwrveSDK - deeplinkListener');
            this._deeplinkListener?.(
              event[SwrveSDK.CALLBACK_KEY_DEEPLINK_ACTION_STRING]
            );
          }
        );
      }
    }
  }

  /**
   * Set the push inbox update listener.
   * This listener is called when push inbox messages have been initially loaded and each time
   * messages are updated/changed.
   *
   * @param pushInboxUpdateListeners - Optional object containing pushInboxUpdateListener callback
   * @example
   * ```typescript
   * SwrveSDK.setPushInboxUpdateListener({
   *   pushInboxUpdateListener: () => {
   *     console.log('Push inbox updated');
   *     // Refresh UI
   *   }
   * });
   * ```
   */
  setPushInboxUpdateListener(
    pushInboxUpdateListeners?: SwrvePushInboxUpdateListener
  ): void {
    if (this._eventHandlers.pushInboxUpdateListener) {
      this._eventHandlers.pushInboxUpdateListener.remove();
    }

    if (
      pushInboxUpdateListeners !== null &&
      pushInboxUpdateListeners !== undefined
    ) {
      this._pushInboxUpdateListener =
        pushInboxUpdateListeners.pushInboxUpdateListener ?? null;

      if (pushInboxUpdateListeners.pushInboxUpdateListener) {
        SwrvePlugin.listeningPushInboxUpdate();

        this._eventHandlers.pushInboxUpdateListener =
          this._eventEmitter.addListener(
            SwrveSDK.PUSHINBOX_UPDATE_EVENT_NAME,
            (_event: any) => {
              if (this._pushInboxUpdateListener) {
                console.log('SwrveSDK - pushInboxUpdateListener');
                this._pushInboxUpdateListener();
              }
            }
          );
      }
    }
  }

  /**
   * Start the SDK if stopped or in Managed mode.
   * Tracking will begin using the userId passed in, or the last user ID if not provided.
   * Can be called multiple times to switch the current userId. A new session is started if not
   * already started or if started with a different userId.
   * The SDK will remain started until stopTracking() is called.
   *
   * @param userId - Optional user ID to start SDK with. If not provided, uses last user or auto-generates one.
   * @example
   * ```typescript
   * SwrveSDK.start(); // Start with last/auto user
   * SwrveSDK.start('user123'); // Start with specific user
   * ```
   */
  start(userId?: string): void {
    if (userId && userId !== '') {
      SwrvePlugin.start(userId);
    } else {
      SwrvePlugin.start(null);
    }
  }

  /**
   * Identify users such that they can be tracked and targeted safely across multiple devices, platforms and channels.
   * Personally identifiable information should not be used. An error may be returned if such information is
   * submitted as the userID (e.g., email, phone number).
   *
   * @param externalUserId - ID that uniquely identifies your user. Personally identifiable information should not be used.
   * @returns Promise resolving to the Swrve internal user ID, or rejects if identification fails
   * @example
   * ```typescript
   * try {
   *   const swrveId = await SwrveSDK.identify('user123');
   *   console.log('Identified as:', swrveId);
   * } catch (error) {
   *   console.error('Identification failed:', error);
   * }
   * ```
   */
  async identify(externalUserId: string): Promise<string> {
    return SwrvePlugin.identify(externalUserId);
  }

  /**
   * Send a group of custom user properties to Swrve.
   * This event would typically be added after session start and at points where properties
   * of your users change (e.g., level up, setting changes).
   *
   * @param attributes - Key-value pairs of user properties. Typical values would be
   *                     level, referrer, coin balance, etc.
   * @example
   * ```typescript
   * SwrveSDK.userUpdate({
   *   level: '10',
   *   vip_status: 'gold',
   *   total_purchases: '5'
   * });
   * ```
   */
  userUpdate(attributes: Record<string, string>): void {
    SwrvePlugin.userUpdate(attributes);
  }

  /**
   * Send a single Date-based custom user property to Swrve.
   * This event would typically be added after session start and at points where date properties
   * of your users change (e.g., registration date, last login).
   *
   * @param name - The identifier for the user property
   * @param date - The Date value to associate with this property
   * @example
   * ```typescript
   * SwrveSDK.userUpdateDate('registration_date', new Date());
   * ```
   */
  userUpdateDate(name: string, date: Date): void {
    SwrvePlugin.userUpdateDate(name, date.toISOString());
  }

  /**
   * Send a named custom event with optional payload.
   * The character '.' is used as grouping syntax in event names.
   *
   * @param eventName - The name of the event
   * @param eventPayload - Optional key-value pairs to be sent with this event. Typically this would be
   *                       information about the event rather than about the user.
   * @example
   * ```typescript
   * SwrveSDK.event('level_completed');
   * SwrveSDK.event('purchase', { item: 'sword', currency: 'gold' });
   * ```
   */
  event(eventName: string, eventPayload?: Record<string, string> | null): void {
    SwrvePlugin.event(eventName, eventPayload ?? null);
  }

  /**
   * Send all queued events to the Swrve servers.
   * If any events cannot be sent they will be re-queued and sent again later.
   * This is called automatically by the SDK, but can be called manually to force immediate sending.
   */
  sendQueuedEvents(): void {
    SwrvePlugin.sendQueuedEvents();
  }

  /**
   * Refresh content from server, which includes campaigns, resources, push inbox, real time user properties.
   * Content is refreshed automatically but you can use this API to refresh manually or if
   * autoDownloadCampaignsAndResources is false.
   * This is an asynchronous operation and the listener will be called when the operation is complete.
   *
   * @param listener - Optional listener with onComplete callback
   * @returns Promise that resolves on success or rejects with error details on failure
   * @example
   * ```typescript
   * // With callback
   * try {
   *   await SwrveSDK.refreshContent({
   *     onComplete: (result) => {
   *       if (result.resultCode === 'SUCCESS') {
   *         console.log('Content refreshed successfully');
   *       } else {
   *         console.error('Refresh failed:', result.errorMessage);
   *       }
   *     }
   *   });
   * } catch (error) {
   *   console.error('Failed:', error);
   * }
   *
   * // Without callback
   * try {
   *   await SwrveSDK.refreshContent();
   *   console.log('Content refresh called successfully');
   * } catch (error) {
   *   console.error('Failed:', error);
   * }
   * ```
   */
  async refreshContent(listener?: SwrveRefreshContentListener): Promise<void> {
    try {
      const result: SwrveRefreshContentResult =
        await SwrvePlugin.refreshContent();

      // Call listener with result (success or error)
      listener?.onComplete?.(result);

      // Throw error if not successful
      if (result.resultCode !== SwrveRefreshContentResultCode.SUCCESS) {
        throw new Error(
          `Refresh content failed: ${result.errorMessage} (HTTP ${result.httpResponseCode})`
        );
      }
    } catch (error) {
      // Re-throw any errors (including network errors from native layer)
      throw error;
    }
  }

  /**
   * Refresh campaigns and resources from the Swrve server.
   * This is called automatically if autoDownloadCampaignsAndResources is enabled (default).
   * You can call this manually to force an immediate refresh.
   * This is an asynchronous operation that will update campaigns and resources when complete.
   * @deprecated Use refreshContent() instead. Will be removed in v8+.
   */
  refreshCampaignsAndResources(): void {
    SwrvePlugin.refreshCampaignsAndResources();
  }

  /**
   * Send a currency_given event when the user has been gifted in-app currency by the app itself.
   * The currency must be declared in the Swrve dashboard before calling this function.
   *
   * @param currency - The name of the in-app currency that the player was rewarded with
   * @param quantity - The amount of in-app currency that the player was rewarded with
   * @example
   * ```typescript
   * SwrveSDK.currencyGiven('gold', 100);
   * ```
   */
  currencyGiven(currency: string, quantity: number): void {
    SwrvePlugin.currencyGiven(currency, quantity);
  }

  /**
   * Send a purchase event when virtual goods are purchased in your app.
   * The currency specified must be one of the currencies known to Swrve that are specified on the Swrve dashboard.
   *
   * @param itemName - Unique name of the purchased item
   * @param currency - Currency in Swrve to be used for this purchase. This currency must be declared
   *                   in the Swrve dashboard before calling this function.
   * @param quantity - Number of items purchased
   * @param cost - Cost of the item in units of currency
   * @example
   * ```typescript
   * SwrveSDK.purchase('sword', 'gold', 1, 500);
   * ```
   */
  purchase(
    itemName: string,
    currency: string,
    quantity: number,
    cost: number
  ): void {
    SwrvePlugin.purchase(itemName, currency, quantity, cost);
  }

  /**
   * Send an unvalidated in-app purchase (IAP) event for real money transactions.
   * Use this for purchases where a single item was purchased (no in-app currency or bundle).
   * This does not validate the receipt data server-side.
   *
   * @param localCost - The price (in real money) of the product that was purchased. This is the per-product
   *                    price, not the total transaction price. Must be >= 0.
   * @param localCurrency - Real world currency used for this transaction. Must be an ISO currency code (e.g., "USD").
   * @param productId - Unique product identifier for the item bought. This should match the Swrve resource name.
   * @param quantity - Quantity purchased. Must be > 0.
   * @example
   * ```typescript
   * SwrveSDK.unvalidatedIap(0.99, 'USD', 'com.myapp.coins100', 1);
   * ```
   */
  unvalidatedIap(
    localCost: number,
    localCurrency: string,
    productId: string,
    quantity: number
  ): void {
    SwrvePlugin.unvalidatedIap(localCost, localCurrency, productId, quantity);
  }

  /**
   * Send an unvalidated in-app purchase (IAP) event with rewards for real money transactions.
   * Use this when in-app currency was purchased or when multiple items and/or currencies were purchased.
   * This does not validate the receipt data server-side.
   *
   * @param localCost - The price (in real money) of the product. This is the per-product price. Must be >= 0.
   * @param localCurrency - Real world currency used for this transaction. Must be an ISO currency code (e.g., "USD").
   * @param productId - Unique product identifier for the item bought. This should match the Swrve resource name.
   * @param quantity - Quantity purchased. Must be > 0.
   * @param reward - IapReward object containing any in-app currency and/or additional items included in this purchase.
   * @example
   * ```typescript
   * const reward = {
   *   items: [{ name: 'sword', amount: 1 }],
   *   currencies: [{ name: 'gold', amount: 100 }]
   * };
   * SwrveSDK.unvalidatedIapWithReward(4.99, 'USD', 'com.myapp.starter_pack', 1, reward);
   * ```
   */
  unvalidatedIapWithReward(
    localCost: number,
    localCurrency: string,
    productId: string,
    quantity: number,
    reward: IapReward
  ): void {
    SwrvePlugin.unvalidatedIapWithReward(
      localCost,
      localCurrency,
      productId,
      quantity,
      reward
    );
  }

  /**
   * Check if the SDK has been started and is currently tracking.
   *
   * @returns Promise resolving to true if SDK is started and tracking, false otherwise
   */
  async isStarted(): Promise<boolean> {
    return SwrvePlugin.isStarted();
  }

  /**
   * Get the current API key used to initialize the SDK.
   *
   * @returns Promise resolving to the API key string
   */
  async getApiKey(): Promise<string> {
    return SwrvePlugin.getApiKey();
  }

  /**
   * Get the current user ID.
   * The user ID is either the one provided at initialization, or an auto-generated UUID.
   *
   * @returns Promise resolving to the current user ID
   */
  async getUserId(): Promise<string> {
    return SwrvePlugin.getUserId();
  }

  /**
   * Get the current external user ID set via the identify() method.
   * Returns empty string if identify() has not been called.
   *
   * @returns Promise resolving to the external user ID
   */
  async getExternalUserId(): Promise<string> {
    return SwrvePlugin.getExternalUserId();
  }

  /**
   * Get the resources for a user, including any modifications from active A/B tests.
   * This function returns immediately with the already cached A/B test data.
   *
   * @returns Promise resolving to an object keyed by resource name, where each value contains
   *          the resource attributes with A/B test modifications applied
   */
  async getUserResources(): Promise<any> {
    return SwrvePlugin.getUserResources();
  }

  /**
   * Get the user resource differences that should be applied to items based on the A/B test
   * the user is involved in. This returns the modifications/diffs, not the full resource values.
   *
   * @returns Promise resolving to an object containing resource differences from A/B tests
   *          (e.g., { oldResourcesValues, newResourcesValues, ... })
   */
  async getUserResourcesDiff(): Promise<any> {
    return SwrvePlugin.getUserResourcesDiff();
  }

  /**
   * Get the dictionary of real-time user properties that have been retrieved along with
   * campaigns and resources for this user.
   * This function returns immediately with the already present data.
   *
   * @returns Promise resolving to a dictionary of real-time user properties
   */
  async getRealTimeUserProperties(): Promise<any> {
    return SwrvePlugin.getRealTimeUserProperties();
  }

  /**
   * Retrieves all Message Center campaigns with optional personalization.
   *
   * Message Center campaigns are in-app messages configured to appear in a
   * dedicated message inbox/center UI within your app.
   *
   * @param personalization - Optional personalization properties to customize campaign content
   * @returns Promise resolving to an array of message center campaigns
   * @example
   * ```typescript
   * const campaigns = await SwrveSDK.getMessageCenterCampaigns();
   * campaigns.forEach(c => console.log(c.name, c.messageCenterDetails?.subject));
   * ```
   */
  async getMessageCenterCampaigns(
    personalization?: Record<string, string>
  ): Promise<SwrveMessageCenterCampaign[]> {
    return SwrvePlugin.getMessageCenterCampaigns(personalization);
  }

  /**
   * Retrieves a specific Message Center campaign by ID with optional personalization.
   *
   * @param campaignId - The unique campaign identifier
   * @param personalization - Optional personalization properties to customize campaign content
   * @returns Promise resolving to the campaign, or rejects if not found
   */
  async getMessageCenterCampaign(
    campaignId: number,
    personalization?: Record<string, string>
  ): Promise<SwrveMessageCenterCampaign> {
    return SwrvePlugin.getMessageCenterCampaign(campaignId, personalization);
  }

  /**
   * Retrieves all embedded campaigns from the message center with optional personalization.
   * @param personalization - Optional personalization properties to customize campaign content
   * @returns Promise resolving to an array of embedded campaigns
   * @example
   * const campaigns = await SwrveSDK.getEmbeddedMessageCenterCampaigns({
   *   user_name: 'John',
   *   user_tier: 'gold'
   * });
   */
  async getEmbeddedMessageCenterCampaigns(
    personalization?: Record<string, string>
  ): Promise<SwrveEmbeddedMessage[]> {
    return SwrvePlugin.getEmbeddedMessageCenterCampaigns(personalization);
  }

  /**
   * Personalizes text content with provided personalization properties.
   * @param text - Template text containing personalization placeholders (e.g., "Hello ${user_name}")
   * @param personalizationProperties - Key-value pairs for personalization substitution
   * @returns Promise resolving to the personalized text string
   * @example
   * const text = await SwrveSDK.getPersonalizedText(
   *   'Hello ${user_name}',
   *   { user_name: 'John' }
   * );
   */
  async getPersonalizedText(
    text: string,
    personalizationProperties: Record<string, string>
  ): Promise<string> {
    return SwrvePlugin.getPersonalizedText(text, personalizationProperties);
  }

  /**
   * Retrieves personalized embedded message data for a specific campaign.
   * @param campaignId - The unique identifier of the embedded campaign
   * @param personalizationProperties - Key-value pairs for personalizing the message data
   * @returns Promise resolving to a JSON string containing the personalized message data
   * @example
   * const data = await SwrveSDK.getPersonalizedEmbeddedMessageData(
   *   12345,
   *   { user_name: 'John', user_tier: 'gold' }
   * );
   * const parsedData = JSON.parse(data);
   */
  async getPersonalizedEmbeddedMessageData(
    campaignId: number,
    personalizationProperties: Record<string, string>
  ): Promise<string> {
    return SwrvePlugin.getPersonalizedEmbeddedMessageData(
      campaignId,
      personalizationProperties
    );
  }

  /**
   * Displays a Message Center campaign to the user.
   *
   * @param campaignId - The unique campaign identifier to display
   * @param personalization - Optional personalization properties to customize campaign content
   * @returns Promise resolving when display is initiated (void)
   */
  async showMessageCenterCampaign(
    campaignId: number,
    personalization?: Record<string, string>
  ): Promise<void> {
    await SwrvePlugin.showMessageCenterCampaign(campaignId, personalization);
  }

  /**
   * Remove a Message Center campaign.
   * It won't be returned anymore by getMessageCenterCampaigns() methods.
   *
   * @param campaignId - The ID of the campaign to remove
   * @returns Promise resolving when the campaign has been removed
   */
  async removeMessageCenterCampaign(campaignId: number): Promise<void> {
    await SwrvePlugin.removeMessageCenterCampaign(campaignId);
  }

  /**
   * Mark a Message Center campaign as seen.
   * This is done automatically by Swrve but you can call this if you are rendering the messages on your own.
   *
   * @param campaignId - The ID of the campaign to mark as seen
   * @returns Promise resolving when the campaign has been marked as seen
   */
  async markMessageCenterCampaignAsSeen(campaignId: number): Promise<void> {
    await SwrvePlugin.markMessageCenterCampaignAsSeen(campaignId);
  }

  /**
   * Mark an embedded message campaign as seen.
   * This is done automatically by Swrve but you can call this if you are rendering the messages on your own.
   *
   * @param campaignId - The ID of the embedded campaign to mark as seen
   * @returns Promise resolving when the campaign has been marked as seen
   */
  async markEmbeddedMessageCampaignAsSeen(campaignId: number): Promise<void> {
    await SwrvePlugin.markEmbeddedMessageCampaignAsSeen(campaignId);
  }

  /**
   * Send an impression event for an embedded control campaign.
   * This function should be called by your implementation to update the campaign information
   * and send the appropriate data to Swrve.
   *
   * @param campaignId - The ID of the embedded control campaign that has been processed
   * @returns Promise resolving when the impression event has been sent
   */
  async embeddedControlMessageImpressionEvent(
    campaignId: number
  ): Promise<void> {
    await SwrvePlugin.embeddedControlMessageImpressionEvent(campaignId);
  }

  /**
   * Process an embedded message button engagement event.
   * This function should be called by your implementation to inform Swrve of a button press.
   *
   * @param campaignId - The ID of the embedded message campaign
   * @param button - The name of the button that was pressed
   * @returns Promise resolving when the button press event has been sent
   */
  async markEmbeddedMessageButtonAsPressed(
    campaignId: number,
    button: string
  ): Promise<void> {
    await SwrvePlugin.markEmbeddedMessageButtonAsPressed(campaignId, button);
  }

  /**
   * Stop the SDK from tracking.
   * The SDK will remain stopped until a start() method is called.
   *
   * @returns Promise resolving when tracking has stopped
   */
  async stopTracking(): Promise<void> {
    await SwrvePlugin.stopTracking();
  }

  /**
   * Get a list of eligible push inbox messages.
   * Returns all push inbox messages that are currently available for this user.
   *
   * @returns Promise resolving to an array of SwrvePushInboxMessage objects
   * @example
   * ```typescript
   * const messages = await SwrveSDK.getPushInboxMessages();
   * messages.forEach(msg => console.log(msg.messageId, msg.state));
   * ```
   */
  async getPushInboxMessages(): Promise<SwrvePushInboxMessage[]> {
    return SwrvePlugin.getPushInboxMessages();
  }

  /**
   * Mark a Push Inbox message as read and send an engagement event.
   * This is an asynchronous operation. Check the returned result object for success or failure.
   *
   * @param messageId - The messageId of the SwrvePushInboxMessage to update as read and engaged
   * @returns Promise resolving to a SwrvePushInboxResult indicating success or failure
   * @example
   * ```typescript
   * const result = await SwrveSDK.engagePushInboxMessage(12345);
   * if (result.resultCode === SwrvePushInboxResultCode.SUCCESS) {
   *   console.log('Message engaged successfully');
   * }
   * ```
   */
  async engagePushInboxMessage(
    messageId: number
  ): Promise<SwrvePushInboxResult> {
    return SwrvePlugin.engagePushInboxMessage(messageId);
  }

  /**
   * Mark a Push Inbox message as read.
   * This is an asynchronous operation. Check the returned result object for success or failure.
   *
   * @param messageId - The messageId of the SwrvePushInboxMessage to update as read
   * @returns Promise resolving to a SwrvePushInboxResult indicating success or failure
   * @example
   * ```typescript
   * const result = await SwrveSDK.readPushInboxMessage(12345);
   * if (result.resultCode === SwrvePushInboxResultCode.SUCCESS) {
   *   console.log('Message marked as read');
   * }
   * ```
   */
  async readPushInboxMessage(messageId: number): Promise<SwrvePushInboxResult> {
    return SwrvePlugin.readPushInboxMessage(messageId);
  }

  /**
   * Delete a Push Inbox message.
   * This is an asynchronous operation. Check the returned result object for success or failure.
   *
   * @param messageId - The messageId of the SwrvePushInboxMessage to delete
   * @returns Promise resolving to a SwrvePushInboxResult indicating success or failure
   * @example
   * ```typescript
   * const result = await SwrveSDK.deletePushInboxMessage(12345);
   * if (result.resultCode === SwrvePushInboxResultCode.SUCCESS) {
   *   console.log('Message deleted');
   * }
   * ```
   */
  async deletePushInboxMessage(
    messageId: number
  ): Promise<SwrvePushInboxResult> {
    return SwrvePlugin.deletePushInboxMessage(messageId);
  }
}

export default new SwrveSDK();

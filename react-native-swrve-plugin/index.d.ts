export type Stack = 'us' | 'eu';
export type InterfaceOrientation = 'portrait' | 'landscape' | 'both';
export type InitMode = 'auto' | 'managed';
export type AndroidPushImportance = 'none' | 'min' | 'low' | 'default' | 'max';
export type PushListener = (payload: string) => void;
export type UserResourcesListener = () => void;
export type MessageCustomButtonPressedListener = (action: string) => void;
export type MessageDismissButtonPressedListener = (campaignSubject: string, buttonName: string) => void;
export type MessageClipboardButtonPressedListener = (processedText: string) => void;
export type EmbeddedMessageCampaignListener = (embeddedMessage: Map, personalizationProperties?: Map) => void;

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

export interface SwrveMessageListeners {
	customButtonPressedListener?: MessageCustomButtonPressedListener;
	dismissButtonPressedListener?: MessageDismissButtonPressedListener;
	clipboardButtonPressedListener?: MessageClipboardButtonPressedListener;
}

export interface SwrveEmbeddedMessageListeners {
	embeddedMessageCampiagnListener?: EmbeddedMessageCampaignListener
}

/// Push and Resource and Campaign Listeners
export function setListeners(
	SwrveListeners?: SwrveListeners,
	swrvePushListeners?: SwrvePushListeners,
	SwrveMessageListeners?: SwrveMessageListeners,
	SwrveEmbeddedMessageListeners?: SwrveEmbeddedMessageListeners
);

export function start(userId: string);

/// User management and data
export function identify(userIdentity: string): Promise<string>;

export function userUpdate(attributes: StringMap);

export function userUpdateDate(name: string, date: Date);

/// Events
export function event(eventName: string, payload?: StringMap);

export function sendQueuedEvents();

export function refreshCampaignsAndResources();

/// In-app purchase and virtual currency

/**
 * Give some currency
 * @param currency
 * @param quantity int
 */
export function currencyGiven(currency: string, quantity: number);

// itemName is a string
// currency is a string
// quantity is an int
// cost is a double on Cordova, int in Android
export function purchase(itemName: string, currency: string, quantity: number, cost: number);

// localCost is a double
// localCurrency is a string
// productId is a string
// quantity is an int
export function unvalidatedIap(localCost: number, localCurrency: string, productId: string, quantity: number);

// localCost is a double
// localCurrency is a string
// productId is a string
// quantity is an int
// reward is an IapReward
export interface IapRewardItems {
	name: string;
	amount: number;
}
export interface IapRewardCurrencies {
	name: string;
	amount: number;
}
export interface IapReward {
	items: [IapRewardItems];
	currencies: [IapRewardCurrencies];
}

export function unvalidatedIapWithReward(
	localCost: number,
	localCurrency: string,
	productId: string,
	quantity: number,
	reward: IapReward
);

export function getApiKey(): Promise<string>;

export function getUserId(): Promise<string>;

export function getExternalUserId(): Promise<string>;

export function isStarted(): Promise<boolean>;

export function getUserResources(): Promise<Map>;

export function getUserResourcesDiff(): Promise<Map>;

export function getRealTimeUserProperties(): Promise<Map>;

export function getMessageCenterCampaigns(personalization?: Map): Promise<Array<Map>>;

export function getPersonalizedText(text: string, personalizationProperties: Map): Promise<string>;

export function getPersonalizedEmbeddedMessageData(campaignId: number, personalizationProperties: Map): Promise<string>;

export function showMessageCenterCampaign(campaignId: number, personalization?: Map);

export function removeMessageCenterCampaign(campaignId: number);

export function markMessageCenterCampaignAsSeen(campaignId: number);

export function markEmbeddedMessageCampaignAsSeen(campaignId: number);

export function markEmbeddedMessageButtonAsPressed(campaignId: number, button: String);

export function stopTracking();
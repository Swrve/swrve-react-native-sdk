import { NativeModules, NativeEventEmitter } from 'react-native';
import SwrveSDK from '../SwrveSDK';
import type { StringMap } from '../SwrveSDK';

jest.mock('react-native', () => {
  const addListener = jest.fn();
  const SwrvePlugin = {
    start: jest.fn(),
    identify: jest.fn().mockResolvedValue('identified-user'),
    userUpdate: jest.fn(),
    userUpdateDate: jest.fn(),
    event: jest.fn(),
    sendQueuedEvents: jest.fn(),
    refreshCampaignsAndResources: jest.fn(),
    refreshContent: jest.fn().mockResolvedValue({
      resultCode: 'SUCCESS',
      errorMessage: '',
      httpResponseCode: 200,
    }),
    currencyGiven: jest.fn(),
    purchase: jest.fn(),
    unvalidatedIap: jest.fn(),
    unvalidatedIapWithReward: jest.fn(),
    isStarted: jest.fn().mockResolvedValue(true),
    getApiKey: jest.fn().mockResolvedValue('test-api-key'),
    getUserId: jest.fn().mockResolvedValue('user-123'),
    getExternalUserId: jest.fn().mockResolvedValue('external-123'),
    getUserResources: jest.fn().mockResolvedValue({ resources: [] }),
    getUserResourcesDiff: jest.fn().mockResolvedValue({ diff: [] }),
    getRealTimeUserProperties: jest.fn().mockResolvedValue({ properties: {} }),
    getPushInboxMessages: jest.fn().mockResolvedValue([]),
    engagePushInboxMessage: jest
      .fn()
      .mockResolvedValue({ resultCode: 'SUCCESS' }),
    readPushInboxMessage: jest
      .fn()
      .mockResolvedValue({ resultCode: 'SUCCESS' }),
    deletePushInboxMessage: jest
      .fn()
      .mockResolvedValue({ resultCode: 'SUCCESS' }),
    getMessageCenterCampaigns: jest.fn().mockResolvedValue([
      {
        ID: 123,
        name: 'Welcome Campaign',
        maxImpressions: 5,
        priority: 9999,
        downloadDate: 1640000000,
        dateStart: 1640000000,
        dateEnd: 1650000000,
        messageCenter: true,
        state: {
          status: 'Unseen',
          impressions: 0,
          next: 0,
        },
        messageCenterDetails: {
          subject: 'Welcome to our app!',
          description: 'Get started with these tips',
          imageAccessibilityText: 'Welcome banner',
          imageURL: 'https://example.com/welcome.png',
          imageSha: 'abc123',
          image: 'file:///path/to/welcome.png',
        },
      },
    ]),
    getMessageCenterCampaign: jest.fn().mockResolvedValue({
      ID: 123,
      name: 'Welcome Campaign',
      maxImpressions: 5,
      priority: 9999,
      downloadDate: 1640000000,
      dateStart: 1640000000,
      dateEnd: 1650000000,
      messageCenter: true,
      state: {
        status: 'Unseen',
        impressions: 0,
        next: 0,
      },
      messageCenterDetails: {
        subject: 'Welcome to our app!',
        description: 'Get started with these tips',
        imageAccessibilityText: 'Welcome banner',
        imageURL: 'https://example.com/welcome.png',
        imageSha: 'abc123',
        image: 'file:///path/to/welcome.png',
      },
    }),
    getEmbeddedMessageCenterCampaigns: jest.fn().mockResolvedValue([
      {
        ID: 456,
        campaignId: 123,
        name: 'Test Campaign',
        data: '{"key":"value"}',
        dataRaw: '{"key":"value"}',
        priority: 10,
        type: 'json',
        control: false,
        downloadDate: 1640000000,
        dateStart: 1640000000,
        dateEnd: 1650000000,
        state: { status: 'Unseen' },
        buttons: ['button1', 'button2'],
      },
    ]),
    getPersonalizedText: jest.fn().mockResolvedValue('personalized text'),
    getPersonalizedEmbeddedMessageData: jest
      .fn()
      .mockResolvedValue('embedded data'),
    showMessageCenterCampaign: jest.fn().mockResolvedValue(undefined),
    removeMessageCenterCampaign: jest.fn().mockResolvedValue(undefined),
    markMessageCenterCampaignAsSeen: jest.fn().mockResolvedValue(undefined),
    markEmbeddedMessageCampaignAsSeen: jest.fn().mockResolvedValue(undefined),
    embeddedControlMessageImpressionEvent: jest
      .fn()
      .mockResolvedValue(undefined),
    markEmbeddedMessageButtonAsPressed: jest.fn().mockResolvedValue(undefined),
    stopTracking: jest.fn().mockResolvedValue(undefined),
    startedListening: jest.fn(),
    startedEmbeddedListening: jest.fn(),
    listeningDeeplink: jest.fn(),
    listeningPushInboxUpdate: jest.fn(),
  };

  function MockNativeEventEmitter() {
    return { addListener };
  }
  (MockNativeEventEmitter as any).addListener = addListener;

  return {
    NativeModules: {
      SwrvePluginEventEmitter: {},
      SwrvePlugin,
    },
    NativeEventEmitter: MockNativeEventEmitter,
  };
});

describe('SwrveSDK', () => {
  let consoleWarnSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
    (NativeEventEmitter as any).addListener.mockReset();
    (NativeEventEmitter as any).addListener.mockReturnValue({
      remove: jest.fn(),
    });
    // Reset global to simulate old architecture by default
    delete (global as any).nativeFabricUIManager;
  });

  afterEach(() => {
    consoleWarnSpy.mockRestore();
  });

  describe('Architecture Detection', () => {
    it('shows deprecation warning when old architecture is detected', () => {
      // Reset module to trigger constructor
      jest.isolateModules(() => {
        require('../SwrveSDK');
      });

      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('[SwrveSDK] DEPRECATION WARNING:')
      );
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining(
          'You are using the React Native legacy architecture'
        )
      );
    });

    it('does not show deprecation warning when new architecture is detected', () => {
      // Mock new architecture by setting nativeFabricUIManager
      (global as any).nativeFabricUIManager = {};

      // Reset module to trigger constructor
      jest.isolateModules(() => {
        require('../SwrveSDK');
      });

      expect(consoleWarnSpy).not.toHaveBeenCalled();

      // Cleanup
      delete (global as any).nativeFabricUIManager;
    });
  });

  describe('SDK Management', () => {
    it('forwards start calls to the native SwrvePlugin', () => {
      SwrveSDK.start('user-123');
      expect(NativeModules.SwrvePlugin.start).toHaveBeenCalledWith('user-123');
    });

    it('calls start with null when no userId is provided', () => {
      SwrveSDK.start();
      expect(NativeModules.SwrvePlugin.start).toHaveBeenCalledWith(null);
    });

    it('calls start with null when an empty userId is provided', () => {
      SwrveSDK.start('');
      expect(NativeModules.SwrvePlugin.start).toHaveBeenCalledWith(null);
    });

    it('forwards identify calls and returns the native result', async () => {
      const result = await SwrveSDK.identify('external-user');
      expect(NativeModules.SwrvePlugin.identify).toHaveBeenCalledWith(
        'external-user'
      );
      expect(result).toBe('identified-user');
    });

    it('calls isStarted and returns the result', async () => {
      const result = await SwrveSDK.isStarted();
      expect(NativeModules.SwrvePlugin.isStarted).toHaveBeenCalled();
      expect(result).toBe(true);
    });

    it('calls getApiKey and returns the result', async () => {
      const result = await SwrveSDK.getApiKey();
      expect(NativeModules.SwrvePlugin.getApiKey).toHaveBeenCalled();
      expect(result).toBe('test-api-key');
    });

    it('calls getUserId and returns the result', async () => {
      const result = await SwrveSDK.getUserId();
      expect(NativeModules.SwrvePlugin.getUserId).toHaveBeenCalled();
      expect(result).toBe('user-123');
    });

    it('calls getExternalUserId and returns the result', async () => {
      const result = await SwrveSDK.getExternalUserId();
      expect(NativeModules.SwrvePlugin.getExternalUserId).toHaveBeenCalled();
      expect(result).toBe('external-123');
    });

    it('calls stopTracking', async () => {
      const result = await SwrveSDK.stopTracking();
      expect(NativeModules.SwrvePlugin.stopTracking).toHaveBeenCalled();
      expect(result).toBeUndefined();
    });
  });

  describe('User Properties and Events', () => {
    it('calls userUpdate with attributes', () => {
      const attributes = { name: 'John', age: '30' };
      SwrveSDK.userUpdate(attributes);
      expect(NativeModules.SwrvePlugin.userUpdate).toHaveBeenCalledWith(
        attributes
      );
    });

    it('calls userUpdateDate with formatted ISO date', () => {
      const date = new Date('2026-02-26T12:00:00Z');
      SwrveSDK.userUpdateDate('lastLogin', date);
      expect(NativeModules.SwrvePlugin.userUpdateDate).toHaveBeenCalledWith(
        'lastLogin',
        '2026-02-26T12:00:00.000Z'
      );
    });

    it('calls event with eventName and payload', () => {
      const payload = { level: '5', score: '1000' };
      SwrveSDK.event('level_complete', payload);
      expect(NativeModules.SwrvePlugin.event).toHaveBeenCalledWith(
        'level_complete',
        payload
      );
    });

    it('calls event with multi-field string payload', () => {
      const payload = {
        level: '5',
        completed: 'true',
        mode: 'hard',
        score: '1000.5',
      };
      SwrveSDK.event('game_event', payload);
      expect(NativeModules.SwrvePlugin.event).toHaveBeenCalledWith(
        'game_event',
        payload
      );
    });

    it('accepts deprecated StringMap type for backward compatibility', () => {
      const attrs: StringMap = { name: 'John', role: 'admin' };
      SwrveSDK.userUpdate(attrs);
      expect(NativeModules.SwrvePlugin.userUpdate).toHaveBeenCalledWith(attrs);

      const payload: StringMap = { key: 'value', action: 'test' };
      SwrveSDK.event('deprecated_test', payload);
      expect(NativeModules.SwrvePlugin.event).toHaveBeenCalledWith(
        'deprecated_test',
        payload
      );
    });

    it('calls event with eventName and null payload', () => {
      SwrveSDK.event('button_click', null);
      expect(NativeModules.SwrvePlugin.event).toHaveBeenCalledWith(
        'button_click',
        null
      );
    });

    it('calls event with eventName only (default null payload)', () => {
      SwrveSDK.event('app_launched');
      expect(NativeModules.SwrvePlugin.event).toHaveBeenCalledWith(
        'app_launched',
        null
      );
    });

    it('calls sendQueuedEvents', () => {
      SwrveSDK.sendQueuedEvents();
      expect(NativeModules.SwrvePlugin.sendQueuedEvents).toHaveBeenCalled();
    });

    it('calls refreshCampaignsAndResources', () => {
      SwrveSDK.refreshCampaignsAndResources();
      expect(
        NativeModules.SwrvePlugin.refreshCampaignsAndResources
      ).toHaveBeenCalled();
    });

    it('calls refreshContent and returns success', async () => {
      const onComplete = jest.fn();

      await SwrveSDK.refreshContent({
        onComplete,
      });

      expect(NativeModules.SwrvePlugin.refreshContent).toHaveBeenCalled();
      expect(onComplete).toHaveBeenCalledWith({
        resultCode: 'SUCCESS',
        errorMessage: '',
        httpResponseCode: 200,
      });
    });

    it('calls refreshContent without listener', async () => {
      await SwrveSDK.refreshContent();

      expect(NativeModules.SwrvePlugin.refreshContent).toHaveBeenCalled();
    });

    it('calls refreshContent and handles error', async () => {
      const onComplete = jest.fn();
      const errorResult = {
        resultCode: 'ERROR',
        errorMessage: 'Network error',
        httpResponseCode: 500,
      };

      NativeModules.SwrvePlugin.refreshContent.mockResolvedValueOnce(
        errorResult
      );

      await expect(
        SwrveSDK.refreshContent({
          onComplete,
        })
      ).rejects.toThrow('Refresh content failed: Network error (HTTP 500)');

      expect(NativeModules.SwrvePlugin.refreshContent).toHaveBeenCalled();
      expect(onComplete).toHaveBeenCalledWith(errorResult);
    });

    it('calls refreshContent and handles ERROR_UNKNOWN', async () => {
      const errorResult = {
        resultCode: 'ERROR_UNKNOWN',
        errorMessage: 'Unknown error',
        httpResponseCode: 0,
      };

      NativeModules.SwrvePlugin.refreshContent.mockResolvedValueOnce(
        errorResult
      );

      await expect(SwrveSDK.refreshContent()).rejects.toThrow(
        'Refresh content failed: Unknown error (HTTP 0)'
      );

      expect(NativeModules.SwrvePlugin.refreshContent).toHaveBeenCalled();
    });

    it('calls refreshContent and handles exception without calling listener', async () => {
      const onComplete = jest.fn();
      const error = new Error('Exception occurred');

      NativeModules.SwrvePlugin.refreshContent.mockRejectedValueOnce(error);

      await expect(
        SwrveSDK.refreshContent({
          onComplete,
        })
      ).rejects.toThrow('Exception occurred');

      expect(NativeModules.SwrvePlugin.refreshContent).toHaveBeenCalled();
      expect(onComplete).not.toHaveBeenCalled();
    });
  });

  describe('User Resources and Properties', () => {
    it('calls getUserResources and returns the native result', async () => {
      const result = await SwrveSDK.getUserResources();
      expect(NativeModules.SwrvePlugin.getUserResources).toHaveBeenCalled();
      expect(result).toEqual({ resources: [] });
    });

    it('calls getUserResourcesDiff and returns the native result', async () => {
      const result = await SwrveSDK.getUserResourcesDiff();
      expect(NativeModules.SwrvePlugin.getUserResourcesDiff).toHaveBeenCalled();
      expect(result).toEqual({ diff: [] });
    });

    it('calls getRealTimeUserProperties and returns the native result', async () => {
      const result = await SwrveSDK.getRealTimeUserProperties();
      expect(
        NativeModules.SwrvePlugin.getRealTimeUserProperties
      ).toHaveBeenCalled();
      expect(result).toEqual({ properties: {} });
    });
  });

  describe('In-App Purchases', () => {
    it('calls currencyGiven with currency and quantity', () => {
      SwrveSDK.currencyGiven('gold', 100);
      expect(NativeModules.SwrvePlugin.currencyGiven).toHaveBeenCalledWith(
        'gold',
        100
      );
    });

    it('calls purchase with all parameters', () => {
      SwrveSDK.purchase('sword', 'gold', 1, 50);
      expect(NativeModules.SwrvePlugin.purchase).toHaveBeenCalledWith(
        'sword',
        'gold',
        1,
        50
      );
    });

    it('calls unvalidatedIap with all parameters', () => {
      SwrveSDK.unvalidatedIap(1.99, 'USD', 'com.example.sku', 1);
      expect(NativeModules.SwrvePlugin.unvalidatedIap).toHaveBeenCalledWith(
        1.99,
        'USD',
        'com.example.sku',
        1
      );
    });

    it('calls unvalidatedIapWithReward with reward object', () => {
      const reward = {
        items: [{ name: 'gems', amount: 100 }],
        currencies: [{ name: 'gold', amount: 500 }],
      };
      SwrveSDK.unvalidatedIapWithReward(
        2.99,
        'USD',
        'com.example.premium',
        1,
        reward
      );
      expect(
        NativeModules.SwrvePlugin.unvalidatedIapWithReward
      ).toHaveBeenCalledWith(2.99, 'USD', 'com.example.premium', 1, reward);
    });
  });

  describe('Push Inbox', () => {
    it('calls getPushInboxMessages and returns the result', async () => {
      const result = await SwrveSDK.getPushInboxMessages();
      expect(NativeModules.SwrvePlugin.getPushInboxMessages).toHaveBeenCalled();
      expect(result).toEqual([]);
    });

    it('calls engagePushInboxMessage and returns the result', async () => {
      const result = await SwrveSDK.engagePushInboxMessage(42);
      expect(
        NativeModules.SwrvePlugin.engagePushInboxMessage
      ).toHaveBeenCalledWith(42);
      expect(result).toEqual({ resultCode: 'SUCCESS' });
    });

    it('calls readPushInboxMessage and returns the result', async () => {
      const result = await SwrveSDK.readPushInboxMessage(123);
      expect(
        NativeModules.SwrvePlugin.readPushInboxMessage
      ).toHaveBeenCalledWith(123);
      expect(result).toEqual({ resultCode: 'SUCCESS' });
    });

    it('calls deletePushInboxMessage and returns the result', async () => {
      const result = await SwrveSDK.deletePushInboxMessage(456);
      expect(
        NativeModules.SwrvePlugin.deletePushInboxMessage
      ).toHaveBeenCalledWith(456);
      expect(result).toEqual({ resultCode: 'SUCCESS' });
    });

    it('can use messageId from getPushInboxMessages in other methods (type consistency)', async () => {
      const mockMessage = {
        messageId: 789,
        variantId: 101,
        sentDate: 1709737200,
        endDate: 1709823600,
        customerJson: '{"title":"Test"}',
        state: 'UNREAD',
      };
      NativeModules.SwrvePlugin.getPushInboxMessages.mockResolvedValueOnce([
        mockMessage,
      ]);

      const messages = await SwrveSDK.getPushInboxMessages();
      expect(messages).toHaveLength(1);
      expect(messages[0].messageId).toBe(789);

      // This validates messageId type is consistent (number → number)
      await SwrveSDK.engagePushInboxMessage(messages[0].messageId);
      expect(
        NativeModules.SwrvePlugin.engagePushInboxMessage
      ).toHaveBeenCalledWith(789);

      await SwrveSDK.readPushInboxMessage(messages[0].messageId);
      expect(
        NativeModules.SwrvePlugin.readPushInboxMessage
      ).toHaveBeenCalledWith(789);

      await SwrveSDK.deletePushInboxMessage(messages[0].messageId);
      expect(
        NativeModules.SwrvePlugin.deletePushInboxMessage
      ).toHaveBeenCalledWith(789);
    });
  });

  describe('Message Center', () => {
    it('calls getMessageCenterCampaigns and returns the result', async () => {
      const result = await SwrveSDK.getMessageCenterCampaigns();
      expect(
        NativeModules.SwrvePlugin.getMessageCenterCampaigns
      ).toHaveBeenCalledWith(undefined);
      expect(result).toHaveLength(1);
      const campaign = result[0]!;

      // Verify all SwrveMessageCenterCampaign properties
      expect(campaign.ID).toBe(123);
      expect(campaign.name).toBe('Welcome Campaign');
      expect(campaign.maxImpressions).toBe(5);
      expect(campaign.priority).toBe(9999);
      expect(campaign.downloadDate).toBe(1640000000);
      expect(campaign.dateStart).toBe(1640000000);
      expect(campaign.dateEnd).toBe(1650000000);
      expect(campaign.messageCenter).toBe(true);

      // Verify SwrveCampaignState object
      expect(campaign.state).toBeDefined();
      expect(campaign.state.status).toBe('Unseen');
      expect(campaign.state.impressions).toBe(0);
      expect(campaign.state.next).toBe(0);

      // Verify SwrveMessageCenterDetails properties
      expect(campaign.messageCenterDetails).toBeDefined();
      expect(campaign.messageCenterDetails?.subject).toBe(
        'Welcome to our app!'
      );
      expect(campaign.messageCenterDetails?.description).toBe(
        'Get started with these tips'
      );
      expect(campaign.messageCenterDetails?.imageAccessibilityText).toBe(
        'Welcome banner'
      );
      expect(campaign.messageCenterDetails?.imageURL).toBe(
        'https://example.com/welcome.png'
      );
      expect(campaign.messageCenterDetails?.imageSha).toBe('abc123');
      expect(campaign.messageCenterDetails?.image).toBe(
        'file:///path/to/welcome.png'
      );
    });

    it('calls getMessageCenterCampaigns with personalization', async () => {
      const personalization = { user_name: 'John' };
      const result = await SwrveSDK.getMessageCenterCampaigns(personalization);
      expect(
        NativeModules.SwrvePlugin.getMessageCenterCampaigns
      ).toHaveBeenCalledWith(personalization);
      expect(result).toHaveLength(1);
      const campaign = result[0]!;
      expect(campaign.ID).toBe(123);
    });

    it('handles empty campaigns array', async () => {
      // Mock empty campaigns response
      NativeModules.SwrvePlugin.getMessageCenterCampaigns.mockResolvedValueOnce(
        []
      );

      const result = await SwrveSDK.getMessageCenterCampaigns();

      expect(result).toEqual([]);
    });

    it('calls getMessageCenterCampaign and returns the result', async () => {
      const result = await SwrveSDK.getMessageCenterCampaign(123);
      expect(
        NativeModules.SwrvePlugin.getMessageCenterCampaign
      ).toHaveBeenCalledWith(123, undefined);

      // Verify all SwrveMessageCenterCampaign properties
      expect(result.ID).toBe(123);
      expect(result.name).toBe('Welcome Campaign');
      expect(result.maxImpressions).toBe(5);
      expect(result.priority).toBe(9999);
      expect(result.downloadDate).toBe(1640000000);
      expect(result.dateStart).toBe(1640000000);
      expect(result.dateEnd).toBe(1650000000);
      expect(result.messageCenter).toBe(true);

      // Verify SwrveCampaignState object
      expect(result.state).toBeDefined();
      expect(result.state.status).toBe('Unseen');
      expect(result.state.impressions).toBe(0);
      expect(result.state.next).toBe(0);

      // Verify all SwrveMessageCenterDetails properties
      expect(result.messageCenterDetails).toBeDefined();
      expect(result.messageCenterDetails?.subject).toBe('Welcome to our app!');
      expect(result.messageCenterDetails?.description).toBe(
        'Get started with these tips'
      );
      expect(result.messageCenterDetails?.imageAccessibilityText).toBe(
        'Welcome banner'
      );
      expect(result.messageCenterDetails?.imageURL).toBe(
        'https://example.com/welcome.png'
      );
      expect(result.messageCenterDetails?.imageSha).toBe('abc123');
      expect(result.messageCenterDetails?.image).toBe(
        'file:///path/to/welcome.png'
      );
    });

    it('calls getMessageCenterCampaign with personalization', async () => {
      const personalization = { user_name: 'John' };
      const result = await SwrveSDK.getMessageCenterCampaign(
        123,
        personalization
      );
      expect(
        NativeModules.SwrvePlugin.getMessageCenterCampaign
      ).toHaveBeenCalledWith(123, personalization);
      expect(result.ID).toBe(123);
    });

    it('handles campaigns without messageCenterDetails', async () => {
      // Mock a campaign without message center details
      NativeModules.SwrvePlugin.getMessageCenterCampaign.mockResolvedValueOnce({
        ID: 456,
        name: 'Basic Campaign',
        maxImpressions: 10,
        priority: 100,
        downloadDate: 1640000000,
        dateStart: 1640000000,
        dateEnd: 1650000000,
        messageCenter: true,
        state: {
          status: 'Seen',
          impressions: 3,
        },
      });

      const result = await SwrveSDK.getMessageCenterCampaign(456);

      // Verify campaign properties exist
      expect(result.ID).toBe(456);
      expect(result.name).toBe('Basic Campaign');
      expect(result.maxImpressions).toBe(10);
      expect(result.priority).toBe(100);
      expect(result.state.status).toBe('Seen');
      expect(result.state.impressions).toBe(3);

      // Verify messageCenterDetails is undefined (optional)
      expect(result.messageCenterDetails).toBeUndefined();
    });

    it('handles state without optional properties', async () => {
      // Mock a campaign with minimal state (no impressions or next)
      NativeModules.SwrvePlugin.getMessageCenterCampaign.mockResolvedValueOnce({
        ID: 789,
        name: 'Minimal State Campaign',
        maxImpressions: 1,
        priority: 50,
        downloadDate: 1640000000,
        dateStart: 1640000000,
        dateEnd: 1650000000,
        messageCenter: true,
        state: {
          status: 'Deleted',
        },
      });

      const result = await SwrveSDK.getMessageCenterCampaign(789);

      // Verify state has required property
      expect(result.state.status).toBe('Deleted');

      // Verify optional state properties are undefined
      expect(result.state.impressions).toBeUndefined();
      expect(result.state.next).toBeUndefined();
    });

    it('calls showMessageCenterCampaign', async () => {
      const result = await SwrveSDK.showMessageCenterCampaign(789);
      expect(
        NativeModules.SwrvePlugin.showMessageCenterCampaign
      ).toHaveBeenCalledWith(789, undefined);
      expect(result).toBeUndefined();
    });

    it('calls removeMessageCenterCampaign', async () => {
      const result = await SwrveSDK.removeMessageCenterCampaign(456);
      expect(
        NativeModules.SwrvePlugin.removeMessageCenterCampaign
      ).toHaveBeenCalledWith(456);
      expect(result).toBeUndefined();
    });

    it('calls markMessageCenterCampaignAsSeen', async () => {
      const result = await SwrveSDK.markMessageCenterCampaignAsSeen(789);
      expect(
        NativeModules.SwrvePlugin.markMessageCenterCampaignAsSeen
      ).toHaveBeenCalledWith(789);
      expect(result).toBeUndefined();
    });
  });

  describe('Embedded Campaigns', () => {
    it('calls getEmbeddedMessageCenterCampaigns and returns the result', async () => {
      const result = await SwrveSDK.getEmbeddedMessageCenterCampaigns();
      expect(
        NativeModules.SwrvePlugin.getEmbeddedMessageCenterCampaigns
      ).toHaveBeenCalledWith(undefined);
      expect(result).toEqual([
        {
          ID: 456,
          campaignId: 123,
          name: 'Test Campaign',
          data: '{"key":"value"}',
          dataRaw: '{"key":"value"}',
          priority: 10,
          type: 'json',
          control: false,
          downloadDate: 1640000000,
          dateStart: 1640000000,
          dateEnd: 1650000000,
          state: { status: 'Unseen' },
          buttons: ['button1', 'button2'],
        },
      ]);
    });

    it('calls getEmbeddedMessageCenterCampaigns with personalization', async () => {
      const personalization = { user_name: 'John' };
      const result = await SwrveSDK.getEmbeddedMessageCenterCampaigns(
        personalization
      );
      expect(
        NativeModules.SwrvePlugin.getEmbeddedMessageCenterCampaigns
      ).toHaveBeenCalledWith(personalization);
      expect(result).toEqual([
        {
          ID: 456,
          campaignId: 123,
          name: 'Test Campaign',
          data: '{"key":"value"}',
          dataRaw: '{"key":"value"}',
          priority: 10,
          type: 'json',
          control: false,
          downloadDate: 1640000000,
          dateStart: 1640000000,
          dateEnd: 1650000000,
          state: { status: 'Unseen' },
          buttons: ['button1', 'button2'],
        },
      ]);
    });

    it('calls getPersonalizedText and returns the result', async () => {
      const personalizationProps = { name: 'John' };
      const result = await SwrveSDK.getPersonalizedText(
        'Hello ${name}',
        personalizationProps
      );
      expect(
        NativeModules.SwrvePlugin.getPersonalizedText
      ).toHaveBeenCalledWith('Hello ${name}', personalizationProps);
      expect(result).toBe('personalized text');
    });

    it('calls getPersonalizedEmbeddedMessageData and returns the result', async () => {
      const personalizationProps = { name: 'John' };
      const result = await SwrveSDK.getPersonalizedEmbeddedMessageData(
        123,
        personalizationProps
      );
      expect(
        NativeModules.SwrvePlugin.getPersonalizedEmbeddedMessageData
      ).toHaveBeenCalledWith(123, personalizationProps);
      expect(result).toBe('embedded data');
    });

    it('calls markEmbeddedMessageCampaignAsSeen', async () => {
      const result = await SwrveSDK.markEmbeddedMessageCampaignAsSeen(456);
      expect(
        NativeModules.SwrvePlugin.markEmbeddedMessageCampaignAsSeen
      ).toHaveBeenCalledWith(456);
      expect(result).toBeUndefined();
    });

    it('calls embeddedControlMessageImpressionEvent', async () => {
      const result = await SwrveSDK.embeddedControlMessageImpressionEvent(789);
      expect(
        NativeModules.SwrvePlugin.embeddedControlMessageImpressionEvent
      ).toHaveBeenCalledWith(789);
      expect(result).toBeUndefined();
    });

    it('calls markEmbeddedMessageButtonAsPressed', async () => {
      const result = await SwrveSDK.markEmbeddedMessageButtonAsPressed(
        123,
        'button-1'
      );
      expect(
        NativeModules.SwrvePlugin.markEmbeddedMessageButtonAsPressed
      ).toHaveBeenCalledWith(123, 'button-1');
      expect(result).toBeUndefined();
    });
  });

  describe('Listener Registration', () => {
    it('registers user resources listener and calls startedListening', () => {
      const callback = jest.fn();
      SwrveSDK.setListeners({ userResourcesUpdatedListener: callback });
      expect(NativeModules.SwrvePlugin.startedListening).toHaveBeenCalled();
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'SwrveUserResourcesUpdated',
        expect.any(Function)
      );
    });

    it('registers push listener and calls startedListening', () => {
      const callback = jest.fn();
      SwrveSDK.setListeners(undefined, { pushListener: callback });
      expect(NativeModules.SwrvePlugin.startedListening).toHaveBeenCalled();
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'PushNotification',
        expect.any(Function)
      );
    });

    it('registers silent push listener and calls startedListening', () => {
      const callback = jest.fn();
      SwrveSDK.setListeners(undefined, { silentPushListener: callback });
      expect(NativeModules.SwrvePlugin.startedListening).toHaveBeenCalled();
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'SilentPushNotification',
        expect.any(Function)
      );
    });

    it('registers deeplink listener and calls listeningDeeplink', () => {
      const callback = jest.fn();
      SwrveSDK.setDeeplinkListener({ deeplinkListener: callback });
      expect(NativeModules.SwrvePlugin.listeningDeeplink).toHaveBeenCalled();
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'SwrveMessageDeeplinkDelegate',
        expect.any(Function)
      );
    });

    it('registers push inbox update listener and calls listeningPushInboxUpdate', () => {
      const callback = jest.fn();
      SwrveSDK.setPushInboxUpdateListener({
        pushInboxUpdateListener: callback,
      });
      expect(
        NativeModules.SwrvePlugin.listeningPushInboxUpdate
      ).toHaveBeenCalled();
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'SwrvePushInboxUpdate',
        expect.any(Function)
      );
    });

    it('triggers user resources listener when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setListeners({ userResourcesUpdatedListener: callback });
      eventHandler && eventHandler({});
      expect(callback).toHaveBeenCalled();
    });

    it('triggers push listener with payload when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setListeners(undefined, { pushListener: callback });
      const payload = { some: 'push-data' };
      eventHandler &&
        eventHandler({
          PushEventPayload: payload,
        });
      expect(callback).toHaveBeenCalledWith(payload);
    });

    it('triggers silent push listener with payload when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setListeners(undefined, { silentPushListener: callback });
      const payload = { some: 'silent-push-data' };
      eventHandler &&
        eventHandler({
          PushEventPayload: payload,
        });
      expect(callback).toHaveBeenCalledWith(payload);
    });

    it('triggers deeplink listener with action string when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setDeeplinkListener({ deeplinkListener: callback });
      const deeplink = 'myapp://deeplink-action';
      eventHandler &&
        eventHandler({
          deeplinkDelegateActionString: deeplink,
        });
      expect(callback).toHaveBeenCalledWith(deeplink);
    });

    it('triggers push inbox update listener when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setPushInboxUpdateListener({
        pushInboxUpdateListener: callback,
      });
      eventHandler && eventHandler({});
      expect(callback).toHaveBeenCalled();
    });

    it('handles null/undefined listeners gracefully', () => {
      expect(() => SwrveSDK.setListeners({})).not.toThrow();
      expect(() => SwrveSDK.setListeners(undefined, {})).not.toThrow();
    });

    it('handles undefined deeplink listener gracefully', () => {
      expect(() => SwrveSDK.setDeeplinkListener(undefined)).not.toThrow();
    });

    it('handles undefined push inbox listener gracefully', () => {
      expect(() =>
        SwrveSDK.setPushInboxUpdateListener(undefined)
      ).not.toThrow();
    });

    it('registers embedded campaign listener and calls startedEmbeddedListening', () => {
      const callback = jest.fn();
      SwrveSDK.setEmbeddedCampaignListener({
        embeddedCampaignListener: callback,
      });
      expect(
        NativeModules.SwrvePlugin.startedEmbeddedListening
      ).toHaveBeenCalled();
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'SwrveEmbeddedCallback',
        expect.any(Function)
      );
    });

    it('triggers embedded campaign listener with all parameters when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setEmbeddedCampaignListener({
        embeddedCampaignListener: callback,
      });
      const embeddedMessage = {
        ID: 456,
        campaignId: 123,
        name: 'Test Campaign',
        data: '{"key":"value"}',
        dataRaw: '{"key":"value"}',
        priority: 10,
        type: 'json' as const,
        control: false,
        downloadDate: 1640000000,
        dateStart: 1640000000,
        dateEnd: 1650000000,
        state: { status: 'Unseen' },
        buttons: ['button1', 'button2'],
      };
      const personalizationProps = { name: 'John' };
      const isControl = false;
      eventHandler &&
        eventHandler({
          embeddedMessage: embeddedMessage,
          embeddedMessagePersonalizationProperties: personalizationProps,
          embeddedMessageIsControl: isControl,
        });
      expect(callback).toHaveBeenCalledWith(
        embeddedMessage,
        personalizationProps,
        isControl
      );
    });

    it('registers in-app message listener', () => {
      const callback = jest.fn();
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });
      expect((NativeEventEmitter as any).addListener).toHaveBeenCalledWith(
        'SwrveMessageDetailsDelegate',
        expect.any(Function)
      );
    });

    it('triggers in-app message listener with all parameters when event is emitted', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      // Create realistic message structure matching SwrveMessageDetails
      const action = 'Custom';
      const message = {
        campaignSubject: 'Special Offer Campaign',
        campaignId: 12345,
        variantId: 67890,
        messageName: 'spring_sale_message',
        buttons: [
          {
            buttonName: 'accept_btn',
            buttonText: 'Accept Offer',
            actionType: 'Custom',
            actionString: 'myapp://offers',
          },
          {
            buttonName: 'dismiss_btn',
            buttonText: 'No Thanks',
            actionType: 'Dismiss',
            actionString: '',
          },
        ],
      };
      const selectedButton = {
        buttonName: 'accept_btn',
        buttonText: 'Accept Offer',
        actionType: 'Custom',
        actionString: 'myapp://offers',
      };

      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
          messageDetailSelectedButton: selectedButton,
        });

      expect(callback).toHaveBeenCalledWith(action, message, selectedButton);

      // Verify all SwrveMessageDetails properties
      expect(callback).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          campaignSubject: 'Special Offer Campaign',
          campaignId: 12345,
          variantId: 67890,
          messageName: 'spring_sale_message',
          buttons: expect.any(Array),
        }),
        expect.any(Object)
      );

      // Verify button properties
      const callArgs = callback.mock.calls[0];
      expect(callArgs[1].buttons).toHaveLength(2);
      expect(callArgs[1].buttons[0]).toEqual({
        buttonName: 'accept_btn',
        buttonText: 'Accept Offer',
        actionType: 'Custom',
        actionString: 'myapp://offers',
      });

      // Verify selectedButton properties
      expect(callArgs[2]).toEqual({
        buttonName: 'accept_btn',
        buttonText: 'Accept Offer',
        actionType: 'Custom',
        actionString: 'myapp://offers',
      });
    });

    it('triggers in-app message listener without selected button for impression action', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      const action = 'Impression';
      const message = {
        campaignSubject: 'Welcome Message',
        campaignId: 98765,
        variantId: 11111,
        messageName: 'welcome_message',
        buttons: [
          {
            buttonName: 'cta_btn',
            buttonText: 'Get Started',
            actionType: 'Install',
            actionString: 'https://example.com/install',
          },
        ],
      };

      // No selected button for impression action
      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
        });

      expect(callback).toHaveBeenCalledWith(action, message, undefined);

      // Verify message structure
      const callArgs = callback.mock.calls[0];
      expect(callArgs[0]).toBe('Impression');
      expect(callArgs[1].campaignId).toBe(98765);
      expect(callArgs[1].buttons).toHaveLength(1);
      expect(callArgs[2]).toBeUndefined();
    });

    it('triggers in-app message listener with clipboard action', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      // Test clipboard action (normalized from Android's CopyToClipboard)
      const action = 'Clipboard';
      const message = {
        campaignSubject: 'Copy Promo Code',
        campaignId: 55555,
        variantId: 77777,
        messageName: 'promo_code_message',
        buttons: [
          {
            buttonName: 'copy_btn',
            buttonText: 'Copy Code',
            actionType: 'Clipboard',
            actionString: 'SAVE20',
          },
        ],
      };
      const selectedButton = {
        buttonName: 'copy_btn',
        buttonText: 'Copy Code',
        actionType: 'Clipboard',
        actionString: 'SAVE20',
      };

      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
          messageDetailSelectedButton: selectedButton,
        });

      expect(callback).toHaveBeenCalledWith(action, message, selectedButton);

      // Verify clipboard action is correctly typed and passed through
      const callArgs = callback.mock.calls[0];
      expect(callArgs[0]).toBe('Clipboard');
      expect(callArgs[1].campaignId).toBe(55555);
      expect(callArgs[1].buttons[0].actionType).toBe('Clipboard');
      expect(callArgs[2].actionString).toBe('SAVE20');
    });

    it('triggers in-app message listener with OpenAppSettings action type', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      // Test OpenAppSettings (iOS normalization: OpenSettings → OpenAppSettings)
      const action = 'Custom';
      const message = {
        campaignSubject: 'Enable Notifications',
        campaignId: 11111,
        variantId: 22222,
        messageName: 'settings_prompt',
        buttons: [
          {
            buttonName: 'settings_btn',
            buttonText: 'Open Settings',
            actionType: 'OpenAppSettings',
            actionString: '',
          },
        ],
      };
      const selectedButton = {
        buttonName: 'settings_btn',
        buttonText: 'Open Settings',
        actionType: 'OpenAppSettings',
        actionString: '',
      };

      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
          messageDetailSelectedButton: selectedButton,
        });

      expect(callback).toHaveBeenCalledWith(action, message, selectedButton);

      // Verify iOS normalized OpenSettings → OpenAppSettings
      const callArgs = callback.mock.calls[0];
      expect(callArgs[1].buttons[0].actionType).toBe('OpenAppSettings');
      expect(callArgs[2].actionType).toBe('OpenAppSettings');
    });

    it('triggers in-app message listener with Capability action type', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      // Test Capability (Android normalization: RequestCapabilty → Capability, fixes typo)
      const action = 'Custom';
      const message = {
        campaignSubject: 'Request Permission',
        campaignId: 33333,
        variantId: 44444,
        messageName: 'permission_request',
        buttons: [
          {
            buttonName: 'permission_btn',
            buttonText: 'Grant Permission',
            actionType: 'Capability',
            actionString: 'camera',
          },
        ],
      };
      const selectedButton = {
        buttonName: 'permission_btn',
        buttonText: 'Grant Permission',
        actionType: 'Capability',
        actionString: 'camera',
      };

      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
          messageDetailSelectedButton: selectedButton,
        });

      expect(callback).toHaveBeenCalledWith(action, message, selectedButton);

      // Verify Android normalized RequestCapabilty → Capability (fixes typo)
      const callArgs = callback.mock.calls[0];
      expect(callArgs[1].buttons[0].actionType).toBe('Capability');
      expect(callArgs[2].actionType).toBe('Capability');
    });

    it('triggers in-app message listener with multiple button action types', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      // Test message with various button action types
      const action = 'Custom';
      const message = {
        campaignSubject: 'Multi-Button Message',
        campaignId: 99999,
        variantId: 88888,
        messageName: 'multi_button_test',
        buttons: [
          {
            buttonName: 'notification_btn',
            buttonText: 'Notifications',
            actionType: 'OpenNotificationSettings',
            actionString: '',
          },
          {
            buttonName: 'page_btn',
            buttonText: 'Next Page',
            actionType: 'PageLink',
            actionString: 'page2',
          },
          {
            buttonName: 'geo_btn',
            buttonText: 'Start Location',
            actionType: 'StartGeo',
            actionString: '',
          },
        ],
      };
      const selectedButton = {
        buttonName: 'notification_btn',
        buttonText: 'Notifications',
        actionType: 'OpenNotificationSettings',
        actionString: '',
      };

      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
          messageDetailSelectedButton: selectedButton,
        });

      expect(callback).toHaveBeenCalledWith(action, message, selectedButton);

      // Verify all button action types
      const callArgs = callback.mock.calls[0];
      expect(callArgs[1].buttons[0].actionType).toBe(
        'OpenNotificationSettings'
      );
      expect(callArgs[1].buttons[1].actionType).toBe('PageLink');
      expect(callArgs[1].buttons[2].actionType).toBe('StartGeo');
      expect(callArgs[2].actionType).toBe('OpenNotificationSettings');
    });

    it('triggers in-app message listener with dismiss message action', () => {
      const callback = jest.fn();
      let eventHandler: ((event: any) => void) | undefined;
      (NativeEventEmitter as any).addListener.mockImplementationOnce(
        (_eventName: string, handler: any) => {
          eventHandler = handler;
          return { remove: jest.fn() };
        }
      );
      SwrveSDK.setInAppMessageListener({ inAppMessageListener: callback });

      // Test Dismiss message action
      const action = 'Dismiss';
      const message = {
        campaignSubject: 'Dismissable Message',
        campaignId: 66666,
        variantId: 77777,
        messageName: 'dismissable_message',
        buttons: [
          {
            buttonName: 'close_btn',
            buttonText: 'Close',
            actionType: 'Dismiss',
            actionString: '',
          },
        ],
      };
      const selectedButton = {
        buttonName: 'close_btn',
        buttonText: 'Close',
        actionType: 'Dismiss',
        actionString: '',
      };

      eventHandler &&
        eventHandler({
          messageDetailAction: action,
          messageDetail: message,
          messageDetailSelectedButton: selectedButton,
        });

      expect(callback).toHaveBeenCalledWith(action, message, selectedButton);

      // Verify dismiss action
      const callArgs = callback.mock.calls[0];
      expect(callArgs[0]).toBe('Dismiss');
      expect(callArgs[2].actionType).toBe('Dismiss');
    });
  });
});

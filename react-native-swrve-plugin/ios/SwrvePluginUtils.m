#import "SwrvePluginUtils.h"
#import <SwrveSDK/SwrveSDK.h>

@implementation SwrvePluginUtils

+ (NSDictionary<NSString*, NSString*>*) toDictionaryJustStrings:(nonnull NSDictionary*)dictionary {
    NSMutableDictionary<NSString*, NSString*>* outDict = [NSMutableDictionary dictionaryWithCapacity:dictionary.count];
    
    [dictionary enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
        if ([obj isKindOfClass:NSString.class]) {
            [outDict setObject:obj forKey:key];
        } else if ([obj isKindOfClass:NSNumber.class]) {
            [outDict setObject:((NSNumber*)obj).description forKey:key];
        }
    }];
    
    return outDict;
}

// Serialize an entire dictionary or serialize a key in the dic.
// This method is used to serialize the just the "SwrveSilentPushPayloadKey" payload content.
// So we return to JS layer just the expected payload to our custumer.
+ (NSString*) serializeDictonaryToJson:(NSDictionary *)dic withKey:(nullable NSString *) key {
    NSError *error;
    NSData *jsonData;
    if (key != nil) {
        jsonData = [NSJSONSerialization dataWithJSONObject:[dic objectForKey:key] options:0 error:&error];
    } else {
        jsonData = [NSJSONSerialization dataWithJSONObject:dic options:0 error:&error];
    }

    if (!jsonData) {
        NSLog(@"Could not serialize remote push notification payload: %@", error);
        return nil;
    }
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    return jsonString;
}

+ (NSError*) produceErrorFromException:(NSException *) exception {
    NSMutableDictionary *info = [NSMutableDictionary dictionary];
    [info setValue:exception forKey:NSUnderlyingErrorKey];
    [info setValue:exception.userInfo forKey:NSDebugDescriptionErrorKey];
    [info setValue:(exception.reason ?: @"unknown reason") forKey:NSLocalizedFailureReasonErrorKey];
    
    return [NSError errorWithDomain:exception.name code:0 userInfo:info];
}

@end

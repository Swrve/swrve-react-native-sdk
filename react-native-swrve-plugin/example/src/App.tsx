import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import SwrveSDK from 'react-native-swrve-plugin';

export default function App() {
  const [isStarted, setIsStarted] = React.useState<boolean>(false);
  const [userId, setUserId] = React.useState<string>('');

  React.useEffect(() => {
    const fetchSDKInfo = async () => {
      const started = await SwrveSDK.isStarted();
      const user = await SwrveSDK.getUserId();

      setIsStarted(started);
      setUserId(user);
    };

    fetchSDKInfo();
  }, []);

  return (
    <View style={styles.container}>
      <Text>SwrveSDK.isStarted: {isStarted ? 'Yes' : 'No'}</Text>
      <Text>SwrveSDK.getUserId: {userId || 'Loading...'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'flex-start',
    justifyContent: 'center',
    paddingLeft: 20,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});

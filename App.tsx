/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import {
    Button,
    NativeModules,
    SafeAreaView,
    ScrollView,
    StatusBar,
    StyleSheet,
    Text,
    useColorScheme,
    View,
} from 'react-native';
const { CalendarModule, NetworkDiscoveryModule } = NativeModules;

function App(): React.JSX.Element {
    const onPress = async () => {
        try {
            const eventId = await CalendarModule.createCalendarEvent(
                'Party',
                'My House',
            );
            console.log(`Created a new event with id ${eventId}`);
        } catch (e) {
            console.error(e);
        }
    };

    const onNetworkDiscovery = async () => {
        try {
            await NetworkDiscoveryModule.getNetworkDiscovery().then((result) => {
                console.log(result)
            }).catch((err) => {
                console.log(err)
            });
        } catch (e) {
            console.error(e);
        }
    };

    const onNetworkDiscoveryCancel = async () => {
        try {
            await NetworkDiscoveryModule.cancelNetworkDiscovery()
        } catch (e) {
            console.error(e);
        }
    };

    const onNetworkDiscoveryActivity = async () => {
        try {
            await NetworkDiscoveryModule.navigateToNetworkDiscoveryActivity()
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <SafeAreaView style={{ flex: 1 }}>
            <Text>Hello</Text>
            <Button
                title="Click to invoke your native module!"
                color="#841584"
                onPress={onPress}
            />
            <Button
                title="Network Discovery Activity"
                color="#001500"
                onPress={onNetworkDiscoveryActivity}
            />

            <View style={{ marginTop: 32, margin: 16, justifyContent: 'space-evenly', flexDirection: 'row' }}>
                <Button
                    title="Network Discovery"
                    color="#001584"
                    onPress={onNetworkDiscovery}
                />

                <Button
                    title="Network Discovery Cancel"
                    color="#FF1712"
                    onPress={onNetworkDiscoveryCancel}
                />
            </View>
        </SafeAreaView>
    );
}

export default App;

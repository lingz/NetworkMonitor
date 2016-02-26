/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';
import React, {
  AppRegistry,
  Component,
  StyleSheet,
  Text,
  View,
  DeviceEventEmitter
} from 'react-native';

import Button from "react-native-button"

class NetworkMonitor extends Component {
  constructor(props) {
    super(props)
    this.state = {
      connectionType: "Unknown",
      ping: 0,
      speed: 0
    }
  }

  componentWillMount() {
    DeviceEventEmitter.addListener("connectionType", (connectionType) => {
      console.log("Got a result! " + connectionType)
      this.setState({ connectionType })
    })
    DeviceEventEmitter.addListener("ping", (ping) => {
      console.log("Got a result! " + ping)
      this.setState({ ping })
    })
    DeviceEventEmitter.addListener("speed", (speed) => {
      console.log("Got a result! " + speed)
      this.setState({ speed })
    })
  }

  testNetwork() {
    React.NativeModules.NetworkModule.testNetwork()
  }

  render() {
    let ping = this.state.ping
    let speed = this.state.speed

    let online
    if (ping == 0) {
      online = "Unknown"
    } else if (ping === -1) {
      online = "No"
    } else {
      online = "Yes"
    }

    let pingString = ping > 0 ? ping : "Unknown"
    let speedString = speed > 0 ? Math.round(speed) + " kB/s" : "Unknown"

    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Network Monitor
        </Text>
        <Text style={styles.instructions}>
          Connection Type: {this.state.connectionType}
        </Text>
        <Text style={styles.instructions}>
          Online: {online}
        </Text>
        <Text style={styles.instructions}>
          Ping: {pingString}
        </Text>
        <Text style={styles.instructions}>
          Speed: {speedString}
        </Text>
        <Button
          onPress={this.testNetwork}
        >
          Press to test
        </Button>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('NetworkMonitor', () => NetworkMonitor);

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
  Image,
  DeviceEventEmitter,
  Linking,
  TouchableHighlight,
  ScrollView,
} from 'react-native';

class NetworkMonitor extends Component {
  constructor(props) {
    super(props)
    this.intervalId = -1;
    this.state = {
      active: undefined,
      connectionType: "Unknown",
      ping: 0,
      speed: 0,
      location: "Unknown",
      rssi: -1,
      nextPing: -1
    }
  }

  componentWillMount() {
    DeviceEventEmitter.addListener("active", (active) => {
      this.setState({ active })
    })
    DeviceEventEmitter.addListener("connectionType", (connectionType) => {
      this.setState({ connectionType })
    })
    DeviceEventEmitter.addListener("ping", (ping) => {
      this.setState({
        ping,
        nextPing: 10
      })
      if (this.intervalId === -1) {
        this.intervalId = setInterval(() => {
          if (this.state.nextPing > 0) {
            this.setState({
              nextPing: this.state.nextPing - 1
            })
          }
        }, 1000)
      }
    })
    DeviceEventEmitter.addListener("speed", (speed) => {
      this.setState({ speed })
    })
    DeviceEventEmitter.addListener("location", (location) => {
      this.setState({ location })
    })
    DeviceEventEmitter.addListener("rssi", (rssi) => {
      this.setState({ rssi })
    })
    React.NativeModules.NetworkModule.checkServiceStatus()
  }

  componentWillUnmount() {
    if (this.intervalId !== -1) {
      clearInterval(this.intervalId)
    }
  }

  toggleMonitor() {
    if (this.state.active) {
      React.NativeModules.NetworkModule.stopMonitoringService()
    } else {
      React.NativeModules.NetworkModule.startMonitoringService()
    }
    this.setState({
      active: !this.state.active
    })
  }

  openResults() {
    Linking.openURL("http://www.thegazelle.org/issue/issue-80/features/wifiexperiment/")
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

    let pingString = ping > 0 ? ping :
      (ping === -1 ? "Test Failed" : "Unknown")
    let speedString = speed > 0 ? Math.round(speed) + " kB/s" :
      (speed === -1 ? "Test Failed" : "Unknown")

    let toggleText = this.state.active ?
      "Stop Monitoring" : "Start Monitoring"

    let qualityNode = (
      <Text>
        <Text style={styles.goldText}>
          {"★".repeat(this.state.rssi + 1)}
        </Text>
        <Text>
          {"★".repeat(5 - this.state.rssi - 1)}
        </Text>
      </Text>
    )

    let nextPing = this.state.nextPing === -1 ?
      "Unknown" : this.state.nextPing + " seconds";

    let body = this.state.active === undefined ?
      (
        <Text>
          Loading...
        </Text>
      ) : (
        <View style={styles.center}>
          <TouchableHighlight
            style={[styles.button, this.state.active ?
              styles.toggleTouchableHighlightActive : styles.toggleTouchableHighlightInactive]}
            onPress={this.toggleMonitor.bind(this)}
          >
            <Text style={styles.whiteText}>
              {toggleText}
            </Text>
          </TouchableHighlight>
          <View style={[styles.information, this.state.active ? null : styles.hidden]}>
            <Text>
              Location: {this.state.location}
            </Text>
            <Text>
              Connection Type: {this.state.connectionType}
            </Text>
            <Text>
              Connection Quality: {qualityNode}
            </Text>
            <Text>
              Online: {online}
            </Text>
            <Text>
              Ping: {pingString}
            </Text>
            <Text>
              Speed: {speedString}
            </Text>
            <Text>
              Next Ping: {nextPing}
            </Text>
          </View>
        </View>
      )

    return (
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.welcome}>
          Network Monitor
        </Text>
        <Image
          resizeMode="contain"
          style={styles.image}
          source={require("./android/app/src/main/res/drawable-nodpi/emoji.png")}
        />
        <Text style={styles.instructions}>
          Welcome to the Crowd Sourced Network Monitoring Experiment.
          When the Monitor is active, your device will check the WiFi conditions every
          10 seconds, and upload it along with your current location to our server.
          All data sent is anonymized and encrypted. This will drain your battery.
        </Text>
        <TouchableHighlight
          style={[styles.button, styles.seeResults]}
          onPress={this.openResults.bind(this)}
        >
          <Text style={styles.whiteText}>
            See Live Results
          </Text>
        </TouchableHighlight>
        {body}
      </ScrollView>
    );
  }
}

const styles = StyleSheet.create({
  container: {
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
    padding: 5,
    marginLeft: 20,
    marginRight: 20
  },
  image: {
    width: 100,
    height: 100,
    marginBottom: 10
  },
  button: {
    padding: 10,
    borderRadius: 45,
    alignItems: 'center',
    justifyContent: 'center'
  },
  seeResults: {
    backgroundColor: '#5bc0de',
    marginBottom: 10
  },
  toggleTouchableHighlightActive: {
    backgroundColor: '#d9534f',
  },
  toggleTouchableHighlightInactive: {
    backgroundColor: '#5cb85c',
  },
  information: {
    marginTop: 10,
    marginBottom: 10
  },
  hidden: {
    opacity: 0
  },
  whiteText: {
    color: 'white'
  },
  goldText: {
    color: '#f0ad4e'
  },
  center: {
    justifyContent: 'center',
    alignItems: 'center',
  }
});

AppRegistry.registerComponent('NetworkMonitor', () => NetworkMonitor);

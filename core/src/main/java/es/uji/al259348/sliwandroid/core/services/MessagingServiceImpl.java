package es.uji.al259348.sliwandroid.core.services;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

import es.uji.al259348.sliwandroid.core.R;
import rx.Observable;

public class MessagingServiceImpl implements MessagingService {

    private Context context;

    private MqttAndroidClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;

    public MessagingServiceImpl(Context context) {
        this.context = context;
        createClientFromContext();
    }

    private void createClientFromContext() {
        String brokerHost = context.getResources().getString(R.string.mqtt_broker_host);
        String brokerUser = context.getResources().getString(R.string.mqtt_broker_user);
        String brokerPass = context.getResources().getString(R.string.mqtt_broker_pass);
        String clientId = context.getResources().getString(R.string.mqtt_client_id) + "-" + UUID.randomUUID().toString();

        mqttClient = new MqttAndroidClient(context, brokerHost, clientId, new MemoryPersistence());

        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(brokerUser);
        mqttConnectOptions.setPassword(brokerPass.toCharArray());
    }

    private Observable<Void> connectAction() {
        return Observable.create(subscriber -> {
            Log.d("MQTT", "Connecting... | " + Thread.currentThread().getName());
            if (mqttClient.isConnected()) {
                Log.d("MQTT", "Already connected! | " + Thread.currentThread().getName());
                subscriber.onCompleted();
            } else {
                try {
                    IMqttToken token = mqttClient.connect(mqttConnectOptions);
                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken iMqttToken) {
                            Log.d("MQTT", "Connected successfully! | " + Thread.currentThread().getName());
                            subscriber.onCompleted();
                        }

                        @Override
                        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                            Log.d("MQTT", "Connection error! | " + Thread.currentThread().getName());
                            subscriber.onError(throwable);
                            mqttClient.unregisterResources();
                        }
                    });
                } catch (MqttException e) {
                    Log.d("MQTT", "Connection error! | " + Thread.currentThread().getName());
                    subscriber.onError(e);
                }
            }
        });
    }

    private Observable<Void> disconnectAction() {
        return Observable.create(subscriber -> {
            Log.d("MQTT", "Disconnecting... | " + Thread.currentThread().getName());
            if (!mqttClient.isConnected()) {
                Log.d("MQTT", "Already disconnected! | " + Thread.currentThread().getName());
                subscriber.onCompleted();
            } else {
                try {
                    IMqttToken token = mqttClient.disconnect();
                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken iMqttToken) {
                            Log.d("MQTT", "Disconnected successfully! | " + Thread.currentThread().getName());
                            subscriber.onCompleted();
                            mqttClient.unregisterResources();
                        }

                        @Override
                        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                            Log.d("MQTT", "Disconnection error! | " + Thread.currentThread().getName());
                            subscriber.onError(throwable);
                        }
                    });
                } catch (MqttException e) {
                    Log.d("MQTT", "Disconnection error! | " + Thread.currentThread().getName());
                    subscriber.onError(e);
                }
            }
        });
    }

    private Observable<Void> subscribeAction(String topic) {
        return Observable.create(subscriber -> {
            Log.d("MQTT", "Subscribing to topic: " + topic + " ... | " + Thread.currentThread().getName());
            try {
                IMqttToken token = mqttClient.subscribe(topic, 2);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        Log.d("MQTT", "Subscribed successfully! | " + Thread.currentThread().getName());
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        Log.d("MQTT", "Subscription error! | " + Thread.currentThread().getName());
                        subscriber.onError(throwable);
                    }
                });
            } catch (MqttException e) {
                Log.d("MQTT", "Subscription error! | " + Thread.currentThread().getName());
                subscriber.onError(e);
            }
        });
    }

    private Observable<Void> unsubscribeAction(String topic) {
        return Observable.create(subscriber -> {
            Log.d("MQTT", "Unsubscribing from topic: " + topic + " ... | " + Thread.currentThread().getName());
            try {
                IMqttToken token = mqttClient.unsubscribe(topic);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        Log.d("MQTT", "Unsubscribed successfully! | " + Thread.currentThread().getName());
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        Log.d("MQTT", "Unsubscription error! | " + Thread.currentThread().getName());
                        subscriber.onError(throwable);
                    }
                });
            } catch (MqttException e) {
                Log.d("MQTT", "Unsubscription error! | " + Thread.currentThread().getName());
                subscriber.onError(e);
            }
        });
    }

    private Observable<Void> publishAction(String topic, String msg) {
        return Observable.create(subscriber -> {
            Log.d("MQTT", "Publishing to topic: " + topic + " ... | " + Thread.currentThread().getName());
            try {
                IMqttDeliveryToken token = mqttClient.publish(topic, msg.getBytes(), 2, false);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        Log.d("MQTT", "Published successfully! | " + Thread.currentThread().getName());
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        Log.d("MQTT", "Publish error! | " + Thread.currentThread().getName());
                        subscriber.onError(throwable);
                    }
                });
            } catch (MqttException e) {
                Log.d("MQTT", "Publish error! | " + Thread.currentThread().getName());
                subscriber.onError(e);
            }
        });
    }

    @Override
    public void onDestroy() {
        mqttClient.unregisterResources();
    }

    @Override
    public Observable<Void> publish(String topic, String msg) {
        return Observable.concat(
                connectAction(),
                publishAction(topic, msg),
                disconnectAction()
        );
    }

    @Override
    public Observable<String> request(String topic, String msg) {
        return Observable.create(subscriber -> {
            Log.d("MQTT", "Requesting to topic: " + topic + " ... | " + Thread.currentThread().getName());

            createClientFromContext();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    Log.d("MQTT", "The response has been successfully received! | " + Thread.currentThread().getName());
                    subscriber.onNext(new String(mqttMessage.getPayload()));

                    Observable.concat(
                            unsubscribeAction(topic + "/response"),
                            disconnectAction()
                    ).doOnError(subscriber::onError).doOnCompleted(subscriber::onCompleted).subscribe();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    Log.d("MQTT", "The request has been successfully delivered! | " + Thread.currentThread().getName());
                }
            });

            Observable.concat(
                    connectAction(),
                    subscribeAction(topic + "/response"),
                    publishAction(topic + "/request", msg)
            ).subscribe((s) -> {}, subscriber::onError);

            try {
                Thread.sleep(5000);
                mqttClient.unregisterResources();
                subscriber.onError(new Throwable("No es posible conectarse al servidor"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });
    }

}

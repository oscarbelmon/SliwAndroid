package es.uji.al259348.sliwandroid.core.controller;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.InterruptedIOException;
import java.util.UUID;

import es.uji.al259348.sliwandroid.core.model.Device;
import es.uji.al259348.sliwandroid.core.model.Sample;
import es.uji.al259348.sliwandroid.core.model.User;
import es.uji.al259348.sliwandroid.core.services.AlarmService;
import es.uji.al259348.sliwandroid.core.services.AlarmServiceImpl;
import es.uji.al259348.sliwandroid.core.services.DeviceService;
import es.uji.al259348.sliwandroid.core.services.DeviceServiceImpl;
import es.uji.al259348.sliwandroid.core.services.MessagingService;
import es.uji.al259348.sliwandroid.core.services.MessagingServiceImpl;
import es.uji.al259348.sliwandroid.core.services.SampleService;
import es.uji.al259348.sliwandroid.core.services.SampleServiceImpl;
import es.uji.al259348.sliwandroid.core.services.UserService;
import es.uji.al259348.sliwandroid.core.services.UserServiceImpl;
import es.uji.al259348.sliwandroid.core.services.WifiService;
import es.uji.al259348.sliwandroid.core.services.WifiServiceImpl;
import es.uji.al259348.sliwandroid.core.view.MainView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainControllerImpl implements MainController {

    private MainView mainView;

    private MessagingService messagingService;
    private DeviceService deviceService;
    private UserService userService;
    private WifiService wifiService;
    private AlarmService alarmService;
    private SampleService sampleService;

    public MainControllerImpl(MainView mainView) {
        this.mainView = mainView;

        Context context = mainView.getContext();
        this.messagingService = new MessagingServiceImpl(context);
        this.deviceService = new DeviceServiceImpl(context, messagingService);
        this.userService = new UserServiceImpl(context, messagingService);
        this.wifiService = new WifiServiceImpl(context);
        this.alarmService = new AlarmServiceImpl(context);
        this.sampleService = new SampleServiceImpl(context);
    }

    @Override
    public void onDestroy() {
        messagingService.onDestroy();
        wifiService.onDestroy();
        sampleService.onDestroy();
    }

    @Override
    public void decideStep() {
        User user = userService.getCurrentLinkedUser();

        if (!deviceService.isCurrentDeviceRegistered()) {
            mainView.hasToRegisterDevice();
        } else if (user == null) {
            mainView.hasToLink();
        } else if (!user.isConfigured()) {
            mainView.hasToConfigure();
        } else {
            alarmService.setTakeSampleAlarm();
            mainView.isOk();
        }
    }

    @Override
    public void registerDevice() {
        deviceService.registerCurrentDevice()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(device -> {
                    mainView.onDeviceRegistered(device);
                }, mainView::onError);
    }

    @Override
    public void link() {
        String deviceId = deviceService.getId();
        userService.getUserLinkedTo(deviceId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    userService.setCurrentLinkedUser(user);
                    mainView.onUserLinked(user);
                }, this::handleError);
    }

    @Override
    public void unlink() {
        userService.setCurrentLinkedUser(null);
        alarmService.cancelTakeSampleAlarm();
    }

    @Override
    public void takeSample() {
        Log.d("MainController", "It has been requested to take a sample.");
        sampleService.take()
                .doOnNext(this::onTakeSampleCompleted)
                .subscribe();
    }

    private void onTakeSampleCompleted(Sample sample) {
        Log.d("MainController", "The sample has been taken.");

        sample.setId(UUID.randomUUID().toString());
        sample.setUserId(userService.getCurrentLinkedUserId());
        sample.setDeviceId(deviceService.getId());

        saveSample(sample);
        mainView.onTakeSampleCompleted();
    }

    private void saveSample(Sample sample) {
        Log.d("MainController", "The sample is gonna be saved.");
        Log.d("MainController", sample.toString());

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String topic = "samples/" + sample.getId() + "/save";
            String msg = objectMapper.writeValueAsString(sample);

            Log.d("MainController", "Trying to publish the sample.");
            messagingService.request(topic, msg)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(
                            s -> {
                                Log.d("MainController", "The sample has been published (next)");
                            },
                            throwable -> {
                                Log.d("MainController", throwable.getClass().getSimpleName());
                                Log.d("MainController", throwable.getLocalizedMessage());
                                throwable.printStackTrace();
                                Log.d("MainController", "The sample couldn't be published.");
                                sampleService.save(sample);

//                                if (throwable instanceof InterruptedIOException) {
//
//                                }
//                                if (throwable instanceof MqttException) {
//
//                                }
//                                else {
//
//                                }
//
//                                Log.d("MainController", "Procedemos a guardar la muestra...");

                            },
                            () -> Log.d("MainController", "The sample has been published (completed)")
                    );

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void handleError(Throwable throwable) {
        mainView.onError(throwable);
    }

}

package es.uji.al259348.sliwandroid.core.controller;

import android.content.Context;

import es.uji.al259348.sliwandroid.core.model.User;
import es.uji.al259348.sliwandroid.core.services.AlarmService;
import es.uji.al259348.sliwandroid.core.services.AlarmServiceImpl;
import es.uji.al259348.sliwandroid.core.services.MessagingService;
import es.uji.al259348.sliwandroid.core.services.MessagingServiceImpl;
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
    private UserService userService;
    private WifiService wifiService;
    private AlarmService alarmService;

    public MainControllerImpl(MainView mainView) {
        this.mainView = mainView;

        Context context = mainView.getContext();
        this.messagingService = new MessagingServiceImpl(context);
        this.userService = new UserServiceImpl(context, messagingService);
        this.wifiService = new WifiServiceImpl(context);
        this.alarmService = new AlarmServiceImpl(context);
    }

    @Override
    public void decideStep() {
        User user = userService.getCurrentLinkedUser();
        if (user == null) {
            mainView.hasToLink();
        } else if (!user.isConfigured()) {
            mainView.hasToConfigure();
        } else {
            alarmService.setTakeSampleAlarm();
            mainView.isOk();
        }
    }

    @Override
    public void onDestroy() {
        messagingService.onDestroy();
    }

    @Override
    public void link() {
        String macAdress = wifiService.getMacAddress();
        userService.getUserLinkedTo(macAdress)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    userService.setCurrentLinkedUser(user);
                    mainView.onUserLinked(user);
                });
    }

    @Override
    public void unlink() {
        userService.setCurrentLinkedUser(null);
        alarmService.cancelTakeSampleAlarm();
    }

}

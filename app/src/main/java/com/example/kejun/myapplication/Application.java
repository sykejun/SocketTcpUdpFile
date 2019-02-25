package com.example.kejun.myapplication;


public class Application extends android.app.Application {
    private static Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;



    }

    public Application() {
    }

    public static Application getApplication() {
        return application;
    }

    public void AppExit() {
        System.exit(0);
    }


}

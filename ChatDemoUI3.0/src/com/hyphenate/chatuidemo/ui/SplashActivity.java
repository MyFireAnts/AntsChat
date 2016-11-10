package com.hyphenate.chatuidemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.util.EasyUtils;

/**
 * 开屏页
 */
public class SplashActivity extends BaseActivity {

    private static final int sleepTime = 2000;

    @Override
    protected void onCreate(Bundle arg0) {
        setContentView(R.layout.em_activity_splash);
        super.onCreate(arg0);

        /**
         * 实例化em_activity_splash页面布局及组件
         */
        RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.splash_root);
        TextView versionText = (TextView) findViewById(R.id.tv_version);

        /**
         * 设置页面版本显示组件显示当前sdk版本信息
         */
        versionText.setText(getVersion());

        /**
         * AlphaAnimation 透明度动画效果
         * ScaleAnimation 缩放动画效果
         * TranslateAnimation 位移动画效果
         * RotateAnimation 旋转动画效果
         */
        //设置透明度渐变动画，实现淡入淡出效果
        AlphaAnimation animation = new AlphaAnimation(0.3f, 1.0f);
        //设置动画持续时间
        animation.setDuration(1500);
        //启动页面设置的动画
        rootLayout.startAnimation(animation);
    }

    @Override
    protected void onStart() {
        super.onStart();

        new Thread(new Runnable() {
            public void run() {
                //获取是否已经登录过
                if (DemoHelper.getInstance().isLoggedIn()) {
                    // auto login mode, make sure all group and conversation is loaed before enter the main screen
                    //获取系统当前时间，得到开始时间点
                    long start = System.currentTimeMillis();
                    //从环信服务器上获取所有群组信息
                    EMClient.getInstance().groupManager().loadAllGroups();
                    //从环信服务器上获取所有聊天信息
                    EMClient.getInstance().chatManager().loadAllConversations();
                    //获得系统时间并计算得出花费的总时间
                    long costTime = System.currentTimeMillis() - start;
                    //wait -->等待
                    if (sleepTime - costTime > 0) {
                        try {
                            Thread.sleep(sleepTime - costTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //获取居于顶部的activity的名称
                    String topActivityName = EasyUtils.getTopActivityName(EMClient.getInstance().getContext());
                    if (topActivityName != null && (topActivityName.equals(VideoCallActivity.class.getName()) || topActivityName.equals(VoiceCallActivity.class.getName()))) {
                        // nop
                        // avoid main screen overlap Calling Activity -->避免主屏幕重叠调用Activity
                    } else {
                        //enter main screen  从启动页面跳转到主页面
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    }
                    //结束当前页面的Activity
                    finish();
                } else {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                    }
                    //从启动页面跳转到登录页面
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            }
        }).start(); //启动线程

    }

    /**
     * get sdk version -->获取SDK版本
     */
    private String getVersion() {
        return EMClient.getInstance().getChatConfig().getVersion();
    }
}

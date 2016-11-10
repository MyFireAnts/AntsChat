/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.chatuidemo.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.DemoApplication;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.db.DemoDBManager;
import com.hyphenate.easeui.utils.EaseCommonUtils;

/**
 * Login screen  -->登录页面
 */
public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    public static final int REQUEST_CODE_SETNICK = 1;
    private EditText usernameEditText;
    private EditText passwordEditText;

    private boolean progressShow;
    private boolean autoLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enter the main activity if already logged in
        //判断是否已经登录过，如果已经登录过则设置autoLogin的状态值为true，并跳转到主页面
        if (DemoHelper.getInstance().isLoggedIn()) {
            autoLogin = true;
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            return;
        }
        setContentView(R.layout.em_activity_login);

        //实例用户名及密码编辑组件
        usernameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);

        // if user changed, clear the password
        //如果用户名称编辑框内容发生了改变，则清空密码编辑框
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //文本改变时，则设置密码编辑框内容为空
                passwordEditText.setText(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //文本改变之前
            }

            @Override
            public void afterTextChanged(Editable s) {
                //文本改变之后
            }
        });
        //获取判断当前用户名是否为空，若不为空则将当前用户名设置到用户名编辑框
        if (DemoHelper.getInstance().getCurrentUsernName() != null) {
            usernameEditText.setText(DemoHelper.getInstance().getCurrentUsernName());
        }
    }

    /**
     * login
     * 点击登录按钮后执行登录的方法
     *
     * @param view
     */
    public void login(View view) {
        //判断当前网络是否可用，如果不可用则在当前显示的页面上提示用户，“网络不可用”
        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }
        //获得编辑框中的内容并去掉输入内容前后的空格
        String currentUsername = usernameEditText.getText().toString().trim();
        String currentPassword = passwordEditText.getText().toString().trim();

        //判断输入的用户名是否为空
        if (TextUtils.isEmpty(currentUsername)) {
            Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断输入的密码是否为空
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        //设置进度显示的状态值为true
        progressShow = true;
        //示例进展对话框在登陆页面
        final ProgressDialog pd = new ProgressDialog(LoginActivity.this);
        //设置点击无法取消关闭进展对话框
        pd.setCanceledOnTouchOutside(false);
        //设置取消监听
        pd.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "EMClient.getInstance().onCancel");
                //设置进度显示的状态值为false
                progressShow = false;
            }
        });
        //设置进展对话框显示“正在登录”
        pd.setMessage(getString(R.string.Is_landing));
        pd.show();

        // After logout，the DemoDB may still be accessed due to async callback, so the DemoDB will be re-opened again.
        //注销后，由于异步回调，DemoDB可能仍然被访问，所以DemoDB将被重新打开。
        // close it before login to make sure DemoDB not overlap  -->在登录之前关闭它，以确保DemoDB不重叠
        DemoDBManager.getInstance().closeDB();

        // reset current user name before login  -->在登录前重置当前用户名
        DemoHelper.getInstance().setCurrentUserName(currentUsername);

        //获取系统当前时间
        final long start = System.currentTimeMillis();
        // call login method  -->调用登录方法
        Log.d(TAG, "EMClient.getInstance().login");
        //调用环信登录的方法，并且进行回调
        EMClient.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "login: onSuccess");

                // manually load all local groups and conversation
                // 手动加载所有本地组和对话
                EMClient.getInstance().groupManager().loadAllGroups();
                EMClient.getInstance().chatManager().loadAllConversations();

                // update current user's display name for APNs  -->更新当前用户昵称到环信服务器
                boolean updatenick = EMClient.getInstance().updateCurrentUserNick(
                        DemoApplication.currentUserNick.trim());
                //判断昵称是否更新成功
                if (!updatenick) {
                    Log.e("LoginActivity", "update current user nick fail");
                }

                //如果当前登录页面正在运行与进展对话框正在显示
                if (!LoginActivity.this.isFinishing() && pd.isShowing()) {
                    //解雇进展对话框
                    pd.dismiss();
                }
                // get user's info (this should be get from App's server or 3rd party service)
                // 获取用户信息（应该从应用服务器或第三方服务获取）
                //采用异步方式获取当前用户信息
                DemoHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();
                //跳转页面从登录页面到主页面
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                //结束当前页面
                finish();
            }

            /**
             * 获取进展状态信息
             * @param progress
             * @param status
             */
            @Override
            public void onProgress(int progress, String status) {
                Log.d(TAG, "login: onProgress");
            }

            @Override
            public void onError(final int code, final String message) {
                Log.d(TAG, "login: onError: " + code);
                //如果进度显示框正在运行，则继续。
                if (!progressShow) {
                    return;
                }
                //将本线程加入到UI主线程
                runOnUiThread(new Runnable() {
                    public void run() {
                        //解雇进展对话框
                        pd.dismiss();
                        //提示用户显示登录失败的信息
                        Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    /**
     * register
     * 点击注册按钮
     *
     * @param view
     */
    public void register(View view) {
        //点击注册按钮跳转到注册页面，当注册页面关闭后将注册页面数据返回到登录页面
        startActivityForResult(new Intent(this, RegisterActivity.class), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (autoLogin) {
            return;
        }
    }
}

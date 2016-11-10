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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.exceptions.HyphenateException;

/**
 * register screen -->注册页面
 *
 */
public class RegisterActivity extends BaseActivity {
    private EditText userNameEditText;
    private EditText passwordEditText;
    private EditText confirmPwdEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_register);
        //实例用户名、密码、确认密码编辑框
        userNameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);
        confirmPwdEditText = (EditText) findViewById(R.id.confirm_password);
    }

    /**
     * 点击注册按钮
     * @param view
     */
    public void register(View view) {
        //获取用户名、密码、确认密码编辑框的内容
        final String username = userNameEditText.getText().toString().trim();
        final String pwd = passwordEditText.getText().toString().trim();
        String confirm_pwd = confirmPwdEditText.getText().toString().trim();
        //判断输入的用户名、密码、确认密码是否为空
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, getResources().getString(R.string.User_name_cannot_be_empty), Toast.LENGTH_SHORT).show();
            userNameEditText.requestFocus();
            return;
        } else if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        } else if (TextUtils.isEmpty(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Confirm_password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            confirmPwdEditText.requestFocus();
            return;
        } else if (!pwd.equals(confirm_pwd)) {  //判断两次输入的密码是否一致
            Toast.makeText(this, getResources().getString(R.string.Two_input_password), Toast.LENGTH_SHORT).show();
            return;
        }

        //如果输入的用户名与密码不为空，则在进展消息对话框显示“正在注册...”
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
            //实例进展消息对话框
            final ProgressDialog pd = new ProgressDialog(this);
            //设置详细对话框显示的内容
            pd.setMessage(getResources().getString(R.string.Is_the_registered));
            pd.show();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        // call method in SDK
                        //在环信服务器上进行注册
                        EMClient.getInstance().createAccount(username, pwd); //同步方法
                        //将本线程加入到UI主线程
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //如果注册页面没有结束，则解雇进展消息对话框
                                if (!RegisterActivity.this.isFinishing())
                                    pd.dismiss();
                                // save current user -->保存当前用户名
                                DemoHelper.getInstance().setCurrentUserName(username);
                                // 显示注册成功提示
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                                //结束注册页面
                                finish();
                            }
                        });
                    } catch (final HyphenateException e) {  //注册失败抛出的异常HyphenateException
                        //将本线程加入到UI主线程
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //如果注册页面没有结束，则解雇进展消息对话框
                                if (!RegisterActivity.this.isFinishing())
                                    pd.dismiss();
                                //获取错误编码
                                int errorCode = e.getErrorCode();
                                //如果错误编码为2,则提示“网络异常，请检查网络！”
                                if (errorCode == EMError.NETWORK_ERROR) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_ALREADY_EXIST) {    //如果错误编码为203，则提示“用户已存在！”
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {    //如果错误编码为202，则提示“注册失败，无权限！”
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {    //如果错误编码为205，则提示“用户名不合法”，否则提示“注册失败”
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }).start();
        }
    }

    /**
     * 退出
     * @param view
     */
    public void back(View view) {
        finish();
    }
}

package com.tencent.wmpf.pos.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.wmpf.pos.R;


/**
 * Created by gaochujia on 2021-2-25.
 */
public class AlertDialog {
    private Context context;
    private Activity activity;
    private Dialog dialog;
    private LinearLayout lLayout_bg;
    private TextView txt_title;
    private TextView txt_msg;
    private TextView btn_neg;
    private TextView btn_pos;
    private Display display;
    private boolean showTitle = false;
    private boolean showMsg = false;
    private boolean showPosBtn = false;
    private boolean showNegBtn = false;

    public AlertDialog(Context context) {
        this.context = context;
        this.activity = null;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public AlertDialog(Activity activity) {
        this.activity = activity;
        this.context = activity;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public Activity getActivity() {
        return activity;
    }

    public AlertDialog builder() {
        // 获取Dialog布局
        View view = LayoutInflater.from(context).inflate(
                R.layout.view_alertdialog, null);

        // 获取自定义Dialog布局中的控件
        lLayout_bg = view.findViewById(R.id.lLayout_bg);
        txt_title = view.findViewById(R.id.txt_title);
        txt_title.setVisibility(View.GONE);
        txt_msg = view.findViewById(R.id.txt_msg);
        txt_msg.setVisibility(View.GONE);
        btn_neg = view.findViewById(R.id.btn_neg);
        btn_neg.setVisibility(View.GONE);
        btn_pos = view.findViewById(R.id.btn_pos);
        btn_pos.setVisibility(View.GONE);

        // 定义Dialog布局和参数
        dialog = new Dialog(context, R.style.AlertDialogStyle);
        dialog.setContentView(view);

        // 调整dialog背景大小
        lLayout_bg.setLayoutParams(new FrameLayout.LayoutParams((int) (display
                .getWidth() * 0.25), LinearLayout.LayoutParams.WRAP_CONTENT));

        return this;
    }

    public AlertDialog setTitle(String title) {
        showTitle = true;
        if ("".equals(title)) {
            txt_title.setText("");
        } else {
            txt_title.setText(title);
        }
        return this;
    }

    public AlertDialog setMsg(String msg) {
        showMsg = true;
        if ("".equals(msg)) {
            txt_msg.setText("");
        } else {
            txt_msg.setText(msg);
        }
        return this;
    }

    public AlertDialog setMsg(String msg, boolean isLeft) {
        showMsg = true;
        if ("".equals(msg)) {
            txt_msg.setText("");
        } else {
            txt_msg.setText(msg);
        }
        if (isLeft) {
            txt_msg.setGravity(Gravity.LEFT);
        }

        return this;
    }

    public AlertDialog setCancelable(boolean cancel) {
        dialog.setCancelable(cancel);
        return this;
    }

    public AlertDialog setCanceledOnTouchOutside(boolean cancel) {
        dialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public AlertDialog setPositiveColor(int color) {
        btn_pos.setTextColor(context.getResources().getColor(color));
        return this;
    }

    public AlertDialog setPositiveBold() {
        btn_pos.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return this;
    }

    public AlertDialog setPositiveButton(String text,
                                         final View.OnClickListener listener) {
        showPosBtn = true;
        if ("".equals(text)) {
            btn_pos.setText("确定");
        } else {
            btn_pos.setText(text);
        }
        btn_pos.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(v);
            }
            dialog.dismiss();
        });
        return this;
    }

    public AlertDialog setPositiveBackground(Drawable drawable) {
        btn_pos.setBackground(drawable);
        return this;
    }

    public AlertDialog setNegativeColor(int color) {
        btn_neg.setTextColor(context.getResources().getColor(color));
        return this;
    }

    public AlertDialog setNegativeBold() {
        btn_neg.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return this;
    }

    public AlertDialog setNegativeButton(String text,
                                         final View.OnClickListener listener) {
        showNegBtn = true;
        if ("".equals(text)) {
            btn_neg.setText("取消");
        } else {
            btn_neg.setText(text);
        }
        btn_neg.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(v);
            }
            dialog.dismiss();
        });
        return this;
    }

    public AlertDialog setNegativeButton(String text, boolean isBold, int color,
                                         final View.OnClickListener listener) {
        showNegBtn = true;
        if ("".equals(text)) {
            btn_neg.setText("取消");
        } else {
            btn_neg.setText(text);
        }
        if (!isBold) {
            btn_neg.setTypeface(Typeface.DEFAULT);
        }
        btn_neg.setTextColor(color);
        btn_neg.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(v);
            }
            dialog.dismiss();
        });
        return this;
    }

    private void setLayout() {
        if (!showTitle && !showMsg) {
            txt_title.setText("提示");
            txt_title.setVisibility(View.VISIBLE);
        }

        if (showTitle) {
            txt_title.setVisibility(View.VISIBLE);
        }

        if (showMsg) {
            txt_msg.setVisibility(View.VISIBLE);
        }

        if (!showPosBtn && !showNegBtn) {
            btn_pos.setText("确定");
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_single_selector);
            btn_pos.setOnClickListener(v -> dialog.dismiss());
        }

        if (showPosBtn && showNegBtn) {
            btn_pos.setVisibility(View.VISIBLE);
            btn_neg.setVisibility(View.VISIBLE);
        }

        if (showPosBtn && !showNegBtn) {
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_single_selector);
        }

        if (!showPosBtn && showNegBtn) {
            btn_neg.setVisibility(View.VISIBLE);
            btn_neg.setBackgroundResource(R.drawable.alertdialog_single_selector);
        }
    }

    public void show() {
        setLayout();
        dialog.show();
    }

    public boolean isShowing() {
        if (dialog != null && dialog.isShowing()) {
            return true;
        } else {
            return false;
        }
    }
}

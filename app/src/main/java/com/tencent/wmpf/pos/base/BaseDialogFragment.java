package com.tencent.wmpf.pos.base;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.MyDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.tencent.wmpf.pos.R;

/**
 * Created by gaochujia on 2020-09-15.
 */

public abstract class BaseDialogFragment extends MyDialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, getStyle());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View contentView = inflater.inflate(getLayout(), container, false);
        initView(contentView);
        return contentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        afterView();
    }

    protected int getStyle() {
        return R.style.sweet_dialog;
    }

    protected abstract int getLayout();

    protected void initView(View contentView) {
        //init view here
    }

    protected void afterView() {
        //may do something here after init view
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (null != dismissCallBack) {
            dismissCallBack.dismiss();
        }
    }

    private DismissCallBack dismissCallBack;

    public void setDismissCallBack(DismissCallBack dismissCallBack) {
        this.dismissCallBack = dismissCallBack;
    }

    public interface DismissCallBack {
        void dismiss();
    }
}

